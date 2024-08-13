import { assert } from "chai"
import { ethers } from "ethers"
import "mocha"
import Notary from "../src/index"
import { mockHeaders, signTestCases, verifyTestCase } from "./test.cases"

describe("OCN Notary model", () => {

    let wallet: ethers.Wallet

    beforeEach(() => {
        wallet = ethers.Wallet.createRandom()
    })

    it("should deserialize OCN-Signature header", async () => {
        const ocnSignature = {
            fields: ["id"],
            hash: "0x12345",
            rsv: "0x12345",
            signatory: "0x12345",
            rewrites: [{
                rewrittenFields: { "$['body']['id']": "LOC1" },
                hash: "0x456",
                rsv: "0x45678",
                signatory: "0x45676"
            }]
        }
        const compressedSignature = await Notary.compress(Buffer.from(JSON.stringify(ocnSignature), "utf-8"))
        const encodedSignature = compressedSignature.toString("base64")
        const notary = await Notary.deserialize(encodedSignature)
        assert.deepEqual(notary.fields, ocnSignature.fields)
        assert.equal(notary.rsv, ocnSignature.rsv)
        assert.equal(notary.hash, ocnSignature.hash)
        assert.deepEqual(JSON.parse(JSON.stringify(notary.rewrites)), ocnSignature.rewrites)
        assert.equal(notary.signatory, ocnSignature.signatory)
    })

    it("should serialize notary object into json string", async () => {
        const ocnSignature = {
            fields: ["id"],
            hash: "0x12345",
            rsv: "0x12345",
            signatory: "0x12345",
            rewrites: [{
                rewrittenFields: { "$['body']['response_url']": "http://ocn.node.com/response/5" },
                hash: "0x456",
                rsv: "0x45678",
                signatory: "0x45676"
            }]
        }
        const compressedSignature = await Notary.compress(Buffer.from(JSON.stringify(ocnSignature), "utf-8"))
        const expected = compressedSignature.toString("base64")

        const notary = await Notary.deserialize(expected)
        const actual = await notary.serialize()

        assert.equal(actual, expected)
    })

    context("sign()", () => {

        for (const testCase of signTestCases) {
            it(testCase.name, async () => {
                const notary = new Notary()

                // add header keys and prepend with `$.header.` or `$.body.` to create full json path
                const keys = [
                    ...Object.keys(mockHeaders).map((k: string) => `$['headers']['${k}']`),
                    ...testCase.keys.map((k: string) => `$['body']${k}`)
                ]

                // add header values
                const values = [
                    ...Object.values(mockHeaders),
                    ...testCase.values
                ]

                // equivalent to using utils.keccak256(utf8Bytes)
                const expectedHash = ethers.utils.id(values.join(""))
                const expectedRSV = await wallet.signMessage(expectedHash)

                await notary.sign(testCase.input, wallet.privateKey)

                assert.deepEqual(notary.fields, keys)
                assert.equal(notary.rsv, expectedRSV)
                assert.equal(notary.hash, expectedHash)
                assert.deepEqual(notary.rewrites, [])
                assert.equal(notary.signatory, wallet.address)
            })
        }

    })

    context("verify()", () => {

        const nodeWallet = ethers.Wallet.createRandom()

        // const stubRegistry = new class implements IRegistry {
        //     public async getNodeAddress(countryCode: string, partyID: string): Promise<string> {
        //         if (countryCode && partyID) {
        //             return nodeWallet.address
        //         }
        //         throw Error("country code or party id is empty")
        //     }
        //     public async getPartyAddress(countryCode: string, partyID: string): Promise<string> {
        //         if (countryCode && partyID) {
        //             return wallet.address
        //         }
        //         throw Error("country code or party id is empty")
        //     }
        // }()

        it("success of basic signature (no rewrites)", async () => {
            const notary = new Notary()
            await notary.sign(verifyTestCase.input, wallet.privateKey)
            const {isValid, error} = notary.verify(verifyTestCase.input)
            assert.isTrue(isValid)
            assert.isUndefined(error)
        })

        it("failure if values are missing", async () => {
            const notary = new Notary()
            await notary.sign(verifyTestCase.input, wallet.privateKey)
            const {isValid, error} = notary.verify({ headers: mockHeaders, body: {id: "1"} })
            assert.isFalse(isValid)
            assert.equal(error, "Request has been modified")
        })

        it("failure if values are changed", async () => {
            const notary = new Notary()
            await notary.sign(verifyTestCase.input, wallet.privateKey)
            const modifiedInput = JSON.parse(JSON.stringify(verifyTestCase.input))
            modifiedInput.body.id = "2"
            const {isValid, error} = notary.verify(modifiedInput)
            assert.isFalse(isValid)
            assert.equal(error, "Request has been modified")
        })

        it("failure if signatory is different", async () => {
            const notary = new Notary()
            await notary.sign(verifyTestCase.input, ethers.Wallet.createRandom().privateKey)
            notary.signatory = nodeWallet.address
            const {isValid, error} = notary.verify(verifyTestCase.input)
            assert.isFalse(isValid)
            assert.equal(error, "Signatories do not match")
        })

        it("success of single rewrite", async () => {
            const notary = new Notary()
            await notary.sign(verifyTestCase.input, wallet.privateKey)
            const modifiedInput = JSON.parse(JSON.stringify(verifyTestCase.input))
            modifiedInput.body.id = "2"
            notary.stash({ "$['body']['id']": verifyTestCase.input.body.id })
            await notary.sign(modifiedInput, nodeWallet.privateKey)
            const {isValid, error} = notary.verify(modifiedInput)
            assert.isTrue(isValid)
            assert.isUndefined(error)
        })

        it("success of multiple rewrites", async () => {
            const notary = new Notary()

            // sign original request
            await notary.sign(verifyTestCase.input, wallet.privateKey)

            // modify request (deep copy!) and stash the previous one
            const modifiedInput = JSON.parse(JSON.stringify((verifyTestCase.input)))
            modifiedInput.body.id = "2"
            notary.stash({ "$['body']['id']": verifyTestCase.input.body.id })
            await notary.sign(modifiedInput, nodeWallet.privateKey)

            // modify request again (deep copy!) and stash the previous one
            const anotherModifiedInput = JSON.parse(JSON.stringify(modifiedInput))
            anotherModifiedInput.body.id = "3"
            notary.stash({ "$['body']['id']": modifiedInput.body.id })
            await notary.sign(anotherModifiedInput, ethers.Wallet.createRandom().privateKey)

            // verify the chain of requests
            const {isValid, error} = notary.verify(anotherModifiedInput)
            assert.isTrue(isValid)
            assert.isUndefined(error)
        })

        it("failure if rewrite not stashed properly", async () => {
            const notary = new Notary()

            // sign original request
            await notary.sign(verifyTestCase.input, wallet.privateKey)

            // modify request (deep copy!) and stash the previous one
            const modifiedInput = JSON.parse(JSON.stringify((verifyTestCase.input)))
            modifiedInput.body.id = "2"
            notary.stash({ "$['body']['id']": modifiedInput.body.id })
            await notary.sign(modifiedInput, nodeWallet.privateKey)

            // verify the chain of requests
            const {isValid, error} = notary.verify(modifiedInput)
            assert.isFalse(isValid)
            assert.equal(error, "Rewrite 0: Rewritten request hash does not match")
        })

        it("failure if rewrite signatory is different", async () => {
            const notary = new Notary()

            // sign original request
            await notary.sign(verifyTestCase.input, wallet.privateKey)

            // modify request (deep copy!) and stash the previous one
            const modifiedInput = JSON.parse(JSON.stringify((verifyTestCase.input)))
            modifiedInput.body.id = "2"
            notary.signatory = nodeWallet.address
            notary.stash({ "$['body']['id']": verifyTestCase.input.body.id })
            await notary.sign(modifiedInput, nodeWallet.privateKey)

            // verify the chain of requests
            const {isValid, error} = notary.verify(modifiedInput)
            assert.isFalse(isValid)
            assert.equal(error, "Rewrite 0: Rewritten signatory incorrect")
        })

    })

    it("should stash rewrite values", () => {
        const notary = new Notary()
        notary.hash = "0x123"
        notary.rsv = "0x456"
        notary.signatory = "0x00001"
        notary.stash({"$['body']['id']": "2"})
        assert.equal(notary.rewrites.length, 1)
        assert.deepEqual(notary.rewrites[0].rewrittenFields, {"$['body']['id']": "2"})
        assert.equal(notary.rewrites[0].hash, notary.hash)
        assert.equal(notary.rewrites[0].rsv, notary.rsv)
        assert.equal(notary.rewrites[0].signatory, notary.signatory)
    })

})
