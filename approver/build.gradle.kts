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
}

android {
    namespace = "co.censo.approver"
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
        versionCode = 50
        versionName = "${libs.versions.versionNameMajor.get()}.${libs.versions.versionNameMinor.get()}.${libs.versions.versionNamePatch.get()}"

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
            manifestPlaceholders["SENTRY_ID"] = sentryProdId
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            manifestPlaceholders["ENVIRONMENT"] = "prod"
            manifestPlaceholders["APPROVER_URL_SCHEME"] = "censo"
            manifestPlaceholders["LINK_HOST"] = "link-${manifestPlaceholders["ENVIRONMENT"]}.censo.dev"
            manifestPlaceholders["L1NK_HOST"] = "l1nk-${manifestPlaceholders["ENVIRONMENT"]}.censo.dev"
            buildConfigField("String", "LINK_HOST", "\"link-${manifestPlaceholders["ENVIRONMENT"]}.censo.dev\"")
            buildConfigField("String", "L1NK_HOST", "\"l1nk-${manifestPlaceholders["ENVIRONMENT"]}.censo.dev\"")
            resValue("string", "app_name", "Approver")
        }
        create("localRelease") {
            resValue("string", "app_name", "Local Release Approver")
            initWith(getByName("release"))
            manifestPlaceholders["SENTRY_ID"] = sentryDevId
        }
        create("staging") {
            resValue("string", "app_name", "Staging Approver")
            buildConfigField("String", "BASE_URL", "\"https://staging.censo.dev/\"")
            manifestPlaceholders["SENTRY_ID"] = sentryDevId
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            applicationIdSuffix = ".staging"
            isDebuggable = false
            manifestPlaceholders["ENVIRONMENT"] = "staging"
            manifestPlaceholders["APPROVER_URL_SCHEME"] = "censo-${manifestPlaceholders["ENVIRONMENT"]}"
            manifestPlaceholders["LINK_HOST"] = "link-${manifestPlaceholders["ENVIRONMENT"]}.censo.dev"
            manifestPlaceholders["L1NK_HOST"] = "l1nk-${manifestPlaceholders["ENVIRONMENT"]}.censo.dev"
            buildConfigField("String", "LINK_HOST", "\"link-${manifestPlaceholders["ENVIRONMENT"]}.censo.dev\"")
            buildConfigField("String", "L1NK_HOST", "\"l1nk-${manifestPlaceholders["ENVIRONMENT"]}.censo.dev\"")
        }
        create("integration") {
            resValue("string", "app_name", "Integration Approver")
            buildConfigField("String", "BASE_URL", "\"https://integration.censo.dev/\"")
            manifestPlaceholders["SENTRY_ID"] = sentryDevId
            buildConfigField("boolean", "STRONGBOX_ENABLED", "true")
            applicationIdSuffix = ".integration"
            isDebuggable = false
            manifestPlaceholders["ENVIRONMENT"] = "integration"
            manifestPlaceholders["APPROVER_URL_SCHEME"] = "censo-${manifestPlaceholders["ENVIRONMENT"]}"
            manifestPlaceholders["LINK_HOST"] = "link-${manifestPlaceholders["ENVIRONMENT"]}.censo.dev"
            manifestPlaceholders["L1NK_HOST"] = "l1nk-${manifestPlaceholders["ENVIRONMENT"]}.censo.dev"
            buildConfigField("String", "LINK_HOST", "\"link-${manifestPlaceholders["ENVIRONMENT"]}.censo.dev\"")
            buildConfigField("String", "L1NK_HOST", "\"l1nk-${manifestPlaceholders["ENVIRONMENT"]}.censo.dev\"")
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

    // Play Integrity
    implementation(libs.integrity)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.kotlinx.coroutines.test)
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
