import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("com.google.gms.google-services")
    id("com.google.firebase.appdistribution")
    id("com.adarshr.test-logger")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val versionNameMajor = 1
val versionNameMinor = 5
val versionNamePatch = 1

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
        versionCode = 45
        versionName = "$versionNameMajor.$versionNameMinor.$versionNamePatch"

        signingConfig = if (signBuild) {
            signingConfigs.getByName("release")
        } else {
            signingConfigs.getByName("debug")
        }

        manifestPlaceholders["STRONGBOX_ENABLED"] = true

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
            buildConfigField("String", "BASE_URL", "\"https://staging.censo.dev/\"")
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            buildConfigField("boolean", "FACETEC_ENABLED", "true")
            applicationIdSuffix = ".staging"
            isDebuggable = false
        }
        create("integration") {
            resValue("string", "app_name", "Integration Censo")
            manifestPlaceholders["SENTRY_ID"] = sentryDevId
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
        jvmTarget = "17"
        freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
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
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    kapt("androidx.hilt:hilt-compiler:1.1.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    //Facetec
    implementation("com.facetec:facetec-sdk:9.6.58@aar")

    //Google Play Billing
    implementation("com.android.billingclient:billing:6.1.0")
    implementation("com.android.billingclient:billing-ktx:6.1.0")

    // Play Integrity
    implementation("com.google.android.play:integrity:1.3.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.mockito:mockito-inline:4.8.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")

    testImplementation("org.robolectric:robolectric:4.9")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    //UI testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")

    val kaspressoVersion = "1.5.2"
    testImplementation("com.kaspersky.android-components:kaspresso:$kaspressoVersion")
    testImplementation("com.kaspersky.android-components:kaspresso-compose-support:$kaspressoVersion")
    androidTestImplementation("com.kaspersky.android-components:kaspresso:$kaspressoVersion")
    androidTestImplementation("com.kaspersky.android-components:kaspresso-compose-support:$kaspressoVersion")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestUtil("androidx.test:orchestrator:1.4.2")
}
