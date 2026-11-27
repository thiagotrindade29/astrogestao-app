plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
<<<<<<< HEAD
=======
    alias(libs.plugins.ksp)
>>>>>>> origin/main
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
<<<<<<< HEAD
        applicationId = "com.aistudio.inventorycontrol.xktvmq"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
=======
        applicationId = "com.aistudio.astrogestao.kxmpzq"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
>>>>>>> origin/main
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
<<<<<<< HEAD
        create("release") {
=======
        getByName("debug") {
>>>>>>> origin/main
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
<<<<<<< HEAD
            signingConfig = signingConfigs.getByName("release")
=======
>>>>>>> origin/main
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
<<<<<<< HEAD
=======
            signingConfig = signingConfigs.getByName("debug")
>>>>>>> origin/main
        }
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }

    compileOptions {
<<<<<<< HEAD
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
=======
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
>>>>>>> origin/main
    }

    buildFeatures {
        compose = true
<<<<<<< HEAD
=======
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
>>>>>>> origin/main
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
<<<<<<< HEAD
=======

>>>>>>> origin/main
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

<<<<<<< HEAD
=======
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.mlkit.vision)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.mlkit.text.recognition)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

>>>>>>> origin/main
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.play.services)

<<<<<<< HEAD
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.mlkit.text.recognition)

    debugImplementation(libs.androidx.compose.ui.tooling)
=======
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
>>>>>>> origin/main
}
