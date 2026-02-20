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

import com.hypixel.hytale.common.util.ArrayUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;

public final class TransformerUtils {
	public static final int ACC_COMPUTE_FRAMES = 0x80000;
	public static final int ASM_BUILD = Opcodes.ASM9;

	public static MethodNode copyMethodNode(MethodNode methodNode) {
		MethodNode methodNodeCopy = new MethodNode(ASM_BUILD, methodNode.access,
				methodNode.name, methodNode.desc, methodNode.signature,
				methodNode.exceptions.toArray(ArrayUtil.EMPTY_STRING_ARRAY));
		Map<LabelNode, LabelNode> map = new IdentityHashMap<>() {
			@Override
			public LabelNode get(Object key) {
				LabelNode labelNode = super.get(key);
				return labelNode == null ? (LabelNode) key : labelNode;
			}
		};
		InsnList insnList = methodNode.instructions;
		for (AbstractInsnNode abstractInsnNode : insnList) {
			if (abstractInsnNode instanceof LabelNode labelNode) {
				map.put(labelNode, new LabelNode());
			}
		}
		if (methodNode.localVariables != null && !methodNode.localVariables.isEmpty()) {
			if (methodNodeCopy.localVariables == null)
				methodNodeCopy.localVariables = new ArrayList<>();
			for (LocalVariableNode localVariableNode : methodNode.localVariables) {
				methodNodeCopy.localVariables.add(new LocalVariableNode(
						localVariableNode.name, localVariableNode.desc, localVariableNode.desc,
						map.get(localVariableNode.start), map.get(localVariableNode.end), localVariableNode.index));
			}
		}
		InsnList copy = methodNodeCopy.instructions;
		for (AbstractInsnNode abstractInsnNode : insnList) {
			copy.add(abstractInsnNode.clone(map));
		}
		methodNodeCopy.maxLocals = methodNode.maxLocals;
		methodNodeCopy.maxStack = methodNode.maxStack;
		if (methodNode.tryCatchBlocks != null &&
				!methodNode.tryCatchBlocks.isEmpty()) {
			if (methodNodeCopy.tryCatchBlocks == null)
				methodNodeCopy.tryCatchBlocks = new ArrayList<>();
			for (TryCatchBlockNode tryCatchBlockNode : methodNode.tryCatchBlocks) {
				methodNodeCopy.tryCatchBlocks.add(new TryCatchBlockNode(map.get(tryCatchBlockNode.start),
						map.get(tryCatchBlockNode.end), map.get(tryCatchBlockNode.handler), tryCatchBlockNode.type));
			}
		}
		if (methodNode.invisibleAnnotations != null) {
			methodNodeCopy.invisibleAnnotations = new ArrayList<>();
			for (AnnotationNode annotationNode : methodNode.invisibleAnnotations) {
				AnnotationNode annotationNodeCopy = new AnnotationNode(annotationNode.desc);
				annotationNode.accept(annotationNodeCopy);
				methodNodeCopy.invisibleAnnotations.add(annotationNodeCopy);
			}
		}
		if (methodNode.visibleAnnotations != null) {
			methodNodeCopy.visibleAnnotations = new ArrayList<>();
			for (AnnotationNode annotationNode : methodNode.visibleAnnotations) {
				AnnotationNode annotationNodeCopy = new AnnotationNode(annotationNode.desc);
				annotationNode.accept(annotationNodeCopy);
				methodNodeCopy.visibleAnnotations.add(annotationNodeCopy);
			}
		}
		return methodNodeCopy;
	}

	public static MethodNode getMethod(ClassNode classNode, String methodName) {
		return findMethod0(classNode, methodName, null, true);
	}

	public static MethodNode getMethod(ClassNode classNode, String methodName, String methodDesc) {
		return findMethod0(classNode, methodName, methodDesc, true);
	}

	public static MethodNode findMethod(ClassNode classNode, String methodName) {
		return findMethod0(classNode, methodName, null, false);
	}

	public static MethodNode findMethod(ClassNode classNode, String methodName, String methodDesc) {
		return findMethod0(classNode, methodName, methodDesc, false);
	}

	private static MethodNode findMethod0(ClassNode classNode, String methodName, String methodDesc, boolean require) {
		MethodNode bridgeMethodNode = null;
		for (MethodNode methodNode:classNode.methods) {
			if (methodNode.name.equals(methodName)
					&& (methodDesc == null || methodNode.desc.equals(methodDesc))) {
				if ((methodNode.access & Opcodes.ACC_BRIDGE) != 0) {
					bridgeMethodNode = methodNode;
				} else return methodNode;
			}
		}
		if (bridgeMethodNode != null) {
			return bridgeMethodNode;
		}
		if (require) {
			throw new NoSuchElementException(classNode.name + "." +
					methodName + (methodDesc == null ? "()" : methodDesc));
		} else {
			return null;
		}
	}

	public static FieldNode getField(ClassNode classNode,String fieldName) {
		return findField0(classNode, fieldName, null, true);
	}

	public static FieldNode getField(ClassNode classNode, String fieldName, String fieldDesc) {
		return findField0(classNode, fieldName, fieldDesc, true);
	}

	public static FieldNode findField(ClassNode classNode,String fieldName) {
		return findField0(classNode, fieldName, null, false);
	}

	public static FieldNode findField(ClassNode classNode, String fieldName, String fieldDesc) {
		return findField0(classNode, fieldName, fieldDesc, false);
	}

	private static FieldNode findField0(ClassNode classNode, String fieldName, String fieldDesc, boolean require) {
		for (FieldNode fieldNode:classNode.fields) {
			if (fieldNode.name.equals(fieldName) &&
					(fieldDesc == null || fieldNode.desc.equals(fieldDesc))) {
				return fieldNode;
			}
		}
		if (require) {
			throw new NoSuchElementException(classNode.name + "." +
					fieldName + (fieldDesc == null ? "" : " " + fieldDesc));
		} else {
			return null;
		}
	}

	public static FieldNode getFieldDesc(ClassNode classNode, String fieldDesc) {
		return findFieldDesc0(classNode, fieldDesc, true);
	}

	public static FieldNode findFieldDesc(ClassNode classNode, String fieldDesc) {
		return findFieldDesc0(classNode, fieldDesc, false);
	}

	private static FieldNode findFieldDesc0(ClassNode classNode, String fieldDesc, boolean required) {
		for (FieldNode fieldNode:classNode.fields) {
			if (fieldNode.desc.equals(fieldDesc)) {
				return fieldNode;
			}
		}
		if (required) {
			throw new NoSuchElementException(classNode.name + ".* " + fieldDesc);
		}
		return null;
	}

	public static AbstractInsnNode getBooleanInsn(boolean bool) {
		return new InsnNode(bool ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
	}

	public static AbstractInsnNode getNumberInsn(int number) {
		if (number >= -1 && number <= 5)
			return new InsnNode(number + 3);
		else if (number >= -128 && number <= 127)
			return new IntInsnNode(Opcodes.BIPUSH, number);
		else if (number >= -32768 && number <= 32767)
			return new IntInsnNode(Opcodes.SIPUSH, number);
		else
			return new LdcInsnNode(number);
	}


	public static String printInsnList(InsnList insnList) {
		final StringBuilder stringBuilder = new StringBuilder();
		printInsnList(insnList, stringBuilder);
		return stringBuilder.toString();
	}

	public static void printInsnList(final InsnList insnList,final StringBuilder stringBuilder) {
		Textifier textifier = new Textifier();
		MethodNode methodNode = new MethodNode(0, "insns", "()V", null, null);
		methodNode.instructions = insnList;
		methodNode.accept(new TraceMethodVisitor(textifier));
		textifier.print(new PrintWriter(new Writer() {
			@Override
			public void write(@NonNullDecl String str, int off, int len) {
				stringBuilder.append(str, off, len);
			}

			@Override
			public void write(@NonNullDecl char[] cbuf, int off, int len) {
				stringBuilder.append(cbuf, off, len);
			}

			@Override public void flush() {}
			@Override public void close() {}
		}));
	}

	public static AbstractInsnNode nextCodeInsn(AbstractInsnNode abstractInsnNode) {
		do {
			abstractInsnNode = abstractInsnNode.getNext();
		} while (abstractInsnNode != null && abstractInsnNode.getOpcode() == -1);
		return abstractInsnNode;
	}

	public static AbstractInsnNode previousCodeInsn(AbstractInsnNode abstractInsnNode, int count) {
		while (count-->0) {
			abstractInsnNode = previousCodeInsn(abstractInsnNode);
		}
		return abstractInsnNode;
	}

	public static AbstractInsnNode previousCodeInsn(AbstractInsnNode abstractInsnNode) {
		do {
			abstractInsnNode = abstractInsnNode.getPrevious();
		} while (abstractInsnNode != null && abstractInsnNode.getOpcode() == -1);
		return abstractInsnNode;
	}

	public static void insertToBeginningOfCode(MethodNode methodNode, AbstractInsnNode abstractInsnNode) {
		InsnList insnList = new InsnList();
		insnList.add(abstractInsnNode);
		insertToBeginningOfCode(methodNode, insnList);
	}

	public static void insertToBeginningOfCode(MethodNode methodNode, InsnList insnList) {
		AbstractInsnNode first = methodNode.instructions.getFirst();
		if (first.getOpcode() == -1) {
			first = nextCodeInsn(first);
		}
		methodNode.instructions.insertBefore(first, insnList);
	}

	public static void insertAfterConstructor(MethodNode methodNode, AbstractInsnNode abstractInsnNode) {
		InsnList insnList = new InsnList();
		insnList.add(abstractInsnNode);
		insertAfterConstructor(methodNode, insnList);
	}

	public static void insertAfterConstructor(MethodNode methodNode, InsnList insnList) {
		AbstractInsnNode first = methodNode.instructions.getFirst();
		while (first.getOpcode() != Opcodes.INVOKESPECIAL ||
				!((MethodInsnNode) first).name.equals("<init>")) {
			first = nextCodeInsn(first);
		}
		methodNode.instructions.insert(first, insnList);
	}

	public static void insertToEndOfCode(MethodNode methodNode, AbstractInsnNode abstractInsnNode) {
		InsnList insnList = new InsnList();
		insnList.add(abstractInsnNode);
		insertToEndOfCode(methodNode, insnList);
	}

	public static void insertToEndOfCode(MethodNode methodNode, InsnList insnList) {
		AbstractInsnNode last = methodNode.instructions.getLast();
		if (last.getOpcode() == -1) {
			last = previousCodeInsn(last);
		}
		int lastOpcode = last.getOpcode();
		AbstractInsnNode lastList = insnList.getLast();
		if (lastList.getOpcode() == -1) {
			lastList = previousCodeInsn(lastList);
		}
		int lastListOpcode = lastList.getOpcode();
		boolean sameEnd = lastOpcode == lastListOpcode;
		if (lastOpcode != Opcodes.RETURN && !sameEnd) {
			boolean fail = true;
			if (lastOpcode == Opcodes.IRETURN) {
				AbstractInsnNode previous = TransformerUtils.previousCodeInsn(last);
				int op = previous.getOpcode();
				if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) {
					last = previous;
					fail = false;
				}
			}
			if (fail) {
				throw new RuntimeException("End of code isn't really the end of the code: " + lastOpcode);
			}
		}
		if (sameEnd) {
			methodNode.instructions.insert(last, insnList);
		} else {
			methodNode.instructions.insertBefore(last, insnList);
		}
	}

	public static LabelNode getBeginingLabelNode(MethodNode methodNode) {
		AbstractInsnNode abstractInsnNode = methodNode.instructions.getFirst();
		while (abstractInsnNode != null &&
				abstractInsnNode.getOpcode() == -1) {
			if (abstractInsnNode instanceof LabelNode) {
				return (LabelNode) abstractInsnNode;
			}
			abstractInsnNode = abstractInsnNode.getNext();
		}
		LabelNode endingLabelNode = new LabelNode();
		methodNode.instructions.insert(endingLabelNode);
		return endingLabelNode;
	}

	public static LabelNode getEndingLabelNode(MethodNode methodNode) {
		return getEndingLabelNode(methodNode.instructions);
	}

	public static LabelNode getEndingLabelNode(InsnList instructions) {
		AbstractInsnNode abstractInsnNode = instructions.getLast();
		while (abstractInsnNode != null &&
				abstractInsnNode.getOpcode() == -1) {
			if (abstractInsnNode instanceof LabelNode) {
				return (LabelNode) abstractInsnNode;
			}
			abstractInsnNode = abstractInsnNode.getPrevious();
		}
		LabelNode endingLabelNode = new LabelNode();
		instructions.add(endingLabelNode);
		return endingLabelNode;
	}

	public static void setParameterName(MethodNode methodNode, int index, String name) {
		if (index == 0 && (methodNode.access & Opcodes.ACC_STATIC) == 0) {
			throw new IllegalArgumentException("Cannot set arg index zero of a non-static method!");
		}
		setParameterNameImpl(methodNode, index, name);
	}

	public static void setThisParameterName(ClassNode classNode, MethodNode methodNode) {
		if ((methodNode.access & Opcodes.ACC_STATIC) != 0) {
			throw new IllegalArgumentException("Expected a non-static method!");
		}
		setParameterNameImpl(methodNode, 0, classNode.name);
	}

	private static void setParameterNameImpl(MethodNode methodNode, int index, String name) {
		boolean thisField = index == 0 && ((methodNode.access & Opcodes.ACC_STATIC) == 0);
		for (LocalVariableNode localVariableNode : methodNode.localVariables) {
			if (localVariableNode.index == index) {
				localVariableNode.name = thisField ? "this" : name;
				return;
			}
		}
		Type[] args = Type.getArgumentTypes(methodNode.desc);
		int left = index - ((methodNode.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0);
		Type type = null;
		if (thisField) {
			type = Type.getObjectType(name);
			name = "this";
		} else {
			for (Type arg : args) {
				if (left == 0) {
					type = arg;
					break;
				}
				left -= arg.getSize();
			}
		}
		Objects.requireNonNull(type, "type");
		LabelNode start = getBeginingLabelNode(methodNode);
		LabelNode end = getEndingLabelNode(methodNode);
		methodNode.localVariables.add(new LocalVariableNode(
				name, type.getDescriptor(), null, start, end, index));
	}

	public static AbstractInsnNode getConsumingInstruction(MethodNode methodNode, AbstractInsnNode from) {
		return getConsumingInstruction(methodNode.instructions, from);
	}

	public static AbstractInsnNode getConsumingInstruction(InsnList insnList, AbstractInsnNode from) {
		int stackLevel = 1;
		while (from != null) {
			// We need special handling for goto instructions
			if (from.getOpcode() == Opcodes.GOTO) {
				int gotoIndexOld = insnList.indexOf(from);
				from = ((JumpInsnNode) from).label.getNext();
				int gotoIndexNew = insnList.indexOf(from);
				if (gotoIndexNew < gotoIndexOld) {
					break;
				}
			}
			stackLevel -= OpcodesUtils.getStackConsume(from);
			if (stackLevel <= 0) {
				return from;
			}
			stackLevel += OpcodesUtils.getStackProduce(from);
			from = from.getNext();
		}
		return null;
	}

	public static void patchInValue$(ClassNode classNode) {
		String desc = "[L" + classNode.name + ";";
		String mDesc = "()[L" + classNode.name + ";";
		if (findMethod(classNode, "values$", mDesc) != null) {
			return; // Skip if it already exists
		}
		MethodNode values = (classNode.access & Opcodes.ACC_ENUM) != 0 &&
				"java/lang/Enum".equals(classNode.superName) ?
				TransformerUtils.getMethod(classNode, "values", mDesc) :
				TransformerUtils.findMethod(classNode, "values", mDesc);
		if (values == null) {
			return; // skip if non enum, or anonymous class of enum.
		}
		MethodNode values$ = TransformerUtils.copyMethodNode(values);
		values$.name = "values$";
		values$.access |= Opcodes.ACC_SYNTHETIC;
		for (AbstractInsnNode abstractInsnNode : values$.instructions) {
			if (abstractInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
				MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;

				if (desc.equals(methodInsnNode.owner) &&
						"clone".equals(methodInsnNode.name) &&
						"()Ljava/lang/Object;".equals(methodInsnNode.desc)) {
					abstractInsnNode = methodInsnNode.getNext();
					if (abstractInsnNode.getOpcode() == Opcodes.CHECKCAST &&
							desc.equals(((TypeInsnNode) abstractInsnNode).desc)) {
						values$.instructions.remove(methodInsnNode);
						values$.instructions.remove(abstractInsnNode);
						break;
					}
				}
			}
		}
		classNode.methods.add(classNode.methods.indexOf(values) + 1, values$);
	}

	public static boolean insnListReturn(InsnList headCode) {
		LabelNode lastLabelNode = TransformerUtils.getEndingLabelNode(headCode);
		AbstractInsnNode lastCodeInsn = TransformerUtils.previousCodeInsn(lastLabelNode);
		int lastOpcode = lastCodeInsn.getOpcode();
		boolean replaceMethod = false;
		if (lastOpcode >= Opcodes.IRETURN && lastOpcode <= Opcodes.RETURN) {
			replaceMethod = true;
			for (AbstractInsnNode abstractInsnNode : headCode) {
				if (abstractInsnNode instanceof JumpInsnNode jumpInsnNode &&
						jumpInsnNode.label == lastLabelNode) {
					replaceMethod = false;
					break;
				}
			}
		}
		return replaceMethod;
	}

	public static InsnList compileStringAppendChain(Collection<AbstractInsnNode> stringConstants) {
		// This is to trick the decompiler to use + sign when concatenating strings or constants.
		InsnList insnList = new InsnList();
		if (stringConstants.isEmpty()) {
			insnList.add(new LdcInsnNode(""));
			return insnList;
		} else if (stringConstants.size() == 1) {
			insnList.add(stringConstants.iterator().next());
			return insnList;
		}
		insnList.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
		insnList.add(new InsnNode(Opcodes.DUP));
		insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
				"java/lang/StringBuilder", "<init>", "()V", false));
		appendStringsInAppendChain(insnList, stringConstants);
		insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
				"java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
		return insnList;
	}

	public static void appendStringsInAppendChain(InsnList insnList, Iterable<AbstractInsnNode> stringConstants) {
		for (AbstractInsnNode abstractInsnNode : stringConstants) {
			insnList.add(abstractInsnNode);
			insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
					"java/lang/StringBuilder", "append",
					"(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
		}
	}
}
