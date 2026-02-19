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

final class PatchPluginManager extends HypertalePatch {
	PatchPluginManager() {
		super(PluginManager);
	}

	@Override
	public ClassNode transform(ClassNode classNode) {
		classNode.access |= TransformerUtils.ACC_COMPUTE_FRAMES;
		MethodNode methodNode = TransformerUtils.getMethod(classNode, "loadPendingPlugin");
		InsnList injection = new InsnList();
		injection.add(new VarInsnNode(ALOAD, 1));
		injection.add(new MethodInsnNode(INVOKEVIRTUAL, PendingLoadPlugin,
				"getIdentifier", "()L" + PluginIdentifier + ";"));
		injection.add(new MethodInsnNode(INVOKESTATIC, HypertaleModLoader,
				"isPreloadedPlugin", "(L" + PluginIdentifier + ";)Z"));
		LabelNode labelNode = new LabelNode();
		injection.add(new JumpInsnNode(IFEQ, labelNode));
		injection.add(new InsnNode(RETURN));
		injection.add(labelNode);
		for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
			if (abstractInsnNode.getOpcode() == Opcodes.NEW) {
				methodNode.instructions.insertBefore(abstractInsnNode, injection);
				break;
			}
		}
		MethodNode loadPendingJavaPlugin = TransformerUtils.getMethod(classNode, "loadPendingJavaPlugin");
		for (AbstractInsnNode abstractInsnNode : loadPendingJavaPlugin.instructions) {
			if (abstractInsnNode.getOpcode() == Opcodes.CHECKCAST &&
					abstractInsnNode instanceof TypeInsnNode typeInsnNode &&
					typeInsnNode.desc.equals(PluginManifest)) {
				methodNode.instructions.insert(typeInsnNode, new MethodInsnNode(
						INVOKESTATIC, HypertaleModLoader, "passPluginManifest",
						"(L" + PluginManifest + ";)L" + PluginManifest + ";"));
			}
		}
		MethodNode loadPluginsInClasspath = TransformerUtils.getMethod(classNode, "loadPluginsInClasspath");
		for (AbstractInsnNode abstractInsnNode : loadPluginsInClasspath.instructions) {
			if (abstractInsnNode.getOpcode() == Opcodes.CHECKCAST &&
					abstractInsnNode instanceof TypeInsnNode typeInsnNode &&
					typeInsnNode.desc.equals(PluginManifest)) {
				methodNode.instructions.insert(typeInsnNode, new MethodInsnNode(
						INVOKESTATIC, HypertaleModLoader, "passPluginManifest",
						"(L" + PluginManifest + ";)L" + PluginManifest + ";"));
			}
		}
		return classNode;
	}
}
