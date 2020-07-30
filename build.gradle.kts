group = "com.klazuka.krussian"
version = "0.0.1-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.3.70"
    application
}

allprojects {
    repositories {
        mavenLocal()
        jcenter()
        maven { setUrl("https://kotlin.bintray.com/ktor") }
    }
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

sourceSets.main {
    resources.srcDir("resources")
}

kotlin {
    sourceSets {
        val main by getting { kotlin.srcDir("src") }
        val test by getting { kotlin.srcDir("test") }
    }
}

dependencies {
    val ktorVersion = "1.3.2"
    val logbackVersion = "1.2.1"

    implementation(kotlin("stdlib-jdk8"))

    fun ktor(module: String) = "io.ktor:ktor-$module:$ktorVersion"
    implementation(ktor("server-core"))
    implementation(ktor("server-netty"))
    implementation(ktor("server-sessions"))
    implementation(ktor("html-builder"))
    implementation(ktor("gson"))
    implementation(ktor("server-host-common"))
    implementation(ktor("auth"))
    implementation(ktor("auth-jwt"))
    implementation(ktor("client-core"))
    implementation(ktor("client-cio"))
    implementation(ktor("client-logging-jvm"))
    implementation(ktor("client-json-jvm"))
    implementation(ktor("client-gson"))
    testImplementation(ktor("server-tests"))
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
}
