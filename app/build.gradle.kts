plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.conveyor)
}

version = "0.1.0"
group = "io.runwork"

kotlin {
    jvmToolchain(25)
    jvm {
        mainRun {
            mainClass = "io.runwork.app.MainKt"
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.bundle.bootstrap)
            implementation(libs.bundle.updater)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.testJunit)
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutinesTest)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

tasks.register<JavaExec>("runManualUpdateTest") {
    group = "application"
    description = "Runs the manual bundle update test"
    mainClass.set("io.runwork.app.ManualUpdateTestKt")
    classpath = kotlin.jvm().compilations["main"].runtimeDependencyFiles +
        kotlin.jvm().compilations["main"].output.allOutputs
}

