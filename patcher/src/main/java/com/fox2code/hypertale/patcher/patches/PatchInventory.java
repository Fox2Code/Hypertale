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
package com.fox2code.hypertale.patcher.patches;

import com.fox2code.hypertale.patcher.TransformerUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.HashSet;

final class PatchInventory extends HypertalePatch {
	PatchInventory() {
		super(Inventory);
	}

	@Override
	public ClassNode transform(ClassNode classNode) {
		// We don't need a full inventory system all the time.
		patchLazyCombinedItemContainer(classNode);
		patchLazyEventRegistration(classNode);

		return classNode;
	}

	private static void patchLazyCombinedItemContainer(ClassNode classNode) {
		MethodNode buildCombinedContains = TransformerUtils.findMethod(classNode, "buildCombinedContains", "()V");
		MethodNode postDecode = TransformerUtils.findMethod(classNode, "postDecode", "()V");
		FieldNode combinedEverything = TransformerUtils.findField(classNode, "combinedEverything");
		if (buildCombinedContains == null || postDecode == null || combinedEverything == null) {
			return;
		}
		classNode.access |= TransformerUtils.ACC_COMPUTE_FRAMES;

		MethodNode clearCombinedContains = new MethodNode(ACC_PRIVATE,
				"hypertaleClearCombinedContains", "()V", null, null);

		HashSet<String> fieldToLookFor = new HashSet<>();
		for (AbstractInsnNode abstractInsnNode : buildCombinedContains.instructions) {
			if (abstractInsnNode.getOpcode() == Opcodes.PUTFIELD &&
					abstractInsnNode instanceof FieldInsnNode fieldInsnNode &&
					fieldInsnNode.owner.equals(Inventory) &&
					!fieldInsnNode.name.startsWith("hypertale") &&
					fieldInsnNode.desc.length() > 1) {
				clearCombinedContains.instructions.add(new VarInsnNode(ALOAD, 0));
				clearCombinedContains.instructions.add(new InsnNode(ACONST_NULL));
				clearCombinedContains.instructions.add(new FieldInsnNode(PUTFIELD,
						Inventory, fieldInsnNode.name, fieldInsnNode.desc));
				fieldToLookFor.add(fieldInsnNode.name);
			}
		}

		clearCombinedContains.instructions.add(new InsnNode(RETURN));

		classNode.methods.add(clearCombinedContains);

		for (MethodNode methodNode : classNode.methods) {
			if (methodNode == postDecode || methodNode.name.equals("<init>")) {
				for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
					if (abstractInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL &&
							abstractInsnNode instanceof MethodInsnNode methodInsnNode &&
							methodInsnNode.owner.equals(Inventory) &&
							methodInsnNode.name.equals("buildCombinedContains") &&
							methodInsnNode.desc.equals("()V")) {
						methodInsnNode.name = "hypertaleClearCombinedContains";
					}
				}
			}
		}
		for (MethodNode methodNode : classNode.methods) {
			if (methodNode == buildCombinedContains || methodNode == clearCombinedContains) {
				continue;
			}
			boolean prepend = false;
			AbstractInsnNode onlyGetField = null;
			String onlyFieldName = null;
			for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
				if (abstractInsnNode.getOpcode() == Opcodes.GETFIELD &&
						abstractInsnNode instanceof FieldInsnNode fieldInsnNode &&
						fieldInsnNode.owner.equals(Inventory) &&
						fieldToLookFor.contains(fieldInsnNode.name)) {
					if (prepend) {
						onlyGetField = null;
						if (!fieldInsnNode.name.equals(onlyFieldName)) {
							onlyFieldName = null;
						}
					} else {
						onlyGetField = abstractInsnNode;
						onlyFieldName = fieldInsnNode.name;
					}
					prepend = true;
				}
			}
			if (prepend) {
				InsnList preCall = new InsnList();
				preCall.add(new VarInsnNode(ALOAD, 0));
				preCall.add(new FieldInsnNode(GETFIELD, classNode.name,
						combinedEverything.name, combinedEverything.desc));
				LabelNode skip = new LabelNode();
				preCall.add(new JumpInsnNode(IFNONNULL, skip));
				preCall.add(new VarInsnNode(ALOAD, 0));
				preCall.add(new MethodInsnNode(INVOKEVIRTUAL, Inventory,
						"buildCombinedContains", "()V", false));
				preCall.add(skip);
				AbstractInsnNode previous;
				if (onlyGetField != null && (previous =
						TransformerUtils.previousCodeInsn(onlyGetField)).getOpcode() == Opcodes.ALOAD) {
					methodNode.instructions.insertBefore(previous, preCall);
				} else {
					TransformerUtils.insertToBeginningOfCode(methodNode, preCall);
				}
			}
		}
	}

	private static void patchLazyEventRegistration(ClassNode classNode) {
		MethodNode registerChangeEvents = TransformerUtils.findMethod(classNode, "registerChangeEvents", "()V");
		FieldNode toolChange = TransformerUtils.findField(classNode, "toolChange");
		FieldNode entity = TransformerUtils.findField(classNode, "entity");
		if (registerChangeEvents == null || toolChange == null || entity == null) {
			return;
		}
		classNode.access |= TransformerUtils.ACC_COMPUTE_FRAMES;
		MethodNode hypertaleRegisterChangeEvents = new MethodNode(ACC_PRIVATE,
				"hypertaleRegisterChangeEvents", "()V", null, null);
		hypertaleRegisterChangeEvents.instructions.add(new VarInsnNode(ALOAD, 0));
		hypertaleRegisterChangeEvents.instructions.add(new FieldInsnNode(
				GETFIELD, classNode.name, toolChange.name, toolChange.desc));
		LabelNode skip = new LabelNode();
		hypertaleRegisterChangeEvents.instructions.add(new JumpInsnNode(IFNONNULL, skip));
		hypertaleRegisterChangeEvents.instructions.add(new VarInsnNode(ALOAD, 0));
		hypertaleRegisterChangeEvents.instructions.add(new FieldInsnNode(
				GETFIELD, classNode.name, entity.name, entity.desc));
		hypertaleRegisterChangeEvents.instructions.add(new JumpInsnNode(IFNULL, skip));
		hypertaleRegisterChangeEvents.instructions.add(new VarInsnNode(ALOAD, 0));
		hypertaleRegisterChangeEvents.instructions.add(new MethodInsnNode(
				INVOKEVIRTUAL, classNode.name, "registerChangeEvents", "()V", false));
		hypertaleRegisterChangeEvents.instructions.add(skip);
		hypertaleRegisterChangeEvents.instructions.add(new InsnNode(RETURN));

		classNode.methods.add(hypertaleRegisterChangeEvents);

		for (MethodNode methodNode : classNode.methods) {
			if (methodNode == registerChangeEvents || methodNode == hypertaleRegisterChangeEvents) {
				continue;
			}
			boolean hasNonNullEntitySet = false;
			for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
				if (abstractInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL &&
						abstractInsnNode instanceof MethodInsnNode methodInsnNode &&
						methodInsnNode.owner.equals(Inventory) &&
						methodInsnNode.name.equals("registerChangeEvents") &&
						methodInsnNode.desc.equals("()V")) {
					methodInsnNode.name = "hypertaleRegisterChangeEvents";
				} else if (abstractInsnNode.getOpcode() == Opcodes.PUTFIELD &&
						abstractInsnNode instanceof FieldInsnNode fieldInsnNode &&
						fieldInsnNode.owner.equals(classNode.name) &&
						fieldInsnNode.name.equals(entity.name) &&
						TransformerUtils.previousCodeInsn(abstractInsnNode).getOpcode() != Opcodes.ACONST_NULL) {
					hasNonNullEntitySet = true;
				}
			}
			if (hasNonNullEntitySet) {
				InsnList injection = new InsnList();
				injection.add(new VarInsnNode(ALOAD, 0));
				injection.add(new MethodInsnNode(INVOKEVIRTUAL, Inventory,
						"hypertaleRegisterChangeEvents", "()V", false));
				TransformerUtils.insertToEndOfCode(methodNode, injection);
			}
		}
	}
}
