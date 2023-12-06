plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.pankajgaming.attendanceapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pankajgaming.attendanceapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures{
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-database:20.3.0")
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.firebase:firebase-firestore-ktx:24.9.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("androidx.browser:browser:1.7.0")
    implementation ("com.google.android.play:integrity:1.3.0")
    // Sdp
    implementation("com.intuit.sdp:sdp-android:1.1.0")

    // Ssp
    implementation("com.intuit.ssp:ssp-android:1.1.0")

    // Gif
    implementation ("pl.droidsonroids.gif:android-gif-drawable:1.2.22")

    // PinView
    implementation ("io.github.chaosleung:pinview:1.4.4")

    // Signature pad
    implementation ("com.github.gcacace:signature-pad:1.3.1")

    // Glide
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")

    // Lottie Animantion
    implementation ("com.airbnb.android:lottie:6.0.0")

}