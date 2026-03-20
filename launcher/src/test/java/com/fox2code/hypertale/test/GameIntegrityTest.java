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

import com.fox2code.hypertale.patcher.TransformerUtils;
import com.fox2code.hypertale.utils.SourceUtil;
import com.hypixel.hytale.Main;
import it.unimi.dsi.fastutil.objects.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.BufferedInputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class GameIntegrityTest {
	@Test
	public void testGameIntegrity() throws Exception {
		ClassExistenceChecker classExistenceChecker = new ClassExistenceChecker();
		// Test game integrity by checking for any hypertale data and by loading classes
		try (JarFile jarFile = new JarFile(SourceUtil.getSourceFile(Main.class))) {
			for (JarEntry jarEntry : jarFile.stream().toList()) {
				String entryName = jarEntry.getName();
				if (entryName.startsWith("com/fox2code/hypertale/")) {
					throw new Exception("Found hypertale data in game jar: " + entryName);
				}
				if (entryName.startsWith("com/hypixel/hytale/") && entryName.endsWith(".class")) {
					String asmClassName = entryName.substring(0, entryName.length() - 6);
					String className = asmClassName.replace('/', '.');
					try {
						Class.forName(className, false, Main.class.getClassLoader());
					} catch (Throwable t) {
						throw new RuntimeException("Failed to load class " + className + " from game jar!", t);
					}
					classExistenceChecker.markClassLoaded(asmClassName);
					try (BufferedInputStream bis = new BufferedInputStream(jarFile.getInputStream(jarEntry))) {
						new ClassReader(bis).accept(new ClassRemapper(null, classExistenceChecker), 0);
					}
				}
			}
		}
		classExistenceChecker.throwErrorIfAppropriate();
	}

	private static class ClassExistenceChecker extends Remapper {
		private final Object2BooleanOpenHashMap<String> classMap = new Object2BooleanOpenHashMap<>();
		private final ObjectArraySet<String> checkedClasses = new ObjectArraySet<>();
		private final Object2BooleanFunction<String> isClassLoaded = clsName ->
				this.getClass().getClassLoader().getResource(clsName + ".class") != null;
		private final StringBuilder sb = new StringBuilder();
		private String currentClass = "";

		ClassExistenceChecker() {
			super(TransformerUtils.ASM_BUILD);
		}

		@Override
		public String map(String typeName) {
			if (typeName.startsWith("com/") &&
					!this.classMap.computeIfAbsent(typeName, this.isClassLoaded) &&
					this.checkedClasses.add(typeName)) {
				if (this.checkedClasses.size() == 1) {
					this.sb.append("\nFor class ").append(this.currentClass).append(" missing:");
				}
				this.sb.append("\n    - ").append(typeName);
			}
			return typeName;
		}

		void markClassLoaded(String clsName) {
			this.classMap.put(clsName, true);
			this.checkedClasses.clear();
			this.currentClass = clsName;
		}

		void throwErrorIfAppropriate() {
			if (!this.sb.isEmpty()) {
				throw new IllegalStateException(this.sb.toString());
			}
		}
	}
}
