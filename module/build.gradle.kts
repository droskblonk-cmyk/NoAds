plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.sevincfurkan.noads"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.sevincfurkan.noads"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// Real ad SDK binaries that SdkContractTest checks the hooks against. They are only read as class files,
// never packaged into the module: at runtime the module reaches each SDK through the hooked app.
fun Configuration.sdkBinaries() {
    isCanBeConsumed = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

val sdkContracts: Configuration by configurations.creating { sdkBinaries() }
val legacySdkContracts: Configuration by configurations.creating { sdkBinaries() }

dependencies {
    compileOnly(libs.xposed.api)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference)
    implementation(libs.material)

    testImplementation(libs.junit)
    testImplementation(libs.asm)
    testImplementation(libs.xposed.api)

    sdkContracts(libs.sdk.admob)
    sdkContracts(libs.sdk.applovin)
    sdkContracts(libs.sdk.unityads)
    sdkContracts(libs.sdk.levelplay)
    legacySdkContracts(libs.sdk.levelplay.legacy)
}

tasks.withType<Test>().configureEach {
    val current = files(sdkContracts)
    val legacy = files(legacySdkContracts)
    inputs.files(current).withPropertyName("sdkContracts").withNormalizer(ClasspathNormalizer::class)
    inputs.files(legacy).withPropertyName("legacySdkContracts").withNormalizer(ClasspathNormalizer::class)
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Dnoads.sdkJars=${current.asPath}", "-Dnoads.legacySdkJars=${legacy.asPath}")
    }
}
