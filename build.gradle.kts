import org.gradle.api.tasks.bundling.Jar

version = "1.0.0"

repositories {
}

dependencies {
}

tasks.named<Jar>("jar") {
	archiveFileName.set("HyGuard-${project.version}.jar")
}