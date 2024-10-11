import org.gradle.jvm.tasks.Jar
import org.gradle.nativeplatform.platform.internal.Architectures
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    signing
    id("org.jetbrains.dokka") version "1.8.10"
    id("eu.kakde.gradle.sonatype-maven-central-publisher") version "1.0.6"
}

group = "com.my-oli"
version = "1.0.2-1"

repositories {
    mavenCentral()
}

val brotliVersion = "1.16.0"
val operatingSystem: OperatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()
val architecture = DefaultNativePlatform.getCurrentArchitecture()
val architectureNativeBinding = "com.aayushatharva.brotli4j:native-" + when {
    operatingSystem.isWindows -> when {
        architecture.isArm() -> "windows-aarch64"
        else -> "windows-x86_64"
    }
    operatingSystem.isMacOsX -> when {
        architecture.isArm() -> "osx-aarch64"
        else -> "osx-x86_64"
    }
    operatingSystem.isLinux -> when {
        Architectures.ARM_V7.isAlias(architecture.name) -> "linux-armv7"
        Architectures.AARCH64.isAlias(architecture.name) -> "linux-aarch64"
        Architectures.X86_64.isAlias(architecture.name) -> "linux-x86_64"
        else -> throw IllegalStateException("Unsupported Linux architecture: ${architecture.name}")
    }
    else -> throw IllegalStateException("Unsupported operating system: $operatingSystem")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.web3j:core:4.9.7")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.0")
    implementation("com.jayway.jsonpath:json-path:2.8.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
    testImplementation(kotlin("test"))

    implementation("com.aayushatharva.brotli4j:brotli4j:$brotliVersion")
    implementation("$architectureNativeBinding:$brotliVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}


tasks.dokkaHtml {
    outputDirectory.set(buildDir.resolve("dokka"))
}


val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

object Meta {
    val COMPONENT_TYPE = "java" // "java" or "versionCatalog"
    val GROUP = "com.my-oli"
    val ARTIFACT_ID = "ocn-notary"
    val VERSION = "1.0.2-1"
    val PUBLISHING_TYPE = "AUTOMATIC" // USER_MANAGED or AUTOMATIC
    val SHA_ALGORITHMS = listOf("SHA-256", "SHA-512") // sha256 and sha512 are supported but not mandatory. Only sha1 is mandatory but it is supported by default.
    val DESC = "GitHub Version Catalog Repository for Personal Projects based on Gradle"
    val LICENSE = "Apache-2.0"
    val LICENSE_URL = "https://opensource.org/licenses/Apache-2.0"
    val GITHUB_REPO = "olisystems/ocn-notary"
    val DEVELOPER_ID = "olisystems"
    val DEVELOPER_NAME = "OLI Systems"
    val DEVELOPER_ORGANIZATION = "OLI Systems GmbH"
    val DEVELOPER_ORGANIZATION_URL = "https://www.my-oli.com/en/"
}

val sonatypeUsername: String? by project // this is defined in ~/.gradle/gradle.properties
val sonatypePassword: String? by project // this is defined in ~/.gradle/gradle.properties

sonatypeCentralPublishExtension {
    // Set group ID, artifact ID, version, and other publication details
    groupId.set(Meta.GROUP)
    artifactId.set(Meta.ARTIFACT_ID)
    version.set(Meta.VERSION)
    componentType.set(Meta.COMPONENT_TYPE) // "java" or "versionCatalog"
    publishingType.set(Meta.PUBLISHING_TYPE) // USER_MANAGED or AUTOMATIC

    // Set username and password for Sonatype repository
    username.set(System.getenv("SONATYPE_USERNAME") ?: sonatypeUsername)
    password.set(System.getenv("SONATYPE_PASSWORD") ?: sonatypePassword)

    // Configure POM metadata
    pom {
        name.set(Meta.ARTIFACT_ID)
        description.set(Meta.DESC)
        url.set("https://github.com/${Meta.GITHUB_REPO}")
        licenses {
            license {
                name.set(Meta.LICENSE)
                url.set(Meta.LICENSE_URL)
            }
        }
        developers {
            developer {
                id.set(Meta.DEVELOPER_ID)
                name.set(Meta.DEVELOPER_NAME)
                organization.set(Meta.DEVELOPER_ORGANIZATION)
                organizationUrl.set(Meta.DEVELOPER_ORGANIZATION_URL)
            }
        }
        scm {
            url.set("https://github.com/${Meta.GITHUB_REPO}")
            connection.set("scm:git:https://github.com/${Meta.GITHUB_REPO}")
            developerConnection.set("scm:git:https://github.com/${Meta.GITHUB_REPO}")
        }
        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/${Meta.GITHUB_REPO}/issues")
        }
    }
}