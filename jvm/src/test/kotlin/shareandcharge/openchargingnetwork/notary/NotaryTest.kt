package shareandcharge.openchargingnetwork.notary

import com.aayushatharva.brotli4j.Brotli4jLoader
import com.aayushatharva.brotli4j.encoder.Encoder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.web3j.crypto.Credentials
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import java.util.*

/**
 * Tests Notary/OCN-Signature serialization and deserialization
 */
class SerializationTest {

    private val objectMapper = jacksonObjectMapper()

    init {
        Brotli4jLoader.ensureAvailability()
    }

    @Test
    fun `should deserialize base64-encoded OCN-Signature header`() {
        val ocnSignature = mapOf(
                "fields" to listOf("id"),
                "hash" to "0x12345",
                "rsv" to "0x12345",
                "signatory" to "0x12345",
                "rewrites" to listOf(mapOf(
                        "rewrittenFields" to mapOf("$['body']['response_url']" to "http://ocn.node.com/response/5"),
                        "hash" to "0x456",
                        "rsv" to "0x45678",
                        "signatory" to "0x45676")))
        val serialized = objectMapper.writeValueAsBytes(ocnSignature)
        val compressed = Encoder.compress(serialized, Encoder.Parameters().setQuality(4))
        val encoded = Base64.getEncoder().encodeToString(compressed)

        val notary = Notary.deserialize(encoded)
        assertEquals(ocnSignature["fields"], notary.fields)
        assertEquals(ocnSignature["hash"], notary.hash)
        assertEquals(ocnSignature["rsv"], notary.rsv)
        assertEquals(ocnSignature["signatory"], notary.signatory)

        val expectedRewrites = objectMapper.writeValueAsString(ocnSignature["rewrites"])
        val actualRewrites = objectMapper.writeValueAsString(notary.rewrites)
        assertEquals(expectedRewrites, actualRewrites)
    }

    @Test
    fun `should serialize notary object into base64-encoded string`() {
        val ocnSignature = mapOf(
                "fields" to listOf("id"),
                "hash" to "0x12345",
                "rsv" to "0x12345",
                "signatory" to "0x12345",
                "rewrites" to listOf(mapOf(
                        "rewrittenFields" to mapOf("$['body']['response_url']" to "http://ocn.node.com/response/5"),
                        "hash" to "0x456",
                        "rsv" to "0x45678",
                        "signatory" to "0x45676")))
        val serialized = objectMapper.writeValueAsBytes(ocnSignature)
        val compressed = Encoder.compress(serialized, Encoder.Parameters().setQuality(4))
        val expected = Base64.getEncoder().encodeToString(compressed)

        val actual = Notary.deserialize(expected).serialize()
        assertEquals(expected, actual)
    }

}

/**
 * Sign method tests parameterized for table-driven testing
 */
class ParameterizedSigningTest() {
    companion object {
        @JvmStatic
        fun data() = signingTestCases.map { it.get(1) }
    }

    private var privateKey = Keys.createEcKeyPair().privateKey.toString(16)
    private var credentials = Credentials.create(privateKey)

    @ParameterizedTest
    @MethodSource("data")
    fun sign(test: SigningTestCase) {
        val expectedHash = Hash.sha3String(test.values.joinToString(""))
        val expectedRSV = signStringMessage(expectedHash, credentials.ecKeyPair)

        val notary = Notary().sign(test.input, privateKey)

        assertEquals(test.keys, notary.fields)
        assertEquals(expectedHash, notary.hash)
        assertEquals(expectedRSV, notary.rsv)
        assertEquals(credentials.address, notary.signatory)
        assertEquals(listOf<Any?>(), notary.rewrites)
    }

}

/**
 * Individual tests for the verify method
 */
class VerifyingTest {

    private var nodePrivateKey = Keys.createEcKeyPair().privateKey.toString(16)
    private val userPrivateKey = Keys.createEcKeyPair().privateKey.toString(16)
    private val userCredentials = Credentials.create(userPrivateKey)

    @Test
    fun `should verify basic signature`() {
        val notary = Notary().sign(verifyTestCase, userPrivateKey)
        val (isValid, error) = notary.verify(verifyTestCase)
        assertTrue(isValid)
        assertNull(error)
    }

    @Test
    fun `should not verify modified values`() {
        val notary = Notary()
        notary.sign(verifyTestCase, userPrivateKey)
        val modifiedTestCase = verifyTestCase.copy(body = mapOf("id" to "2"))
        val (isValid, error) = notary.verify(modifiedTestCase)
        assertFalse(isValid)
        assertEquals("Request has been modified.", error)
    }

    @Test
    fun `should not verify different signatory`() {
        val notary = Notary().sign(verifyTestCase, Keys.createEcKeyPair().privateKey.toString(16))
        notary.signatory = userCredentials.address
        val (isValid, error) = notary.verify(verifyTestCase)
        assertFalse(isValid)
        assertEquals("Signatories do not match.", error)
    }

    @Test
    fun `should verify single rewrite`() {
        val notary = Notary().sign(verifyTestCase, userPrivateKey)
        val modifiedTestCase = verifyTestCase.copy(headers = mockHeaders.copy(toPartyId = "SNC"))
        notary.stash(mapOf("$['headers']['ocpi-to-party-id']" to verifyTestCase.headers?.toPartyId))
        notary.sign(modifiedTestCase, nodePrivateKey)
        val (isValid, error) = notary.verify(modifiedTestCase)
        assertTrue(isValid)
        assertNull(error)
    }

    @Test
    fun `should verify multiple rewrites`() {
        val notary = Notary().sign(verifyTestCase, userPrivateKey)

        // first rewrite
        val modifiedTestCase = verifyTestCase.copy(headers = mockHeaders.copy(toPartyId = "XXX"))
        notary.stash(mapOf("$['headers']['ocpi-to-party-id']" to verifyTestCase.headers?.toPartyId))
        notary.sign(modifiedTestCase, nodePrivateKey)

        // second rewrite
        val anotherModifiedTestCase = modifiedTestCase.copy(params = mapOf("limit" to "25"))
        notary.stash(mapOf("$['params']['limit']" to null))
        notary.sign(anotherModifiedTestCase, nodePrivateKey)

        val (isValid, error) = notary.verify(anotherModifiedTestCase)
        assertTrue(isValid)
        assertNull(error)
    }

    @Test
    fun `should not verify rewrite if not stashed properly`() {
        val notary = Notary().sign(verifyTestCase, userPrivateKey)

        // modify and use incorrect previous value during stash
        val modifiedTestCase = verifyTestCase.copy(headers = mockHeaders.copy(toPartyId = "XXX"))
        notary.stash(mapOf("$['headers']['ocpi-to-party-id']" to "ZZZ"))
        notary.sign(modifiedTestCase, nodePrivateKey)

        val (isValid, error) = notary.verify(modifiedTestCase)
        assertFalse(isValid)
        assertEquals("Rewrite 0: stashed values do not match original.", error)
    }

    @Test
    fun `should not verify rewrite if signatory incorrect`() {
        val notary = Notary().sign(verifyTestCase, userPrivateKey)
        notary.signatory = nodePrivateKey

        val modifiedTestCase = verifyTestCase.copy(headers = mockHeaders.copy(toPartyId = "XXX"))
        notary.stash(mapOf("$['headers']['ocpi-to-party-id']" to verifyTestCase.headers?.toPartyId))
        notary.sign(modifiedTestCase, nodePrivateKey)

        val (isValid, error) = notary.verify(modifiedTestCase)
        assertFalse(isValid)
        assertEquals("Rewrite 0: signatory of stashed rewrite incorrect.", error)
    }

}