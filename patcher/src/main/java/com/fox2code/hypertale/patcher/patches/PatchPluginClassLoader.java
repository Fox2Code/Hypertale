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

final class PatchPluginClassLoader extends HypertalePatch {
	PatchPluginClassLoader() {
		super(PluginClassLoader);
	}

	@Override
	public ClassNode transform(ClassNode classNode) {
		classNode.access |= TransformerUtils.ACC_COMPUTE_FRAMES;
		MethodNode isFromThirdPartyPlugin = TransformerUtils.getMethod(classNode, "isFromThirdPartyPlugin");
		for (AbstractInsnNode abstractInsnNode : isFromThirdPartyPlugin.instructions) {
			if (abstractInsnNode.getOpcode() == Opcodes.LDC &&
					abstractInsnNode instanceof LdcInsnNode ldcInsnNode &&
					"ThirdPartyPlugin".equals(ldcInsnNode.cst) &&
					abstractInsnNode.getNext() instanceof VarInsnNode varInsnNode) {
				InsnList injection = new InsnList();
				injection.add(new VarInsnNode(Opcodes.ALOAD, varInsnNode.var));
				injection.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
						StackTraceElement, "getClassName", "()L" + ASMString + ";", false));
				injection.add(new LdcInsnNode("com.fox2code.hypertale."));
				injection.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
						ASMString, "startsWith", "(L" + ASMString + ";)Z", false));
				LabelNode skip = new LabelNode();
				injection.add(new JumpInsnNode(Opcodes.IFEQ, skip));
				injection.add(new InsnNode(Opcodes.ICONST_1));
				injection.add(new InsnNode(Opcodes.IRETURN));
				injection.add(skip);
				isFromThirdPartyPlugin.instructions.insertBefore(ldcInsnNode, injection);
				break;
			}
		}
		return classNode;
	}
}
