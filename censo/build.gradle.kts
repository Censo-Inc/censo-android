import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.version)
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    alias(libs.plugins.google.services)
    alias(libs.plugins.app.distribution)
    id(libs.plugins.test.logger.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.play.triplet)
}

play {
    serviceAccountCredentials.set(file("${project.rootDir}/${BuildUtils.SERVICE_FILE_NAME}"))

    defaultToAppBundles.set(true)
    track.set("internal")
}

android {
    namespace = "co.censo.censo"
    compileSdk = 34

    var signBuild = false
    var sentryDevId = ""
    var sentryProdId = ""
    val configProperties = Properties()

    if (file("../config.properties").exists()) {
        configProperties.load(FileInputStream(file("../config.properties")))

        signBuild = (configProperties["SIGN_BUILD"] as String).toBoolean()
        sentryDevId = configProperties["SENTRY_DEV_ID"] as String
        sentryProdId = configProperties["SENTRY_PROD_ID"] as String
    }

    if (signBuild) {
        signingConfigs {
            create("release") {

                keyAlias = configProperties["OWNER_RELEASE_KEY_ALIAS"] as String
                keyPassword = configProperties["OWNER_RELEASE_STORE_PASSWORD"] as String
                storeFile = file("keystore.jks")
                storePassword = configProperties["OWNER_RELEASE_STORE_PASSWORD"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "co.censo.censo"
        minSdk = 33
        targetSdk = 33
        versionCode = BuildUtils.gitCommitCount()
        versionName = "${libs.versions.versionNameMajor.get()}.${libs.versions.versionNameMinor.get()}.${libs.versions.versionNamePatch.get()}"

        signingConfig = if (signBuild) {
            signingConfigs.getByName("release")
        } else {
            signingConfigs.getByName("debug")
        }

        manifestPlaceholders["STRONGBOX_ENABLED"] = true

        buildConfigField("boolean", "BENEFICIARY_ENABLED", "true")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            resValue("string", "app_name", "Censo")
            isMinifyEnabled = false
            isDebuggable = false
            manifestPlaceholders["SENTRY_ID"] = sentryProdId
            manifestPlaceholders["OWNER_URL_SCHEME"] = "censo-main"
            buildConfigField("String", "BASE_URL", "\"https://api.censo.co/\"")
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            buildConfigField("boolean", "FACETEC_ENABLED", "true")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("localRelease") {
            resValue("string", "app_name", "Local Release Censo")
            initWith(getByName("release"))
            manifestPlaceholders["SENTRY_ID"] = sentryDevId
        }
        create("staging") {
            resValue("string", "app_name", "Staging Censo")
            manifestPlaceholders["SENTRY_ID"] = sentryDevId
            manifestPlaceholders["OWNER_URL_SCHEME"] = "censo-main-staging"
            buildConfigField("String", "BASE_URL", "\"https://staging.censo.dev/\"")
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            buildConfigField("boolean", "FACETEC_ENABLED", "true")
            applicationIdSuffix = ".staging"
            isDebuggable = false
            firebaseAppDistribution {
                testers = "censoqa@gmail.com, strikepqa@gmail.com"
            }
        }
        create("integration") {
            resValue("string", "app_name", "Integration Censo")
            manifestPlaceholders["SENTRY_ID"] = sentryDevId
            manifestPlaceholders["OWNER_URL_SCHEME"] = "censo-main-integration"
            buildConfigField("String", "BASE_URL", "\"https://integration.censo.dev/\"")
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            buildConfigField("boolean", "FACETEC_ENABLED", "false")
            applicationIdSuffix = ".integration"
            isDebuggable = false
        }
        debug {
            initWith(getByName("integration"))
            resValue("string", "app_name", "Debug Censo")
            manifestPlaceholders["STRONGBOX_ENABLED"] = false
            buildConfigField("boolean", "STRONGBOX_ENABLED", "false")
            buildConfigField("boolean", "FACETEC_ENABLED", "false")
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }
    sourceSets.getByName("release") {
        kotlin.setSrcDirs(listOf("src/paid/kotlin"))
    }
    sourceSets.getByName("localRelease") {
        kotlin.setSrcDirs(listOf("src/paid/kotlin"))
    }
    sourceSets.getByName("staging") {
        kotlin.setSrcDirs(listOf("src/paid/kotlin"))
    }
    sourceSets.getByName("integration") {
        kotlin.setSrcDirs(listOf("src/demo/kotlin"))
    }
    sourceSets.getByName("debug") {
        kotlin.setSrcDirs(listOf("src/demo/kotlin"))
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = libs.versions.jvm.get()

        freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/*"
        }
    }
}

dependencies {
    implementation(project(":shared"))

    //Dagger - Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    kapt(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    //Facetec
    implementation("com.facetec:facetec-sdk:9.6.58@aar")

    //CameraX
    implementation(libs.cameraX)
    implementation(libs.cameraX.lifecycle)
    implementation(libs.cameraX.view)

    //Compose Accompanist
    implementation(libs.accompanist)

    //Google Play Billing
    implementation(libs.billing)
    implementation(libs.billing.ktx)

    // Play Integrity
    implementation(libs.integrity)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.kotlinx.coroutines.test)

    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    //UI testing
    androidTestImplementation(libs.androidx.ui.test.junit4)

    testImplementation(libs.kaspresso)
    testImplementation(libs.kaspresso.compose.support)
    androidTestImplementation(libs.kaspresso)
    androidTestImplementation(libs.kaspresso.compose.support)
    androidTestImplementation(libs.androidx.runner)
    androidTestUtil(libs.androidx.orchestrator)
}
