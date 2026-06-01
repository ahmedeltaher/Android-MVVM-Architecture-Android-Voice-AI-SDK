plugins {
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*Activity",
                    "*Application",
                    "*.di.*",
                    "*.BuildConfig",
                    "*.demo.*",
                )
            }
        }
        verify {
            rule {
                bound {
                    minValue = 80
                }
            }
        }
    }
}
