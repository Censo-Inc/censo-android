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

val versionNameMajor = 0
val versionNameMinor = 0
val versionNamePatch = 1

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
        }
        create("staging") {
            resValue("string", "app_name", "Staging Vault")
            buildConfigField("String", "BASE_URL", "\"https://staging.censo.dev/\"")
            buildConfigField("String[]", "GOOGLE_AUTH_CLIENT_IDS", googleAuthClientIdsArrayRepresentation)
            buildConfigField("String", "GOOGLE_AUTH_SERVER_ID", "\"$googleAuthServerId\"")
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
        }
        create("aintegration") {
            resValue("string", "app_name", "A Integration Vault")
            buildConfigField("String", "BASE_URL", "\"https://integration.censo.dev/\"")
            buildConfigField("String[]", "GOOGLE_AUTH_CLIENT_IDS", googleAuthClientIdsArrayRepresentation)
            buildConfigField("String", "GOOGLE_AUTH_SERVER_ID", "\"$googleAuthServerId\"")
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
        }
        create("bintegration") {
            initWith(getByName("aintegration"))
        }
        create("cintegration") {
            initWith(getByName("aintegration"))
        }
        create("dintegration") {
            initWith(getByName("aintegration"))
        }
        debug {
            initWith(getByName("aintegration"))
            manifestPlaceholders["STRONGBOX_ENABLED"] = false
            buildConfigField("boolean", "STRONGBOX_ENABLED", "false")
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

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-tooling")
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

    //Google API Java Client
    implementation("com.google.api-client:google-api-client:2.2.0")

    //One Tap
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    //GoogleAuth (utilizes the One Tap dependency above)
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation("com.google.api-client:google-api-client-android:2.0.0")

    //GoogleDrive
    implementation("com.google.android.gms:play-services-drive:17.0.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0") {
        exclude("org.apache.httpcomponents")
        exclude(module = "guava-jdk5")
    }

    //Push Notifications
    implementation(platform("com.google.firebase:firebase-bom:32.2.3"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    
    //auth0 JWT
    implementation("com.auth0.android:jwtdecode:2.0.1")

    //Encrypted Preferences
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")

    //Dagger - Hilt
    implementation("com.google.dagger:hilt-android:2.44")
    kapt("com.google.dagger:hilt-android-compiler:2.44")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")

    // Base58
    implementation("org.bouncycastle:bcprov-jdk15to18:1.70")
    implementation("io.github.novacrypto:Base58:2022.01.17")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.mockito:mockito-inline:4.8.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
}