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
package com.fox2code.hypertale.test;

import com.fox2code.hypertale.utils.SourceUtil;
import com.hypixel.hytale.Main;
import org.junit.jupiter.api.Test;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class GameIntegrityTest {
	@Test
	public void testGameIntegrity() throws Exception {
		// Test game integrity by checking for any hypertale data and by loading classes
		try (JarFile jarFile = new JarFile(SourceUtil.getSourceFile(Main.class))) {
			for (JarEntry jarEntry : jarFile.stream().toList()) {
				String entryName = jarEntry.getName();
				if (entryName.startsWith("com/fox2code/hypertale/")) {
					throw new Exception("Found hypertale data in game jar: " + entryName);
				}
				if (entryName.startsWith("com/hypixel/hytale/") && entryName.endsWith(".class")) {
					String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
					try {
						Class.forName(className, false, Main.class.getClassLoader());
					} catch (Throwable t) {
						throw new RuntimeException("Failed to load class " + className + " from game jar!", t);
					}
				}
			}
		}
	}
}
