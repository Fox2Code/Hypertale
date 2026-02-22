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
package com.fox2code.hypertale.patcher;

import com.fox2code.hypertale.launcher.EarlyLogger;
import com.hypixel.hytale.plugin.early.ClassTransformer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.ServiceLoader;

/**
 * Implement support for hytale early-plugins class transformers.
 */
final class HytalePatcherHelper {
	private static final ArrayList<ClassTransformer> classTransformers = new ArrayList<>();

	static void init(boolean logProgress) {
		if (!classTransformers.isEmpty()) return;
		System.setProperty("hypertale.patcherProcess", "true");
		for(ClassTransformer transformer : ServiceLoader.load(ClassTransformer.class)) {
			Class<?> transformerClass;
			if (transformer == null || (transformerClass = transformer.getClass())
					.getName().startsWith("com.fox2code.hypertale.init.")) {
				continue;
			}
			if (transformerClass.getName().startsWith("com.build_9.hyxin.")) {
				continue;
			}
			if (logProgress) {
				EarlyLogger.log("Loading Hytale early plugin transformer: " +
						transformerClass.getName() +
						" (priority=" + transformer.priority() + ")");
			}
			classTransformers.add(transformer);
		}
		classTransformers.sort(Comparator.comparingInt(ClassTransformer::priority).reversed());
	}

	static byte[] patchClass(byte[] bytes, String internalClassName) {
		if (!classTransformers.isEmpty()) {
			String className = internalClassName.replace('/', '.');
			for (ClassTransformer classTransformer : classTransformers) {
				byte[] transformed = classTransformer.transform(className, internalClassName, bytes);
				if (transformed != null) bytes = transformed;
			}
		}
		return bytes;
	}
}
