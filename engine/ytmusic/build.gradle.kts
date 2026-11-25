plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.core)
    implementation(libs.ktor.okhttp)
    implementation(libs.ktor.negotiation)
    implementation(libs.ktor.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.newpipe.extractor)

    testImplementation(libs.junit)
}

tasks.withType<Test> {
    systemProperty("runLiveStreamProbe", System.getProperty("runLiveStreamProbe") ?: "false")
    testLogging {
        showStandardStreams = true
    }
}
