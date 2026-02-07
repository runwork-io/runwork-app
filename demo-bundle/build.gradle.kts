import io.runwork.bundle.gradle.BundleCreatorTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

buildscript {
    repositories {
        mavenCentral()
        maven("https://maven.runwork.io/releases")
    }
    dependencies {
        classpath(libs.bundle.creator.gradle.task)
    }
}

kotlin {
    jvm {
        mainRun {
            mainClass = "demo.Main"
        }
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
                }
            }
        }
    }

    sourceSets {
        jvmMain.dependencies {
        }
    }
}

// Generate key pair once and store in files for consistent signing
val keysDir = file("keys")
val privKeyFile = file("keys/private.key")
val pubKeyFile = file("keys/public.key")

tasks.register("generateKeys") {
    group = "bundle"
    description = "Generates a new Ed25519 key pair for bundle signing"

    outputs.files(privKeyFile, pubKeyFile)

    doLast {
        keysDir.mkdirs()
        val keyPair = BundleCreatorTask.generateKeyPair()
        privKeyFile.writeText(keyPair.first)
        pubKeyFile.writeText(keyPair.second)
        println("Generated new key pair:")
        println("  Private key: ${privKeyFile.absolutePath}")
        println("  Public key: ${pubKeyFile.absolutePath}")
        println("\nPublic key for AppConfig.kt:")
        println("  ${keyPair.second}")
    }
}

tasks.register<BundleCreatorTask>("createBundle") {
    group = "bundle"
    description = "Creates a signed bundle for distribution"

    dependsOn("jvmJar")

    // Ensure keys exist
    if (!privKeyFile.exists()) {
        throw GradleException("Private key not found. Run './gradlew :demo-bundle:generateKeys' first.")
    }

    inputDirectory.set(layout.buildDirectory.dir("libs"))
    outputDirectory.set(file(System.getProperty("user.home") + "/.moscow-demo-bundle"))
    platforms.set(listOf("macos-arm64", "macos-x64"))
    buildNumber.set(1L)
    mainClass.set("demo.Main")
    minShellVersion.set(1)
    privateKeyFile.set(privKeyFile)

    doFirst {
        println("Public key for AppConfig: ${pubKeyFile.readText()}")
    }
}
