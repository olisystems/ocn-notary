package shareandcharge.openchargingnetwork.notary

import com.fasterxml.jackson.annotation.JsonProperty

data class SigningTestCase(val input: ValuesToSign<*>, val keys: List<String>, val values: List<String>)

data class ExampleDataClass(@JsonProperty("is_valid") val isValid: Boolean, val message: String)

val mockHeaders = SignableHeaders("456", "DE", "ABC", "DE", "XYZ")

val headersJsonPaths = listOf(
        "x-correlation-id",
        "ocpi-from-country-code",
        "ocpi-from-party-id",
        "ocpi-to-country-code",
        "ocpi-to-party-id").map { "$['headers']['$it']" }

val headersValues = listOf("456", "DE", "ABC", "DE", "XYZ")

val signingTestCases = listOf(
        arrayOf("string values", SigningTestCase(
                input = ValuesToSign(mockHeaders, body = mapOf("id" to "1", "city" to "Essen")),
                keys = headersJsonPaths + listOf("['id']", "['city']").map { "$['body']$it" },
                values = headersValues + listOf("1", "Essen"))),
        arrayOf("mixedValues", SigningTestCase(
                input = ValuesToSign(mockHeaders, body = mapOf("id" to "1", "city" to "Essen", "valid" to true, "count" to 3)),
                keys = headersJsonPaths + listOf("['id']", "['city']", "['valid']", "['count']").map { "$['body']$it" },
                values = headersValues + listOf("1", "Essen", "true", "3"))),
        arrayOf("array values", SigningTestCase(
                input = ValuesToSign(mockHeaders, body = mapOf(
                        "id" to "1",
                        "city" to "Essen",
                        "types" to listOf("WHITELIST", "ALWAYS"))),
                keys = headersJsonPaths + listOf("['id']", "['city']", "['types'][0]", "['types'][1]").map { "$['body']$it" },
                values = headersValues + listOf("1", "Essen", "WHITELIST", "ALWAYS"))),
        arrayOf("objects within arrays", SigningTestCase(
                input = ValuesToSign(mockHeaders, body = mapOf(
                        "id" to "1",
                        "city" to "Essen",
                        "evses" to setOf(mapOf(
                                "id" to "1234",
                                "status" to "BLOCKED")))),
                keys = headersJsonPaths + listOf("['id']", "['city']", "['evses'][0]['id']", "['evses'][0]['status']").map { "$['body']$it" },
                values = headersValues + listOf("1", "Essen", "1234", "BLOCKED"))),
        arrayOf("nested objects within arrays", SigningTestCase(
                input = ValuesToSign(mockHeaders, body = mapOf(
                        "id" to "1",
                        "city" to "Essen",
                        "evses" to arrayOf(mapOf(
                                "id" to "1234",
                                "status" to "BLOCKED",
                                "connectors" to arrayOf(mapOf("id" to "1")))))),
                keys = headersJsonPaths + listOf("['id']", "['city']", "['evses'][0]['id']", "['evses'][0]['status']", "['evses'][0]['connectors'][0]['id']").map { "$['body']$it" },
                values = headersValues + listOf("1", "Essen", "1234", "BLOCKED", "1"))),
        arrayOf("null value", SigningTestCase(
                input = ValuesToSign(mockHeaders, params = null, body = mapOf("id" to "", "city" to "Essen")),
                keys = headersJsonPaths + listOf("$['body']['city']"),
                values = headersValues + listOf("Essen"))),
        arrayOf("custom value", SigningTestCase(
                input = ValuesToSign(mockHeaders, body = ExampleDataClass(false, "i'm a teapot")),
                keys = headersJsonPaths + listOf("$['body']['is_valid']", "$['body']['message']"),
                values = headersValues + listOf("false", "i'm a teapot"))))

val verifyTestCase = ValuesToSign(mockHeaders, body = mapOf(
        "id" to "1",
        "city" to "Essen",
        "evses" to listOf(mapOf(
                "id" to "1234",
                "status" to "BLOCKED",
                "connectors" to listOf(mapOf("id" to "1"))))))