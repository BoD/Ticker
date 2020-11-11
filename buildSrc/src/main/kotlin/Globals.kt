object Versions {
    // Misc and plugins
    const val GRADLE = "6.7"
    const val KOTLIN = "1.4.10"
    const val BEN_MANES_VERSIONS_PLUGIN = "0.36.0"
    const val ANDROID_GRADLE_PLUGIN = "4.1.1"

    // App dependencies
    const val ANDROIDX_APPCOMPAT = "1.2.0"
    const val ANDROIDX_EMOJI = "1.1.0"
    const val ANDROIDX_MULTIDEX = "2.0.1"
    const val ANDROIDX_CONSTRAINT_LAYOUT = "2.0.4"
    const val MATERIAL = "1.2.1"
    const val RX_JAVA = "2.2.20"
    const val RX_KOTLIN = "2.4.0"
    const val KPREFS = "1.6.0"
    const val LIB_TICKER = "1.5.1"
    const val SLF4J = "1.7.30"
    const val KLAXON = "5.4"
    const val GLIDE = "4.11.0"
    const val SUNRISE_SUNSET = "1.1.1"
    const val GRPC = "1.33.1"

    // Testing dependencies
    const val ESPRESSO = "3.3.0"
    const val JUNIT = "4.13.1"
}

object AppConfig {
    const val APPLICATION_ID = "org.jraf.android.ticker"
    const val COMPILE_SDK = 30
    const val TARGET_SDK = 30
    const val MIN_SDK = 19

    var buildNumber: Int = 0
    val buildProperties = mutableMapOf<String, String>()
}