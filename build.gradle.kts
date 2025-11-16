plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.serialization") version "1.9.22"
	id("org.springframework.boot") version "3.2.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "org.nevis"
version = "0.0.1-SNAPSHOT"
description = "NevisSearch"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web:3.2.0")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.2.0")
	implementation("org.hibernate.orm:hibernate-core:6.4.1.Final")
	implementation("org.hibernate.orm:hibernate-vector:6.4.1.Final")
	implementation("com.pgvector:pgvector:0.1.4")
	implementation("com.squareup.okhttp3:okhttp:4.12.0")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.mockito:mockito-core:5.11.0")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
