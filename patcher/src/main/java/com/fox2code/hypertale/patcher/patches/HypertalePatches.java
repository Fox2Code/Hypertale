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

import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashMap;

public final class HypertalePatches {
	private static final ArrayList<HypertalePatch> noPatches = new ArrayList<>();
	private static final ArrayList<HypertalePatch> globalHypertalePatches = new ArrayList<>();
	private static final HashMap<String, ArrayList<HypertalePatch>> hypertaleClassPatches = new HashMap<>();
	static void addPatch(HypertalePatch hypertalePatch) {
		if (hypertalePatch.targets == null) {
			globalHypertalePatches.add(hypertalePatch);
		} else {
			for (String target : hypertalePatch.targets) {
				if (!target.isEmpty()) {
					hypertaleClassPatches.computeIfAbsent(target, ignored -> new ArrayList<>()).add(hypertalePatch);
				}
			}
		}
	}

	static {
		addPatch(new PatchMat4f());
		addPatch(new PatchPluginClassLoader());
		addPatch(new PatchWorld());
		addPatch(new PatchChunkStore());
		addPatch(new PatchPacketIO());
		addPatch(new PatchPlayer());
		addPatch(new PatchPlayerRef());
		addPatch(new PatchPluginManager());
		addPatch(new PatchBundled());
		addPatch(new PatchPlugins());
		addPatch(new PatchExtraAuthenticationData());
		addPatch(new PatchHytaleLogFormatter());
		addPatch(new PatchRedirectToOptimisedGetPlayers());
		addPatch(new PatchRedirectToOptimisedGetChunkIfInMemory());
		HypertalePatchesPlus.registerPatches();
	}

	public static ClassNode patchClassNode(ClassNode classNode) {
		for (HypertalePatch hypertalePatch : hypertaleClassPatches.getOrDefault(classNode.name, noPatches)) {
			ClassNode classNodeTmp = hypertalePatch.transform(classNode);
			if (classNodeTmp != null) classNode = classNodeTmp;
		}
		for (HypertalePatch hypertalePatch : globalHypertalePatches) {
			ClassNode classNodeTmp = hypertalePatch.transform(classNode);
			if (classNodeTmp != null) classNode = classNodeTmp;
		}
		return classNode;
	}
}
