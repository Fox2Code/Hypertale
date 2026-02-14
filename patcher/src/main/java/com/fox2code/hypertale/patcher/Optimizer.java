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
package com.fox2code.hypertale.patcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;


/**
 * Uses {@link com.fox2code.hypertale.utils.EmptyArrays}
 * Uses {@link com.hypixel.hytale.common.util.ArrayUtil}
 * Uses {@link io.netty.util.internal.EmptyArrays}
 */
public final class Optimizer implements Opcodes {
	private static final boolean agentless = Boolean.getBoolean("hypertale.agentless");
	private static final String HypertaleEmptyArrays = "com/fox2code/hypertale/utils/EmptyArrays";
	private static final String HytaleArrayUtils = "com/hypixel/hytale/common/util/ArrayUtil";
	private static final String NettyEmptyArrays = "io/netty/util/internal/EmptyArrays";

	public static void patchClass(ClassNode classNode) {
		if (!canOptimize(classNode.name)) return;
		TransformerUtils.patchInValue$(classNode);
		if (PatcherMain.devMode) return;
		for (MethodNode methodNode : classNode.methods) {
			final InsnList insnList = methodNode.instructions;
			for (AbstractInsnNode abstractInsnNode : insnList) {
				// This is to avoid using toArray() for iterating
				abstractInsnNode = abstractInsnNode.getPrevious();
				if (abstractInsnNode == null) {
					continue;
				}
				switch (abstractInsnNode.getOpcode()) {
					case ANEWARRAY -> tryOptimiseANewArray(insnList, abstractInsnNode);
					case INVOKESTATIC -> tryOptimiseInvokeStatic(methodNode, (MethodInsnNode) abstractInsnNode);
				}
			}
		}
	}

	private static void tryOptimiseANewArray(final InsnList insnList,final AbstractInsnNode abstractInsnNode) {
		AbstractInsnNode previous = abstractInsnNode.getPrevious();
		if (previous == null || previous.getOpcode() != ICONST_0) {
			return;
		}
		String fieldName;
		String owner;
		switch (((TypeInsnNode) abstractInsnNode).desc) {
			case "java/lang/Class" -> {
				fieldName = "EMPTY_CLASS_ARRAY";
				owner = HypertaleEmptyArrays;
			}
			case "java/lang/Integer" -> {
				fieldName = "EMPTY_INTEGER_ARRAY";
				owner = HytaleArrayUtils;
			}
			case "java/lang/Object" -> {
				fieldName = "EMPTY_OBJECT_ARRAY";
				owner = HypertaleEmptyArrays;
			}
			case "java/lang/String" -> {
				fieldName = "EMPTY_STRING_ARRAY";
				owner = HypertaleEmptyArrays;
			}
			case "java/lang/Throwable" -> {
				fieldName = "EMPTY_THROWABLES";
				owner = NettyEmptyArrays;
			}
			case "java/security/cert/X509Certificate" -> {
				fieldName = "EMPTY_X509_CERTIFICATES";
				owner = NettyEmptyArrays;
			}
			default -> {
				return;
			}
		}
		insnList.insert(abstractInsnNode, new FieldInsnNode(GETSTATIC, owner, fieldName,
				"[L" + ((TypeInsnNode) abstractInsnNode).desc + ";"));
		insnList.remove(abstractInsnNode);
		insnList.remove(previous);
	}

	private static void tryOptimiseInvokeStatic(MethodNode methodNode, MethodInsnNode methodInsnNode) {
		if (methodInsnNode.name.equals("values") && canOptimize(methodInsnNode.owner) &&
				!PatcherMain.skipPatch(methodInsnNode.owner) &&
				methodInsnNode.desc.equals("()[L" + methodInsnNode.owner + ";")) {
			AbstractInsnNode next = methodInsnNode.getNext();
			AbstractInsnNode next2 = next.getNext();
			AbstractInsnNode consuming = TransformerUtils.getConsumingInstruction(methodNode, next);
			AbstractInsnNode nextX;
			if (next2 == null || (next.getOpcode() != ARRAYLENGTH &&
					// Always replace it if it is always a get array index element.
					!(consuming != null && consuming.getOpcode() == AALOAD) &&
					// Try to detect the bytecode pattern of an array loop, not very precise.
					!(next.getOpcode() == ASTORE && next2.getOpcode() == ALOAD &&
							((nextX = next2.getNext()) != null && nextX.getOpcode() == ARRAYLENGTH) &&
							((nextX = nextX.getNext()) != null && nextX.getOpcode() == ISTORE) &&
							((nextX = nextX.getNext()) != null && nextX.getOpcode() == ICONST_0) &&
							((nextX = nextX.getNext()) != null && nextX.getOpcode() == ISTORE)) &&
					// Ignore self-constructor, unless we know it's bad, not very precise.
					!(next.getOpcode() != PUTSTATIC && next2.getOpcode() != AASTORE &&
							methodNode.name.equals("<clinit>")))) {
				return;
			}
			methodInsnNode.name = "values$";
		} else if (methodInsnNode.name.equals("wrap") && "java/nio/ByteBuffer".equals(methodInsnNode.owner) &&
				methodInsnNode.desc.equals("([B)Ljava/nio/ByteBuffer;")) {
			AbstractInsnNode previous = methodInsnNode.getPrevious();
			if (previous.getOpcode() == INVOKESTATIC &&
					previous instanceof MethodInsnNode previousMethodInsnNode &&
					"writeToBytes".equals(previousMethodInsnNode.name) &&
					"(Lorg/bson/BsonDocument;)[B".equals(previousMethodInsnNode.desc)) {
				methodNode.instructions.remove(previousMethodInsnNode);
				methodInsnNode.owner = "com/fox2code/hypertale/io/HypertaleBsonUtil";
				methodInsnNode.name = "writeToByteBuffer";
				methodInsnNode.desc = "(Lorg/bson/BsonDocument;)Ljava/nio/ByteBuffer;";
			}
		}
	}

	public static boolean canOptimize(String path) {
		if (agentless) {
			return path.startsWith("com/hypixel/");
		}
		return !path.startsWith("javax/") && !path.startsWith("java/") &&
				!path.startsWith("sun/") &&
				!path.startsWith("org/objectweb/asm/") &&
				!path.startsWith("com/fox2code/hypertale/utils/") &&
				!HytaleArrayUtils.equals(path) &&
				!NettyEmptyArrays.equals(path);
	}
}
