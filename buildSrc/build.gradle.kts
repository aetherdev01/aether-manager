plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.10.0")
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-commons:9.7")
}

gradlePlugin {
    plugins {
        create("stringEncrypt") {
            id = "dev.aether.plugin.string-encrypt"
            implementationClass = "dev.aether.plugin.StringEncryptPlugin"
        }
    }
}
