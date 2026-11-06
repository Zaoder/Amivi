plugins {
    id("com.android.application")
}

android {
    namespace = "gr.teo.amoivi"
    compileSdk = 34

    defaultConfig {
        applicationId = "gr.teo.amoivi"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Σταθερό κλειδί υπογραφής, ώστε οι νέες εκδόσεις να εγκαθίστανται πάνω από τις παλιές
    signingConfigs {
        create("fixed") {
            storeFile = file("amoivi.keystore")
            storePassword = "amoivi123"
            keyAlias = "amoivi"
            keyPassword = "amoivi123"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("fixed")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixed")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
