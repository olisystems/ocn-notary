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
import { ethers, utils } from "ethers"
import jp from "jsonpath"
import * as zlib from "zlib"
import { IValuesToSign } from "./ocpi-request.interface"
import { Rewrite } from "./rewrite"

export interface IVerifyResult {
    isValid: boolean
    error?: string
}

/**
 * The Open Charging Network Notary signs OCPI requests and verifies OCN Signatures.
 */
export default class Notary {

    public static async compress(input: Buffer): Promise<Buffer> {
        return new Promise((resolve, reject) => {
            const opts = { params: {[zlib.constants.BROTLI_PARAM_QUALITY]: 4} }
            zlib.brotliCompress(input, opts, (error, result) => {
                if (error) {
                    reject(error.message)
                }
                resolve(result)
            })
        })
    }

    public static async decompress(input: Buffer): Promise<Buffer> {
        return new Promise((resolve, reject) => {
            const opts = { params: {[zlib.constants.BROTLI_PARAM_QUALITY]: 4} }
            zlib.brotliDecompress(input, opts, (error, result) => {
                if (error) {
                    reject(error.message)
                }
                resolve(result)
            })
        })
    }

    /**
     * Deserialize an OCN-Signature header (base64-encoded JSON-serialized string) into a Notary object which can verify the request
     * @param input the header value of "OCN-Signature"
     * @returns Notary object
     */
    public static async deserialize(input: string): Promise<Notary> {
        const decompressed = await Notary.decompress(Buffer.from(input, "base64"))
        const json = JSON.parse(decompressed.toString("utf-8"))
        const notary = new Notary()
        notary.fields = json.fields
        notary.hash = json.hash
        notary.rsv = json.rsv
        notary.signatory = json.signatory
        if (json.rewrites) {
            notary.rewrites = json.rewrites.map((rewrite: any) => {
                return new Rewrite(rewrite.rewrittenFields, rewrite.hash, rewrite.rsv, rewrite.signatory)
            })
        }
        return notary
    }

    /**
     * The list of fields whose values are included in the signature. The order of the fields is relevant
     * for the verification.
     */
    public fields: string[]

    /**
     * The hash of the values in the fields list (this is the value that gets signed).
     */
    public hash: string

    /**
     * The concatenated parts of a signature R + S + V The result is a 130 hex characters string.
     */
    public rsv: string

    /**
     * The 40 bytes public Ethereum address of the signatory.
     */
    public signatory: string

    /**
     * If the value of one (or more) of the fields had to be overwritten by the OCN node, it must add this value
     * to the list of rewrites in order to allow the recipient of the message to still be able to validate the
     * original message.
     *
     * The list contains the replaced fields and their values in the form of a Rewrite object containing the
     * a hash map of previous values, the previous rsv (concatenated signature) and previous signatory.
     */
    public rewrites: Rewrite[]

    constructor() {
        this.fields = []
        this.hash = ""
        this.rsv = ""
        this.signatory = ""
        this.rewrites = []
    }

    /**
     * Serialize a Brotli compressed OCN Notary JSON object into a base64-encoded string
     */
    public async serialize(): Promise<string> {
        const serialized = JSON.stringify(this)
        const compressed = await Notary.compress(Buffer.from(serialized, "utf-8"))
        return compressed.toString("base64")
    }

    /**
     * Create the signature based on a JSON object
     * @param valuesToSign The list of Key-Value pairs to be signed
     * @param privateKey The Etheruem private key to sign the parameters with
     */
    public async sign(valuesToSign: IValuesToSign, privateKey: string) {

        // instantiate ethers wallet which will provide signing function
        const wallet = new ethers.Wallet(privateKey)

        this.fields = []
        const message = this.walk("$", valuesToSign)
        const messageBytes = ethers.utils.toUtf8Bytes(message)

        this.hash = ethers.utils.keccak256(messageBytes)
        this.rsv = await wallet.signMessage(this.hash)
        this.signatory = wallet.address
    }

    /**
     * Verify that the signature was correctly signed by the right signatory
     * @returns true if signature is valid
     */
    public verify(valuesToVerify: IValuesToSign): IVerifyResult {
        // 1. recreate message/hash using jsonpath fields and valuesToVerify
        let message = ""

        for (const field of this.fields) {
            message += jp.query(valuesToVerify, field.toLowerCase())
        }

        const messageBytes = ethers.utils.toUtf8Bytes(message)
        const expectedHash = ethers.utils.keccak256(messageBytes)

        if (this.hash !== expectedHash) {
            return {isValid: false, error: "Request has been modified"}
        }

        // 2. verify message
        const actualSigner = ethers.utils.getAddress(utils.verifyMessage(this.hash, this.rsv))

        if (actualSigner !== ethers.utils.getAddress(this.signatory)) {
            return {isValid: false, error: "Signatories do not match"}
        }

        // 3. verify rewritten requests
        let modifiedRequest: IValuesToSign = valuesToVerify
        for (const [index, rewrite] of this.rewrites.reverse().entries()) {
            const {isValid, previousRequest, error} = rewrite.verify(this.fields, modifiedRequest)
            if (!isValid) {
                return {isValid, error: `Rewrite ${index}: ${error}`}
            }
            if (!previousRequest) {
                throw Error("Bad state: previous request not returned from rewrite verification")
            }
            modifiedRequest = previousRequest
        }

        return {isValid: true}
    }

    /**
     * Add a rewrite to the Notary object.
     * @param modifiedValues Map<string, any> where the key is the json path of overwritten value and the value is the
     * original which will be replaced, e.g. { "$['body']['id']: "LOC1" }
     */
    public stash(modifiedValues: object): void {
        const rewrite = new Rewrite(modifiedValues, this.hash, this.rsv, this.signatory)
        this.rewrites.push(rewrite)
    }

    /**
     * walk through values in a JSON object, appending json path to fields and its value
     * to the message to be signed
     * @param jsonPath the json path which should be walked through
     * @param value the value corresponding to the json path provided
     * @param message the resultant string of values which should be signed
     * @returns the string message which should be hashed and signed, containing values as ordered
     * by the fields array
     */
    private walk(jsonPath: string, value: any, message: string = ""): string {
        if (value !== undefined && value !== null && value !== "") {
            if (Array.isArray(value)) {
                for (const [index, subvalue] of value.entries()) {
                    message = this.walk(`${jsonPath}[${index}]`, subvalue, message)
                }
            } else if (typeof value === "object") {
                for (const [key, subvalue] of Object.entries(value)) {
                    message = this.walk(`${jsonPath}['${key}']`, subvalue, message)
                }
            } else {
                this.fields.push(jsonPath)
                message += value
            }
        }
        return message
    }

}
