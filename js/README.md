# OCN Notary JavaScript Package

This readme contains specific information pertaining to the JavaScript package of the Open Charging Network
Notary, including: how to test, build, publish, install and use.

This package is written in TypeScript to provide type definitions for developers.

## Install

```
npm install @shareandcharge/ocn-notary
```

## Usage

Import with JavaScript:
```js
const Notary = require("@shareandcharge/ocn-notary").default
```

or TypeScript:
```ts
import Notary from "@shareandcharge/ocn-notary")
```


### Signing a request or response:

First, create an object containing the headers, url-encoded parameters and body of the OCPI request/response.
Each field is optional - if signing a request there may be no JSON body; if signing a response there may 
be no headers. Note that the order of fields in the header and the body should match their order in the OCPI spec. 
See [issue #4](https://bitbucket.org/shareandcharge/ocn-notary/issues/4/) for details.

```js
const valuesToSign = {
    headers: {
        "x-correlation-id": "123"
        ...
        // example response header: "x-total-count": 150
    },
    body: {
        "id": "LOC1",
        ...
    }
}
``` 

Next, instantiate an empty notary object and sign the request with your Ethereum private key.
Note that the `sign` method is asynchronous. You should wait for it to resolve before you retrieve
the signature from the Notary instance.

```js
const notary = new Notary()
await notary.sign(request, privateKey)
```

The `OCN-Signature` is a base64-encoded, JSON serialized string of the Notary. Retrieving it is possible with the Notary's `serialize`
method.

```js
const ocnSignature = notary.serialize()
console.log(ocnSignature)
// eyJmaWVsZHMiOlsiJFsnaGVhZGVycyddWyd4LWNvcnJlbGF0aW9uLWlkJ10iLCIkWydoZWFkZXJzJ11bJ...
```

This string should be appended to your headers with the key `OCN-Signature` before sending a request to the Open
Charging Network.

If responding to a request, the signature should instead be placed in the OCPI response body, for example:

```
{
  "status_code": 1000,
  "timestamp": "2020-03-25T09:49:05.429Z"
  "ocn_signature": "eyJmaWVsZHMiOlsiJFsnaGVhZGVycyddWyd4LWNvcnJlbGF0aW9uLWlkJ10iLCIkWydoZWFkZXJzJ11bJ..."
}
```

### Verifying a request:

In order to verify the request, first deserialize the signature received from a counterparty.

```js
const notary = Notary.deserialize(ocnSignature)
```

Then, call the verify method on the notary object with the headers, url-encoded parameters and body of the request
received (see above for how this looks like).

```js
const isValid = notary.verify(request)
console.log(isValid)
// true
```

### Modifying a request:

It might be the case that an OCN node needs to modify a request before forwarding it to the recipient, such as in 
the case where the sender has specified a `response_url`. In order to do this, we can stash the values that will be
changed:

```js
const notary = Notary.deserialize(ocnSignature)
notary.stash({
    "$['body']['response_url']": "https://amazing.msp.org/commands/START_SESSION/42"
})
```

It is important to sign the new request afterwards:

```js
await notary.sign(modifiedRequest, someOtherPrivateKey)
```

And finally, include the signature in the forwarded request headers by serializing the notary object:

```js
headers["ocn-signature"] = notary.serialize()
```

or body:
```js
body.ocn_signature = notary.serialize()
```