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
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;

public final class HypertaleUnpickConstants {
	private static final int STATUS_DISABLED = -1;
	private static final int STATUS_DEFAULT = 0;

	public static HypertaleUnpickConstants fromPatchedJar(JarFile patchedJar) throws IOException {
		return new HypertaleUnpickConstants();
	}

	public List<ConstantCheck> getClassConstantChecks(ClassNode classNode) {
		ArrayList<ConstantCheck> constantChecks = null;
		HashSet<String> usedConstants = null;
		for (FieldNode fieldNode : classNode.fields) {
			if (((fieldNode.access & (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) ==
					(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) && fieldNode.value != null &&
					"Ljava/lang/String;".equals(fieldNode.desc)) {
				String value = (String) fieldNode.value;
				if (value.length() <= 1) continue;
				if (usedConstants == null) {
					usedConstants = new HashSet<>();
				}
				if (usedConstants.add(value)) {
					if (constantChecks == null) constantChecks = new ArrayList<>();
					constantChecks.add(new ClassConstantCheck(value, classNode.name, fieldNode.name));
				} else {
					System.out.println("Duplicate constant in " + classNode.name + " for \"" + value + "\"");
				}
			}
		}

		return constantChecks == null ? Collections.emptyList() : constantChecks;
	}

	public int methodStatus(ClassNode classNode, MethodNode methodNode) {
		return STATUS_DEFAULT;
	}

	public void patchStringConstant(IdentityHashMap<AbstractInsnNode, InsnList> constantPatching,
									List<ConstantCheck> classConstantCheck, int state,
									LdcInsnNode ldcInsnNode, ArrayList<AbstractInsnNode> list) {
		if (state == STATUS_DISABLED) return;
		if (!list.isEmpty()) {
			list.clear();
		}
		list.add(ldcInsnNode);
		for (ConstantCheck constantCheck : classConstantCheck) {
			constantCheck.applyCheck(list);
		}
		if (list.size() != 1 || list.getFirst().getOpcode() != Opcodes.LDC) {
			constantPatching.put(ldcInsnNode,
					TransformerUtils.compileStringAppendChain(list));
		}
	}

	public static abstract class ConstantCheck {
		private final String value;

		private ConstantCheck(String value) {
			this.value = value;
		}

		public final String getValue() {
			return this.value;
		}

		public void applyCheck(ArrayList<AbstractInsnNode> list) {
			for (int listIndex = 0; listIndex < list.size(); listIndex++) {
				AbstractInsnNode abstractInsnNode = list.get(listIndex);
				if (abstractInsnNode.getOpcode() == Opcodes.LDC) {
					String data = (String) ((LdcInsnNode) abstractInsnNode).cst;
					int valIndex = data.indexOf(this.value);
					if (valIndex == -1) continue;
					if (valIndex == 0) {
						list.set(listIndex, this.makeFieldInsnNode());
						if (data.length() != this.value.length()) {
							list.add(listIndex + 1, new LdcInsnNode(data.substring(this.value.length())));
						}
					} else {
						if (list.size() == 1) {
							list.set(listIndex, new LdcInsnNode(data.substring(0, valIndex)));
						} else {
							((LdcInsnNode) abstractInsnNode).cst = data.substring(0, valIndex);
						}
						list.add(listIndex + 1, this.makeFieldInsnNode());
						if (data.length() != (valIndex + this.value.length())) {
							list.add(listIndex + 2, new LdcInsnNode(data.substring(valIndex + this.value.length())));
						}
						// skip next instruction as we know it is our field instruction
						listIndex++;
					}
				}
			}
		}

		public abstract FieldInsnNode makeFieldInsnNode();
	}

	private static class ClassConstantCheck extends ConstantCheck {
		private final String className;
		private final String fieldName;

		private ClassConstantCheck(String value, String className, String fieldName) {
			super(value);
			this.className = className;
			this.fieldName = fieldName;
		}

		@Override
		public FieldInsnNode makeFieldInsnNode() {
			return new FieldInsnNode(Opcodes.GETSTATIC,
					this.className, this.fieldName, "Ljava/lang/String;");
		}
	}
}
