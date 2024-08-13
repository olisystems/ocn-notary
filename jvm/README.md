# OCN Notary JVM Package

This readme contains specific information for the JVM package of the OCN Notary, written in Kotlin. This includes how 
to test, build, publish, install and use the library.

## Install

The Notary is hosted on JCenter and can be included in a project via:

**Gradle**
```groovy
implementation 'shareandcharge.openchargingnetwork:notary:0.0.1'
```

**Maven**
```xml
<dependency>
  <groupId>shareandcharge.openchargingnetwork</groupId>
  <artifactId>notary</artifactId>
  <version>0.0.1</version>
  <type>pom</type>
</dependency>
```

## Usage

Examples are provided in Java.

### Sending a signed OCPI request

If an OCN node or party requires message signing, an `OCN-Signature` header should be added to the HTTP request.

Firstly, make sure to have the private key which will sign the message ready. If a key needs to be generated, the 
[web3j](https://github.com/web3j/web3j) `crypto` package can do this: 
```java
String privateKey = Keys.createEcKeyPair().getPrivateKey().toString(16);
```

Next, prepare the request, including the headers as well as the (optional) url-encoded parameters and body that will
be sent to the OCN node.

```java
OcpiHeaders headers = new OcpiHeaders(
    "123",  /* x-correlation-id */
    "CH",   /* ocpi-from-country-code */
    "SNC",  /* ocpi-from-party-id */
    "DE",   /* ocpi-to-country-code */
    "XXX"   /* ocpi-to-party-id */
);

OcpiUrlEncodedParameters params = new OcpiUrlEncodedParameters()
params.setLimit("20")
```

The body can be any type. For example a string or a map:
```java
String body = "ACCEPTED"

Map<String, Object> body = new HashMap<>();
body.put("response_url", "https://some.server.net/callback/1");
```

It is also possible to use custom types. The OCN Notary uses [Jackson](https://github.com/FasterXML/jackson) to
de/serialize OCN Signatures. In order to make sure custom data type properties are correctly validated, they should be
annotated with a `JsonProperty` pointing to the appropriate JSON field, for example:
```java
public class StartRequest {
    public String responseUrl;
    public Token token;
    public String locationId;
    public String evseUid;

    public StartRequest(@JsonProperty("response_url") String responseUrl,
                        Token token,
                        @JsonProperty("location_id") String locationId,
                        @JsonProperty("evse_uid") String evseUid) {
        this.responseUrl = responseUrl;
        this.token = token;
        this.locationId = locationId;
        this.evseUid = evseUid;
    }   
};

StartRequest body = new StartRequest("https://some.server.net/callback/1", token, "location-1", "evse-1");
```

Once the request properties are ready, build the request:
```java
OcpiRequest request = new OcpiRequest(headers, params, body);
```

With the request and private key ready, the OCN signature can be obtained:
```java
Notary notary = new Notary();
notary.sign(request, privateKey);

String signature = notary.serialize();

System.out.println(signature);
// eyJmaWVsZHMiOlsiJFsnaGVhZGVycyddWyd4LWNvcnJlbGF0aW9uLWlkJ10iLCIkWydoZWFkZXJzJ11bJ...
```

The signature can now be added to the request headers under the key `OCN-Signature`.

### Verifying a signed OCPI request

Upon receiving a signed OCPI request, the signature may be verified to ensure data has not been tampered in transit.
Note that the Notary does not verify the integrity of the signatory, only verifies that the signature is correct. With
a verified request the signatory can then be inspected to ensure it is from the correct counter-party.

First, deserialize the OCN Signature header of the received request:

```java
Notary notary = Notary.deserialize("eyJmaWVsZHMiOlsiJFsnaGVhZGVycyddWyd4LWNvcnJlbGF0aW9uLWlkJ10iLCIkWydoZWFkZXJzJ11bJ...");
```

Next, build the OCPI request object with the received headers, url-encoded parameters and body, as before:
```java
OcpiRequest request = new OcpiRequest<>(/* headers, params, body */);
```

Then tell the Notary to verify the request:
```java
VerifyResult result = notary.verify(request);

System.out.println(result.isValid());
// true
System.out.println(result.getError());
// null
```

### Modifying a request

An OCN node or other party may need to alter values in a party's request before it reaches its destination. For example,
an OCN node will need to change the `repsonse_url` of a START_SESSION request, as the recipient does not have access
to the given url on the sender's server. To do this, while maintaining the integrity of the signature, we can stash
the overwritten values:

```java
OcpiRequest modifiedRequest = new OcpiRequest(/* modified request properties */);

Map<String, Object> rewrites = new HashMap()
rewrites.put("$['body']['response_url']", "https://some.emsp.server.com/ocpi/2.2/sender/commands/START_SESSION/1");

notary.stash(rewrites);
```

Here, we create a map of rewritten values. This ensures that the recipient can verify the original signatory of the 
message, by rebuilding the original request. The map has the JsonPath as key, pointing to the overwritten value.

The recipient only needs to verify the signature as usual. The verify method will check for any rewrites and ensure
that they are valid.  

## Development

Testing, building and publishing can be done with Gradle:

```
$ ./gradlew test
$ ./gradlew build
```

Uploading a new version to Jcenter can be done as follows:

- Bump the version number in `build.gradle.kts`:
```kotlin
version = "1.0.1-beta2" 
```

- Export the following environment variables:
```
export ORG_GRADLE_PROJECT_bintrayUser={{BINTRAY_USERNAME}}
export ORG_GRADLE_PROJECT_bintrayKey={{BINTRAY_APIKEY}}
```

- Run the upload command:
```
./gradlew bintrayUpload
```
