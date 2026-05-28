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
import com.fox2code.hypertale.utils.jvm.CompatStringConcatFactory;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSetContainer;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Patcher used to improve mods compiled with Hypertale gradle plugin.
 */
public final class HypertaleDevModPatcher implements HypertaleASMConstants {
	private static final HashSet<String> classToInject = new HashSet<>(Arrays.asList(
			JavaPlugin, ClassTransformer
	));
	private static final Remapper bundlerRemapper = new SimpleRemapper(
			TransformerUtils.ASM_BUILD, HypertaleBundled, $HypertaleBundled);
	private static final Remapper noOpRemapper = new Remapper(TransformerUtils.ASM_BUILD) {};

	public static void patch(Project project) {
		SourceSetContainer sourceSet = (SourceSetContainer) project.getExtensions().getByName("sourceSets");
		for (File directory : sourceSet.getByName("main").getOutput().getClassesDirs()) {
			if (directory.isDirectory()) {
				try {
					Files.walk(directory.toPath()).filter(path ->
							path.toString().endsWith(".class")).forEach(path -> {
						try {
							Files.write(path, patchClass(Files.readAllBytes(path), false));
						} catch (IOException _) {}
					});
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private static byte[] patchClass(byte[] bytes, boolean exclusive) {
		ClassReader classReader = new ClassReader(bytes);
		SafeClassWriter safeClassWriter = new SafeClassWriter(0);
		final boolean[] forceASMVerTo8 = new boolean[]{false};
		classReader.accept(new ClassRemapper(
				TransformerUtils.ASM_BUILD, safeClassWriter,
				exclusive ? noOpRemapper : bundlerRemapper) {
			boolean makeJVMCompatGlobal = false;
			boolean openCst = false;

			@Override
			public void visit(int version, int access, String name, String signature,
			                  String superName, String[] interfaces) {
				boolean doInjection = classToInject.contains(superName);
				for (String interfaceName : interfaces) {
					doInjection |= classToInject.contains(interfaceName);
				}
				if (doInjection && !exclusive && !Arrays.asList(interfaces).contains($HypertaleBundled)) {
					interfaces = Arrays.copyOf(interfaces, interfaces.length + 1);
					interfaces[interfaces.length - 1] = $HypertaleBundled;
				}
				super.visit(version, access, name, signature, superName, interfaces);
				this.openCst = (access & Opcodes.ACC_ENUM) != 0;
			}

			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				if (descriptor.equals("L" + MakeJVMCompat + ";")) {
					this.makeJVMCompatGlobal = true;
					forceASMVerTo8[0] = true;
				}
				return super.visitAnnotation(descriptor, visible);
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor,
			                                 String signature, String[] exceptions) {
				if (this.openCst && name.equals("<init>")) {
					access &= ~Opcodes.ACC_PRIVATE;
				}
				if (name.equals("main") && descriptor.equals("([Ljava/lang/String;)V")) {
					access |= Opcodes.ACC_PUBLIC;
				}
				return new MethodVisitor(TransformerUtils.ASM_BUILD,
						super.visitMethod(access, name, descriptor, signature, exceptions)) {
					boolean makeJVMCompat = makeJVMCompatGlobal;

					@Override
					public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
						if (descriptor.equals("L" + MakeJVMCompat + ";")) {
							this.makeJVMCompat = true;
							forceASMVerTo8[0] = true;
						}

						return super.visitAnnotation(descriptor, visible);
					}

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

					@Override
					public void visitInvokeDynamicInsn(
							String name, String descriptor,
							Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
						if (makeJVMCompat && bootstrapMethodHandle.getOwner().equals(StringConcatFactory)) {
							bootstrapMethodHandle = new Handle(
									bootstrapMethodHandle.getTag(),
									CompatStringConcatFactory,
									bootstrapMethodHandle.getName(),
									bootstrapMethodHandle.getDesc(),
									false
							);
						}
						super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
					}
				};
			}
		}, 0);
		byte[] classBytes = safeClassWriter.toByteArray();
		if (forceASMVerTo8[0]) {
			classBytes[7] = 52;
		}
		return classBytes;
	}

	static void main(String... args) throws IOException {
		if (args.length != 1) {
			System.out.println("Expecting 1 argument: [INPUT-JAR]");
			System.exit(-1);
			return;
		}
		File input = new File(args[0]).getAbsoluteFile();
		if (!input.exists()) {
			System.out.println("Input file does not exist: " + input);
			System.exit(-1);
			return;
		}
		File tempOutput = new File(input.getAbsolutePath() + ".tmp");
		try (ZipFile jarFile = new ZipFile(input);
		     ZipOutputStream jarOutputStream = new ZipOutputStream(
					 new BufferedOutputStream(new FileOutputStream(tempOutput)))) {
			jarFile.stream().forEach(entry -> {
				try {
					JarEntry newEntry = new JarEntry(entry.getName());
					newEntry.setTime(entry.getTime());
					jarOutputStream.putNextEntry(newEntry);
					if (entry.getName().endsWith(".class")) {
						byte[] classBytes = jarFile.getInputStream(entry).readAllBytes();
						jarOutputStream.write(patchClass(classBytes, true));
					} else {
						jarFile.getInputStream(entry).transferTo(jarOutputStream);
					}
					jarOutputStream.closeEntry();
				} catch (IOException e) {
					throw new RuntimeException(entry.getName(), e);
				}
			});
			jarOutputStream.finish();
		}
		if (!input.delete()) {
			System.out.println("Failed to delete original jar file");
			System.exit(-1);
			return;
		}
		if (!tempOutput.renameTo(input)) {
			System.out.println("Failed to rename patched jar file");
			System.exit(-1);
			return;
		}
		System.out.println("Done!");
	}
}
