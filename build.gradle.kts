import org.gradle.api.tasks.bundling.Jar

version = "1.0.9"

val simpleClaimsJar = if (rootProject.file("../../SimpleClaims-1.0.34.jar").exists()) {
	rootProject.file("../../SimpleClaims-1.0.34.jar")
} else {
	rootProject.file("../SimpleClaims-1.0.34.jar")
}

repositories {
}

dependencies {
	add("compileOnly", files(simpleClaimsJar))
}

tasks.named<Jar>("jar") {
	archiveFileName.set("HyGuard-${project.version}.jar")
}