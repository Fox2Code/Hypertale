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

import com.fox2code.hypertale.patcher.HypertaleASMConstants;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Modifier;

abstract class HypertalePatch implements Opcodes, HypertaleASMConstants {
	static final Void ALL_CLASSES = null;
	final String[] targets;

	HypertalePatch(Void ignored) {
		this.targets = null;
		this.checkAccess();
	}

	HypertalePatch(String target) {
		this.targets = target == null ? null : new String[]{target};
		this.checkAccess();
	}

	HypertalePatch(String[] targets) {
		this.targets = targets == null || targets.length == 0 ? null : targets;
		this.checkAccess();
	}

	HypertalePatch(String target, String... targets) {
		String[] newTargets = new String[targets.length + 1];
		newTargets[0] = target;
		System.arraycopy(targets, 0, newTargets, 1, targets.length);
		this(newTargets);
	}

	private void checkAccess() {
		if (this.getClass().getModifiers() != Modifier.FINAL) {
			throw new RuntimeException("GamePatch \"" + this.getClass().getName() + "\" has an invalid modifier");
		}
	}

	public abstract ClassNode transform(ClassNode classNode);
}
