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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.util.Objects;

public final class SafeClassWriter extends ClassVisitor {
	private final ClassWriter classWriter;
	private String className;

	public SafeClassWriter(int flags) {
		this(new ClassWriter(flags) {
			@Override
			protected String getCommonSuperClass(String type1, String type2) {
				if ("java/lang/Object".equals(type1) ||
						"java/lang/Object".equals(type2)) {
					return "java/lang/Object";
				}
				if (Objects.equals(type1, type2)) {
					return type1;
				}
				return super.getCommonSuperClass(type1, type2);
			}
		});
	}

	private SafeClassWriter(ClassWriter classWriter) {
		super(TransformerUtils.ASM_BUILD, classWriter);
		this.classWriter = classWriter;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.className = name;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new MethodVisitor(TransformerUtils.ASM_BUILD,
				super.visitMethod(access, name, descriptor, signature, exceptions)) {
			@Override
			public void visitMaxs(int maxStack, int maxLocals) {
				try {
					super.visitMaxs(maxStack, maxLocals);
				} catch (RuntimeException e) {
					throw new RuntimeException("Failed to compute frames on " +
							SafeClassWriter.this.className + "." + name + descriptor, e);
				}
			}
		};
	}

	public byte[] toByteArray() {
		return this.classWriter.toByteArray();
	}
}
