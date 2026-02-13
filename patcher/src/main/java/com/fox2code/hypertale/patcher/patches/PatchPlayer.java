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

final class PatchPlayer extends HypertalePatch {
	PatchPlayer() {
		super(Player);
	}

	@Override
	public ClassNode transform(ClassNode classNode) {
		classNode.access |= TransformerUtils.ACC_COMPUTE_FRAMES;
		final FieldNode hypertalePlayer = new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
				"hypertalePlayer", "L" + HypertalePlayer + ";", null, null);
		classNode.fields.add(hypertalePlayer);
		MethodNode constructor = TransformerUtils.getMethod(classNode, "<init>");
		InsnList cstInjection = new InsnList();
		cstInjection.add(new VarInsnNode(Opcodes.ALOAD, 0));
		cstInjection.add(new TypeInsnNode(Opcodes.NEW, HypertalePlayer));
		cstInjection.add(new InsnNode(Opcodes.DUP));
		cstInjection.add(new VarInsnNode(Opcodes.ALOAD, 0));
		cstInjection.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
				HypertalePlayer, "<init>", "(L" + Player + ";)V"));
		cstInjection.add(new FieldInsnNode(Opcodes.PUTFIELD,
				classNode.name, hypertalePlayer.name, hypertalePlayer.desc));
		TransformerUtils.insertToEndOfCode(constructor, cstInjection);
		// Label Hypertale APIs explicitly.
		injectHypertaleGetter(classNode, hypertalePlayer);
		return classNode;
	}
}
