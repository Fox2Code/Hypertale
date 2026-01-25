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

final class PatchWorld extends HypertalePatch {

	PatchWorld() {
		super(World);
	}

	@Override
	public ClassNode transform(ClassNode classNode) {
		classNode.access |= TransformerUtils.ACC_COMPUTE_FRAMES;
		final FieldNode hypertaleWorld = new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
				"hypertaleWorld", "L" + HypertaleWorld + ";", null, null);
		classNode.fields.add(hypertaleWorld);
		MethodNode constructor = TransformerUtils.getMethod(classNode, "<init>");
		InsnList cstInjection = new InsnList();
		cstInjection.add(new VarInsnNode(Opcodes.ALOAD, 0));
		cstInjection.add(new TypeInsnNode(Opcodes.NEW, HypertaleWorld));
		cstInjection.add(new InsnNode(Opcodes.DUP));
		cstInjection.add(new VarInsnNode(Opcodes.ALOAD, 0));
		cstInjection.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
				HypertaleWorld, "<init>", "(L" + World + ";)V"));
		cstInjection.add(new FieldInsnNode(Opcodes.PUTFIELD,
				classNode.name, hypertaleWorld.name, hypertaleWorld.desc));
		TransformerUtils.insertToEndOfCode(constructor, cstInjection);
		for (String invalidateMethodName : new String[]{"trackPlayerRef", "untrackPlayerRef"}) {
			MethodNode methodNode = TransformerUtils.getMethod(classNode, invalidateMethodName);
			InsnList postInjection = new InsnList();
			postInjection.add(new VarInsnNode(Opcodes.ALOAD, 0));
			postInjection.add(new FieldInsnNode(Opcodes.GETFIELD,
					classNode.name, hypertaleWorld.name, hypertaleWorld.desc));
			postInjection.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
					HypertaleWorld, "invalidatePlayerCache", "()V"));
			TransformerUtils.insertToEndOfCode(methodNode, postInjection);
		}
		// getPlayers optimization, while we redirect some Hytale calls to Hypertale,
		// plugins still call the Hytale API for getPlayers()
		MethodNode getPlayers = TransformerUtils.findMethod(classNode, "getPlayers");
		boolean patchGetPlayers = HypertaleMethodHeadPatch.shouldApply();
		if (getPlayers == null)  {
			// Install invisible stub if the API has been deleted.
			getPlayers = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
					"getPlayers", "()L" + ASMList + ";", null, null);
			classNode.methods.add(getPlayers);
			patchGetPlayers = true;
		}
		if (patchGetPlayers) {
			HypertaleMethodHeadPatch.apply(classNode, getPlayers, (code, aggressive) -> {
				if (!aggressive) {
					code.add(new TypeInsnNode(Opcodes.NEW, ObjectArrayList));
					code.add(new InsnNode(Opcodes.DUP));
				}
				code.add(new VarInsnNode(Opcodes.ALOAD, 0));
				code.add(new FieldInsnNode(Opcodes.GETFIELD,
						classNode.name, hypertaleWorld.name, hypertaleWorld.desc));
				code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
						HypertaleWorld, "getPlayers", "()L" + ASMList + ";", false));
				if (!aggressive) {
					code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
							ObjectArrayList, "<init>", "(L" + ObjectList + ";)V", false));
				}
				code.add(new InsnNode(Opcodes.ARETURN));
			});
		}
		// Hook onShutdown and redirect it to Hypertale onShutdown
		boolean didPatch = false;
		MethodNode onShutdown = TransformerUtils.getMethod(classNode, "onShutdown");
		for (AbstractInsnNode abstractInsnNode : onShutdown.instructions) {
			if (abstractInsnNode.getOpcode() == Opcodes.INVOKEINTERFACE &&
					abstractInsnNode instanceof MethodInsnNode methodInsnNode &&
					methodInsnNode.name.equals("log") && methodInsnNode.desc.endsWith(")V")) {
				InsnList injection = new InsnList();
				injection.add(new VarInsnNode(Opcodes.ALOAD, 0));
				injection.add(new FieldInsnNode(Opcodes.GETFIELD,
						classNode.name, hypertaleWorld.name, hypertaleWorld.desc));
				injection.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
						HypertaleWorld, "onShutdown", "()V"));
				onShutdown.instructions.insert(methodInsnNode, injection);
				didPatch = true;
				break;
			}
		}
		if (!didPatch) {
			throw new RuntimeException("Failed to apply patch");
		}
		// Label Hypertale APIs explicitly.
		MethodNode hypertale = new MethodNode(Opcodes.ACC_PUBLIC,
				"hypertale", "()" + hypertaleWorld.desc, null, null);
		hypertale.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		hypertale.instructions.add(new FieldInsnNode(Opcodes.GETFIELD,
				classNode.name, hypertaleWorld.name, hypertaleWorld.desc));
		hypertale.instructions.add(new InsnNode(Opcodes.ARETURN));
		classNode.methods.add(hypertale);
		return classNode;
	}
}
