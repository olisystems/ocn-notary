import { IValuesToSign } from "../src/ocpi-request.interface"

interface ISigningTestCase {
    name: string,
    input: IValuesToSign,
    keys: string[]
    values: string[]
}

export const mockHeaders = {
    "x-correlation-id": "456",
    "ocpi-from-country-code": "DE",
    "ocpi-from-party-id": "ABC",
    "ocpi-to-country-code": "DE",
    "ocpi-to-party-id": "XYZ"
}

export const signTestCases: ISigningTestCase[] = [
    {
        name: "string values",
        input: {
            headers: mockHeaders,
            body: { id: "1", city: "Essen" }
        },
        keys: ["['id']", "['city']"],
        values: ["1", "Essen"]
    },
    {
        name: "mixed values",
        input: {
            headers: mockHeaders,
            body: { id: 1, city: "Essen", valid: true }
        },
        keys: ["['id']", "['city']", "['valid']"],
        values: ["1", "Essen", "true"]
    },
    {
        name: "nested values",
        input: {
            headers: mockHeaders,
            body: { id: "1", city: "Essen", coordinates: { latitude: 54.002, longitude: -0.783 } }
        },
        keys: ["['id']", "['city']", "['coordinates']['latitude']", "['coordinates']['longitude']"],
        values: ["1", "Essen", "54.002", "-0.783"]
    },
    {
        name: "array values",
        input: {
            headers: mockHeaders,
            body: { id: "1", city: "Essen", types: ["WHITELIST", "ALWAYS"] }
        },
        keys: ["['id']", "['city']", "['types'][0]", "['types'][1]"],
        values: ["1", "Essen", "WHITELIST", "ALWAYS"]
    },
    {
        name: "objects within arrays",
        input: {
            headers: mockHeaders,
            body: { id: "1", city: "Essen", evses: [{ id: "1234", status: "BLOCKED" }] }
        },
        keys: ["['id']", "['city']", "['evses'][0]['id']", "['evses'][0]['status']"],
        values: ["1", "Essen", "1234", "BLOCKED"]
    },
    {
        name: "nested objects within arrays",
        input: {
            headers: mockHeaders,
            body: {
                id: "1", city: "Essen", evses: [{ id: "1234", status: "BLOCKED", connectors: [{ id: "1" }] }]
            }
        },
        keys: ["['id']", "['city']", "['evses'][0]['id']", "['evses'][0]['status']", "['evses'][0]['connectors'][0]['id']"],
        values: ["1", "Essen", "1234", "BLOCKED", "1"]
    },
    {
        name: "null value",
        input: {
            headers: mockHeaders,
            params: undefined,
            body: { id: "", city: "Essen" }
        },
        keys: ["['city']"],
        values: ["Essen"]
    }
]

export const verifyTestCase = {
    input: {
        headers: mockHeaders,
        body: {
            id: "1", city: "Essen", evses: [{ id: "1234", status: "BLOCKED", connectors: [{ id: "1" }] }]
        }
    }
}
