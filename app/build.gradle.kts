

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
}

dependencies {
    val work_version = "2.9.0"
    // (Java only)
    implementation("androidx.work:work-runtime:$work_version")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.gridlayout:gridlayout:1.0.0")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation ("com.google.code.gson:gson:2.8.7")              //JSON 파싱, 생성 오픈소스 라이브러리 , GOOGLE

    implementation(files("libs/mraa-2.2.0.aar"))            // MRAA library , GPIO ,I2C , SPI , 라이브러리 폴더에 파일 Mraa.arr 파일 추가 해야함

    testImplementation("junit:junit:4.13.2")

}