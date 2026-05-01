plugins {
    java
    application
}

group = "io.github.ryan_glgr.hansel_grapher"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jogamp.org/deployment/maven")
    }
}

dependencies {

    // used to make visualizations of the interview stats
    implementation("org.knowm.xchart:xchart:3.8.8")
    implementation("de.erichseifert.vectorgraphics2d:VectorGraphics2D:0.13")

    // used for the bitset operations during interview
    implementation("org.roaringbitmap:RoaringBitmap:1.3.0")

    // ----------------------------- OpenGL Dependencies
    // JOGL core
    implementation("org.jogamp.gluegen:gluegen-rt:2.4.0")
    implementation("org.jogamp.jogl:jogl-all:2.4.0")

    val os = System.getProperty("os.name").lowercase()

    when {
        os.contains("linux") -> {
            runtimeOnly("org.jogamp.gluegen:gluegen-rt:2.4.0:natives-linux-amd64")
            runtimeOnly("org.jogamp.jogl:jogl-all:2.4.0:natives-linux-amd64")
        }
        os.contains("windows") -> {
            runtimeOnly("org.jogamp.gluegen:gluegen-rt:2.4.0:natives-windows-amd64")
            runtimeOnly("org.jogamp.jogl:jogl-all:2.4.0:natives-windows-amd64")
        }
        os.contains("mac") -> {
            runtimeOnly("org.jogamp.gluegen:gluegen-rt:2.4.0:natives-macosx-universal")
            runtimeOnly("org.jogamp.jogl:jogl-all:2.4.0:natives-macosx-universal")
        }
    }
}

application {
    mainClass.set("io.github.ryan_glgr.hansel_grapher.Main")
}