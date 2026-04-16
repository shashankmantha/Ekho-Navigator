plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ekhonavigator.core.testing"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(projects.core.model)
    api(projects.core.data)
    
    implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-auth")

    api(libs.junit)
    api(kotlin("test"))
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
