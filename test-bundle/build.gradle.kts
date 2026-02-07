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
            mainClass = "testbundle.bundle1.Main"
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
            implementation(libs.bundle.updater)
            implementation(libs.kotlinx.coroutinesSwing)
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

// Stage separate input directories so each bundle has distinct content
val stageBundle1 = tasks.register<Copy>("stageBundle1") {
    dependsOn("jvmJar")
    from(layout.buildDirectory.dir("libs"))
    into(layout.buildDirectory.dir("bundle-staging/1"))
    doLast {
        File(destinationDir, "bundle-version.txt").writeText("1")
    }
}

val stageBundle2 = tasks.register<Copy>("stageBundle2") {
    dependsOn("jvmJar")
    from(layout.buildDirectory.dir("libs"))
    into(layout.buildDirectory.dir("bundle-staging/2"))
    doLast {
        File(destinationDir, "bundle-version.txt").writeText("2")
    }
}

tasks.register<BundleCreatorTask>("createBundle1") {
    group = "bundle"
    description = "Creates signed bundle version 1"

    dependsOn(stageBundle1)

    if (!privKeyFile.exists()) {
        throw GradleException("Private key not found. Run './gradlew :test-bundle:generateKeys' first.")
    }

    inputDirectory.set(layout.buildDirectory.dir("bundle-staging/1"))
    outputDirectory.set(file("${rootDir}/bundles/1"))
    platforms.set(listOf("macos-arm64", "macos-x64"))
    buildNumber.set(1L)
    mainClass.set("testbundle.bundle1.Main")
    minShellVersion.set(1)
    privateKeyFile.set(privKeyFile)

    doFirst {
        println("Public key for AppConfig: ${pubKeyFile.readText()}")
    }
}

tasks.register<BundleCreatorTask>("createBundle2") {
    group = "bundle"
    description = "Creates signed bundle version 2"

    dependsOn(stageBundle2)

    if (!privKeyFile.exists()) {
        throw GradleException("Private key not found. Run './gradlew :test-bundle:generateKeys' first.")
    }

    inputDirectory.set(layout.buildDirectory.dir("bundle-staging/2"))
    outputDirectory.set(file("${rootDir}/bundles/2"))
    platforms.set(listOf("macos-arm64", "macos-x64"))
    buildNumber.set(2L)
    mainClass.set("testbundle.bundle2.Main")
    minShellVersion.set(1)
    privateKeyFile.set(privKeyFile)

    doFirst {
        println("Public key for AppConfig: ${pubKeyFile.readText()}")
    }
}

tasks.register("createBundles") {
    group = "bundle"
    description = "Creates both signed bundle versions"
    dependsOn("createBundle1", "createBundle2")
}
