import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val liteRtLmVersion = "0.17.1"

android {
    namespace = "org.chaitu.chatbotpro"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.chaitu.chatbotpro"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "LITERT_LM_RUNTIME_VERSION", "\"$liteRtLmVersion\"")
        buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"github\"")
        manifestPlaceholders["appLabel"] = "ChatBotPro"
    }

    // No product flavors: standard debug and release builds only.

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
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    lint {
        // AGP 8.7's lint bundles an older Kotlin analysis API than Kotlin 2.4.
        // This AndroidX detector crashes on the mismatch; the rest of lint still runs.
        disable += "NullSafeMutableLiveData"
    }
}


val sherpaOnnxSource by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val onnxRuntimeSource by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val strippedSherpaAar = layout.buildDirectory.file(
    "generated/native-integrity/sherpa-onnx-static-link-onnxruntime-1.13.4-stripped.aar"
)

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.onnxruntime.android)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation (libs.json.json)
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")
    implementation("io.noties.markwon:ext-latex:4.6.2")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation ("com.google.android.material:material:1.12.0")
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    // Google ML Kit on-device OCR.
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.ai.edge.litertlm:litertlm-android:$liteRtLmVersion")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    add(sherpaOnnxSource.name, "com.k2fsa.sherpa.onnx:sherpa-onnx-static-link-onnxruntime:1.13.4@aar")
    add(
        onnxRuntimeSource.name,
        "com.microsoft.onnxruntime:onnxruntime-android:${libs.versions.onnxruntimeAndroid.get()}@aar"
    )
}

val verifyOnnxRuntimeNativeCompatibility by tasks.registering {
    group = "verification"
    description = "Verifies the ONNX Runtime/sherpa ABI contract and removes sherpa's x86 duplicate."
    inputs.files(sherpaOnnxSource, onnxRuntimeSource)
    outputs.file(strippedSherpaAar)

    doLast {
        fun entry(archive: File, path: String): ByteArray? = ZipFile(archive).use { zip ->
            zip.getEntry(path)?.let { zipEntry -> zip.getInputStream(zipEntry).use { it.readBytes() } }
        }

        fun sha256(bytes: ByteArray): String = MessageDigest
            .getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

        val onnxVersion = libs.versions.onnxruntimeAndroid.get()
        val onnxAar = onnxRuntimeSource.singleFile
        val sherpaAar = sherpaOnnxSource.singleFile
        val abis = listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")

        abis.forEach { abi ->
            check(entry(onnxAar, "jni/$abi/libonnxruntime.so") != null) {
                "ONNX Runtime $onnxVersion is missing libonnxruntime.so for $abi."
            }
            check(entry(onnxAar, "jni/$abi/libonnxruntime4j_jni.so") != null) {
                "ONNX Runtime $onnxVersion is missing libonnxruntime4j_jni.so for $abi."
            }
            check(entry(sherpaAar, "jni/$abi/libsherpa-onnx-jni.so") != null) {
                "sherpa-onnx 1.13.4 is missing libsherpa-onnx-jni.so for $abi."
            }
        }

        val sherpaX86Runtime = checkNotNull(entry(sherpaAar, "jni/x86/libonnxruntime.so")) {
            "sherpa-onnx 1.13.4 requires its shared ONNX Runtime library on x86."
        }
        val directX86Runtime = checkNotNull(entry(onnxAar, "jni/x86/libonnxruntime.so"))
        val sherpaHash = sha256(sherpaX86Runtime)
        val directHash = sha256(directX86Runtime)
        check(sherpaX86Runtime.toString(Charsets.ISO_8859_1).contains(onnxVersion)) {
            "sherpa-onnx 1.13.4 x86 was not built for ONNX Runtime $onnxVersion."
        }
        val sherpaX86Jni = checkNotNull(entry(sherpaAar, "jni/x86/libsherpa-onnx-jni.so"))
        check(sherpaX86Jni.toString(Charsets.ISO_8859_1).contains("libonnxruntime.so")) {
            "sherpa-onnx 1.13.4 x86 no longer declares the shared ONNX Runtime dependency."
        }

        abis.filterNot { it == "x86" }.forEach { abi ->
            check(entry(sherpaAar, "jni/$abi/libonnxruntime.so") == null) {
                "sherpa-onnx 1.13.4 unexpectedly changed its static-runtime packaging for $abi."
            }
        }

        val output = strippedSherpaAar.get().asFile
        output.parentFile.mkdirs()
        val temporaryOutput = File(output.parentFile, output.name + ".tmp")
        ZipInputStream(FileInputStream(sherpaAar)).use { input ->
            ZipOutputStream(FileOutputStream(temporaryOutput)).use { zipOutput ->
                while (true) {
                    val zipEntry = input.nextEntry ?: break
                    if (zipEntry.name != "jni/x86/libonnxruntime.so") {
                        zipOutput.putNextEntry(ZipEntry(zipEntry.name))
                        input.copyTo(zipOutput)
                        zipOutput.closeEntry()
                    }
                    input.closeEntry()
                }
            }
        }
        Files.move(
            temporaryOutput.toPath(),
            output.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        )
        check(entry(output, "jni/x86/libonnxruntime.so") == null) {
            "The generated sherpa AAR still contains the x86 ONNX Runtime duplicate."
        }
        logger.lifecycle(
            "Selected official ONNX Runtime $onnxVersion for x86; " +
                "direct SHA-256=$directHash, sherpa build-time copy SHA-256=$sherpaHash"
        )
    }
}

val strippedSherpaAarFiles = files(strippedSherpaAar).builtBy(verifyOnnxRuntimeNativeCompatibility)

dependencies {
    implementation(strippedSherpaAarFiles)
}

tasks.named("preBuild").configure {
    dependsOn(verifyOnnxRuntimeNativeCompatibility)
}

// Android Studio may request this legacy Kotlin model task during sync. AGP 9
// provides Kotlin support directly, so keep this as a no-op compatibility task.
tasks.register("prepareKotlinBuildScriptModel")

// The in-app Privacy Policy and Terms of Use read their contact details from legal_contact.xml.
val legalContactFile = layout.projectDirectory.file("src/main/res/values/legal_contact.xml")
val verifyLegalContactFilled by tasks.registering {
    group = "verification"
    description = "Fails release builds while legal contact details are still TODO."
    inputs.file(legalContactFile)
    doLast {
        val text = legalContactFile.asFile.readText()
        check(!Regex(">\\s*TODO\\s*<").containsMatchIn(text)) {
            "Fill in src/main/res/values/legal_contact.xml before building a release."
        }
    }
}

tasks.matching { it.name.startsWith("pre") && it.name.endsWith("ReleaseBuild") }.configureEach {
    dependsOn(verifyLegalContactFilled)
}
