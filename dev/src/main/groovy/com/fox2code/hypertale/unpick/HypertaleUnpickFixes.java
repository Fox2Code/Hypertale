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
package com.fox2code.hypertale.unpick;

import com.fox2code.hypertale.patcher.TransformerUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarFile;

final class HypertaleUnpickFixes {
	private static final HashMap<String, List<UnpickFix>> fixes = new HashMap<>();

	static {
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
				Objects.requireNonNull(HypertaleUnpickFixes.class.getResourceAsStream("fixes.txt")),
				StandardCharsets.UTF_8))) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.isEmpty()) continue;
				String[] tokens = line.split(" ", 2);
				if (tokens.length != 2) continue;
				UnpickFix fix = switch (tokens[0]) {
					case "-<clinit>" -> new NoClInitUnpickFix(tokens[1]);
					case "-assert" -> new NoAssertUnpickFix(tokens[1]);
					case "param" -> new ParamUnpickFix(tokens[1]);
					default -> null;
				};
				if (fix != null) {
					fixes.computeIfAbsent(fix.target, _ -> new LinkedList<>()).add(fix);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read fixes.txt", e);
		}
	}

	static ClassNode fix(JarFile jarFile, ClassNode classNode) throws IOException {
		List<UnpickFix> unpickFixes = fixes.get(classNode.name);
		if (unpickFixes == null) {
			return classNode;
		}
		for (UnpickFix unpickFix : unpickFixes) {
			classNode = unpickFix.apply(jarFile, classNode);
		}
		return classNode;
	}

	private static abstract class UnpickFix {
		final String target;

		private UnpickFix(String target) {
			this.target = target;
		}

		abstract ClassNode apply(JarFile jarFile, ClassNode classNode) throws IOException;
	}

	private static class NoClInitUnpickFix extends UnpickFix {

		private NoClInitUnpickFix(String target) {
			super(target);
		}

		@Override
		ClassNode apply(JarFile jarFile, ClassNode classNode) {
			classNode.methods.removeIf(methodNode -> methodNode.name.equals("<clinit>"));
			return classNode;
		}
	}

	private static class NoAssertUnpickFix extends UnpickFix {

		private NoAssertUnpickFix(String target) {
			super(target);
		}

		@Override
		ClassNode apply(JarFile jarFile, ClassNode classNode) {
			classNode.methods.forEach(methodNode -> {
				boolean needDeadCodeRemoval = false;
				for (AbstractInsnNode abstractInsnNode : methodNode.instructions.toArray()) {
					if (abstractInsnNode != null && abstractInsnNode.getOpcode() == Opcodes.GETSTATIC &&
							abstractInsnNode instanceof FieldInsnNode fieldInsnNode &&
							"$assertionsDisabled".equals(fieldInsnNode.name)) {
						AbstractInsnNode next = TransformerUtils.nextCodeInsn(abstractInsnNode);
						if (next.getOpcode() == Opcodes.IFNE) {
							methodNode.instructions.remove(abstractInsnNode);
							methodNode.instructions.set(next, new JumpInsnNode(
									Opcodes.GOTO, ((JumpInsnNode) next).label));
							needDeadCodeRemoval = true;
						} else if (next.getOpcode() == Opcodes.IFEQ) {
							methodNode.instructions.remove(abstractInsnNode);
							methodNode.instructions.remove(next);
							needDeadCodeRemoval = true;
						} else {
							methodNode.instructions.set(abstractInsnNode, new InsnNode(Opcodes.ICONST_1));
						}
					}
				}
				if (needDeadCodeRemoval) {
					Analyzer<BasicValue> a =
							new Analyzer<>(new BasicInterpreter());
					try {
						a.analyze(classNode.name, methodNode);
						Frame<BasicValue>[] frames = a.getFrames();
						AbstractInsnNode[] insns = methodNode.instructions.toArray();
						for (int i = 0; i < frames.length; ++i) {
							if (frames[i] == null && !(insns[i] instanceof LabelNode)) {
								methodNode.instructions.remove(insns[i]);
							}
						}
					} catch (AnalyzerException e) {
						throw new RuntimeException("Failed to remove dead code", e);
					}

				}
			});

			return classNode;
		}
	}

	private static class ParamUnpickFix extends UnpickFix {
		private final String[] tokens;
		private final int argCount;

		private ParamUnpickFix(String target) {
			String[] tokens = target.split(" ", 2);
			super(tokens[0]);
			this.tokens = tokens;
			this.argCount = tokens.length - 1;
		}

		@Override
		ClassNode apply(JarFile jarFile, ClassNode classNode) throws IOException {
			for (MethodNode methodNode : classNode.methods) {
				if (methodNode.name.equals(this.tokens[0]) &&
						Type.getArgumentCount(methodNode.name) == this.argCount) {
					int index = (methodNode.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
					if (methodNode.localVariables == null) {
						methodNode.localVariables = new ArrayList<>(this.argCount + index);
					}
					if (methodNode.parameters == null) {
						methodNode.parameters = new ArrayList<>(this.argCount);
					} else {
						methodNode.parameters.clear();
					}
					if ((methodNode.access & Opcodes.ACC_STATIC) == 0) {
						TransformerUtils.setThisParameterName(classNode, methodNode);
					}
					Type[] arguments = Type.getArgumentTypes(methodNode.desc);
					for (int i = 0; i < arguments.length; ++i) {
						TransformerUtils.setParameterName(methodNode, index, this.tokens[i + 1]);
						methodNode.parameters.add(new ParameterNode(this.tokens[i + 1], 0));
						index += arguments[i].getSize();
					}
				}
			}
			return classNode;
		}
	}
}
