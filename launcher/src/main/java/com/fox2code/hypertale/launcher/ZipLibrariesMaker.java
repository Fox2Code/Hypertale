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

import com.fox2code.hypertale.utils.IOUtils;
import com.fox2code.hypertale.utils.SourceUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ZipLibrariesMaker {
	static void makeZip(File destination) throws IOException {
		if (!destination.getParentFile().isDirectory()){
			System.out.println("Parent directory of destination not found!");
			return;
		}
		ArrayList<PendingDependency> pendingDependencies = new ArrayList<>(
				DependencyHelper.patcherDependencies.length);
		for (DependencyHelper.Dependency dependency : DependencyHelper.patcherDependencies) {
			DependencyHelper.loadDependency(dependency);
			try {
				File source = SourceUtil.getSourceFile(Class.forName(
						dependency.classCheck(), false, Main.class.getClassLoader()));
				if (!source.isFile()) {
					throw new IOException("Source file not found: " + source.getPath());
				}
				pendingDependencies.add(new PendingDependency(source, dependency,
						"lib/" + source.getName().replace(' ', '_'), dependency.name().replace(':', '@')));
			} catch (ClassNotFoundException e) {
				throw new IOException("Failed to load: " + dependency.name(), e);
			}
		}
		if (!destination.isFile() && !destination.createNewFile()) {
			System.out.println("Failed to create destination file!");
			return;
		}
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(
				new BufferedOutputStream(new FileOutputStream(destination)))) {
			zipOutputStream.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
			zipOutputStream.write(("Manifest-Version: 1.0\n" +
					"Hypertale-Library-Version: " + BuildConfig.HYPERTALE_LIBRARIES_VERSION + "\n")
					.getBytes(StandardCharsets.UTF_8));
			zipOutputStream.closeEntry();
			zipOutputStream.putNextEntry(new ZipEntry("META-INF/hypertale-dependencies.properties"));
			StringBuilder properties = new StringBuilder();
			for (PendingDependency pendingDependency : pendingDependencies) {
				properties.append(pendingDependency.mixName).append("=")
						.append(pendingDependency.dependency.classCheck())
						.append(" ").append(pendingDependency.path)
						.append(" ").append(pendingDependency.dependency.sha256Sum())
						.append("\n");
			}
			zipOutputStream.write(properties.toString().getBytes(StandardCharsets.UTF_8));
			zipOutputStream.closeEntry();
			zipOutputStream.putNextEntry(new ZipEntry("manifest.json"));
			zipOutputStream.write(("{\"Group\": \"Hypertale\"," +
					"\"Name\":\"HypertaleLibraries\"," +
					"\"Version\":\"" + BuildConfig.HYPERTALE_LIBRARIES_VERSION + "\"," +
					"\"Description\": \"Hypertale libraries for offline use of the mod!\"," +
					"\"Authors\": [{" +
					"\"Name\": \"Fox2Code\"," +
					"\"Email\": \"fox2code@gmail.com\"," +
					"\"Url\": \"https://discord.gg/MYcjAhgTKC\"" +
					"}]," +
					"\"Website\": \"https://github.com/Fox2Code/Hypertale\"," +
					"\"ServerVersion\": \"*\"," +
					"\"Dependencies\": {}," +
					"\"OptionalDependencies\": {}," +
					"\"DisabledByDefault\": false," +
					"\"Main\": \"com.fox2code.hypertale.loader.HypertaleLibsPlugin\"" +
					"}").getBytes(StandardCharsets.UTF_8));
			zipOutputStream.closeEntry();
			zipOutputStream.putNextEntry(new ZipEntry("com/fox2code/hypertale/loader/HypertaleLibsPlugin.class"));
			try (InputStream pluginClassStream = new BufferedInputStream(Objects.requireNonNull(
					ZipLibrariesMaker.class.getResourceAsStream(
							"/com/fox2code/hypertale/loader/HypertaleLibsPlugin.class")))) {
				IOUtils.copy(pluginClassStream, zipOutputStream);
			}
			zipOutputStream.closeEntry();
			for (PendingDependency pendingDependency : pendingDependencies) {
				zipOutputStream.putNextEntry(new ZipEntry(pendingDependency.path));
				IOUtils.copy(new FileInputStream(pendingDependency.source), zipOutputStream);
				zipOutputStream.closeEntry();
			}
			zipOutputStream.finish();
			zipOutputStream.flush();
		}
	}

	record PendingDependency(File source, DependencyHelper.Dependency dependency, String path, String mixName) {}
}
