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

val versionNameMajor = 0
val versionNameMinor = 0
val versionNamePatch = 1

android {
    namespace = "co.censo.vault"
    compileSdk = 33

    var signBuild = false
    val configProperties = Properties()

    if (file("../config.properties").exists()) {
        configProperties.load(FileInputStream(file("../config.properties")))

        signBuild = (configProperties["SIGN_BUILD"] as String).toBoolean()
    }

    if (signBuild) {
        signingConfigs {
            create("release") {

                keyAlias = configProperties["RELEASE_KEY_ALIAS"] as String
                keyPassword = configProperties["RELEASE_STORE_PASSWORD"] as String
                storeFile = file("keystore.jks")
                storePassword = configProperties["RELEASE_STORE_PASSWORD"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "co.censo.vault"
        minSdk = 33
        targetSdk = 33
        versionCode = 1
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
            resValue("string", "app_name", "Debug Vault")
            isMinifyEnabled = false
            isDebuggable = false
            resValue("string", "RAYGUN_APP_ID", "\"vuxX53AURVfZS87D1WPqeg\"")
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("staging") {
            resValue("string", "app_name", "Staging Vault")
            resValue("string", "RAYGUN_APP_ID", "\"CtOnGQjIo1U8dELkoUf0iw\"")
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            applicationIdSuffix = ".staging"
            isDebuggable = false
        }
        create("aintegration") {
            resValue("string", "app_name", "A Integration Vault")
            resValue("string", "RAYGUN_APP_ID", "\"L9T2bPaEjr3Lede3SNpFJw\"")
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            applicationIdSuffix = ".aintegration"
            isDebuggable = false
        }
        create("bintegration") {
            initWith(getByName("aintegration"))
            resValue("string", "app_name", "B Integration Vault")
            applicationIdSuffix = ".bintegration"
        }
        create("cintegration") {
            initWith(getByName("aintegration"))
            resValue("string", "app_name", "C Integration Vault")
            applicationIdSuffix = ".cintegration"
        }
        create("dintegration") {
            initWith(getByName("aintegration"))
            resValue("string", "app_name", "D Integration Vault")
            applicationIdSuffix = ".dintegration"
        }
        debug {
            initWith(getByName("aintegration"))
            resValue("string", "app_name", "Debug Vault")
            manifestPlaceholders["STRONGBOX_ENABLED"] = false
            buildConfigField("boolean", "STRONGBOX_ENABLED", "false")
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
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
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.6.0")
    implementation("androidx.compose.material:material-icons-extended:1.4.3")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")


    //Dagger - Hilt
    implementation("com.google.dagger:hilt-android:2.44")
    kapt("com.google.dagger:hilt-android-compiler:2.44")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")

    //Biometrics
    implementation("androidx.biometric:biometric:1.1.0")

    // BIP39
    implementation("cash.z.ecc.android:kotlin-bip39:1.0.2")

    //Raygun crash reporting
    implementation("com.raygun:raygun4android:4.0.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.mockito:mockito-inline:4.8.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("org.bouncycastle:bcprov-jdk15to18:1.70")

    //UI testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.4.3")

    val kaspressoVersion = "1.5.2"
    testImplementation("com.kaspersky.android-components:kaspresso:$kaspressoVersion")
    testImplementation("com.kaspersky.android-components:kaspresso-compose-support:$kaspressoVersion")
    androidTestImplementation("com.kaspersky.android-components:kaspresso:$kaspressoVersion")
    androidTestImplementation("com.kaspersky.android-components:kaspresso-compose-support:$kaspressoVersion")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestUtil("androidx.test:orchestrator:1.4.2")
}