import { assert } from "chai"
import { ethers } from "ethers"
import "mocha"
import Notary from "../src/index"
import { IValuesToSign } from "../src/ocpi-request.interface"

describe("Compatibility Test", () => {

    context("de/serialization", () => {

        const signature = "GxkFAGQzVV9/dmS0iglwgPSve/zcBRJgoyz5JGaswZvOU74mAz3PNlplUXwuzrNAH+GMwjwATrfpIUL8jqUZXaF7t6aNeiKLWW/Mw83OGLbpZef3ZudqGESb74T7OJ0No+z8Nk9hG1PdjkObp9Ohmfjq10OdRMFttzz3a8bqfbJ4n0wok1zv/N7snFXj3/P5xM/36VjPdn5vdmIG/pqBMF7P310oU+i5v2a/3rInrtNZn7n2tdq4LOc6Q0i/ejdNd3k4dj0Zl86R+4SX/vd8K9w1DvP/EFv+TC8W/Zv7f//91BzycmjWxl7lUpkkF0hR1RUUSkFbVARsksm2qo6KTdh8kpxqU6acLIMkUQjVPDXzcmHWxl4JU2sUibDWnCqBBapBIxQUDTk6tda7WEOLIqzFM2ptpTRrwQdACKypSS0ewCuAkjRpkL0iB4dFAkYKMWAllhwapOgbqmsZBFkRqnlqlvH/GfdpvjZrY6/SS8wxPXv9oinTM/X0+ll4Bt6/AqLnr196RnrhXoVsnppZL+ex62LWm1sz6+U89q5nr0c9kcWsb83DzU4pmOmyHlmbw963y3q14u14sJ0nOTidyngy9utRguRdfN+DcVpNdTuu8ABXi56Jzqs6nZ7ymSyrHz+fff/598erHz/effm8Aoe+uZaH6qUOnpMdClc3MBP7gtikqbkv5uMFWvM5EQYo5JhzTdE6JsZiOaETFXHRV3RFiHOKhN43AfTMPkeU71PZVWWXY7LRpaQQChaFhhWjxmKVuNVcuEEMwhFahCRciibvqw0QA6EtxWXICFjFQkbrbCogSSWoI/KlsLgcgy1RayUmlEzNKqXQQG4FKPjSB/u6xcyvhHx2Kb/Q9Cwnr89Rn2HJUF+juf99Dw=="

        const notaryJson = {
            "fields": [
                "$['headers']['x-correlation-id']",
                "$['headers']['ocpi-from-country-code']",
                "$['headers']['ocpi-from-party-id']",
                "$['headers']['ocpi-to-country-code']",
                "$['headers']['ocpi-to-party-id']",
                "$['body']['response_url']",
                "$['body']['token']['country_code']",
                "$['body']['token']['party_id']",
                "$['body']['token']['uid']",
                "$['body']['token']['type']",
                "$['body']['token']['contract_id']",
                "$['body']['token']['issuer']",
                "$['body']['token']['valid']",
                "$['body']['token']['whitelist']",
                "$['body']['token']['last_updated']",
                "$['body']['location_id']",
                "$['body']['evse_uid']"
            ],
            "hash": "0x9bca7d9b186ee3b2d785ef6e212fd970fce37b082f48d98cfea7980a1d8de15c",
            "rsv": "0xda7ff76772cc98c71017c5e61b2de5963e00436c5f6ddaeb4a2ecfbbf001451215ae8fdcb4114e11e7dfdf194e2a532bd52675652c7ad95f1864f2e3f91d2ae21c",
            "signatory": "0x8D2968AFCfea7Ae47FA5A144E177BFD4a27C3E59",
            "rewrites": [{
                "rewrittenFields": { "$['body']['response_url']": "https://api.prod.mobilityserviceprovider.io/ocpi/2.2/sender/commands/START_SESSION/1324f3f9-c4dc-4a80-bac3-aa7a4b22fdfe" },
                "hash": "0xd1ff4987251b73aa9c8603a7a2b0a823dedd364c23bd7a9867244fd124aa4962",
                "rsv": "0xca3cea396806388e15b2be1f2c26e6b0e7afc9baf165da61f618dabbe844c05165720bb3919212cd01920308b1d8ed5e3774bbad39650b6ecc7a72d97f0e785f1c",
                "signatory": "0x11b2D450Ff69aEd749389Ce8A984eB2eA2b91cF2"
            }]
        }

        it("deserialization", async () => {
            const got = await Notary.deserialize(signature)

            assert.deepEqual(got.fields, notaryJson.fields)
            assert.equal(got.hash, notaryJson.hash)
            assert.equal(got.rsv, notaryJson.rsv)
            assert.equal(got.signatory, notaryJson.signatory)
            assert.deepEqual(JSON.parse(JSON.stringify(got.rewrites)), notaryJson.rewrites)
        })

        it("serialization", async () => {
            const notary = await Notary.deserialize(signature)
            const got = await notary.serialize()
            assert.equal(got, signature)
        })

    })

    context("sign/verify", () => {

        const userPrivateKey = "0x7f2797a1a866312ce20f7811fd24ccdb001786b035b516399c848eabfb9d992e"
        const userAddress = "0x11b2D450Ff69aEd749389Ce8A984eB2eA2b91cF2"

        const nodePrivateKey = "0x8f4b958bdeebe2912567acda9a31f8f32468108a29116cf238d2562e8243bae3"
        const nodeAddress = "0x8D2968AFCfea7Ae47FA5A144E177BFD4a27C3E59"

        const request: IValuesToSign = {
            headers: {
                "x-correlation-id": "32387c67-cf96-4c9d-83db-0ed9665b2f29",
                "ocpi-from-country-code": "CH",
                "ocpi-from-party-id": "SNC",
                "ocpi-to-country-code": "DE",
                "ocpi-to-party-id": "XXX"
            },
            body: {
                "response_url": "https://api.prod.mobilityserviceprovider.io/ocpi/2.2/sender/commands/START_SESSION/1324f3f9-c4dc-4a80-bac3-aa7a4b22fdfe",
                "token": {
                    "country_code": "CH",
                    "party_id": "SNC",
                    "uid": "135ff452-1497-49dd-b84f-1e44eb4a497e",
                    "type": "APP_USER",
                    "contract_id": "CH-SNC-7d44ada4f3d9",
                    "issuer": "Share&Charge Foundation",
                    "valid": true,
                    "whitelist": "NEVER",
                    "last_updated": "2020-01-08T12:35:53.380Z"
                },
                "location_id": "ae6d2483",
                "evse_uid": "fe16-429"
            }
        }
        const expectedFields = ["$['headers']['x-correlation-id']", "$['headers']['ocpi-from-country-code']", "$['headers']['ocpi-from-party-id']",
        "$['headers']['ocpi-to-country-code']", "$['headers']['ocpi-to-party-id']", "$['body']['response_url']", "$['body']['token']['country_code']",
        "$['body']['token']['party_id']", "$['body']['token']['uid']", "$['body']['token']['type']", "$['body']['token']['contract_id']",
        "$['body']['token']['issuer']", "$['body']['token']['valid']", "$['body']['token']['whitelist']", "$['body']['token']['last_updated']",
        "$['body']['location_id']", "$['body']['evse_uid']"]

        it("signing", async () => {
            const got = new Notary()
            await got.sign(request, userPrivateKey)
            assert.deepEqual(got.fields, expectedFields)
            assert.equal(got.hash, "0xd1ff4987251b73aa9c8603a7a2b0a823dedd364c23bd7a9867244fd124aa4962")
            assert.equal(got.rsv, "0xca3cea396806388e15b2be1f2c26e6b0e7afc9baf165da61f618dabbe844c05165720bb3919212cd01920308b1d8ed5e3774bbad39650b6ecc7a72d97f0e785f1c")
            assert.equal(ethers.utils.getAddress(got.signatory), ethers.utils.getAddress(userAddress))
            assert.deepEqual(got.rewrites, [])
        })

        it("verifying", async () => {
            const notary = new Notary()
            await notary.sign(request, userPrivateKey)
            const {isValid, error} = notary.verify(request)
            assert.isTrue(isValid)
            assert.isUndefined(error)
        })

        it("verifying rewrite", async () => {
            const notary = new Notary()
            await notary.sign(request, userPrivateKey)

            const modifiedRequest = JSON.parse(JSON.stringify(request))
            modifiedRequest.body.response_url = "https://node.ocn.thirdpartyprovider.net/ocpi/2.2/sender/commands/START_SESSION/1f5c7b28-8314-498b-937c-d43a5b6c79e1"

            const rewrittenFields = { "$['body']['response_url']": request.body.response_url }
            notary.stash(rewrittenFields)
            await notary.sign(modifiedRequest, nodePrivateKey)

            assert.deepEqual(notary.fields, expectedFields)
            assert.equal(notary.hash, "0x9bca7d9b186ee3b2d785ef6e212fd970fce37b082f48d98cfea7980a1d8de15c")
            assert.equal(notary.rsv, "0xda7ff76772cc98c71017c5e61b2de5963e00436c5f6ddaeb4a2ecfbbf001451215ae8fdcb4114e11e7dfdf194e2a532bd52675652c7ad95f1864f2e3f91d2ae21c")
            assert.equal(ethers.utils.getAddress(notary.signatory), ethers.utils.getAddress(nodeAddress))
            assert.equal(notary.rewrites.length, 1)

            assert.deepEqual(notary.rewrites[0].rewrittenFields, rewrittenFields)
            assert.equal(notary.rewrites[0].hash, "0xd1ff4987251b73aa9c8603a7a2b0a823dedd364c23bd7a9867244fd124aa4962")
            assert.equal(notary.rewrites[0].rsv, "0xca3cea396806388e15b2be1f2c26e6b0e7afc9baf165da61f618dabbe844c05165720bb3919212cd01920308b1d8ed5e3774bbad39650b6ecc7a72d97f0e785f1c")
            assert.equal(ethers.utils.getAddress(notary.rewrites[0].signatory), ethers.utils.getAddress(userAddress))

            const {isValid, error} = notary.verify(modifiedRequest)
            assert.isTrue(isValid)
            assert.isUndefined(error)
        })

        it("verifying multiple rewrites", async () => {
            const notary = new Notary()
            await notary.sign(request, userPrivateKey)

            const modifiedRequest = JSON.parse(JSON.stringify(request))
            modifiedRequest.body.response_url = "https://node.ocn.thirdpartyprovider.net/ocpi/2.2/sender/commands/START_SESSION/1f5c7b28-8314-498b-937c-d43a5b6c79e1"

            const rewrittenFields = { "$['body']['response_url']": request.body.response_url }
            notary.stash(rewrittenFields)
            await notary.sign(modifiedRequest, nodePrivateKey)

            const secondModifiedRequest = JSON.parse(JSON.stringify(modifiedRequest))
            secondModifiedRequest.headers["ocpi-from-party-id"] = "ABC"

            const secondRewrittenFields = { "$['headers']['ocpi-from-party-id']": request.headers["ocpi-from-party-id"] }
            notary.stash(secondRewrittenFields)
            await notary.sign(secondModifiedRequest, nodePrivateKey)

            assert.deepEqual(notary.fields, expectedFields)
            assert.equal(notary.hash, "0xce64a518e2723e8832863378783a9e459f3c7b614e8f67b792a15bed43a66fa9")
            assert.equal(notary.rsv, "0xa68163d7b685e4e87db564f5a96a408b312daa90e06b28b6a5d0330cb2afa7362d21c3b1c04d0ea7ba242b0dc771bbe2740745560bcbb41b3bef564e2f0d37b41c")
            assert.equal(ethers.utils.getAddress(notary.signatory), ethers.utils.getAddress(nodeAddress))
            assert.equal(notary.rewrites.length, 2)

            assert.deepEqual(notary.rewrites[0].rewrittenFields, rewrittenFields)
            assert.equal(notary.rewrites[0].hash, "0xd1ff4987251b73aa9c8603a7a2b0a823dedd364c23bd7a9867244fd124aa4962")
            assert.equal(notary.rewrites[0].rsv, "0xca3cea396806388e15b2be1f2c26e6b0e7afc9baf165da61f618dabbe844c05165720bb3919212cd01920308b1d8ed5e3774bbad39650b6ecc7a72d97f0e785f1c")
            assert.equal(ethers.utils.getAddress(notary.rewrites[0].signatory), ethers.utils.getAddress(userAddress))

            assert.deepEqual(notary.rewrites[1].rewrittenFields, secondRewrittenFields)
            assert.equal(notary.rewrites[1].hash, "0x9bca7d9b186ee3b2d785ef6e212fd970fce37b082f48d98cfea7980a1d8de15c")
            assert.equal(notary.rewrites[1].rsv, "0xda7ff76772cc98c71017c5e61b2de5963e00436c5f6ddaeb4a2ecfbbf001451215ae8fdcb4114e11e7dfdf194e2a532bd52675652c7ad95f1864f2e3f91d2ae21c")
            assert.equal(ethers.utils.getAddress(notary.rewrites[1].signatory), ethers.utils.getAddress(nodeAddress))

            const {isValid, error} = notary.verify(secondModifiedRequest)
            assert.isTrue(isValid)
            assert.isUndefined(error)
        })

    })

})
