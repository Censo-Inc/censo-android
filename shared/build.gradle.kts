import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("com.adarshr.test-logger")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val versionNameMajor = 1
val versionNameMinor = 8
val versionNamePatch = 0

fun createBuildConfigStringArray(strings: List<String>) : String {
    val builder = StringBuilder()
    builder.append("{")
    for (string in strings) {
        builder.append('"')
        builder.append(string);
        builder.append('"')
        builder.append(", ")
    }
    builder.setLength(builder.length - 2)
    builder.append("}")
    return builder.toString()
}

android {
    namespace = "co.censo.shared"
    compileSdk = 34

    val configProperties = Properties()

    if (file("../config.properties").exists()) {
        configProperties.load(FileInputStream(file("../config.properties")))
    }

    defaultConfig {
        minSdk = 33

        val versionName = "$versionNameMajor.$versionNameMinor.$versionNamePatch"
        val versionCode = 1

        buildConfigField("String", "VERSION_NAME", "\"$versionName\"")
        buildConfigField("String", "VERSION_CODE", "\"${versionCode}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    val googleAuthClientIds = (configProperties["GOOGLE_AUTH_CLIENT_IDS"] as String).split("#")
    val googleAuthClientIdsArrayRepresentation = createBuildConfigStringArray(googleAuthClientIds)

    val googleAuthServerId = configProperties["GOOGLE_AUTH_SERVER_CLIENT_ID"] as String

    buildTypes {
        release {
            isMinifyEnabled = false
            buildConfigField("String", "BASE_URL", "\"https://api.censo.co/\"")
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            buildConfigField("String[]", "GOOGLE_AUTH_CLIENT_IDS", googleAuthClientIdsArrayRepresentation)
            buildConfigField("String", "GOOGLE_AUTH_SERVER_ID", "\"$googleAuthServerId\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "ENVIRONMENT", "\"prod\"")
            buildConfigField("String", "APPROVER_URL_SCHEME", "\"censo\"")
            buildConfigField("String", "OWNER_URL_SCHEME", "\"censo-main\"")
            buildConfigField("boolean", "PLAY_INTEGRITY_ENABLED", "true")
        }
        create("localRelease") {
            initWith(getByName("release"))
        }
        create("staging") {
            buildConfigField("String", "BASE_URL", "\"https://staging.censo.dev/\"")
            buildConfigField("String[]", "GOOGLE_AUTH_CLIENT_IDS", googleAuthClientIdsArrayRepresentation)
            buildConfigField("String", "GOOGLE_AUTH_SERVER_ID", "\"$googleAuthServerId\"")
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            buildConfigField("String", "ENVIRONMENT", "\"staging\"")
            buildConfigField("String", "APPROVER_URL_SCHEME", "\"censo-staging\"")
            buildConfigField("String", "OWNER_URL_SCHEME", "\"censo-main-staging\"")
            buildConfigField("boolean", "PLAY_INTEGRITY_ENABLED", "true")
        }
        create("integration") {
            buildConfigField("String", "BASE_URL", "\"https://integration.censo.dev/\"")
            buildConfigField("String[]", "GOOGLE_AUTH_CLIENT_IDS", googleAuthClientIdsArrayRepresentation)
            buildConfigField("String", "GOOGLE_AUTH_SERVER_ID", "\"$googleAuthServerId\"")
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            buildConfigField("String", "ENVIRONMENT", "\"integration\"")
            buildConfigField("String", "APPROVER_URL_SCHEME", "\"censo-integration\"")
            buildConfigField("String", "OWNER_URL_SCHEME", "\"censo-main-integration\"")
            buildConfigField("boolean", "PLAY_INTEGRITY_ENABLED", "true")
        }
        debug {
            initWith(getByName("integration"))
            manifestPlaceholders["STRONGBOX_ENABLED"] = false
            buildConfigField("boolean", "STRONGBOX_ENABLED", "false")
            buildConfigField("boolean", "PLAY_INTEGRITY_ENABLED", "false")
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
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/*"
        }
    }
}

dependencies {

    api("androidx.core:core-ktx:1.12.0")
    api("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    api("androidx.activity:activity-compose:1.8.0")
    api(platform("androidx.compose:compose-bom:2023.03.00"))
    api("androidx.compose.ui:ui")
    api("androidx.compose.ui:ui-graphics")
    api("androidx.compose.ui:ui-tooling-preview")
    api("androidx.compose.ui:ui-tooling")
    api("androidx.compose.material3:material3")
    api("androidx.navigation:navigation-compose:2.7.5")
    api("androidx.compose.material:material-icons-extended:1.5.4")

    //KotlinX Serialization
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // Retrofit
    api("com.squareup.retrofit2:retrofit:2.9.0")
    api("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    //noinspection GradleDependency
    api("com.squareup.okhttp3:okhttp:5.0.0-alpha.2")
    //noinspection GradleDependency
    api("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")

    //Google API Java Client
    implementation("com.google.api-client:google-api-client:2.2.0")

    //Google Auth
    api("com.google.android.gms:play-services-auth:20.7.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")

    //GoogleAuth (utilizes the One Tap dependency above)
    implementation("com.google.auth:google-auth-library-oauth2-http:1.20.0")
    implementation("com.google.api-client:google-api-client-android:2.0.0")

    //Biometrics
    api("androidx.biometric:biometric:1.1.0")

    //Push Notifications
    api(platform("com.google.firebase:firebase-bom:32.2.3"))
    api("com.google.firebase:firebase-messaging-ktx")

    //GoogleDrive
    api("com.google.android.gms:play-services-drive:17.0.0")
    api("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0") {
        exclude("org.apache.httpcomponents")
        exclude(module = "guava-jdk5")
    }

    //auth0 JWT
    implementation("com.auth0.android:jwtdecode:2.0.1")

    //Encrypted Preferences
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")

    //Sentry
    implementation("io.sentry:sentry-android:6.33.0")

    //Dagger - Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
    kapt("androidx.hilt:hilt-compiler:1.1.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Play integrity
    implementation("com.google.android.play:integrity:1.3.0")

    // Base58
    api("org.bouncycastle:bcprov-jdk15to18:1.70")
    api("io.github.novacrypto:Base58:2022.01.17")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.mockito:mockito-inline:4.8.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
}
