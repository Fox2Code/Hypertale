/*
 * MIT License
 * 
 * Copyright (c) 2026 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.fox2code.hypertale.dev

import com.diffplug.gradle.spotless.SpotlessApply
import com.fox2code.hypertale.launcher.BuildConfig
import com.fox2code.hypertale.utils.HypertalePaths
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService

import java.util.jar.JarFile

class HypertaleGradlePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.pluginManager.apply('eclipse')
        project.pluginManager.apply('java-library')
        project.pluginManager.apply('net.ltgt.errorprone')
        project.pluginManager.apply('com.diffplug.spotless')
        project.extensions.create("hypertale", HypertaleDevConfig)
        ((HypertaleDevConfig) project.extensions.getByName("hypertale"))
                .localProperties = HypertaleDevLocalProperties.loadProperties(project)
        final File hypertaleGradleFolder = getHypertaleGradleDirectory(project)
        if (!hypertaleGradleFolder.isDirectory() && !hypertaleGradleFolder.mkdirs()) {
            throw new IOException("Failed to create Hypertale gradle folder!")
        }
        final File licenseFile = getLicenseFile(project.rootDir)

        project.java {
            toolchain {
                languageVersion = JavaLanguageVersion.of(25)
            }

            withSourcesJar()
            withJavadocJar()
        }
        project.configurations {
            javaAgent {
                transitive = false
                canBeConsumed = false
            }
        }

        project.javadoc {
            failOnError = false
        }
        project.repositories {
            maven {
                url = hypertaleGradleFolder.toURI().toString()
                content {
                    includeGroup 'com.hypixel'
                }
            }
            maven {
                url = 'https://cdn.fox2code.com/maven'
                content {
                    includeGroup 'com.fox2code.Hypertale'
                    includeGroup 'com.fox2code'
                }
            }
            mavenCentral()
        }
        if (BuildConfig.HYPERTALE_VERSION.endsWith("-dev")) {
            project.repositories.mavenLocal()
        }
        project.dependencies {
            errorprone "com.google.errorprone:error_prone_core:${BuildConfig.ERRORPRONE_VERSION}"
        }
        project.tasks.register("checkHytale").configure {
            group = "Hypertale"
            description = "Check if Hytale is present, and download it if needed"
        }
        project.tasks.register("runServer", JavaExec).configure {
            group = "Hypertale"
            description = "Run Hypertale with your mod loaded"
            dependsOn(project.tasks.assemble)
            // We must cap Hytale server RAM usage so it does not crash Linux machines!
            jvmArgs("-Xmx6G")
        }
        project.tasks.register("generateBuildConfig").configure {
            group = "Hypertale"
            description = "Regenerate build config"
        }
        project.afterEvaluate {
            final JavaToolchainService toolchain = project.extensions.getByType(JavaToolchainService.class)
            final File javaExec = toolchain.launcherFor {
                it.languageVersion.set(JavaLanguageVersion.of(25))
            }.get().executablePath.asFile
            HypertaleDevConfig config = ((HypertaleDevConfig) project.extensions.getByName("hypertale"))
            config.buildConfig("MOD_VERSION", project.version)
            config.internalMakeImmutable()
            HypertaleIntellijIDEASupport.installIdeaDictionaryOnIDEASync(project, config.customDictionary)
            final File generatedSources = new File(project.layout.buildDirectory.getAsFile().get(),
                    "generated/sources/hypertale")
            project.sourceSets {
                main {
                    java {
                        srcDir generatedSources.getPath()
                    }
                }
            }
            HypertaleBuildConfigHelper.makeBuildConfig(config, generatedSources, false)
            if (config.useUnmodifiedHytale) {
                project.dependencies {
                    api "com.hypixel:hytale:${config.hytaleBranch}"
                }
            } else {
                project.dependencies {
                    api "com.hypixel:hytale:${config.hytaleBranch}${HypertaleDepMaker.HYPERTALE_VERSION_SUFFIX}"
                    api "com.fox2code.Hypertale:launcher:${BuildConfig.HYPERTALE_VERSION}"
                    javaAgent "com.fox2code.Hypertale:launcher:${BuildConfig.HYPERTALE_VERSION}"
                }
                if (config.useBundling) {
                    project.tasks.compileJava {
                        doLast {
                            HypertaleBundlerPatcher.patch(project)
                        }
                    }
                    project.tasks.jar {
                        from(project.zipTree(HypertalePaths.hypertaleJar)) {
                            include("META-INF/bundled/*")
                        }
                    }
                }
            }
            File hytaleAssets = HypertaleHytaleDownloader.getVanillaAssets(
                    hypertaleGradleFolder, config)
            if (hytaleAssets != null) {
                project.dependencies {
                    compileOnly(project.files(hytaleAssets))
                }
            }
            project.tasks.checkHytale {
                doLast {
                    if (config.useUnmodifiedHytale) {
                        HypertaleHytaleDownloader.downloadHytale(hypertaleGradleFolder, config, false)
                    } else {
                        HypertalePatcher.patch(javaExec, hypertaleGradleFolder, config, false)
                    }
                }
            }
            if (HypertaleIntellijIDEASupport.ideaSync) {
                if (config.useUnmodifiedHytale) {
                    HypertaleHytaleDownloader.downloadHytale(hypertaleGradleFolder, config, false)
                } else {
                    HypertalePatcher.patch(javaExec, hypertaleGradleFolder, config, false)
                }
            }
            project.tasks.generateBuildConfig {
                doLast {
                    HypertaleBuildConfigHelper.makeBuildConfig(config, generatedSources, true)
                }
            }
            project.tasks.compileJava.dependsOn(project.tasks.checkHytale)
            project.tasks.compileJava.dependsOn(project.tasks.generateBuildConfig)
            if (project.pluginManager.hasPlugin("groovy")) {
                project.tasks.compileGroovy.dependsOn(project.tasks.checkHytale)
                project.tasks.compileGroovy.dependsOn(project.tasks.generateBuildConfig)
            }
            if (project.pluginManager.hasPlugin("org.jetbrains.kotlin.jvm")) {
                project.tasks.compileKotlin.dependsOn(project.tasks.checkHytale)
                project.tasks.compileKotlin.dependsOn(project.tasks.generateBuildConfig)
            }
            File runDirectory = new File(project.rootDir, "run")
            File runDirectoryHypertale = new File(runDirectory, ".hypertale")
            File runDirectoryMods = new File(runDirectory, "mods")
            if ((config.useUnmodifiedHytale || // Don't require ".hypertale" folder if using unmodified Hytale.
                    runDirectoryHypertale.isDirectory() || runDirectoryHypertale.mkdirs()) &&
                    (runDirectoryMods.isDirectory() || runDirectoryMods.mkdirs())) {
                project.tasks.runServer.configure {
                    classpath = project.sourceSets.main.runtimeClasspath
                    workingDir(runDirectory)
                    systemProperty("hypertale.gradleInit", "true")
                    jvmArgs(project.configurations.javaAgent.collect { "-javaagent:" + it.path })
                    mainClass = "com.fox2code.hypertale.launcher.Main"
                    standardInput = System.in
                    args("--launch-dev")
                    dependsOn("assemble")
                }
            }
            // Using filesMatching in processResources during import confuse IntellijIDEA
            if (!HypertaleIntellijIDEASupport.ideaSync) {
                String serverVersion = "Unknown"
                File hytaleServer = HypertaleHytaleDownloader.getVanillaServer(
                        hypertaleGradleFolder, config)
                if (hytaleServer == null) {
                    hytaleServer = HypertalePatcher.getPatchedHypertale(
                            hypertaleGradleFolder, config.getHytaleBranch())
                }
                if (hytaleServer != null) {
                    try (JarFile jarFile = new JarFile(hytaleServer)) {
                        serverVersion = jarFile.manifest.mainAttributes.getValue(
                                "Implementation-Version")
                    }
                }
                project.tasks.processResources {
                    filesMatching("manifest.json") {
                        expand(version: project.version, serverVersion: serverVersion)
                    }
                }
            }
            project.tasks.jar {
                manifest {
                    attributes 'Built-With-Hypertale': BuildConfig.HYPERTALE_VERSION
                    attributes 'Hypertale-Repo': 'https://github.com/Fox2Code/Hypertale'
                }
                if (licenseFile != null) {
                    from(project.rootDir) {
                        include(licenseFile.getName())
                    }
                }
            }
            project.tasks.withType(Javadoc).configureEach {
                ((Javadoc) it).options {
                    addStringOption('Xdoclint:-missing', '-quiet')
                }
            }
            project.tasks.withType(JavaCompile).configureEach {
                ((JavaCompile) it).options.errorprone {
                        disableWarningsInGeneratedCode.set(true)
                        allErrorsAsWarnings.set(true)
                        disable("EmptyCatch", "ThreadPriorityCheck",
                                "NonApiType", "MutablePublicArray",
                                "StringSplitter", "FloggerFormatString",
                                "FloggerLogString", "FloggerStringConcatenation")
                }
            }
            if (config.getUseSpotless()) {
                project.tasks.register("safeSpotlessApply") {
                    doLast {
                        try {
                            project.tasks.withType(SpotlessApply.class).forEach {
                                it.performAction()
                            }
                        } catch (Exception ignored) {}
                    }
                }
                final String processedLicenseHeader = !config.includeLicenseHeader ||
                        licenseFile == null ? "" : '/*\n' + licenseFile.readLines()
                                .collect { ' * ' + it }.join('\n') + '\n */\n'
                project.spotless {
                    enforceCheck = false
                    java {
                        targetExclude "build/generated/**/*.java"
                        formatAnnotations()
                        removeUnusedImports()
                        licenseHeader(processedLicenseHeader)
                    }
                }
                project.tasks.compileJava.dependsOn(project.tasks.safeSpotlessApply)
                project.tasks.spotlessJavaCheck.enabled = false

                if (project.pluginManager.hasPlugin("groovy")) {
                    project.spotless {
                        groovy {
                            licenseHeader(processedLicenseHeader)
                        }
                    }

                    project.tasks.compileGroovy.dependsOn(project.tasks.safeSpotlessApply)
                }
                if (project.pluginManager.hasPlugin("org.jetbrains.kotlin.jvm")) {
                    project.spotless {
                        kotlin {
                            licenseHeader(processedLicenseHeader)
                        }
                    }

                    project.tasks.compileKotlin.dependsOn(project.tasks.safeSpotlessApply)
                }
            }
        }
    }

    File getHypertaleGradleDirectory(Project project) {
        return new File(project.gradle.gradleUserHomeDir, "hypertale")
    }

    static File getLicenseFile(File rootDir) {
        for (String possibleName : new String[]{"LICENSE", "LICENSE.md",
                "License", "License.md", "license", "license.md", "COPYING"}) {
            File licenseFile = new File(rootDir, possibleName)
            if (licenseFile.isFile()) {
                return licenseFile
            }
        }
        return null
    }
}
