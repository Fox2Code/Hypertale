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

import com.fox2code.hypertale.patcher.HypertaleASMConstants;
import com.fox2code.hypertale.patcher.SafeClassWriter;
import com.fox2code.hypertale.patcher.TransformerUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSetContainer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;

public final class HypertaleBundlerPatcher implements HypertaleASMConstants {
	private static final HashMap<String, String> bundlerHashMap = new HashMap<>();
	private static final HashMap<String, String> injectionHashMap = new HashMap<>();
	private static final HashMap<String, String> fieldRedirectHashMap = new HashMap<>();
	private static final Remapper bundlerRemapper = new SimpleRemapper(TransformerUtils.ASM_BUILD, bundlerHashMap);

	static {
		addBundledMapping(JavaPlugin, HypertaleJavaPlugin, "hasHypertale");
	}

	private static void addBundledMapping(String hytaleClass, String hypertaleClass, String optionalField) {
		String bundledClass = "META-INF/bundled/#" +
				hypertaleClass.substring(hypertaleClass.lastIndexOf('/') + 1);
		bundlerHashMap.put(hypertaleClass, bundledClass);
		injectionHashMap.put(hytaleClass, bundledClass);
		if (optionalField != null) {
			fieldRedirectHashMap.put(hytaleClass + "#" + optionalField, bundledClass);
		}
	}

	public static void patch(Project project) {
		SourceSetContainer sourceSet = (SourceSetContainer) project.getExtensions().getByName("sourceSets");
		for (File directory : sourceSet.getByName("main").getOutput().getClassesDirs()) {
			if (directory.isDirectory()) {
				try {
					Files.walk(directory.toPath()).filter(path ->
							path.toString().endsWith(".class")).forEach(path -> {
						try {
							ClassReader classReader = new ClassReader(Files.readAllBytes(path));
							SafeClassWriter safeClassWriter = new SafeClassWriter(0);
							classReader.accept(new ClassRemapper(
									TransformerUtils.ASM_BUILD, safeClassWriter, bundlerRemapper) {
								@Override
								public void visit(int version, int access, String name, String signature,
												  String superName, String[] interfaces) {
									String interfaceInjection = injectionHashMap.get(superName);
									if (interfaceInjection != null && Arrays.stream(interfaces)
											.noneMatch(interfaceInjection::equals)) {
										interfaces = Arrays.copyOf(interfaces, interfaces.length + 1);
										interfaces[interfaces.length - 1] = interfaceInjection;
									}
									super.visit(version, access, name, signature, superName, interfaces);
								}

								@Override
								public MethodVisitor visitMethod(int access, String name, String descriptor,
																 String signature, String[] exceptions) {
									return new MethodVisitor(TransformerUtils.ASM_BUILD,
											super.visitMethod(access, name, descriptor, signature, exceptions)) {
										@Override
										public void visitFieldInsn(
												int opcode, String owner, String name, String descriptor) {
											if (opcode == Opcodes.GETSTATIC) {
												owner = fieldRedirectHashMap.getOrDefault(
														owner + "#" + name, owner);
											}
											super.visitFieldInsn(opcode, owner, name, descriptor);
										}
									};
								}
							}, 0);
							Files.write(path, safeClassWriter.toByteArray());
						} catch (IOException _) {}
					});
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
