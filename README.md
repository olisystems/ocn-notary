# OCN Notary

The Open Charging Network Notary provides functions in popular programming languages to sign an OCN request and verify an `OCN-Signature` header.

# Usage

### Java

1. Include the dependency from JCenter using e.g. Gradle/Maven

Gradle:
```groovy
implementation 'shareandcharge.openchargingnetwork:notary:1.0.0'
```

Maven:
```xml
<dependency>
  <groupId>shareandcharge.openchargingnetwork</groupId>
  <artifactId>notary</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>
```

Check the [JVM readme](jvm/README.md) for further usage information.

### JavaScript

Install the package using npm:
```
npm install @shareandcharge/ocn-notary
```

Check the [JS readme](js/README.md) for further usage information.


# Development

The seperate libraries are contained in their respective directories. Build scripts should provide 
functionality to test, build and publish the individual packages. A top-level `Makefile` exists to 
coordinate between the different libraries, such that they can be tested, built and published in
single steps within the CD pipeline.
