import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"
    id("com.gradleup.shadow") version "8.3.2"
    id("io.micronaut.application") version "4.4.2"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.0.21"
}

version = "0.1"
group "no.nav.arbeidsplassen.stihibi"

val kotlinVersion= project.properties["kotlinVersion"]
val micronautKafkaVersion= project.properties["micronautKafkaVersion"]
val logbackEncoderVersion= project.properties["logbackEncoderVersion"]

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("no.nav.arbeidsplassen.stihibi.*")
    }
}

dependencies {
    annotationProcessor("io.micronaut.validation:micronaut-validation-processor")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("javax.annotation:javax.annotation-api")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    implementation("io.micronaut.kafka:micronaut-kafka:${micronautKafkaVersion}")
    implementation("io.micronaut:micronaut-jackson-databind")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:${logbackEncoderVersion}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")

    implementation("org.apache.kafka:kafka-clients:3.8.0")

    runtimeOnly("org.yaml:snakeyaml")

    api(platform("com.google.cloud:libraries-bom:26.48.0"))
    implementation("com.google.cloud:google-cloud-bigquery")
    implementation("com.google.cloud:google-cloud-bigquerystorage")
    implementation("io.micronaut.micrometer:micronaut-micrometer-core")
    implementation("io.micronaut.micrometer:micronaut-micrometer-registry-prometheus")
    implementation("io.micronaut:micronaut-management")

    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
}

application {
    mainClass.set("no.nav.arbeidsplassen.stihibi.Application")
}
java {
    sourceCompatibility = JavaVersion.toVersion("21")
}

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            javaParameters = true
        }
    }
    compileTestKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            javaParameters = true
        }
    }
    test {
        exclude("**/*IT.class")
    }
}

