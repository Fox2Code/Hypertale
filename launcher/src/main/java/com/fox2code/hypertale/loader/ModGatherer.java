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

import com.fox2code.hypertale.launcher.EarlyLogger;
import com.fox2code.hypertale.utils.HypertalePaths;
import com.fox2code.hypertale.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ModGatherer {
	private final List<File> hypertaleMods;
	private final List<File> mods;
	private final int modHash;

	private ModGatherer(List<File> hypertaleMods, List<File> mods, int modHash) {
		this.hypertaleMods = hypertaleMods;
		this.mods = mods;
		this.modHash = modHash;
	}

	public List<File> getHypertaleMods() {
		return this.hypertaleMods;
	}

	public List<File> getMods() {
		return this.mods;
	}

	public int getModHash() {
		return this.modHash;
	}

	public static ModGatherer gatherMods(String[] args) {
		// TODO: Process launch arguments
		ArrayList<File> mods = new ArrayList<>();
		ArrayList<File> hypertaleMods = new ArrayList<>();
		if (HypertalePaths.hytaleEarlyPlugins.isDirectory()) {
			appendEarlyLoaderMods(mods);
		}
		if (HypertalePaths.hytaleMods.isDirectory()) {
			appendMods(hypertaleMods, mods);
		}
		long[] fileSizes = new long[mods.size()];
		for (int i = 0; i < fileSizes.length; i++) {
			fileSizes[i] = mods.get(i).length();
		}
		Arrays.sort(fileSizes);
		return new ModGatherer(Collections.unmodifiableList(hypertaleMods),
				Collections.unmodifiableList(mods), Arrays.hashCode(fileSizes));
	}

	private static void appendEarlyLoaderMods(ArrayList<File> mods) {
		for (File file : Objects.requireNonNull(HypertalePaths.hytaleEarlyPlugins.listFiles())) {
			if (file.isFile() && file.getName().endsWith(".jar")) {
				mods.add(file);
			}
		}
	}

	private static void appendMods(ArrayList<File> hypertaleMods, ArrayList<File> mods) {
		for (File file : Objects.requireNonNull(HypertalePaths.hytaleMods.listFiles())) {
			if (file.isFile() && file.getName().endsWith(".jar")) {
				try (ZipFile zipFile = new ZipFile(file)) {
					ZipEntry zipEntry;
					if ((zipEntry = zipFile.getEntry("manifest.json")) != null) {
						String modInfo;
						try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
							modInfo = new String(IOUtils.readAllBytes(inputStream), StandardCharsets.UTF_8);
						} catch (RuntimeException _) {
							modInfo = "";
						}
						if (modInfo.contains("\"Hypertale")) {
							hypertaleMods.add(file);
							mods.add(file);
							continue;
						}
					}
					if (zipFile.getEntry(
							"META-INF/services/com.hypixel.hytale.plugin.early.ClassTransformer") != null) {
						EarlyLogger.log("Early plugin detected in mods -> " + file.getName());
						mods.add(file);
					}
				} catch (IOException e) {
					EarlyLogger.log("Failed to open " + file.getName() + " as a zip file!");
				}
			}
		}
	}
}
