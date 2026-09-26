import java.util.Properties

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.kapt") }

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "in.localdukaan"
    compileSdk = 35

    defaultConfig {
        javaCompileOptions { annotationProcessorOptions { arguments["room.schemaLocation"] = "$projectDir/schemas" } }
        applicationId = "in.localdukaan"
        minSdk = 26
        targetSdk = 35
        versionCode = 15
        versionName = "1.12.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"${project.findProperty("LOCALDUKAAN_API_URL") ?: "https://localdukaan-api.premdeep336.workers.dev"}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(keystoreProps.getProperty("storeFile", "localdukaan-release.jks"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias", "localdukaan")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packagingOptions.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1"); implementation("androidx.activity:activity-compose:1.7.2"); implementation("androidx.compose.ui:ui:1.4.3"); implementation("androidx.compose.ui:ui-tooling-preview:1.4.3"); debugImplementation("androidx.compose.ui:ui-tooling:1.4.3");    implementation("androidx.compose.material3:material3:1.1.1"); implementation("androidx.compose.material:material-icons-extended:1.4.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1"); implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1"); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("androidx.camera:camera-camera2:1.2.3"); implementation("androidx.camera:camera-lifecycle:1.2.3"); implementation("androidx.camera:camera-view:1.2.3"); implementation("com.google.zxing:core:3.5.2"); implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("androidx.room:room-runtime:2.6.1"); implementation("androidx.room:room-ktx:2.6.1"); kapt("androidx.room:room-compiler:2.6.1"); implementation("androidx.work:work-runtime-ktx:2.8.1")
    testImplementation("junit:junit:4.13.2"); testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1"); testImplementation("androidx.test:core:1.5.0"); testImplementation("org.robolectric:robolectric:4.10.3")
}
