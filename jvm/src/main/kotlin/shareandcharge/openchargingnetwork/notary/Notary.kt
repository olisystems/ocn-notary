/*
    Copyright 2019-2020 eMobilify GmbH

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package shareandcharge.openchargingnetwork.notary

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import org.web3j.crypto.Credentials
import org.web3j.crypto.Hash
import java.util.*
import com.aayushatharva.brotli4j.Brotli4jLoader
import com.aayushatharva.brotli4j.decoder.Decoder
import com.aayushatharva.brotli4j.encoder.Encoder

/**
 * The Open Charging Network Notary signs OCPI requests and verifies OCN Signatures.
 */
class Notary {

    /**
     * The list of fields whose values are included in the signature. The order of the fields is relevant
     * for the verification. Follows JsonPath notation.
     */
    var fields: MutableList<String> = mutableListOf()

    /**
     * The hash of the values in the fields list (this is the value that gets signed).
     */
    var hash: String = ""

    /**
     * The concatenated parts of a signature R + S + V The result is a 130 hex characters string.
     */
    var rsv: String = ""

    /**
     * The 40 bytes public Ethereum address of the signatory.
     */
    var signatory: String = ""

    /**
     * If the value of one (or more) of the fields had to be overwritten by the OCN node, it must add this value
     * to the list of rewrites in order to allow the recipient of the message to still be able to validate the
     * original message.
     *
     * The list contains the replaced fields and their values in the form of a Rewrite object containing the
     * a hash map of previous values, the previous rsv (concatenated signature) and previous signatory.
     */
    var rewrites: MutableList<Rewrite> = mutableListOf()

    companion object {
        private val objectMapper = jacksonObjectMapper()
        private val jsonPath = JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))

        init {
            Brotli4jLoader.ensureAvailability()
        }

        /**
         * Compress signature using Brotli
         */
        @JvmStatic
        fun compress(input: ByteArray): ByteArray {
            val opts = Encoder.Parameters().setQuality(4)
            return Encoder.compress(input, opts)
        }

        /**
         * Decompress signature using Brotli
         */
        @JvmStatic
        fun decompress(input: ByteArray): ByteArray {
            return Decoder.decompress(input).decompressedData
        }

        /**
         * Deserialize an OCN-Signature header (base64-encoded JSON-serialized string) into a Notary object which can verify the request.
         * @param ocnSignature the header value of "OCN-Signature"
         * @returns Notary object
         */
        @JvmStatic
        fun deserialize(ocnSignature: String): Notary {
            val decoded = Base64.getDecoder().decode(ocnSignature)
            val decompressed = decompress(decoded)
            return jacksonObjectMapper().readValue(decompressed)
        }
    }

    /**
     * Serialize an OCN Notary object into a base64-encoded JSON string
     */
    fun serialize(): String {
        val serialized = objectMapper.writeValueAsBytes(this)
        val compressed = compress(serialized)
        return Base64.getEncoder().encodeToString(compressed)
    }

    /**
     * Create the signature based on an ValuesToSign object containing headers, url-encoded parameters and a generic body.
     * @param valuesToSign The ValuesToSign object whose values should be signed
     * @param privateKey The Etheruem private key to sign the values with
     */
    fun sign(valuesToSign: ValuesToSign<*>, privateKey: String): Notary {
        val credentials = Credentials.create(privateKey)
        fields = mutableListOf()
        val message = walk("$", valuesToSign)
        hash = Hash.sha3String(message)
        rsv = signStringMessage(hash, credentials.ecKeyPair)
        signatory = credentials.address
        return this
    }

    /**
     * Verify that the signature was correctly signed by the provided signatory.
     * @returns VerifyResult object containing isValid (Boolean) and optional error message (String)
     */
    fun verify(valuesToVerify: ValuesToSign<*>): VerifyResult {
        val valuesAsJsonString = objectMapper.writeValueAsString(valuesToVerify)
        val parser = jsonPath.parse(valuesAsJsonString)

        // 1. recreate and verify message/hash using JsonPath fields and valuesToVerify
        var message = ""
        fields.forEach { message += parser.read(it.lowercase()) }

        if (hash != Hash.sha3String(message)) {
            return VerifyResult(false, "Request has been modified.")
        }

        // 2. verify signer of message
        if (signatory.toChecksumAddress() != signerOfMessage(hash, rsv).toChecksumAddress()) {
            return VerifyResult(false, "Signatories do not match.")
        }

        // 3. verify rewrites
        var nextValues = valuesToVerify.copy()
        for ((index, rewrite) in rewrites.reversed().withIndex()) {

            val (isValid, error, previousValues) = rewrite.verify(fields, nextValues, jsonPath, objectMapper)

            if (!isValid) {
                return VerifyResult(isValid, "Rewrite $index: $error")
            }
            if (previousValues == null) {
                throw IllegalStateException("Rewrite $index: Previous values missing in rewrite verification")
            }

            nextValues = previousValues.copy()
        }

        return VerifyResult(true)
    }

    /**
     * Add a rewrite to the Notary object.
     * @param rewrittenFields Key-Value pairs of JsonPath to value that has since been overwritten
     * e.g. `{ "$['body']['id']: "LOC1" }`
     */
    fun stash(rewrittenFields: Map<String, Any?>): Notary {
        val rewrite = Rewrite(rewrittenFields, hash, rsv, signatory)
        rewrites.add(rewrite)
        return this
    }

    /**
     * "Walk" through an object, appending JsonPaths containing a basic type (i.e. not an array or map) to the Notary's
     * fields property and returning a string message of values that should be signed.
     *
     * @param jsonPath the path of the current root node
     * @param value the value that the current jsonPath points to
     * @param message the current message string of values
     * @return the updated message with the value appending to it
     */
    private fun walk(jsonPath: String, value: Any?, message: String = ""): String {
        var mutableMsg = message

        fun walkThroughListLike(value: List<*>) {
            for ((index, subValue) in value.withIndex()) {
                mutableMsg = walk("$jsonPath[$index]", subValue, mutableMsg)
            }
        }

        if (value != null && value != "") {
            when (value) {
                is Array<*> -> walkThroughListLike(value.toList())
                is Set<*> -> walkThroughListLike(value.toList())
                is List<*> -> walkThroughListLike(value)
                is Map<*, *> -> {
                    for ((key, subValue) in value.entries) {
                        mutableMsg = walk("$jsonPath['$key']", subValue, mutableMsg)
                    }
                }
                is String, is Boolean, is Int, is Byte, is Short, is Long, is Float, is Double, is Char -> {
                    fields.add(jsonPath)
                    mutableMsg += value
                }
                else -> {
                    // convert custom types to map
                    val valueAsJsonString = objectMapper.writeValueAsString(value)
                    val valueAsMap: Map<String, Any?> = objectMapper.readValue(valueAsJsonString)
                    mutableMsg = walk(jsonPath, valueAsMap, mutableMsg)
                }
            }
        }
        return mutableMsg
    }

}
