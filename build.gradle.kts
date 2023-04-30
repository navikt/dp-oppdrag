import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer

/*
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/7.6/userguide/building_java_projects.html
 */

project.setProperty("mainClassName", "dp.oppdrag.AppKt")

val ktorVersion = "2.3.0"
val micrometerVersion = "1.10.6"
val jacksonVersion = "2.15.0"
val openApiGeneratorVersion = "0.6.1"
val tokenValidationVersion = "3.0.10"
val kotlinLoggerVersion = "3.0.5"
val logbackVersion = "1.4.7"
val logstashVersion = "7.3"
val postgresVersion = "42.6.0"
val hikariVersion = "5.0.1"
val flywayVersion = "9.16.3"
val navTjenesterVersion = "2612.db4dc68"
val navCommonVersion = "2.2023.01.10_13.49-81ddc732df3a"
val ibmMqVersion = "9.3.2.0"
val mockOauth2Version = "0.5.8"
val jupiterVersion = "5.9.2"
val testcontainersVersion = "1.18.0"
val mockkVersion = "1.13.5"

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.8.20"
    // Apply io.ktor.plugin to build a fat JAR
    id("io.ktor.plugin") version "2.3.0"

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://build.shibboleth.net/nexus/content/repositories/releases/")
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")

    // OpenAPI / Swagger UI
    implementation("dev.forst:ktor-openapi-generator:$openApiGeneratorVersion")

    // Security
    implementation("no.nav.security:token-validation-ktor-v2:$tokenValidationVersion")

    // Log
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggerVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    // DB
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")

    //
    implementation("com.github.navikt.tjenestespesifikasjoner:nav-virksomhet-oppdragsbehandling-v1-meldingsdefinisjon:$navTjenesterVersion")
    implementation("com.github.navikt.tjenestespesifikasjoner:avstemming-v1-tjenestespesifikasjon:$navTjenesterVersion")
    implementation("com.ibm.mq:com.ibm.mq.allclient:$ibmMqVersion")

    // Simulering
    implementation("com.github.navikt.tjenestespesifikasjoner:nav-system-os-simuler-fp-service-tjenestespesifikasjon:$navTjenesterVersion")
    // implementation("com.github.navikt.common-java-modules:cxf:$navCommonVersion")
    implementation("no.nav.common:cxf:$navCommonVersion")
    implementation("javax.xml.ws:jaxws-api:2.3.1")
    implementation("javax.jws:javax.jws-api:1.1")

    // Test
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2Version")
    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    // Use the JUnit 5 integration.
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
    // Testcontainers
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    // MockK
    testImplementation("io.mockk:mockk:$mockkVersion")
}

application {
    // Define the main class for the application.
    mainClass.set(project.property("mainClassName").toString())
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
    // Set environmental variable so as isCurrentlyRunningOnNais returns true
    environment("NAIS_APP_NAME", "test")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = project.property("mainClassName").toString()
    }
}

tasks.withType<ShadowJar> {
    isZip64 = true
    transform(ServiceFileTransformer::class.java) {
        setPath("META-INF/cxf")
        include("bus-extensions.txt")
    }
}

tasks {
    register("runServerTest", JavaExec::class) {
        environment["AZURE_APP_WELL_KNOWN_URL"] = "https://login.microsoftonline.com/77678b69-1daf-47b6-9072-771d270ac800/v2.0/.well-known/openid-configuration"
        environment["AZURE_APP_CLIENT_ID"] = "test"

        environment["DB_HOST"] = "localhost"
        environment["DB_PORT"] = "5433"
        environment["DB_DATABASE"] = "dp-oppdrag"
        environment["DB_USERNAME"] = "dp-oppdrag-user"
        environment["DB_PASSWORD"] = "dp-oppdrag-password"

        environment["MQ_ENABLED"] = "true"
        environment["MQ_HOSTNAME"] = "localhost"
        environment["MQ_PORT"] = "1414"
        environment["MQ_CHANNEL"] = "DEV.APP.SVRCONN"
        environment["MQ_QUEUEMANAGER"] = "QM1"
        environment["MQ_USER"] = "app"
        environment["MQ_PASSWORD"] = "passw0rd"
        environment["MQ_OPPDRAG_QUEUE"] = "DEV.QUEUE.1"
        environment["MQ_KVITTERING_QUEUE"] = "DEV.QUEUE.2"
        environment["MQ_AVSTEMMING_QUEUE"] = "DEV.QUEUE.3"

        environment["OPPDRAG_SERVICE_URL"] = "https://cics-q1.adeo.no/oppdrag/simulerFpServiceWSBinding"
        environment["STS_URL"] = "https://sts-q1.preprod.local/SecurityTokenServiceProvider/"

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set(project.property("mainClassName").toString())
    }
}
