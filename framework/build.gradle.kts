plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
}

group = "com.punshub.punskit"
version = System.getenv("GITHUB_REF_NAME")?.replace("v", "") ?: "1.0.0-SNAPSHOT"

dependencies {
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    testCompileOnly("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")

    // SLF4J API
    compileOnly("org.slf4j:slf4j-api:2.0.12")

    // Paper API — compileOnly vì server đã có sẵn lúc runtime
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveClassifier.set("") // bỏ suffix "-all" mặc định

        // ── RELOCATION BẮT BUỘC ──────────────────────────────────────────────
        // Mỗi plugin nhúng framework sẽ có bản riêng biệt trong namespace của nó.
        // Điều này ngăn ClassLoader conflict khi nhiều plugin cùng dùng framework.
        // Khi bạn publish framework, người dùng sẽ relocate sang namespace của họ.
        // Ví dụ: "com.punshub.punskit" → "com.myplugin.shaded.punskit"
        // ─────────────────────────────────────────────────────────────────────
    }

    // Khi build, tự động chạy shadowJar thay vì jar thông thường
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
            url = uri("https://maven.pkg.github.com/punshub/PunsKit")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
