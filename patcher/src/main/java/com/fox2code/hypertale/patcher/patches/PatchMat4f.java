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

final class PatchMat4f extends HypertalePatch {
	PatchMat4f() {
		super(Mat4f);
	}

	@Override
	public ClassNode transform(ClassNode classNode) {
		MethodNode identity = TransformerUtils.findMethod(classNode, "identity", "()L" + Mat4f + ";");
		if (identity == null || (identity.access & ACC_STATIC) == 0) return classNode;
		classNode.access |= TransformerUtils.ACC_COMPUTE_FRAMES;
		FieldNode fieldNode = new FieldNode(ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
				"HYPERTALE_IDENTITY", "L" + Mat4f + ";", null, null);
		classNode.fields.addFirst(fieldNode);
		MethodNode init = TransformerUtils.findMethod(classNode, "<clinit>");
		if (init == null) {
			init = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
			init.instructions.add(new InsnNode(RETURN));
			classNode.methods.addFirst(init);
		}
		// Replace the identity method with the get field!
		MethodNode identityProxy = TransformerUtils.copyMethodNode(identity);
		identityProxy.instructions.clear();
		identityProxy.instructions.add(new FieldInsnNode(GETSTATIC, classNode.name,
				"HYPERTALE_IDENTITY", "L" + Mat4f + ";"));
		identityProxy.instructions.add(new InsnNode(ARETURN));
		classNode.methods.add(identityProxy);
		identity.name = "identityOriginal";
		identity.access = ACC_PRIVATE | ACC_STATIC;

		InsnList injection = new InsnList();
		injection.add(new MethodInsnNode(INVOKESTATIC, Mat4f, identity.name, "()L" + Mat4f + ";", false));
		injection.add(new FieldInsnNode(PUTSTATIC, classNode.name, "HYPERTALE_IDENTITY", "L" + Mat4f + ";"));
		injection.add(new InsnNode(RETURN));

		TransformerUtils.insertToBeginningOfCode(init, injection);

		return classNode;
	}
}
