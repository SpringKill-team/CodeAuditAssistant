plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.skgroup"
version = "Preview-v1.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("me.gosimple:nbvcxz:1.5.1")
    // https://mvnrepository.com/artifact/org.jboss.windup.decompiler/decompiler-fernflower
    implementation("org.jboss.windup.decompiler:decompiler-fernflower:6.3.9.Final")
    implementation("org.apache.maven:maven-model:3.6.3")
    implementation("org.apache.maven:maven-model-builder:3.6.3")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.2.6")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf("Git4Idea", "java"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("242.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    shadowJar {
        archiveClassifier.set("")
        dependencies {
            include(dependency("me.gosimple:nbvcxz:1.5.1"))
        }
    }

    buildPlugin {
        dependsOn(shadowJar)
    }

    runIde {
        jvmArgs = listOf(
            "-Dsun.java2d.opengl=true",
            "-Dsun.java2d.d3d=false",
            "-Dsun.java2d.noddraw=true"
        )
    }


}
