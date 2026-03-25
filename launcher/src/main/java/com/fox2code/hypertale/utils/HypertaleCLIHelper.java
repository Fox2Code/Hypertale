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
package com.fox2code.hypertale.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Predicate;

public final class HypertaleCLIHelper {
	private HypertaleCLIHelper() {}

	private static ArrayList<File> getArgsPaths(@Nonnull String[] args, String argType, Predicate<File> filter) {
		ArrayList<File> paths = new ArrayList<>();
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals(argType) && i + 1 < args.length) {
				for (String pathStr : args[i + 1].split(",")) {
					File file = pathToFile(pathStr);
					if (file != null && (filter == null || filter.test(file))) {
						paths.add(file);
					}
				}
			} else if (args[i].startsWith(argType + "=")) {
				String value = args[i].substring(argType.length() + 1);
				for (String pathStr : value.split(",")) {
					File file = pathToFile(pathStr);
					if (file != null && (filter == null || filter.test(file))) {
						paths.add(file);
					}
				}
			}
		}

		return paths;
	}

	public static ArrayList<File> getEarlyPluginPaths(@Nonnull String[] args) {
		return getArgsPaths(args, "--early-plugins", File::isDirectory);
	}

	public static ArrayList<File> getModsPaths(@Nonnull String[] args) {
		return getArgsPaths(args, "--mods", File::isDirectory);
	}

	public static @Nullable File pathToFile(String path) {
		// Filter out the most egregious invalid paths early.
		if (path == null || path.indexOf('\0') != -1 ||
				(path = path.trim()).isEmpty()) {
			return null;
		}
		// Will throw InvalidPathException or UnsupportedOperationException if the path is not a file.
		try {
			return Path.of(path).toFile();
		} catch (RuntimeException _) {
			return null;
		}
	}
}
