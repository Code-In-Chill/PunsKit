plugins {
    id("com.gradleup.shadow") version "8.3.5"
    `maven-publish`
}

group = "com.punshub.punskit"
version = project.findProperty("version")?.toString()?.replace("v", "") 
    ?: System.getenv("JITPACK_VERSION")?.replace("v", "")
    ?: System.getenv("GITHUB_REF_NAME")?.replace("v", "") 
    ?: "1.0.0-SNAPSHOT"

dependencies {
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    testCompileOnly("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")

    // SLF4J API
    compileOnly("org.slf4j:slf4j-api:2.0.12")
    testImplementation("org.slf4j:slf4j-api:2.0.12")

    // Paper API — compileOnly vì server đã có sẵn lúc runtime
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")

    // ClassGraph for fast scanning
    implementation("io.github.classgraph:classgraph:4.8.174")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.11.0")
}

tasks {
    test {
        useJUnitPlatform()
    }

    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("") // bỏ suffix "-all" mặc định
        
        // Relocate ClassGraph to avoid conflicts with other plugins
        relocate("io.github.classgraph", "com.punshub.punskit.shaded.classgraph")
        relocate("nonapi.io.github.classgraph", "com.punshub.punskit.shaded.nonapi.classgraph")
    }

    // Khi build, tự động chạy shadowJar
    build {
        dependsOn(shadowJar)
    }
}

publishing {
    publications {
        create<MavenPublication>("gpr") {
            project.shadow.component(this)
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Code-In-Chill/PunsKit")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
