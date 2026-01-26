plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "Hypertale"
include("launcher")
include("dev")
include("patcher")
include("init")