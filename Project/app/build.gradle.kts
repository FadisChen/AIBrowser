plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.aireader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.aireader"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    
    // 解決META-INF文件衝突問題
    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

configurations.all {
    resolutionStrategy {
        // 強制所有模塊使用相同版本的依賴庫
        force("androidx.core:core:1.12.0")
        force("androidx.core:core-ktx:1.12.0")
        
        // 排除使用舊版Support庫
        exclude(group = "com.android.support", module = "support-v4")
        exclude(group = "com.android.support", module = "support-compat")
        exclude(group = "com.android.support", module = "support-media-compat")
        exclude(group = "com.android.support", module = "support-core-utils")
        exclude(group = "com.android.support", module = "support-core-ui")
        exclude(group = "com.android.support", module = "support-fragment")
        exclude(group = "com.android.support", module = "support-annotations")
    }
}

dependencies {
    // 基本Android依賴
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // 協程支持
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Google AI服務 - Gemini
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    
    // WebView增強工具
    implementation("androidx.webkit:webkit:1.7.0")
    
    // UI組件 - ConstraintLayout, CardView
    implementation("androidx.cardview:cardview:1.0.0")
    
    // 單元測試
    testImplementation("junit:junit:4.13.2")
    
    // Android測試
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("org.json:json:20240303")
    
    // Gson for JSON handling
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Retrofit for API calls (可選)
    // implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}