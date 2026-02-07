import io.runwork.bundle.gradle.BundleCreatorTask

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.bundle.creator.gradle.task)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.bundle.updater)
    implementation(libs.kotlinx.coroutinesSwing)
}

val privKeyFile = file("keys/private.key")
val pubKeyFile = file("keys/public.key")

// Stage separate input directories so each bundle has distinct content
val stageBundle1 = tasks.register<Copy>("stageBundle1") {
    dependsOn("jar")
    from(layout.buildDirectory.dir("libs"))
    into(layout.buildDirectory.dir("bundle-staging/1"))
    doLast {
        File(destinationDir, "bundle-version.txt").writeText("1")
    }
}

val stageBundle2 = tasks.register<Copy>("stageBundle2") {
    dependsOn("jar")
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
        throw GradleException("Private key not found at ${privKeyFile.absolutePath}")
    }

    inputDirectory.set(layout.buildDirectory.dir("bundle-staging/1"))
    outputDirectory.set(file("${rootDir}/bundles/1"))
    platforms.set(listOf("macos-arm64", "macos-x64"))
    buildNumber.set(1L)
    mainClass.set("testbundle.bundle1.Main")
    minShellVersion.set(1)
    privateKeyFile.set(privKeyFile)
}

tasks.register<BundleCreatorTask>("createBundle2") {
    group = "bundle"
    description = "Creates signed bundle version 2"

    dependsOn(stageBundle2)

    if (!privKeyFile.exists()) {
        throw GradleException("Private key not found at ${privKeyFile.absolutePath}")
    }

    inputDirectory.set(layout.buildDirectory.dir("bundle-staging/2"))
    outputDirectory.set(file("${rootDir}/bundles/2"))
    platforms.set(listOf("macos-arm64", "macos-x64"))
    buildNumber.set(2L)
    mainClass.set("testbundle.bundle2.Main")
    minShellVersion.set(1)
    privateKeyFile.set(privKeyFile)
}

tasks.register("createBundles") {
    group = "bundle"
    description = "Creates both signed bundle versions"
    dependsOn("createBundle1", "createBundle2")
}
