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
import java.util.HashSet;

public final class HypertaleBundlerPatcher implements HypertaleASMConstants {
	private static final HashSet<String> classToInject = new HashSet<>(Arrays.asList(
			JavaPlugin, ClassTransformer
	));
	private static final Remapper bundlerRemapper = new SimpleRemapper(
			TransformerUtils.ASM_BUILD, HypertaleBundled, $HypertaleBundled);

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
									boolean doInjection = classToInject.contains(superName);
									for (String interfaceName : interfaces) {
										doInjection |= classToInject.contains(interfaceName);
									}
									if (doInjection && !Arrays.asList(interfaces).contains($HypertaleBundled)) {
										interfaces = Arrays.copyOf(interfaces, interfaces.length + 1);
										interfaces[interfaces.length - 1] = $HypertaleBundled;
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
											if (opcode == Opcodes.GETSTATIC && ("hasHypertale".equals(name) ||
													"inHypertalePatcherProcess".equals(name)) &&
													owner.startsWith("com/hypixel/")) {
												owner = $HypertaleBundled;
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
