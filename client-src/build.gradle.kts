plugins {
 java
 id("gg.essential.loom") version "0.10.0.5"
 id("dev.architectury.architectury-pack200") version "0.1.3"
}
group = "dev.zyon"
version = "1.2.1"
base.archivesName.set("zyon-1.8.9")
java { toolchain.languageVersion.set(JavaLanguageVersion.of(8)); withSourcesJar() }
loom {
 forge { pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter()) }
 runConfigs { remove(getByName("server")) }
}
repositories { mavenCentral() }
dependencies {
 minecraft("com.mojang:minecraft:1.8.9")
 mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
 forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
 testImplementation("junit:junit:4.13.2")
}
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
