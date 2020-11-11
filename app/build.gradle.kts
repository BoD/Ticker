plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdkVersion(AppConfig.COMPILE_SDK)

    defaultConfig {
        applicationId = AppConfig.APPLICATION_ID
        minSdkVersion(AppConfig.MIN_SDK)
        targetSdkVersion(AppConfig.TARGET_SDK)
        versionCode = AppConfig.buildNumber
        versionName = AppConfig.buildProperties["versionName"]

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // For now we enable strict mode for all the configs
        buildConfigField("boolean", "STRICT_MODE", "true")
        // For now we enable debug logs all the configs
        buildConfigField("boolean", "DEBUG_LOGS", "true")

        resConfigs("en", "fr")

        // Useful for api keys in the manifest (Maps, Crashlytics, ...)
        setManifestPlaceholders(AppConfig.buildProperties as Map<String, Any>)

        // Setting this to false enables the png generation at buildtime
        // (see http://android-developers.blogspot.fr/2016/02/android-support-library-232.html)
        vectorDrawables.useSupportLibrary = false

        // Multidex :(
        multiDexEnabled = true
    }

    signingConfigs {
        create("release") {
            storeFile = file(AppConfig.buildProperties["signingStoreFile"]!!)
            storePassword = AppConfig.buildProperties["signingStorePassword"]
            keyAlias = AppConfig.buildProperties["signingKeyAlias"]
            keyPassword = AppConfig.buildProperties["signingKeyPassword"]
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"

            buildConfigField("String", "GIT_SHA1", "\"dev\"")
            buildConfigField("String", "BUILD_DATE", "\"dev\"")
        }

        getByName("release") {
            buildConfigField("String", "GIT_SHA1", "\"${getGitSha1()}\"")
            buildConfigField("String", "BUILD_DATE", "\"${buildDate}\"")

            // XXX Proguard is completely disabled for now as I can't find rules that work
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        dataBinding = true
    }

    lintOptions {
        isAbortOnError = true
        textReport = true
        isIgnoreWarnings = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
    }

    packagingOptions {
        pickFirst("META-INF/io.netty.versions.properties")
        pickFirst("META-INF/INDEX.LIST")
        pickFirst("META-INF/kotlinx-html.kotlin_module")
        pickFirst("META-INF/DEPENDENCIES")
    }
}

afterEvaluate {
    android.applicationVariants.forEach { variant ->
        // Create new copy tasks, for release builds
        if (variant.buildType.name == "release") {
            variant.outputs.forEach { output ->
                val outputApkFile =
                    file("build/outputs/apk/${variant.flavorName}/release/${output.outputFile.name}")
                val destinationApkFileName =
                    "${rootProject.name}-${project.name}-${android.defaultConfig.versionCode}-${variant.flavorName}-signed.apk"

                // Copy the apk to the 'etc' folder
                val copyApkToEtc =
                    tasks.register<Copy>("copy${variant.name.capitalize()}ApkToEtc") {
                        from(outputApkFile)
                        into("../etc/apk")
                        rename(output.outputFile.name, destinationApkFileName)
                    }

                // Copy the apk to the deploy folder
                val copyApkToDeploy =
                    tasks.register<Copy>("copy${variant.name.capitalize()}ApkToDeploy") {
                        from(outputApkFile)
                        into("${AppConfig.buildProperties["deployFolder"]}/${rootProject.name}/${android.defaultConfig.versionCode}")
                        rename(output.outputFile.name, destinationApkFileName)
                    }

                // Make the copy tasks run after the assemble tasks of the variant
                variant.assembleProvider!!.get().finalizedBy(copyApkToEtc, copyApkToDeploy)
            }
        }
    }
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib", Versions.KOTLIN))

    // Rx
    implementation("io.reactivex.rxjava2", "rxjava", Versions.RX_JAVA)
    implementation("io.reactivex.rxjava2", "rxkotlin", Versions.RX_KOTLIN)

    // Support library
    implementation("androidx.appcompat", "appcompat", Versions.ANDROIDX_APPCOMPAT)
    implementation("androidx.emoji", "emoji-bundled", Versions.ANDROIDX_EMOJI)
    implementation("androidx.multidex", "multidex", Versions.ANDROIDX_MULTIDEX)
    implementation(
        "androidx.constraintlayout",
        "constraintlayout",
        Versions.ANDROIDX_CONSTRAINT_LAYOUT
    )

    // Material
    implementation("com.google.android.material", "material", Versions.MATERIAL)


    // JRAF
    implementation("org.jraf", "kprefs", Versions.KPREFS)
//    implementation("com.github.BoD", "jraf-android-util", "-SNAPSHOT")
    implementation("org.jraf", "jraf-android-util", "1.0.0")

    // Libticker
    implementation("org.jraf", "libticker-core", Versions.LIB_TICKER)
    implementation("org.jraf", "libticker-plugins", Versions.LIB_TICKER) {
        exclude(group = "io.grpc", module = "grpc-netty-shaded")
    }
    implementation("org.jraf", "libticker-httpconf", Versions.LIB_TICKER)

    // Klaxon
    implementation("com.beust", "klaxon", Versions.KLAXON)

    // Slf4j
    implementation("org.slf4j", "slf4j-android", Versions.SLF4J)

    // Glide
    implementation("com.github.bumptech.glide", "glide", Versions.GLIDE)
    kapt("com.github.bumptech.glide", "compiler", Versions.GLIDE)

    // Sunrise/sunset
    implementation("ca.rmen", "lib-sunrise-sunset", Versions.SUNRISE_SUNSET)

    // Grpc
    implementation("io.grpc", "grpc-protobuf", Versions.GRPC)
    implementation("io.grpc", "grpc-stub", Versions.GRPC)
    implementation("io.grpc", "grpc-okhttp", Versions.GRPC)

    // Testing
    androidTestImplementation("androidx.test.espresso", "espresso-core", Versions.ESPRESSO) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation("junit", "junit", Versions.JUNIT)
}
