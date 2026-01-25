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

import com.fox2code.hypertale.launcher.EarlyLogger;
import com.fox2code.hypertale.loader.HypertaleConfig;
import com.fox2code.hypertale.patcher.HypertaleASMConstants;
import com.fox2code.hypertale.patcher.PatcherMain;
import com.fox2code.hypertale.patcher.TransformerUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Objects;
import java.util.function.Consumer;

final class HypertaleMethodHeadPatch {
	private final Patch patch;

	HypertaleMethodHeadPatch(final Consumer<InsnList> regularPatch,final Consumer<InsnList> aggressivePatch) {
		Objects.requireNonNull(regularPatch);
		Objects.requireNonNull(aggressivePatch);
		this((code, aggressive) -> {
			if (aggressive) {
				aggressivePatch.accept(code);
			} else {
				regularPatch.accept(code);
			}
		});
	}

	HypertaleMethodHeadPatch(Patch patch) {
		Objects.requireNonNull(patch);
		this.patch = patch;
	}

	void apply(ClassNode classNode, MethodNode methodNode) {
		if (PatcherMain.devMode) {
			boolean empty = methodNode.instructions.size() == 0;
			if (empty) EarlyLogger.log("Debug: empty");
			classNode.access |= TransformerUtils.ACC_COMPUTE_FRAMES;
			InsnList patchRegular = new InsnList();
			InsnList patchAggressive = new InsnList();
			this.patch.apply(patchRegular, false);
			this.patch.apply(patchAggressive, true);
			InsnList headInjection = new InsnList();
			LabelNode end = new LabelNode();
			LabelNode checkRegular = new LabelNode();
			boolean agressiveReturn = TransformerUtils.insnListReturn(patchAggressive);
			if (empty) EarlyLogger.log("Debug: agressiveReturn");
			if (empty && !(agressiveReturn && TransformerUtils.insnListReturn(patchRegular))) {
				throw new RuntimeException("Stub method support requires always returning stub!");
			}
			headInjection.add(new FieldInsnNode(Opcodes.GETSTATIC,
					HypertaleASMConstants.HypertaleConfig,
					"aggressivelyOptimizePluginOnlyAPIs", "Z"));
			headInjection.add(new JumpInsnNode(Opcodes.IFEQ, checkRegular));
			headInjection.add(patchAggressive);
			if (agressiveReturn) {
				headInjection.add(new JumpInsnNode(Opcodes.GOTO, end));
			}
			headInjection.add(checkRegular);
			if (!empty) {
				headInjection.add(new FieldInsnNode(Opcodes.GETSTATIC,
						HypertaleASMConstants.HypertaleConfig,
						"optimizePluginOnlyAPIs", "Z"));
				headInjection.add(new JumpInsnNode(Opcodes.IFEQ, end));
			}
			headInjection.add(patchRegular);
			headInjection.add(end);
			if (empty) {
				methodNode.instructions = headInjection;
			} else {
				TransformerUtils.insertToBeginningOfCode(methodNode, headInjection);
			}
		} else if (HypertaleConfig.optimizePluginOnlyAPIs || methodNode.instructions.size() == 0) {
			classNode.access |= TransformerUtils.ACC_COMPUTE_FRAMES;
			InsnList headCode = new InsnList();
			this.patch.apply(headCode,
					HypertaleConfig.aggressivelyOptimizePluginOnlyAPIs);
			if (TransformerUtils.insnListReturn(headCode)) {
				methodNode.tryCatchBlocks.clear();
				methodNode.localVariables.clear();
				methodNode.instructions = headCode;
				TransformerUtils.setThisParameterName(classNode, methodNode);
			} else if (methodNode.instructions.size() != 0) {
				TransformerUtils.insertToBeginningOfCode(methodNode, headCode);
			} else {
				throw new RuntimeException("Stub method support requires always returning stub!");
			}
		}
	}

	public static void apply(ClassNode classNode, MethodNode methodNode, Patch patch) {
		new HypertaleMethodHeadPatch(patch).apply(classNode, methodNode);
	}

	public static boolean shouldApply() {
		return PatcherMain.devMode || HypertaleConfig.optimizePluginOnlyAPIs;
	}

	interface Patch {
		void apply(InsnList code, boolean aggressive);
	}
}
