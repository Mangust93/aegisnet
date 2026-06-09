plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localLibboxAar = rootProject.layout.projectDirectory.file("local-libs/libbox.aar").asFile
val localSfaLibboxClassesJar = rootProject.layout.projectDirectory.file("local-libs/sfa-libbox/classes.jar").asFile

android {
    namespace = "net.aegisnet.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.aegisnet.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            if (!localSfaLibboxClassesJar.isFile) {
                java.srcDir(rootProject.layout.projectDirectory.dir("local-libs/sfa-libbox/java"))
            }
            jniLibs.srcDir(rootProject.layout.projectDirectory.dir("local-libs/sfa-libbox/jniLibs"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    if (localLibboxAar.isFile) {
        implementation(files(localLibboxAar))
    }
    if (localSfaLibboxClassesJar.isFile) {
        implementation(files(localSfaLibboxClassesJar))
    }

    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
