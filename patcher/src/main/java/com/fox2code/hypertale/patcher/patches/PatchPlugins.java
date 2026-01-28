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

import java.awt.*;

final class PatchPlugins extends HypertalePatch {
	PatchPlugins() {
		super(PluginBase, JavaPlugin);
	}

	@Override
	public ClassNode transform(ClassNode classNode) {
		if (classNode.name.equals(PluginBase)) {
			transformPluginBase(classNode);
		} else if (classNode.name.equals(JavaPlugin)) {
			transformJavaPlugin(classNode);
		}
		return classNode;
	}

	private static void transformPluginBase(ClassNode classNode) {
		classNode.access |= TransformerUtils.ACC_COMPUTE_FRAMES;
		final FieldNode logger = TransformerUtils.getField(classNode, "logger");
		final FieldNode hypertalePlugin = new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
				"hypertalePlugin", "L" + HypertaleBasePlugin + ";", null, null);
		classNode.fields.add(hypertalePlugin);
		MethodNode constructor = TransformerUtils.getMethod(classNode, "<init>");
		InsnList cstInjection = new InsnList();
		cstInjection.add(new VarInsnNode(Opcodes.ALOAD, 0));
		cstInjection.add(new TypeInsnNode(Opcodes.NEW, HypertaleBasePlugin));
		cstInjection.add(new InsnNode(Opcodes.DUP));
		cstInjection.add(new VarInsnNode(Opcodes.ALOAD, 0));
		cstInjection.add(new VarInsnNode(Opcodes.ALOAD, 0));
		cstInjection.add(new FieldInsnNode(Opcodes.GETFIELD,
				classNode.name, logger.name, logger.desc));
		cstInjection.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
				HypertaleBasePlugin, "<init>", "(L" + PluginBase + ";L" + HytaleLogger + ";)V"));
		cstInjection.add(new FieldInsnNode(Opcodes.PUTFIELD,
				classNode.name, hypertalePlugin.name, hypertalePlugin.desc));
		TransformerUtils.insertToEndOfCode(constructor, cstInjection);
		// Path Logger API to use HypertaleLogger
		final MethodNode getLogger = TransformerUtils.getMethod(classNode, "getLogger");
		for (AbstractInsnNode abstractInsnNode : getLogger.instructions) {
			if (abstractInsnNode.getOpcode() == GETFIELD &&
					abstractInsnNode instanceof FieldInsnNode fieldInsnNode &&
					fieldInsnNode.name.equals(logger.name) &&
					fieldInsnNode.desc.equals(logger.desc)) {
				InsnList injection = new InsnList();
				injection.add(new FieldInsnNode(Opcodes.GETFIELD,
						fieldInsnNode.owner, hypertalePlugin.name, hypertalePlugin.desc));
				injection.add(new MethodInsnNode(INVOKEVIRTUAL,
						HypertaleBasePlugin, getLogger.name, getLogger.desc));
				getLogger.instructions.insert(fieldInsnNode, injection);
				getLogger.instructions.remove(fieldInsnNode);
				break;
			}
		}
		// Label Hypertale APIs explicitly.
		MethodNode hypertale = new MethodNode(Opcodes.ACC_PUBLIC,
				"hypertale", "()" + hypertalePlugin.desc, null, null);
		hypertale.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		hypertale.instructions.add(new FieldInsnNode(Opcodes.GETFIELD,
				classNode.name, hypertalePlugin.name, hypertalePlugin.desc));
		hypertale.instructions.add(new InsnNode(Opcodes.ARETURN));
		classNode.methods.add(hypertale);
	}

	private static void transformJavaPlugin(ClassNode classNode) {
		classNode.interfaces.add(HypertaleJavaPlugin);
	}
}
