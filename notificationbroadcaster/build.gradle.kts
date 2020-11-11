plugins {
    id("com.android.application")
}

android {
    compileSdkVersion(AppConfig.COMPILE_SDK)

    defaultConfig {
        applicationId = "org.jraf.android.ticker.notificationbroadcaster"
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

        // Setting this to true enables the png generation at buildtime
        // (see http://android-developers.blogspot.fr/2016/02/android-support-library-232.html)
        vectorDrawables.useSupportLibrary = true
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

            isMinifyEnabled = true
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

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
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
    // Support library
    implementation("androidx.appcompat", "appcompat", Versions.ANDROIDX_APPCOMPAT)

    // JRAF
    implementation("org.jraf", "kprefs", Versions.KPREFS)
//    implementation("com.github.BoD", "jraf-android-util", "-SNAPSHOT")
    implementation("org.jraf", "jraf-android-util", "1.0.0")

    // Testing
    androidTestImplementation("androidx.test.espresso", "espresso-core", Versions.ESPRESSO) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation("junit", "junit", Versions.JUNIT)
}
