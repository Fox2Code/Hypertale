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
package com.fox2code.hypertale.dev;


import java.io.*;
import java.util.Map;

public final class HypertaleBuildConfigHelper {
	private static void appendField(PrintStream printStream, String key, String value) {
		printStream.println("    public static final String " + key + " = deInline(\"" + value + "\");");
	}

	public static void makeBuildConfig(
			HypertaleDevConfig hypertaleDevConfig,
			File sourceRoot, boolean force) throws IOException {
		String buildConfigPackage = hypertaleDevConfig.getBuildConfigPackage();
		if (buildConfigPackage == null || (sourceRoot.exists() && !force)) {
			return; // Can't build package if empty or null
		}
		File destinationFolder = new File(sourceRoot, buildConfigPackage.replace('.', File.separatorChar));
		if (!destinationFolder.isDirectory() && !destinationFolder.mkdirs()) {
			throw new IOException("Failed to create build config destination directory!");
		}
		File destinationFile = new File(destinationFolder, "Buildconfig.java");
		if (!destinationFile.isFile() && !destinationFile.createNewFile()) {
			throw new IOException("Failed to create build config destination file!");
		}
		try (FileOutputStream fileOutputStream = new FileOutputStream(destinationFile)) {
			PrintStream printStream = new PrintStream(new BufferedOutputStream(fileOutputStream));
			printStream.println("// Auto generated class, do not modify.");
			printStream.println("package com.fox2code.hypertale.launcher;");
			printStream.println();
			printStream.println("public final class BuildConfig {");
			printStream.println("    private BuildConfig() {}");
			printStream.println("    private static String deInline(String str) { return str; }");
			for (Map.Entry<String, String> entry : hypertaleDevConfig.getBuildConfig().entrySet()) {
				if (entry.getValue() == null || !entry.getKey().matches("[a-zA-Z_][a-zA-Z0-9_]+")) {
					continue; // Skip invalid fields
				}
				appendField(printStream, entry.getKey(), entry.getValue());
			}
			printStream.println("}");
			printStream.flush();
		}
	}
}
