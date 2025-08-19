import java.text.SimpleDateFormat
import java.util.*

buildscript {
    dependencies {
        classpath("org.jsonschema2pojo:jsonschema2pojo-gradle-plugin:1.2.2")
    }
}

plugins {
    java
    //war
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "6.2.0.5505"
}

group = "org.omnomnom"
version = "1.0.0-SNAPSHOT"

apply(plugin = "jsonschema2pojo")
apply(plugin = "maven-publish")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}

repositories {
    mavenCentral()
}

ext {
    set("springCloudVersion", "2024.0.0")
    set("build.timestamp", SimpleDateFormat("yyyyMMddHHmmss").format(Date()))
    set("groupId","$group")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.springframework.cloud:spring-cloud-starter-vault-config")
    implementation("org.springframework.vault:spring-vault-core")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-quartz")

    implementation("net.javacrumbs.shedlock:shedlock-spring:6.3.0")
    implementation("javax.validation:validation-api:1.0.0.GA")
    implementation("org.springframework.boot:spring-boot-starter-data-rest")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    runtimeOnly("com.microsoft.sqlserver:mssql-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configure <org.jsonschema2pojo.gradle.JsonSchemaExtension> {
    targetPackage = group.toString() + "." +  rootProject.name
    includeJsr303Annotations = true
    includeAdditionalProperties = false
    dateType = "java.time.LocalDate"
    dateTimeType = "java.time.Instant"
    useBigDecimals = true
    useBigIntegers = true
    initializeCollections = true
    includeHashcodeAndEquals = true
    includeToString = true
    removeOldOutput = true
    outputEncoding = "UTF-8"
    setSource(files("src/main/json"))
    setAnnotationStyle("jackson2")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
