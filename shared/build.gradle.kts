import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.version)
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id(libs.plugins.test.logger.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
}

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

        val versionName = "${libs.versions.versionNameMajor.get()}.${libs.versions.versionNameMinor.get()}.${libs.versions.versionNamePatch.get()}"
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
        jvmTarget = libs.versions.jvm.get()
        freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
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

    api(libs.androidx.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.activity.compose)
    api(platform("androidx.compose:compose-bom:2023.03.00"))
    api("androidx.compose.ui:ui")
    api("androidx.compose.ui:ui-graphics")
    api("androidx.compose.ui:ui-tooling-preview")
    api("androidx.compose.ui:ui-tooling")
    api("androidx.compose.material3:material3")
    api(libs.androidx.navigation.compose)
    api(libs.androidx.material.icons.extended)

    //KotlinX Serialization
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)

    // Retrofit
    api(libs.retrofit)
    api(libs.retrofit2.kotlinx.serialization.converter)

    //noinspection GradleDependency
    api(libs.okhttp)
    //noinspection GradleDependency
    api(libs.logging.interceptor)

    //Google API Java Client
    implementation(libs.google.api.client)

    //Google Auth
    api(libs.play.services.auth)
    api(libs.kotlinx.coroutines.play.services)

    //GoogleAuth (utilizes the One Tap dependency above)
    implementation(libs.google.auth.library.oauth2.http)
    implementation(libs.google.api.client.android)

    //Biometrics
    api(libs.androidx.biometric)

    //Push Notifications
    api(platform("com.google.firebase:firebase-bom:32.2.3"))
    api("com.google.firebase:firebase-messaging-ktx")

    //GoogleDrive
    api(libs.play.services.drive)
    api("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0") {
        exclude("org.apache.httpcomponents")
        exclude(module = "guava-jdk5")
    }

    //auth0 JWT
    implementation(libs.jwtdecode)

    //Encrypted Preferences
    implementation(libs.androidx.security.crypto.ktx)

    //Sentry
    implementation(libs.sentry.android)

    //Dagger - Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    kapt(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Play integrity
    implementation(libs.integrity)

    // Base58
    api(libs.bcprov.jdk15to18)
    api(libs.base58)

    //CameraX
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-camera2:$cameraxVersion")

    //Compose Accompanist
    implementation("com.google.accompanist:accompanist-permissions:0.18.0")

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.kotlinx.coroutines.test)
}
