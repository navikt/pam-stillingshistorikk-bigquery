plugins {
    kotlin("jvm") version "2.4.20"
    id("com.gradleup.shadow") version "9.6.1"
    application
}

version = "0.1"
group "no.nav.arbeidsplassen.stillingshistorikk"

application {
    mainClass.set("no.nav.arbeidsplassen.stillingshistorikk.ApplicationKt")
}

kotlin {
    jvmToolchain(25)
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
            javaParameters = true
        }
    }
    compileTestKotlin {
        compilerOptions {
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

val javalinVersion = "7.2.3"
val micrometerVersion = "1.17.1"
val jacksonVersion = "2.22.3"
val tokenSupportVersion = "6.0.13"
val testContainersVersion = "1.21.4"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.javalin:javalin:$javalinVersion")
    implementation("io.javalin:javalin-micrometer:$javalinVersion")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:2.31.1")
    implementation("io.micrometer:micrometer-core:$micrometerVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
    implementation("io.prometheus:simpleclient_common:0.16.0")

    api(platform("com.google.cloud:libraries-bom:26.89.0"))
    implementation("com.google.cloud:google-cloud-bigquery")
    implementation("com.google.cloud:google-cloud-bigquerystorage")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    implementation("no.nav.security:token-validation-core:$tokenSupportVersion")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")

    implementation("org.apache.kafka:kafka-clients:4.3.0")
    implementation("ch.qos.logback:logback-classic:1.6.4")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("com.papertrailapp:logback-syslog4j:1.0.0")
    implementation("org.codehaus.janino:janino:3.1.12")
    testImplementation(kotlin("test"))
    testImplementation("no.nav.security:mock-oauth2-server:6.0.3")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:gcloud:$testContainersVersion")
    testImplementation("org.testcontainers:kafka:$testContainersVersion")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("io.mockk:mockk:1.14.11")
}
