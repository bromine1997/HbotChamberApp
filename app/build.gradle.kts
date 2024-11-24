plugins {
    id("com.android.application")
}

android {
    namespace = "com.mcsl.hbotchamberapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mcsl.hbotchamberapp"
        minSdk = 30
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true // buildConfig 기능 활성화
    }

    buildTypes {
        debug {
            buildConfigField("String", "SERVER_ADDRESS", "\"192.168.0.125:8080\"")
        }
        release {
            buildConfigField("String", "SERVER_ADDRESS", "\"192.168.0.125:8080\"")
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
}

dependencies {
    val work_version = "2.9.0"
    // WorkManager
    implementation("androidx.work:work-runtime:$work_version")

    implementation ("io.socket:socket.io-client:2.1.0")



    // JWT decoding
    implementation("com.auth0.android:jwtdecode:2.0.0")
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    // Retrofit2
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson converter for Retrofit
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // UI libraries
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Gson library
    implementation("com.google.code.gson:gson:2.8.7")

    // MRAA library for GPIO, I2C, SPI
    implementation(files("libs/mraa-2.2.0.aar"))

    // Unit testing
    testImplementation("junit:junit:4.13.2")
}
