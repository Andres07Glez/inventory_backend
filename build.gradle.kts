import org.gradle.kotlin.dsl.testImplementation

plugins {
    java
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
    id("org.sonarqube") version "6.2.0.5505"
}

group = "mx.edu.unpa"
version = "0.0.1-SNAPSHOT"
description = "inventory_backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

val mapstructVersion = "1.6.3"
val jjwtVersion = "0.12.6"
val lombokMapstructBindingVersion = "0.2.0"

dependencies {
    // --- DESTINO: IMPLEMENTATION (Producción / Core) ---
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.yaml:snakeyaml")
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // --- DESTINO: COMPILE ONLY ---
    compileOnly("org.projectlombok:lombok")

    // --- DESTINO: RUNTIME / DEVELOPMENT ONLY ---
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // --- DESTINO: ANNOTATION PROCESSORS ---
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    // --- DESTINO: TEST IMPLEMENTATION ---
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")

    // --- DESTINO: TEST COMPILE ONLY ---
    testCompileOnly("org.projectlombok:lombok")

    // --- DESTINO: TEST RUNTIME ONLY ---
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")

    // --- DESTINO: TEST ANNOTATION PROCESSORS ---
    testAnnotationProcessor("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/dtos/**",
                    "**/domains/**",
                    "**/exceptions/**",
                    "**/mappers/**",
                    "**/config/**"
                )
            }
        })
    )
}

sonar {
    properties {
        property("sonar.gradle.skipCompile", "true")
        property("sonar.host.url", "http://sonarqube-server:9000")
        property("sonar.coverage.jacoco.xmlReportPaths",
            "${layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.coverage.exclusions",
            "**/dtos/**, **/domains/**, **/exceptions/**, **/mappers/**, **/config/**")
    }
}