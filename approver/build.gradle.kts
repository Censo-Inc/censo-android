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
    namespace = "co.censo.approver"
    compileSdk = 34

    var signBuild = false
    val configProperties = Properties()

    if (file("../config.properties").exists()) {
        configProperties.load(FileInputStream(file("../config.properties")))

        signBuild = (configProperties["SIGN_BUILD"] as String).toBoolean()
    }

    if (signBuild) {
        signingConfigs {
            create("release") {

                keyAlias = configProperties["GUARDIAN_RELEASE_KEY_ALIAS"] as String
                keyPassword = configProperties["GUARDIAN_RELEASE_STORE_PASSWORD"] as String
                storeFile = file("approver_keystore.jks")
                storePassword = configProperties["GUARDIAN_RELEASE_STORE_PASSWORD"] as String
            }
        }
    }


    defaultConfig {
        applicationId = "co.censo.approver"
        minSdk = 33
        targetSdk = 33
        versionCode = 20
        versionName = "$versionNameMajor.$versionNameMinor.$versionNamePatch"

        signingConfig = if (signBuild) {
            signingConfigs.getByName("release")
        } else {
            signingConfigs.getByName("debug")
        }

        manifestPlaceholders["STRONGBOX_ENABLED"] = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://api.censo.co/\"")
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            manifestPlaceholders["URL_SCHEME"] = "censo"
            resValue("string", "app_name", "Approver")
        }
        create("staging") {
            resValue("string", "app_name", "Staging Approver")
            buildConfigField("String", "BASE_URL", "\"https://staging.censo.dev/\"")
            resValue("string", "RAYGUN_APP_ID", "\"CtOnGQjIo1U8dELkoUf0iw\"")
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            applicationIdSuffix = ".staging"
            isDebuggable = false
            manifestPlaceholders["URL_SCHEME"] = "censo-staging"
        }
        create("integration") {
            resValue("string", "app_name", "Integration Approver")
            buildConfigField("String", "BASE_URL", "\"https://integration.censo.dev/\"")
            resValue("string", "RAYGUN_APP_ID", "\"L9T2bPaEjr3Lede3SNpFJw\"")
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            applicationIdSuffix = ".integration"
            isDebuggable = false
            manifestPlaceholders["URL_SCHEME"] = "censo-integration"
        }
        debug {
            initWith(getByName("integration"))
            resValue("string", "app_name", "Debug Approver")
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
            excludes += "/META-INF/*"
        }
    }
}

dependencies {
    implementation(project(":shared"))

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.4")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")

    //KotlinX Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    //noinspection GradleDependency
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.2")
    //noinspection GradleDependency
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")

    //Dagger - Hilt
    implementation("com.google.dagger:hilt-android:2.44")
    kapt("com.google.dagger:hilt-android-compiler:2.44")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")

    //Biometrics
    implementation("androidx.biometric:biometric:1.1.0")

    //Raygun crash reporting
    implementation("com.raygun:raygun4android:4.0.1")

    //Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:32.2.3"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")

    //Push Notifications
    implementation("com.google.firebase:firebase-messaging-ktx")


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
    implementation("io.github.novacrypto:Base58:2022.01.17")

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