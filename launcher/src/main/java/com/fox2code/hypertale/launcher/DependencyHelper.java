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
package com.fox2code.hypertale.launcher;

import com.fox2code.hypertale.loader.HypertaleConfig;
import com.fox2code.hypertale.utils.HypertalePlatform;
import com.fox2code.hypertale.utils.IOUtils;
import com.fox2code.hypertale.utils.NetUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Hypertale Dependency Manager class, used for download or extracting dependencies when needed.
 */
public final class DependencyHelper {
	private static final boolean DEBUG = false;
	public static final boolean OFFLINE_ONLY = // Always true on some edition
			MainPlus.forceOffline() || HypertaleConfig.isOfflineMode() ||
					!HypertaleConfig.allowDownloadLibraries;
	private static final File librariesDir = Main.singleplayer ?
			new File(HypertalePlatform.getPlatform().getHytaleHome(), "HypertaleLibraries").getAbsoluteFile() :
			new File(System.getProperty("hypertale.librariesDir", "libraries")).getAbsoluteFile();
	@SuppressWarnings("unchecked")
	private static final Consumer<URL> initCLAddURL = Boolean.getBoolean("hypertale.useInitWrapper") ?
			(Consumer<URL>) System.getProperties().get("hypertale.initCLAddURL") : null;
	public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";
	public static final String FOX2CODE = "https://cdn.fox2code.com/maven";
	private static final HashMap<String, Dependency> hypertaleDependencies = new HashMap<>();

	private static final Dependency[] patcherDependenciesDirect = new Dependency[]{
			new Dependency("org.ow2.asm:asm:" + BuildConfig.ASM_VERSION, MAVEN_CENTRAL,
					"org.objectweb.asm.ClassVisitor", null,
					"ed825d10ab1399c8c0cb669e688cf0c8c82629b4c8399b58352b68e92ca10fcb"),
			new Dependency("org.ow2.asm:asm-tree:" + BuildConfig.ASM_VERSION, MAVEN_CENTRAL,
					"org.objectweb.asm.tree.ClassNode", null,
					"3dfb0d5b6a106cd40b5b250e39935fbf2f927f4477546a5369a3ac609cf0506b"),
			new Dependency("org.ow2.asm:asm-analysis:" + BuildConfig.ASM_VERSION,
					MAVEN_CENTRAL, "org.objectweb.asm.tree.analysis.Analyzer", null,
					"dede75a21306b65974ecd8f87114ff6970f09fb794157a4ca09ab25c888c2bfc"),
			new Dependency("org.ow2.asm:asm-commons:" + BuildConfig.ASM_VERSION,
					MAVEN_CENTRAL, "org.objectweb.asm.commons.InstructionAdapter", null,
					"6d0abefb7cbf972ea16edb37ec14835372505063a45f976ab7ea889ed9497895"),
			new Dependency("org.ow2.asm:asm-util:" + BuildConfig.ASM_VERSION,
					MAVEN_CENTRAL, "org.objectweb.asm.util.CheckClassAdapter", null,
					"1bb99d091fba2597dc6d51193e9bbcf0d8447e7ed96bd8f0198b18152f09655c"),
			new Dependency("it.unimi.dsi:fastutil:" + BuildConfig.FASTUTIL_VERSION,
					MAVEN_CENTRAL, "it.unimi.dsi.fastutil.Arrays", null,
					"9094ae67d01d0ad246f886f11ad557fc2e79c72cbf3feed83e1512a8ae90a74a"),
			new Dependency("com.google.code.gson:gson:" + BuildConfig.GSON_VERSION,
					MAVEN_CENTRAL, "com.google.gson.Gson", null,
					"2cbd119bf1961c28788310963dc80ba65f58cdeec1dd139c8bdb1240faa2c36f"),
			new Dependency("com.google.guava:failureaccess:" + BuildConfig.GUAVA_FAILURE_ACCESS_VERSION,
					MAVEN_CENTRAL, "com.google.common.util.concurrent.internal.InternalFutureFailureAccess", null,
					"cbfc3906b19b8f55dd7cfd6dfe0aa4532e834250d7f080bd8d211a3e246b59cb"),
			new Dependency("com.google.guava:guava:" + BuildConfig.GUAVA_VERSION,
					MAVEN_CENTRAL, "com.google.common.io.Files", null,
					"dc573e1fca4fd5454f4a5fd3d7da2df03002876a4175bafc14a95980dd7713b3"),
			new Dependency("net.fabricmc:sponge-mixin:" + BuildConfig.FABRIC_MIXIN_VERSION,
					MAVEN_CENTRAL, "org.spongepowered.asm.mixin.Mixins", null,
					"9e90efec71d2bad5b96c9089f019d14a8603227d3c5f408d12f53fae89d99d41"),
			new Dependency("io.github.llamalad7:mixinextras-common:" + BuildConfig.MIXIN_EXTRAS_VERSION,
					MAVEN_CENTRAL, "com.llamalad7.mixinextras.MixinExtrasBootstrap", null,
					"e2de9240ce39af783a83bcd315445b84d675c01e4655f3ad55c163071ce15fee"),
			new Dependency("com.github.bawnorton.mixinsquared:mixinsquared-common:" + BuildConfig.MIXIN_SQUARED_VERSION,
					FOX2CODE, "com.bawnorton.mixinsquared.MixinSquaredBootstrap", null,
					"5ae421a724f2cc9b06ade3da79fb3224e8073fdeac9a35df6f16ae59147a1abb")
	};

	/**
	 * List of all Hypertale Dependencies.
	 */
	public static final List<DependencyHelper.Dependency> patcherDependencies = List.of(patcherDependenciesDirect);

	/**
	 * Load a dependency into the Hypertale environment, if Hypertale cannot fulfill this request,
	 * either by IO failure, either by the edition/configuration, an error will be thrown.
	 *
	 * @param dependency The dependency to load into the root class loader!
	 */
	public static void loadDependency(Dependency dependency) {
		dependency = hypertaleDependencies.getOrDefault(dependency.name, dependency);
		if (hasClass(dependency.classCheck)) return;
		if (!librariesDir.isDirectory() && !librariesDir.mkdirs())
			throw new IOError(new IOException("Failed to create libraries directory"));
		String postURL = resolvePostURL(dependency.name);
		File file = new File(librariesDir, fixUpPath(postURL));
		if (!file.isAbsolute()) file = file.getAbsoluteFile();
		checkHashOrDelete(file, dependency, false);
		if (file.exists()) {
			// Return early if the local file is already fine!
			addDependencyToClassLoaderWithSha256Verification(file, dependency, true);
			return;
		}
		File parentFile = file.getParentFile();
		if (!parentFile.isDirectory() && !parentFile.mkdirs()) {
			throw new RuntimeException("Cannot create dependency directory for " + dependency.name);
		}
		if (dependency.repository == null && dependency.fallbackUrl == null) {
			throw new RuntimeException("Cannot download a library without a source for " + dependency.name);
		} else if (dependency.sha256Sum == null) {
			// Do not allow download without checksums as they are a potential security risk.
			throw new RuntimeException("Download is forbidden for libraries without a checksum " + dependency.name);
		} else if (OFFLINE_ONLY) {
			// Extra safe path so reviewers are sure Hypertale will not connect to the internet.
			int index;
			if (dependency.fallbackUrl != null && !file.exists() &&
					dependency.fallbackUrl.startsWith("jar:file:/") &&
					(index = dependency.fallbackUrl.indexOf("!/")) != -1) {
				File libraryContainer = new File(URI.create(dependency.fallbackUrl.substring(4, index)));
				String entry = dependency.fallbackUrl.substring(index + 2);
				if (libraryContainer.isFile()) {
					try (JarFile jarFile = new JarFile(libraryContainer)) {
						ZipEntry libraryEntry;
						if ((libraryEntry = jarFile.getEntry(entry)) != null) {
							IOUtils.copy(jarFile.getInputStream(libraryEntry),
									Files.newOutputStream(file.toPath()));
							addDependencyToClassLoaderWithSha256Verification(
									file, dependency, true);
							return;
						}
					} catch (IOException e) {
						throw new RuntimeException("Failed to load jar file", e);
					}
				}
			}

			// Do not allow download in offline mode.
			throw new RuntimeException("Missing Hypertale Library Pack! Failed to find: " + dependency.name);
		}
		boolean justDownloaded = false;
		IOException fallBackIoe = null;
		if (dependency.repository != null) {
			try (OutputStream os = Files.newOutputStream(file.toPath())) {
				justDownloaded = true;
				NetUtils.downloadTo(URI.create(dependency.repository.endsWith(".jar") ?
						dependency.repository : dependency.repository + "/" + postURL).toURL(), os);
			} catch (IOException ioe) {
				if (dependency.fallbackUrl != null) {
					fallBackIoe = ioe;
				} else {
					if (file.exists() && !file.delete()) file.deleteOnExit();
					throw new RuntimeException("Cannot download " + dependency.name, ioe);
				}
			}
		}
		if (fallBackIoe != null || dependency.repository == null) {
			try (OutputStream os = Files.newOutputStream(file.toPath())) {
				justDownloaded = true;
				NetUtils.downloadTo(URI.create(dependency.fallbackUrl).toURL(), os);
			} catch (IOException ioe) {
				if (file.exists() && !file.delete()) file.deleteOnExit();
				throw new RuntimeException("Cannot download " + dependency.name, fallBackIoe);
			}
		}
		addDependencyToClassLoaderWithSha256Verification(file, dependency, justDownloaded);
	}

	private static void addDependencyToClassLoaderWithSha256Verification(
			File file, Dependency dependency, boolean justDownloaded) {
		checkHashOrDelete(file, dependency, true);
		try {
			addFileToClasspath(file);
			if (hasClass(dependency.classCheck)) {
				if (DEBUG) {
					EarlyLogger.log("Loaded " +
							dependency.name + " -> " + file.getPath());
				}
			} else {
				if (!justDownloaded) {
					// Assume the file is corrupted if the load failed.
					if (file.exists() && !file.delete()) {
						file.deleteOnExit();
					} else {
						loadDependency(dependency);
						return;
					}
				}
				throw new RuntimeException("Failed to load " +
						dependency.name + " -> " + file.getPath());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void addFileToClasspath(File file) throws IOException {
		Objects.requireNonNull(file, "File must not be null");
		Instrumentation instrumentation;
		if (initCLAddURL != null) {
			if (!file.isFile()) {
				throw new FileNotFoundException("File must be a regular file!");
			}
			initCLAddURL.accept(file.toURI().toURL());
		} else if ((instrumentation = HypertaleAgent.getInstrumentation()) != null) {
			instrumentation.appendToSystemClassLoaderSearch(new JarFile(file));
		} else {
			throw new RuntimeException("Failed to load " + file.getPath() + " (Init system failure)");
		}
	}

	private static String fixUpPath(String path) {
		return File.separatorChar == '\\' ?
				path.replace('/', '\\') : path;
	}

	public static boolean hasClass(String cls) {
		try {
			Class.forName(cls, false, DependencyHelper.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	static String resolvePostURL(String string) {
		String[] depKeys = string.split(":");
		// "org.joml:rrrr:${jomlVersion}"      => ${repo}/org/joml/rrrr/1.9.12/rrrr-1.9.12.jar
		// "org.joml:rrrr:${jomlVersion}:rrrr" => ${repo}/org/joml/rrrr/1.9.12/rrrr-1.9.12-rrrr.jar
		if (depKeys.length == 3) {
			return depKeys[0].replace('.','/')+"/"+depKeys[1]+"/"+depKeys[2]+"/"+depKeys[1]+"-"+depKeys[2]+".jar";
		}
		if (depKeys.length == 4) {
			return depKeys[0].replace('.','/')+"/"+depKeys[1]+"/"+depKeys[2]+"/"+depKeys[1]+"-"+depKeys[2]+"-"+depKeys[3]+".jar";
		}
		throw new RuntimeException("Invalid Dep");
	}

	private static void checkHashOrDelete(File file, Dependency dependency, boolean errorOut) {
		if (dependency.sha256Sum == null || !file.exists()) return;
		String hashString;
		try {
			hashString = IOUtils.toHex(IOUtils.sha256Of(file));
		} catch (IOException e) {
			hashString = "";
		}
		if (!dependency.sha256Sum.equals(hashString)) {
			boolean deleteSuccessful = file.delete();
			if (errorOut) {
				throw new RuntimeException("Remote dependency " + dependency.name + " checksum mismatch " +
						"(got: " + hashString + ", expected: " + dependency.sha256Sum + ")");
			}
			if (!deleteSuccessful) {
				throw new RuntimeException("Can't delete dependency with checksum mismatch " + dependency.name);
			}
		}
	}

	static void loadLocalDependencyPack(Dependency dependency) {
		if (dependency.repository != null || dependency.fallbackUrl == null ||
				!dependency.fallbackUrl.startsWith("jar:file:")) {
			EarlyLogger.log("Failed to load local dependency " + dependency.name + " -> " + dependency.fallbackUrl);
			return;
		}
		if (hypertaleDependencies.isEmpty()) {
			for (Dependency dep : patcherDependenciesDirect) {
				hypertaleDependencies.put(dep.name, dep);
			}
		}
		Dependency existingDependency = hypertaleDependencies.get(dependency.name);
		if (existingDependency != null) {
			Dependency mergedDep = existingDependency.tryMerge(dependency);
			if (mergedDep != null) {
				hypertaleDependencies.put(mergedDep.name, mergedDep);
			}
		} else {
			EarlyLogger.log("Loaded local dependency " + dependency.name + " -> " + dependency.fallbackUrl);
			hypertaleDependencies.put(dependency.name, dependency);
		}
	}

	/**
	 * Dependency record, used to store information about a dependency.
	 *
	 * @param name the full name of the dependency as used in Gradle
	 * @param repository the repository to download the dependency from
	 * @param classCheck the class to check for in the dependency
	 * @param fallbackUrl the fallback URL to download the dependency from
	 * @param sha256Sum the SHA-256 checksum of the dependency
	 */
	public record Dependency(@Nonnull String name, @Nullable String repository,@Nonnull String classCheck,
							 @Nullable String fallbackUrl,@Nullable String sha256Sum) {
		public Dependency {
			Objects.requireNonNull(name, "name must not be null");
			Objects.requireNonNull(classCheck, "classCheck must not be null");
		}

		public Dependency(String name, String repository, String classCheck) {
			this(name, repository, classCheck, null, null);
		}

		public Dependency(String name, String repository, String classCheck, String fallbackUrl) {
			this(name, repository, classCheck, fallbackUrl, null);
		}

		public Dependency tryMerge(Dependency other) {
			// Assume "this" is more trusted than "other"
			// Cannot merge dependencies with different name or checksums
			if (!this.name.equals(other.name) || !this.classCheck.equals(other.classCheck) ||
					(this.sha256Sum != null && other.sha256Sum != null && !this.sha256Sum.equals(other.sha256Sum))) {
				return null;
			}
			String newFallback = this.fallbackUrl == null ? other.fallbackUrl : this.fallbackUrl;
			return new Dependency(this.name, this.repository, this.classCheck, newFallback, this.sha256Sum);
		}
	}
}
