import org.gradle.api.tasks.bundling.Jar

version = "1.0.9"

repositories {
}

dependencies {
}

tasks.named<Jar>("jar") {
	archiveFileName.set("HyGuard-${project.version}.jar")
}