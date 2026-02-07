plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
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

