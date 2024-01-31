// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.version) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.app.distribution) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.play.triplet) apply false
}

buildscript {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.hilt.android.gradle.plugin)
        classpath(libs.gradle.test.logger.plugin)
        classpath(libs.firebase.appdistribution.gradle)
        classpath(libs.sentry.android.gradle.plugin)
    }
}