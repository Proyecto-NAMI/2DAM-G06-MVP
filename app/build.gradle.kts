import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val firebaseLocalProperties =
    Properties().apply {
        val configFile = rootProject.file("firebase.local.properties")
        if (configFile.exists()) {
            configFile.inputStream().use(::load)
        }
    }

fun firebaseProperty(
    name: String,
    defaultValue: String = "",
): String =
    providers.gradleProperty(name).orNull
        ?: firebaseLocalProperties.getProperty(name)
        ?: defaultValue

fun buildConfigString(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

fun firebasePort(
    name: String,
    defaultValue: Int,
): Int = firebaseProperty(name, defaultValue.toString()).toIntOrNull() ?: defaultValue

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kover)
}

// RNF-08: enforce 70% line coverage for every domain package, including the
// shared rules in core.domain introduced by T-06.
kover {
    reports {
        filters {
            includes {
                packages("com.proyectonami.app.feature.*.domain")
                packages("com.proyectonami.app.core.domain.*")
            }
        }
        verify {
            rule {
                groupBy.set(kotlinx.kover.gradle.plugin.dsl.GroupingEntityType.PACKAGE)
                minBound(70)
            }
        }
    }
}

android {
    namespace = "com.proyectonami.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.proyectonami.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "USE_FIREBASE_EMULATORS", "true")
            buildConfigField(
                "String",
                "FIREBASE_PROJECT_ID",
                buildConfigString(firebaseProperty("NAMI_FIREBASE_PROJECT_ID", "demo-nami")),
            )
            buildConfigField(
                "String",
                "FIREBASE_APP_ID",
                buildConfigString(
                    firebaseProperty(
                        "NAMI_FIREBASE_APP_ID",
                        "1:1234567890:android:0000000000000000000000",
                    ),
                ),
            )
            buildConfigField(
                "String",
                "FIREBASE_API_KEY",
                buildConfigString(firebaseProperty("NAMI_FIREBASE_API_KEY", "AIzaSyDUMMY0123456789012345678901234567")),
            )
            buildConfigField(
                "String",
                "FIREBASE_STORAGE_BUCKET",
                buildConfigString(
                    firebaseProperty("NAMI_FIREBASE_STORAGE_BUCKET", "demo-nami.appspot.com"),
                ),
            )
            buildConfigField(
                "String",
                "FIREBASE_EMULATOR_HOST",
                buildConfigString(
                    firebaseProperty("NAMI_FIREBASE_EMULATOR_HOST", "10.0.2.2"),
                ),
            )
            buildConfigField(
                "int",
                "FIREBASE_AUTH_EMULATOR_PORT",
                firebasePort("NAMI_FIREBASE_AUTH_EMULATOR_PORT", 9099).toString(),
            )
            buildConfigField(
                "int",
                "FIREBASE_FIRESTORE_EMULATOR_PORT",
                firebasePort("NAMI_FIREBASE_FIRESTORE_EMULATOR_PORT", 8080).toString(),
            )
            buildConfigField(
                "int",
                "FIREBASE_FUNCTIONS_EMULATOR_PORT",
                firebasePort("NAMI_FIREBASE_FUNCTIONS_EMULATOR_PORT", 5001).toString(),
            )
            buildConfigField(
                "int",
                "FIREBASE_STORAGE_EMULATOR_PORT",
                firebasePort("NAMI_FIREBASE_STORAGE_EMULATOR_PORT", 9199).toString(),
            )
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "USE_FIREBASE_EMULATORS", "false")
            buildConfigField(
                "String",
                "FIREBASE_PROJECT_ID",
                buildConfigString(firebaseProperty("NAMI_FIREBASE_PROJECT_ID")),
            )
            buildConfigField(
                "String",
                "FIREBASE_APP_ID",
                buildConfigString(firebaseProperty("NAMI_FIREBASE_APP_ID")),
            )
            buildConfigField(
                "String",
                "FIREBASE_API_KEY",
                buildConfigString(firebaseProperty("NAMI_FIREBASE_API_KEY")),
            )
            buildConfigField(
                "String",
                "FIREBASE_STORAGE_BUCKET",
                buildConfigString(firebaseProperty("NAMI_FIREBASE_STORAGE_BUCKET")),
            )
            buildConfigField("String", "FIREBASE_EMULATOR_HOST", buildConfigString(""))
            buildConfigField("int", "FIREBASE_AUTH_EMULATOR_PORT", "0")
            buildConfigField("int", "FIREBASE_FIRESTORE_EMULATOR_PORT", "0")
            buildConfigField("int", "FIREBASE_FUNCTIONS_EMULATOR_PORT", "0")
            buildConfigField("int", "FIREBASE_STORAGE_EMULATOR_PORT", "0")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    releaseImplementation(libs.firebase.appcheck.playintegrity)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
