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
import { ethers } from "ethers"
import jp from "jsonpath"
import { IVerifyResult } from "./index"
import { IValuesToSign } from "./ocpi-request.interface";

/**
 * Result of the rewrite verify method, containing isValid, error and previous request
 */
interface IRewriteVerifyResult extends IVerifyResult {
    previousRequest?: IValuesToSign
}

/**
 * A Rewrite object contains information about a previous Notary object.
 */
export class Rewrite {

    /**
     * Create a new Rewrite object
     * @param rewrittenFields a hash map<string, any> (where the key is a json path) of original values which have
     * since been modified
     * @param hash the original hash of values which should be signed
     * @param rsv the original concatenated signature of the hash by signatory's private key
     * @param signatory the original signatory
     */
    constructor(
        public readonly rewrittenFields: object,
        public readonly hash: string,
        public readonly rsv: string,
        public readonly signatory: string) {}

    /**
     * Verify a rewrite given a modified request
     * @param fields array of json paths dictating the order of values signed
     * @param modifiedRequest the newly modified request
     * @returns true if valid rewrite signature
     */
    public verify(fields: string[], modifiedRequest: IValuesToSign): IRewriteVerifyResult {
        // 1. re-build the original request from rewritten json paths
        const originalRequest = JSON.parse(JSON.stringify(modifiedRequest))
        for (const [key, value] of Object.entries(this.rewrittenFields)) {
            jp.value(originalRequest, key, value)
        }

        let message = ""

        for (const field of fields) {
            const value = jp.query(originalRequest, field.toLowerCase())
            if (value) {
                message += value
            }
        }

        // 2. assert hash matches the stashed hash
        const messageBytes = ethers.utils.toUtf8Bytes(message)
        const expectedHash = ethers.utils.keccak256(messageBytes)

        if (this.hash !== expectedHash) {
            return {isValid: false, error: "Rewritten request hash does not match"}
        }

        // 3. assert signatory matches the stashed signatory
        const expectedSignatory = ethers.utils.verifyMessage(this.hash, this.rsv)

        const validSignatory = this.signatory === expectedSignatory

        if (validSignatory) {
            return {isValid: true, previousRequest: originalRequest}
        } else {
            return {isValid: false, error: "Rewritten signatory incorrect"}
        }
    }

}
