plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ekhonavigator.core.model"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
