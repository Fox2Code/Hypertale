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
import org.objectweb.asm.tree.*;

final class PatchUniverse extends HypertalePatch {
	PatchUniverse() {
		super(Universe);
	}

	@Override
	public ClassNode transform(ClassNode classNode) {
		// Add checks on player reference validity before doing any actions!
		// Fix a bug when "/stop" is run while players are online.
		MethodNode methodNode = TransformerUtils.findMethod(classNode, "removePlayer");
		MethodNode lambda = TransformerUtils.findMethod(classNode, "lambda$removePlayer$0");
		if (methodNode == null || lambda == null ||
				(lambda.access & ACC_STATIC) == 0) {
			return classNode;
		}
		// Check reference inside the method
		for (AbstractInsnNode abstractInsnNode : methodNode.instructions.toArray()) {
			if (abstractInsnNode.getOpcode() == IFNONNULL &&
					abstractInsnNode instanceof JumpInsnNode jumpInsnNode) {
				abstractInsnNode = TransformerUtils.nextCodeInsn(jumpInsnNode);
				if (abstractInsnNode.getOpcode() != ALOAD ||
						!(abstractInsnNode instanceof VarInsnNode nextVarInsnNode) ||
						nextVarInsnNode.var != 0) {
					continue;
				}
				abstractInsnNode = TransformerUtils.previousCodeInsn(jumpInsnNode);
				if (abstractInsnNode.getOpcode() != ALOAD ||
						!(abstractInsnNode instanceof VarInsnNode varInsnNode)) {
					continue;
				}
				AbstractInsnNode previous2 = TransformerUtils.previousCodeInsn(varInsnNode);
				if (previous2.getOpcode() != ASTORE ||
						!(previous2 instanceof VarInsnNode previousVarInsnNode) ||
						previousVarInsnNode.var != varInsnNode.var) {
					continue;
				}
				int varIndex = varInsnNode.var;
				InsnList injection = new InsnList();
				LabelNode skipNextCheck = new LabelNode();
				injection.add(new JumpInsnNode(IFNULL, skipNextCheck));
				injection.add(new VarInsnNode(ALOAD, varIndex));
				injection.add(new MethodInsnNode(INVOKEVIRTUAL,
						Ref, "isValid", "()Z", false));
				injection.add(new JumpInsnNode(IFNE, jumpInsnNode.label));
				injection.add(skipNextCheck);
				methodNode.instructions.insert(jumpInsnNode, injection);
				methodNode.instructions.remove(jumpInsnNode);
				classNode.access |= TransformerUtils.ACC_COMPUTE_FRAMES;
				break;
			}
		}
		// Check reference validity inside the lambda that is run later.
		AbstractInsnNode firstCodeInsnNode = null;
		for (AbstractInsnNode abstractInsnNode : lambda.instructions.toArray()) {
			if (firstCodeInsnNode == null) {
				if (abstractInsnNode.getOpcode() != -1) {
					if (abstractInsnNode.getOpcode() != ALOAD ||
							!(abstractInsnNode instanceof VarInsnNode varInsnNode) ||
							varInsnNode.var != 0) {
						return classNode;
					}
					firstCodeInsnNode = abstractInsnNode;
				}
			} else if (abstractInsnNode.getOpcode() == ASTORE &&
					abstractInsnNode instanceof VarInsnNode varInsnNode) {
				InsnList injectionPre = new InsnList();
				injectionPre.add(new VarInsnNode(ALOAD, 0));
				injectionPre.add(new MethodInsnNode(INVOKEVIRTUAL, Ref, "isValid", "()Z", false));
				LabelNode valid = new LabelNode();
				injectionPre.add(new JumpInsnNode(IFEQ, valid));
				injectionPre.add(new InsnNode(ACONST_NULL));
				LabelNode invalid = new LabelNode();
				injectionPre.add(new JumpInsnNode(GOTO, invalid));
				injectionPre.add(valid);
				lambda.instructions.insertBefore(firstCodeInsnNode, injectionPre);
				lambda.instructions.insertBefore(varInsnNode, invalid);
				classNode.access |= TransformerUtils.ACC_COMPUTE_FRAMES;
				break;
			}
		}
		return classNode;
	}
}
