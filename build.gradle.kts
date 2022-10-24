plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.kotlin.kapt") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.micronaut.application") version "3.6.2"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.7.20"
}

version = "0.1"
group "no.nav.arbeidsplassen.stihibi"

val kotlinVersion=project.properties.get("kotlinVersion")
val micronautKafkaVersion=project.properties.get("micronautKafkaVersion")
val logbackEncoderVersion=project.properties.get("logbackEncoderVersion")

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jcenter.bintray.com")
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
    kapt("io.micronaut:micronaut-http-validation")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("javax.annotation:javax.annotation-api")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    implementation("io.micronaut.kafka:micronaut-kafka:${micronautKafkaVersion}")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:${logbackEncoderVersion}")
    implementation("io.micronaut:micronaut-validation")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")

    //Snyk fixes
    implementation("org.apache.kafka:kafka-clients:3.3.1")
    implementation("org.yaml:snakeyaml:1.33")
    //implementation("io.micronaut:micronaut-graal:3.7.2")
    implementation("io.micronaut:micronaut-inject:3.7.2")
    implementation("io.micronaut:micronaut-runtime:3.7.2")


    api(platform("com.google.cloud:libraries-bom:26.1.3"))
    implementation("com.google.cloud:google-cloud-bigquery")
    implementation("com.google.cloud:google-cloud-bigquerystorage")
    //implementation("com.google.cloud:google-cloud-graalvm-support:0.7.0")
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
    sourceCompatibility = JavaVersion.toVersion("17")
}


tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
            javaParameters = true
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "17"
            javaParameters = true
        }
    }

//    build {
//        finalizedBy(generateResourcesConfigFile)
//    }

    test {
        exclude("**/*IT.class")
    }


}
