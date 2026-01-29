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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

final class PatchExtraAuthenticationData extends HypertalePatch {
	PatchExtraAuthenticationData() {
		super(HandshakeHandler, PasswordPacketHandler, AuthenticationPacketHandler);
	}

	@Override
	public ClassNode transform(ClassNode classNode) {
		classNode.access |= TransformerUtils.ACC_COMPUTE_FRAMES;
		if (AuthenticationPacketHandler.equals(classNode.name)) {
			boolean didPatch = false;
			for (MethodNode methodNode : classNode.methods) {
				for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
					if (abstractInsnNode.getOpcode() == Opcodes.INVOKESPECIAL &&
							abstractInsnNode instanceof MethodInsnNode methodInsnNode &&
							methodInsnNode.owner.equals(PasswordPacketHandler) &&
							methodInsnNode.name.equals("<init>")) {
						methodInsnNode.desc = methodInsnNode.desc.replace(")",
								"L" + IdentityTokenClaims + ";)");
						InsnList injection = new InsnList();
						injection.add(new VarInsnNode(ALOAD, 0));
						injection.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name,
								"hypertaleIdentityToken", "L" + IdentityTokenClaims + ";"));
						methodNode.instructions.insertBefore(methodInsnNode, injection);
						didPatch = true;
						break;
					}
				}
			}
			if (!didPatch) {
				throw new RuntimeException("Failed patch!");
			}
			return classNode;
		}
		boolean handshakeHandler = HandshakeHandler.equals(classNode.name);
		FieldNode identityTokenClaimsField;
		if (handshakeHandler) {
			identityTokenClaimsField = new FieldNode(
					0, "hypertaleIdentityToken",
					"L" + IdentityTokenClaims + ";", null, null);
			classNode.fields.add(identityTokenClaimsField);
			MethodNode registered0 = TransformerUtils.getMethod(classNode, "registered0");
			boolean didPatch = false;
			int var = -1;
			for (AbstractInsnNode abstractInsnNode : registered0.instructions) {
				if (abstractInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL &&
						abstractInsnNode instanceof MethodInsnNode methodInsnNode) {
					if (methodInsnNode.desc.endsWith(")L" + IdentityTokenClaims + ";")) {
						AbstractInsnNode next = TransformerUtils.nextCodeInsn(methodInsnNode);
						if (next.getOpcode() == Opcodes.ASTORE) {
							var = ((VarInsnNode) next).var;
						} else {
							throw new RuntimeException("What? " + next.getOpcode());
						}
					}
					if (methodInsnNode.owner.equals(classNode.name) &&
							methodInsnNode.name.equals("requestAuthGrant")) {
						if (var == -1) {
							throw new RuntimeException("Failed to get var!");
						}
						InsnList injection = new InsnList();
						injection.add(new VarInsnNode(Opcodes.ALOAD, 0));
						injection.add(new VarInsnNode(Opcodes.ALOAD, var));
						injection.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name,
								identityTokenClaimsField.name, identityTokenClaimsField.desc));
						registered0.instructions.insert(methodInsnNode, injection);
						didPatch = true;
						break;
					}
				}
			}
			if (!didPatch) {
				throw new RuntimeException("Failed patch!");
			}
		} else {
			identityTokenClaimsField = new FieldNode(
					ACC_PRIVATE, "hypertaleIdentityToken",
					"L" + IdentityTokenClaims + ";", null, null);
			classNode.fields.add(identityTokenClaimsField);
			MethodNode constructor = TransformerUtils.getMethod(classNode, "<init>");
			int index = Type.getArgumentsAndReturnSizes(constructor.desc) >> 2;
			constructor.desc = constructor.desc.replace(")",
					"L" + IdentityTokenClaims + ";)");
			InsnList injection = new InsnList();
			injection.add(new VarInsnNode(ALOAD, 0));
			injection.add(new VarInsnNode(ALOAD, index));
			injection.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name,
					identityTokenClaimsField.name, identityTokenClaimsField.desc));
			TransformerUtils.insertToEndOfCode(constructor, injection);
		}
		boolean didPatch = false;
		for (MethodNode methodNode : classNode.methods) {
			for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
				if (abstractInsnNode.getOpcode() == Opcodes.NEW &&
						abstractInsnNode instanceof TypeInsnNode typeInsnNode &&
						typeInsnNode.desc.equals(PlayerAuthentication)) {
					typeInsnNode.desc = HypertalePlayerAuthentication;
				} else if (abstractInsnNode.getOpcode() == Opcodes.INVOKESPECIAL &&
						abstractInsnNode instanceof MethodInsnNode methodInsnNode &&
						methodInsnNode.name.equals("<init>")) {
					if (methodInsnNode.owner.equals(PlayerAuthentication)) {
						methodInsnNode.desc = methodInsnNode.desc.replace(")",
								"L" + IdentityTokenClaims + ";)");
						methodInsnNode.owner = HypertalePlayerAuthentication;
						InsnList injection = new InsnList();
						injection.add(new VarInsnNode(ALOAD, 0));
						injection.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name,
								identityTokenClaimsField.name, identityTokenClaimsField.desc));
						methodNode.instructions.insertBefore(methodInsnNode, injection);
						didPatch = true;
						break;
					}
				}
			}
		}
		if (!didPatch) {
			throw new RuntimeException("Failed patch!");
		}
		return classNode;
	}
}
