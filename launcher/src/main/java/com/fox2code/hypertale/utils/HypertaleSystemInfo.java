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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public final class HypertaleSystemInfo {
	public static final String SYSTEM;

	static {
		String os = null;
		if (HypertalePlatform.getPlatform() == HypertalePlatform.LINUX) {
			for (String possiblePath : new String[]{
					"/etc/os-release", "/usr/lib/os-release",
					"/etc/initrd-release", "/etc/lsb-release"}) {
				File possibleFile = new File(possiblePath);
				if (possibleFile.exists() && possibleFile.canRead()) {
					Optional<String> optional = Optional.empty();
					try {
						optional = Files.readAllLines(possibleFile.toPath()).stream()
								.filter(line -> line.startsWith("PRETTY_NAME=") ||
										line.startsWith("DISTRIB_DESCRIPTION=")).findFirst();
					} catch (IOException _) {}
					if (optional.isPresent()) {
						os = optional.get();
						int equal = os.indexOf('=');
						os = os.substring(equal + 1).trim();
						if (os.startsWith("\"") && os.endsWith("\"")) {
							os = os.substring(1, os.length() - 1).trim();
						}
						break;
					}
				}
			}
			if (os == null) {
				os = System.getProperty("os.name") + " " + System.getProperty("os.version");
			} else {
				os = os + " (Using: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + ")";
			}
		} else if (HypertalePlatform.getPlatform() == HypertalePlatform.WINDOWS) {
			os = System.getProperty("os.name") + " (Using: NT " + System.getProperty("os.version") + ")";
		} else {
			os = System.getProperty("os.name") + " " + System.getProperty("os.version");
		}
		SYSTEM = os;
	}
}
