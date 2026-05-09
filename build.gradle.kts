plugins {
    java
    `maven-publish`
}

group = "com.bitaspire"
version = "0.3.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.hsqldb:hsqldb:2.7.2")
}

tasks.jar {
    from(rootDir) {
        include("AGENTS.md", "AI_REFERENCE.md")
        into("META-INF/jdborm")
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name = "jdborm"
                description = "Lightweight JDBC ORM library with fluent API"
                url = "https://github.com/BitAspire/jdborm"

                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }

                developers {
                    developer {
                        id = "BitAspire"
                        name = "BitAspire"
                        email = "bitaspire@users.noreply.github.com"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/BitAspire/jdborm.git"
                    developerConnection = "scm:git:ssh://github.com/BitAspire/jdborm.git"
                    url = "https://github.com/BitAspire/jdborm"
                }
            }
        }
    }

}
