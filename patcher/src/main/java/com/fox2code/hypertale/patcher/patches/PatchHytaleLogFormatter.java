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

import java.util.Objects;

final class PatchHytaleLogFormatter extends HypertalePatch {
	PatchHytaleLogFormatter() {
		super(HytaleLogFormatter);
	}

	@Override
	public ClassNode transform(ClassNode classNode) {
		classNode.access |= TransformerUtils.ACC_COMPUTE_FRAMES;
		MethodNode stripAnsi = TransformerUtils.getMethod(classNode, "stripAnsi");
		MethodNode format = TransformerUtils.getMethod(classNode, "format");
		int ansiLocal = -1;
		MethodInsnNode getLoggerName = null;
		for (AbstractInsnNode abstractInsnNode : format.instructions) {
			if (abstractInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL &&
					abstractInsnNode instanceof MethodInsnNode methodInsnNode) {
				if (methodInsnNode.owner.equals(ASMString) &&
								methodInsnNode.name.equals("length") &&
								methodInsnNode.desc.equals("()I")) {
					format.instructions.insertBefore(methodInsnNode, new MethodInsnNode(
							Opcodes.INVOKESTATIC, classNode.name, stripAnsi.name, stripAnsi.desc));
					break;
				} else if (methodInsnNode.name.equals("getLoggerName")) {
					getLoggerName = methodInsnNode;
				}
			} else if (abstractInsnNode.getOpcode() == Opcodes.INVOKEINTERFACE &&
					abstractInsnNode instanceof MethodInsnNode methodInsnNode &&
					methodInsnNode.name.equals("getAsBoolean") &&
					TransformerUtils.nextCodeInsn(methodInsnNode) instanceof VarInsnNode varInsnNode) {
				ansiLocal = varInsnNode.var;
			}
		}
		if (ansiLocal == -1) throw new RuntimeException("Failed to get ansiLocal!");
		Objects.requireNonNull(getLoggerName, "getLoggerName");
		AbstractInsnNode next = TransformerUtils.nextCodeInsn(getLoggerName);
		if (next.getOpcode() != Opcodes.ASTORE) {
			throw new RuntimeException("Next opcode isn't ASTORE");
		}
		VarInsnNode store = (VarInsnNode) next;
		InsnList injection = new InsnList();
		injection.add(new VarInsnNode(ILOAD, ansiLocal));
		LabelNode skip = new LabelNode();
		injection.add(new JumpInsnNode(IFNE, skip));
		injection.add(new VarInsnNode(ALOAD, store.var));
		injection.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
				classNode.name, stripAnsi.name, stripAnsi.desc));
		injection.add(new VarInsnNode(ASTORE, store.var));
		injection.add(skip);
		format.instructions.insert(store, injection);
		return classNode;
	}
}
