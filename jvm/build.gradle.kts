import org.gradle.jvm.tasks.Jar
import org.gradle.nativeplatform.platform.internal.Architectures
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "shareandcharge.openchargingnetwork"
version = "1.0.1-temp"

plugins {
    `maven-publish`
    kotlin("jvm") version "1.6.21"
    id("org.jetbrains.dokka") version "1.8.10"
    id("com.jfrog.artifactory") version "4.29.1"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        apiVersion = "1.5" // Explicitly set API version
        languageVersion = "1.5" // Explicitly set language version
    }
}


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

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
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

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}


//publishing {
//    publications {
//        create<MavenPublication>("default") {
//            from(components["java"])
//            artifact(dokkaJar)
//            artifact(sourcesJar)
//        }
//    }
//}

//// Artifactory configuration (replace Bintray)
//artifactory {
//    setContextUrl("https://your-artifactory-url.com/artifactory")
//    publish {
//        repository {
//            setRepoKey("openchargingnetwork")
//            setUsername(project.findProperty("artifactoryUser") as String?)
//            setPassword(project.findProperty("artifactoryApiKey") as String?)
//        }
//        defaults {
//            publications("default")
//            setPublishArtifacts(true)
//            setPublishPom(true)
//        }
//    }
//}