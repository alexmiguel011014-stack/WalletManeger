import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.allufinancas"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.allufinancas"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-demo"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

androidComponents {
    beforeVariants(selector().withBuildType("release")) {
        it.enable = false
    }
}

val verifySdkLevels = tasks.register("verifySdkLevels") {
    doLast {
        val compile = android.compileSdk
        val target = android.defaultConfig.targetSdk
        check((compile ?: 0) >= 36 && (target ?: 0) >= 36) {
            "ALLU Finanças requires compileSdk and targetSdk >= 36 (found $compile/$target)"
        }
        println("Android SDK: compile=$compile, target=$target, min=${android.defaultConfig.minSdk}")
    }
}

tasks.named("preBuild") {
    dependsOn(verifySdkLevels)
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    debugImplementation(libs.compose.ui.tooling)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.test.espresso.core)
}
