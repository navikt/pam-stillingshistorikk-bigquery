import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"
    id("com.gradleup.shadow") version "8.3.3"
    application
}

version = "0.1"
group "no.nav.arbeidsplassen.stillingshistorikk"

application {
    mainClass.set("no.nav.arbeidsplassen.stillingshistorikk.ApplicationKt")
}

java {
    sourceCompatibility = JavaVersion.toVersion("21")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
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
        useJUnitPlatform()
        exclude("**/*IT.class")
    }
}

tasks.named("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveFileName.set("stillingshistorikk-$version-all.jar")
    mergeServiceFiles()
}

val javalinVersion = "6.5.0"
val micrometerVersion = "1.14.5"
val jacksonVersion = "2.18.3"
val tokenSupportVersion = "5.0.5"
val testContainersVersion = "1.20.6"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.javalin:javalin:$javalinVersion")
    implementation("io.javalin:javalin-micrometer:$javalinVersion")
    implementation("org.eclipse.jetty:jetty-util")
    implementation("io.micrometer:micrometer-core:$micrometerVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
    implementation("io.prometheus:simpleclient_common:0.16.0")

    api(platform("com.google.cloud:libraries-bom:26.56.0"))
    implementation("com.google.cloud:google-cloud-bigquery")
    implementation("com.google.cloud:google-cloud-bigquerystorage")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    implementation("no.nav.security:token-validation-core:$tokenSupportVersion")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")

    implementation("org.apache.kafka:kafka-clients:3.9.0")
    implementation("org.apache.avro:avro:1.12.0")
    implementation("io.confluent:kafka-avro-serializer:7.9.0")

    implementation("ch.qos.logback:logback-classic:1.5.17")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("com.papertrailapp:logback-syslog4j:1.0.0")
    implementation("org.codehaus.janino:janino:3.1.12")
    implementation("com.auth0:java-jwt:4.5.0")

    testImplementation(kotlin("test"))
    testImplementation("no.nav.security:mock-oauth2-server:2.0.0")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:gcloud:$testContainersVersion")
    testImplementation("org.testcontainers:kafka:$testContainersVersion")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("io.mockk:mockk:1.13.17")
}

