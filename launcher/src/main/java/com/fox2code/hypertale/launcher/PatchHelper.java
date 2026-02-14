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

import com.fox2code.hypertale.patcher.Optimizer;
import com.fox2code.hypertale.patcher.SafeClassWriter;
import com.fox2code.hypertale.patcher.mixin.MixinLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.function.BiFunction;
import java.util.function.Consumer;

final class PatchHelper {
	private static boolean installed = false;

	private PatchHelper() {}

	@SuppressWarnings("unchecked")
	private static final Consumer<BiFunction<String, byte[], byte[]>> initCLSetClassTransformer =
			(Consumer<BiFunction<String, byte[], byte[]>>)
					System.getProperties().get("hypertale.initCLSetClassTransformer");

	static void install() {
		if (installed) return;
		installed = true;
		Instrumentation instrumentation = HypertaleAgent.getInstrumentation();
		if (instrumentation != null) {
			EarlyLogger.log("Using agent transformer for patching!");
			instrumentation.addTransformer(new ClassFileTransformer() {
				@Override
				public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
										ProtectionDomain protectionDomain, byte[] classfileBuffer) {
					if (className.startsWith("com/hypixel/hytale/")) {
						classfileBuffer = MixinLoader.transformClass(className.replace('/', '.'), classfileBuffer);
					}
					if (loader == null || loader == Optimizer.class.getClassLoader() ||
							loader.getClass().getName().startsWith("jdk.internal.") ||
							!Optimizer.canOptimize(className)) {
						return classfileBuffer;
					}
					ClassReader classReader = new ClassReader(classfileBuffer);
					ClassNode classNode = new ClassNode();
					classReader.accept(classNode, 0);
					Optimizer.patchClass(classNode);
					SafeClassWriter classWriter = new SafeClassWriter(0);
					classNode.accept(classWriter);
					return classWriter.toByteArray();
				}
			});
		} else if (initCLSetClassTransformer != null) {
			EarlyLogger.log("Using late transformer for patching!");
			initCLSetClassTransformer.accept(MixinLoader::transformClass);
		} else {
			throw new IllegalStateException("Unsupported environment!");
		}
	}
}
