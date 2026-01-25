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

final class PatchChunkStore extends HypertalePatch {
	PatchChunkStore() {
		super(ChunkStore);
	}

	@Override
	public ClassNode transform(ClassNode classNode) {
		classNode.access |= TransformerUtils.ACC_COMPUTE_FRAMES;
		MethodNode methodNode = TransformerUtils.getMethod(classNode, "remove");
		FieldNode worldField = TransformerUtils.getFieldDesc(classNode, "L" + World + ";");
		boolean didPatch = false;
		for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
			if (abstractInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL &&
					abstractInsnNode instanceof MethodInsnNode methodInsnNode &&
					Long2ObjectConcurrentHashMap.equals(methodInsnNode.owner) &&
					"remove".equals(methodInsnNode.name)) {
				AbstractInsnNode insnNode = TransformerUtils.previousCodeInsn(methodInsnNode, 2);
				if (insnNode.getOpcode() != LLOAD) {
					throw new RuntimeException("Failed to apply patch");
				}
				VarInsnNode varInsnNode = (VarInsnNode) insnNode;
				InsnList injection = new InsnList();
				injection.add(new VarInsnNode(ALOAD, 0));
				injection.add(new FieldInsnNode(GETFIELD,
						ChunkStore, worldField.name, worldField.desc));
				injection.add(new MethodInsnNode(INVOKEVIRTUAL,
						World, "hypertale", "()L" + HypertaleWorld + ";"));
				injection.add(new VarInsnNode(LLOAD, varInsnNode.var));
				injection.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
						HypertaleWorld, "invalidateChunkCache", "(J)V"));
				methodNode.instructions.insert(TransformerUtils.nextCodeInsn(methodInsnNode), injection);
				didPatch = true;
				break;
			}
		}
		if (!didPatch) {
			throw new RuntimeException("Failed to apply patch");
		}
		return classNode;
	}
}
