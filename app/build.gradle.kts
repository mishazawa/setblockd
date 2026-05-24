version = "1.0-SNAPSHOT"

plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    maven {
      name = "papermc"
      url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    // This dependency is used by the application.
    implementation(libs.guava)
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    // Define the main class for the application.
    mainClass = "setblockd.SetBlockPlugin"
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("setblockd")
    
    // 2. Dynamically attach the project version (e.g., "1.0.0")
    archiveVersion.set(project.version.toString())
    
    // 3. (Optional) Clear the classifier if you don't want words like "-plain" or "-all" appended
    archiveClassifier.set("")
}
tasks.register<Copy>("copyToPlugins") {
    dependsOn("jar")
    outputs.upToDateWhen { false }
    from(layout.buildDirectory.dir("libs"))
    include("*.jar")
    into(rootProject.file(".server/plugins"))
}