rootProject.name = "HyGuard"

plugins {
    id("dev.scaffoldit") version "0.2.+"
}

hytale {
    usePatchline("release")
    useVersion("latest")

    repositories {
    }

    dependencies {
    }

    manifest {
        Group = "thenexusgates"
        Name = "HyGuard"
        Main = "dev.thenexusgates.hyguard.HyGuardPlugin"
    }
}