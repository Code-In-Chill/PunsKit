plugins {
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.yourname.testplugin"
version = "1.0.0-SNAPSHOT"

dependencies {
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    // Nhúng framework vào plugin — dùng "implementation" để shadow đóng gói luôn
    implementation(project(path = ":framework", configuration = "shadow"))

    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveClassifier.set("")

        // Relocate framework sang namespace riêng của plugin này.
        // Quan trọng: thay "yourname" bằng tên thật của bạn.
        relocate("com.punshub.punskit", "com.yourname.testplugin.shaded.punskit")
    }

    build {
        dependsOn(shadowJar)
    }
}
