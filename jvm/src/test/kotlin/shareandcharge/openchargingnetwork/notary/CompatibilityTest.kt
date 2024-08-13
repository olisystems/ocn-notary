package shareandcharge.openchargingnetwork.notary

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.web3j.crypto.Credentials

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests interoperability between JVM and JS versions of the Notary
 */
class CompatibilityTest {

    private val objectMapper = jacksonObjectMapper()

    
    /**
     * De/serialization
     */

    private val signature = "GxkFAGQzVV9/dmS0iglwgPSve/zcBRJgoyz5JGaswZvOU74mAz3PNlplUXwuzrNAH+GMwjwATrfpIUL8jqUZXaF7t6aNeiKLWW/Mw83OGLbpZef3ZudqGESb74T7OJ0No+z8Nk9hG1PdjkObp9Ohmfjq10" +
            "OdRMFttzz3a8bqfbJ4n0wok1zv/N7snFXj3/P5xM/36VjPdn5vdmIG/pqBMF7P310oU+i5v2a/3rInrtNZn7n2tdq4LOc6Q0i/ejdNd3k4dj0Zl86R+4SX/vd8K9w1DvP/EFv+TC8W/Zv7f//91BzycmjWxl7lUpkkF0hR1RUU" +
            "SkFbVARsksm2qo6KTdh8kpxqU6acLIMkUQjVPDXzcmHWxl4JU2sUibDWnCqBBapBIxQUDTk6tda7WEOLIqzFM2ptpTRrwQdACKypSS0ewCuAkjRpkL0iB4dFAkYKMWAllhwapOgbqmsZBFkRqnlqlvH/GfdpvjZrY6/SS8wxPX" +
            "v9oinTM/X0+ll4Bt6/AqLnr196RnrhXoVsnppZL+ex62LWm1sz6+U89q5nr0c9kcWsb83DzU4pmOmyHlmbw963y3q14u14sJ0nOTidyngy9utRguRdfN+DcVpNdTuu8ABXi56Jzqs6nZ7ymSyrHz+fff/598erHz/effm8Aoe+" +
            "uZaH6qUOnpMdClc3MBP7gtikqbkv5uMFWvM5EQYo5JhzTdE6JsZiOaETFXHRV3RFiHOKhN43AfTMPkeU71PZVWWXY7LRpaQQChaFhhWjxmKVuNVcuEEMwhFahCRciibvqw0QA6EtxWXICFjFQkbrbCogSSWoI/KlsLgcgy1Ray" +
            "UmlEzNKqXQQG4FKPjSB/u6xcyvhHx2Kb/Q9Cwnr89Rn2HJUF+juf99Dw=="

    private val notaryMap = mapOf<String, Any>(
            "fields" to listOf(
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
                "$['body']['evse_uid']"),
            "hash" to "0x9bca7d9b186ee3b2d785ef6e212fd970fce37b082f48d98cfea7980a1d8de15c",
            "rsv" to "0xda7ff76772cc98c71017c5e61b2de5963e00436c5f6ddaeb4a2ecfbbf001451215ae8fdcb4114e11e7dfdf194e2a532bd52675652c7ad95f1864f2e3f91d2ae21c",
            "signatory" to "0x8D2968AFCfea7Ae47FA5A144E177BFD4a27C3E59",
            "rewrites" to listOf(mapOf(
                "rewrittenFields" to mapOf("$['body']['response_url']" to "https://api.prod.mobilityserviceprovider.io/ocpi/2.2/sender/commands/START_SESSION/1324f3f9-c4dc-4a80-bac3-aa7a4b22fdfe"),
                "hash" to "0xd1ff4987251b73aa9c8603a7a2b0a823dedd364c23bd7a9867244fd124aa4962",
                "rsv" to "0xca3cea396806388e15b2be1f2c26e6b0e7afc9baf165da61f618dabbe844c05165720bb3919212cd01920308b1d8ed5e3774bbad39650b6ecc7a72d97f0e785f1c",
                "signatory" to "0x11b2D450Ff69aEd749389Ce8A984eB2eA2b91cF2")))

    @Test
    fun deserialization() {
        val got = Notary.deserialize(signature)
        assertEquals(notaryMap["fields"], got.fields)
        assertEquals(notaryMap["hash"], got.hash)
        assertEquals(notaryMap["rsv"], got.rsv)
        assertEquals(notaryMap["signatory"], got.signatory)

        val expectedRewrites = objectMapper.writeValueAsString(notaryMap["rewrites"])
        val actualRewrites = objectMapper.writeValueAsString(got.rewrites)
        assertEquals(expectedRewrites, actualRewrites)
    }

    @Test
    fun serialization() {
        val notary = Notary.deserialize(signature)
        val got = notary.serialize()
        assertEquals(signature, got)
    }


    /**
     * Sign and verify
     */

    private val userPrivateKey = "0x7f2797a1a866312ce20f7811fd24ccdb001786b035b516399c848eabfb9d992e"
    private val userAddress = "0x11b2D450Ff69aEd749389Ce8A984eB2eA2b91cF2"

    private val nodePrivateKey = "0x8f4b958bdeebe2912567acda9a31f8f32468108a29116cf238d2562e8243bae3"
    private val nodeAddress = "0x8D2968AFCfea7Ae47FA5A144E177BFD4a27C3E59"

    private val body = mapOf<String, Any>(
            "response_url" to "https://api.prod.mobilityserviceprovider.io/ocpi/2.2/sender/commands/START_SESSION/1324f3f9-c4dc-4a80-bac3-aa7a4b22fdfe",
            "token" to mapOf<String, Any>(
                    "country_code" to "CH",
                    "party_id" to "SNC",
                    "uid" to "135ff452-1497-49dd-b84f-1e44eb4a497e",
                    "type" to "APP_USER",
                    "contract_id" to "CH-SNC-7d44ada4f3d9",
                    "issuer" to "Share&Charge Foundation",
                    "valid" to true,
                    "whitelist" to "NEVER",
                    "last_updated" to "2020-01-08T12:35:53.380Z"),
            "location_id" to "ae6d2483",
            "evse_uid" to "fe16-429")

    private val request = ValuesToSign(
            headers = SignableHeaders(
                    correlationId = "32387c67-cf96-4c9d-83db-0ed9665b2f29",
                    fromCountryCode = "CH",
                    fromPartyId = "SNC",
                    toCountryCode = "DE",
                    toPartyId = "XXX"),
            body = body)

    private val expectedFields = listOf("$['headers']['x-correlation-id']", "$['headers']['ocpi-from-country-code']", "$['headers']['ocpi-from-party-id']",
            "$['headers']['ocpi-to-country-code']", "$['headers']['ocpi-to-party-id']", "$['body']['response_url']", "$['body']['token']['country_code']",
            "$['body']['token']['party_id']", "$['body']['token']['uid']", "$['body']['token']['type']", "$['body']['token']['contract_id']",
            "$['body']['token']['issuer']", "$['body']['token']['valid']", "$['body']['token']['whitelist']", "$['body']['token']['last_updated']",
            "$['body']['location_id']", "$['body']['evse_uid']")

    @Test
    fun signing() {
        val got = Notary().sign(request, userPrivateKey)
        assertEquals(expectedFields, got.fields)
        assertEquals("0xd1ff4987251b73aa9c8603a7a2b0a823dedd364c23bd7a9867244fd124aa4962", got.hash)
        assertEquals("0xca3cea396806388e15b2be1f2c26e6b0e7afc9baf165da61f618dabbe844c05165720bb3919212cd01920308b1d8ed5e3774bbad39650b6ecc7a72d97f0e785f1c", got.rsv)
        assertEquals(userAddress.toChecksumAddress(), got.signatory.toChecksumAddress())
        assertEquals(listOf<Rewrite>(), got.rewrites)
    }

    @Test
    fun verifying() {
        val notary = Notary().sign(request, userPrivateKey)
        val (isValid, error) = notary.verify(request)
        assertTrue(isValid)
        assertNull(error)
    }

    @Test
    fun verifying_with_rewrite() {
        val notary = Notary().sign(request, userPrivateKey)

        val modifiedBody = body.toMutableMap()
        modifiedBody["response_url"] = "https://node.ocn.thirdpartyprovider.net/ocpi/2.2/sender/commands/START_SESSION/1f5c7b28-8314-498b-937c-d43a5b6c79e1"
        val modifiedRequest = request.copy(body = modifiedBody)

        val rewrittenFields = mapOf("$['body']['response_url']" to body["response_url"])
        notary.stash(rewrittenFields)
        notary.sign(modifiedRequest, nodePrivateKey)

        assertEquals(expectedFields, notary.fields)
        assertEquals("0x9bca7d9b186ee3b2d785ef6e212fd970fce37b082f48d98cfea7980a1d8de15c", notary.hash)
        assertEquals("0xda7ff76772cc98c71017c5e61b2de5963e00436c5f6ddaeb4a2ecfbbf001451215ae8fdcb4114e11e7dfdf194e2a532bd52675652c7ad95f1864f2e3f91d2ae21c", notary.rsv)
        assertEquals(nodeAddress.toChecksumAddress(), notary.signatory.toChecksumAddress())
        assertEquals(1, notary.rewrites.count())

        assertEquals(rewrittenFields, notary.rewrites[0].rewrittenFields)
        assertEquals("0xd1ff4987251b73aa9c8603a7a2b0a823dedd364c23bd7a9867244fd124aa4962", notary.rewrites[0].hash)
        assertEquals("0xca3cea396806388e15b2be1f2c26e6b0e7afc9baf165da61f618dabbe844c05165720bb3919212cd01920308b1d8ed5e3774bbad39650b6ecc7a72d97f0e785f1c", notary.rewrites[0].rsv)
        assertEquals(userAddress.toChecksumAddress(), notary.rewrites[0].signatory.toChecksumAddress())

        val (isValid, error) = notary.verify(modifiedRequest)
        assertTrue(isValid)
        assertNull(error)
    }

    @Test
    fun verifying_with_multiple_rewrites() {
        val notary = Notary().sign(request, userPrivateKey)

        val modifiedBody = body.toMutableMap()
        modifiedBody["response_url"] = "https://node.ocn.thirdpartyprovider.net/ocpi/2.2/sender/commands/START_SESSION/1f5c7b28-8314-498b-937c-d43a5b6c79e1"
        val modifiedRequest = request.copy(body = modifiedBody)

        val rewrittenFields = mapOf("$['body']['response_url']" to body["response_url"])
        notary.stash(rewrittenFields)
        notary.sign(modifiedRequest, nodePrivateKey)

        val secondModifiedRequest = modifiedRequest.copy(headers = request.headers?.copy(fromPartyId = "ABC"))
        val secondRewrittenFields = mapOf("$['headers']['ocpi-from-party-id']" to modifiedRequest.headers?.fromPartyId)
        notary.stash(secondRewrittenFields)
        notary.sign(secondModifiedRequest, nodePrivateKey)

        assertEquals(expectedFields, notary.fields)
        assertEquals("0xce64a518e2723e8832863378783a9e459f3c7b614e8f67b792a15bed43a66fa9", notary.hash)
        assertEquals("0xa68163d7b685e4e87db564f5a96a408b312daa90e06b28b6a5d0330cb2afa7362d21c3b1c04d0ea7ba242b0dc771bbe2740745560bcbb41b3bef564e2f0d37b41c", notary.rsv)
        assertEquals(nodeAddress.toChecksumAddress(), notary.signatory.toChecksumAddress())
        assertEquals(2, notary.rewrites.count())

        // first rewrite
        assertEquals(rewrittenFields, notary.rewrites[0].rewrittenFields)
        assertEquals("0xd1ff4987251b73aa9c8603a7a2b0a823dedd364c23bd7a9867244fd124aa4962", notary.rewrites[0].hash)
        assertEquals("0xca3cea396806388e15b2be1f2c26e6b0e7afc9baf165da61f618dabbe844c05165720bb3919212cd01920308b1d8ed5e3774bbad39650b6ecc7a72d97f0e785f1c", notary.rewrites[0].rsv)
        assertEquals(userAddress.toChecksumAddress(), notary.rewrites[0].signatory.toChecksumAddress())

        // second rewrite
        assertEquals(secondRewrittenFields, notary.rewrites[1].rewrittenFields)
        assertEquals("0x9bca7d9b186ee3b2d785ef6e212fd970fce37b082f48d98cfea7980a1d8de15c", notary.rewrites[1].hash)
        assertEquals("0xda7ff76772cc98c71017c5e61b2de5963e00436c5f6ddaeb4a2ecfbbf001451215ae8fdcb4114e11e7dfdf194e2a532bd52675652c7ad95f1864f2e3f91d2ae21c", notary.rewrites[1].rsv)
        assertEquals(nodeAddress.toChecksumAddress(), notary.rewrites[1].signatory.toChecksumAddress())

        val (isValid, error) = notary.verify(secondModifiedRequest)
        assertTrue(isValid)
        assertNull(error)
    }

}