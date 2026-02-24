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
package com.fox2code.hypertale.loader;

import com.fox2code.hypertale.launcher.DependencyHelper;
import com.fox2code.hypertale.launcher.EarlyLogger;
import com.fox2code.hypertale.utils.EmptyArrays;
import com.fox2code.hypertale.utils.HypertalePaths;
import com.fox2code.hypertale.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class HypertaleModGatherer {
	private final List<File> hypertaleMods;
	private final List<File> mods;
	private final List<File> libraries;
	private final File modSyncBootstrap;
	private final int modHash;
	private final List<ClassPathModCandidate> classPathManifests;
	private final boolean usingMixins;
	private final boolean hasHyxin;
	private final Collection<DependencyHelper.Dependency> localDependencies;

	private HypertaleModGatherer(List<File> hypertaleMods, List<File> mods, List<File> libraries,
								 File modSyncBootstrap, int modHash, List<ClassPathModCandidate> classPathManifests,
								 boolean usingMixins, boolean hasHyxin,
								 Collection<DependencyHelper.Dependency> localDependencies) {
		this.hypertaleMods = hypertaleMods;
		this.mods = mods;
		this.libraries = libraries;
		this.modSyncBootstrap = modSyncBootstrap;
		this.modHash = modHash;
		this.classPathManifests = classPathManifests;
		this.usingMixins = usingMixins;
		this.hasHyxin = hasHyxin;
		this.localDependencies = localDependencies;
	}

	public List<File> getHypertaleMods() {
		return this.hypertaleMods;
	}

	public List<File> getMods() {
		return this.mods;
	}

	public List<File> getLibraries() {
		return this.libraries;
	}

	public File getModSyncBootstrap() {
		return this.modSyncBootstrap;
	}

	public int getModHash() {
		return this.modHash;
	}

	public List<ClassPathModCandidate> getClassPathManifests() {
		return this.classPathManifests;
	}

	public boolean isUsingMixins() {
		return this.usingMixins;
	}

	public boolean hasHyxin() {
		return this.hasHyxin;
	}

	public boolean shouldEnableMixinSubsystem() {
		// Hypertale is a Mixin backend on its own.
		return this.usingMixins || this.hasHyxin;
	}

	public Collection<DependencyHelper.Dependency> getLocalDependencies() {
		return this.localDependencies;
	}

	public static HypertaleModGatherer gatherModsDev() {
		return gatherMods(EmptyArrays.EMPTY_STRING_ARRAY);
	}

	public static HypertaleModGatherer gatherMods(String[] args) {
		// TODO: Process launch arguments
		boolean[] useMixins = new boolean[]{false, false};
		ArrayList<File> mods = new ArrayList<>();
		ArrayList<File> hypertaleMods = new ArrayList<>();
		ArrayList<File> libraries = new ArrayList<>();
		HashMap<String, DependencyHelper.Dependency> localDependencies = new HashMap<>();
		File modSyncBootstrap = null;
		if (HypertalePaths.hytaleEarlyPlugins.isDirectory()) {
			modSyncBootstrap = appendEarlyLoaderMods(
					hypertaleMods, mods, useMixins, localDependencies);
		}
		if (HypertalePaths.hytaleMods.isDirectory()) {
			appendMods(hypertaleMods, mods, libraries, useMixins, localDependencies);
		}
		long[] fileSizes = new long[mods.size()];
		for (int i = 0; i < fileSizes.length; i++) {
			fileSizes[i] = mods.get(i).length();
		}
		Arrays.sort(fileSizes);
		return new HypertaleModGatherer(Collections.unmodifiableList(hypertaleMods),
				Collections.unmodifiableList(mods), Collections.unmodifiableList(libraries),
				modSyncBootstrap, Arrays.hashCode(fileSizes),
				Collections.unmodifiableList(gatherClassPathMods(useMixins)),
				useMixins[0], useMixins[1],
				Collections.unmodifiableCollection(localDependencies.values()));
	}

	private static List<ClassPathModCandidate> gatherClassPathMods(boolean[] useMixins) {
		try {
			Enumeration<URL> urlEnumeration = HypertaleModGatherer.class.getClassLoader().getResources("manifest.json");
			ArrayList<ClassPathModCandidate> classPathManifests = new ArrayList<>();
			while (urlEnumeration.hasMoreElements()) {
				URL url = urlEnumeration.nextElement();
				if (url.getProtocol().equals("file")) {
					File file = new File(url.toURI());
					if (file.isFile()) {
						classPathManifests.add(new ClassPathModCandidate(url, file.getParentFile()));
					}
				} else if (url.getProtocol().equals("jar")) {
					String path = url.getPath();
					int separatorIndex = path.indexOf("!/");
					if (separatorIndex != -1) {
						File file = new File(path.substring(5, separatorIndex)).getAbsoluteFile();
						if (file.exists() && !HypertalePaths.hypertaleJar.equals(file) &&
								!HypertalePaths.getHytaleJar().equals(file)) {
							classPathManifests.add(new ClassPathModCandidate(url, file));
						}
					}
				}
				if (!useMixins[0]) {
					try (InputStream inputStream = url.openStream()) {
						byte[] manifestData = IOUtils.readAllBytes(inputStream);
						String modInfo = new String(manifestData, StandardCharsets.UTF_8);
						if (modInfo.contains("\"HypertaleMixinConfig\"") || modInfo.contains("\"Hyxin\"")) {
							useMixins[0] = true;
						}
					}
				}
			}
			return classPathManifests;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static File appendEarlyLoaderMods(
			ArrayList<File> hypertaleMods, ArrayList<File> mods, boolean[] useMixins,
			HashMap<String, DependencyHelper.Dependency> localDependencies) {
		File modSyncBootstrap = null;
		for (File file : Objects.requireNonNull(HypertalePaths.hytaleEarlyPlugins.listFiles())) {
			if (file.isFile() && file.getName().endsWith(".jar")) {
				try(ZipFile zipFile = new ZipFile(file)) {
					if (preprocessFile(localDependencies, useMixins, file, zipFile)) {
						continue;
					}
					ZipEntry manifestEntry;
					if ((manifestEntry = zipFile.getEntry("manifest.json")) != null) {
						String modInfo;
						try (InputStream inputStream = zipFile.getInputStream(manifestEntry)) {
							modInfo = new String(IOUtils.readAllBytes(inputStream), StandardCharsets.UTF_8);
						} catch (RuntimeException _) {
							modInfo = "";
						}
						if (modInfo.contains("\"Hyxin\"")) {
							useMixins[0] = true;
							hypertaleMods.add(file);
							mods.add(file);
							continue;
						}
					}
					if (zipFile.getEntry(HypertaleCompatibility.entryModSyncBootstrap) != null) {
						modSyncBootstrap = file; // Mod sync bootstrap need special handling
					} else {
						mods.add(file);
					}
				} catch (Exception _) {}
			}
		}
		return modSyncBootstrap;
	}

	private static void appendMods(ArrayList<File> hypertaleMods, ArrayList<File> mods,
								   ArrayList<File> libraries, boolean[] useMixins,
								   HashMap<String, DependencyHelper.Dependency> localDependencies) {
		for (File file : Objects.requireNonNull(HypertalePaths.hytaleMods.listFiles())) {
			if (file.isFile() && file.getName().endsWith(".jar") &&
					!file.getName().equals(HypertalePaths.hypertaleJar.getName())) {
				try (ZipFile zipFile = new ZipFile(file)) {
					if (preprocessFile(localDependencies, useMixins, file, zipFile)) {
						continue;
					}
					ZipEntry manifestEntry;
					if ((manifestEntry = zipFile.getEntry("manifest.json")) != null) {
						String modInfo;
						try (InputStream inputStream = zipFile.getInputStream(manifestEntry)) {
							modInfo = new String(IOUtils.readAllBytes(inputStream), StandardCharsets.UTF_8);
						} catch (RuntimeException _) {
							modInfo = "";
						}
						boolean modUseMixins = false;
						if (modInfo.contains("\"HypertaleMixinConfig\"") || modInfo.contains("\"Hyxin\"")) {
							useMixins[0] = true;
							modUseMixins = true;
						}
						if (modInfo.contains("\"Hypertale") || modUseMixins) {
							hypertaleMods.add(file);
							mods.add(file);
							continue;
						}
					}
					if (zipFile.getEntry(
							"META-INF/services/com.hypixel.hytale.plugin.early.ClassTransformer") != null) {
						EarlyLogger.log("Early plugin detected in mods -> " + file.getName());
						mods.add(file);
						continue;
					}
					if (manifestEntry != null && zipFile.getEntry("kotlin/KotlinVersion.class") != null) {
						libraries.add(file);
					}
				} catch (IOException e) {
					EarlyLogger.log("Failed to open " + file.getName() + " as a zip file!");
				}
			}
		}
	}

	private static boolean preprocessFile(
			HashMap<String, DependencyHelper.Dependency> localDependencies,
			boolean[] useMixins, File file, ZipFile zipFile) {
		if (zipFile.getEntry(
				"com/fox2code/hypertale/init/Main.class") != null) {
			return true; // <- Do not consider HypertaleInit as a mod!
		}
		if (zipFile.getEntry(HypertaleCompatibility.entryHyxinMixinService) != null ||
				zipFile.getEntry(HypertaleCompatibility.entryHyxinTransformer) != null) {
			useMixins[1] = true;
			return true; // <- We already implement Hyxin APIs
		}
		ZipEntry hyperDependencies;
		if ((hyperDependencies = zipFile.getEntry("META-INF/hypertale-dependencies.properties")) != null) {
			Properties properties = new Properties();
			try {
				properties.load(zipFile.getInputStream(hyperDependencies));
			} catch (IOException e) {
				EarlyLogger.log("Failed to read hypertale-dependencies.properties in " + file.getName());
			}
			for (Map.Entry<Object, Object> entry : properties.entrySet()) {
				String dependencyIdentifier = String.valueOf(entry.getKey()).replace('@', ':');
				String dependencyAttributes = String.valueOf(entry.getValue());
				String[] attributes = dependencyAttributes.split("\\s");
				if (attributes.length != 3) {
					continue;
				}
				String path = attributes[1];
				if (path.startsWith("/") || zipFile.getEntry(path) == null) {
					EarlyLogger.log("Failed to find dependency " + dependencyIdentifier + " in " + file.getName());
					continue;
				}
				path = "jar:" + file.toURI() + "!/" + path;
				DependencyHelper.Dependency newDependency =  new DependencyHelper.Dependency(
						dependencyIdentifier, null, attributes[0], path, attributes[2]);
				DependencyHelper.Dependency oldDependency = localDependencies.get(dependencyIdentifier);
				if (oldDependency != null) {
					newDependency = oldDependency.tryMerge(newDependency);
					if (newDependency == null) {
						EarlyLogger.log("Failed to merge dependency " + dependencyIdentifier + " from multiple jars!");
						continue;
					}
				}

				localDependencies.put(dependencyIdentifier, newDependency);
			}
		}

		return false;
	}

	public static record ClassPathModCandidate(URL manifest, File file) {}
}
