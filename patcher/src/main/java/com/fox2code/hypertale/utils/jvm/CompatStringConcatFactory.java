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
package com.fox2code.hypertale.utils.jvm;

import com.fox2code.hypertale.annotations.MakeJVMCompat;

import java.lang.invoke.*;

/**
 * Used by Hypertale to make Java 9+ String concatenation work on Java 8
 */
@MakeJVMCompat
public final class CompatStringConcatFactory {
	private static final boolean HAS_CONCAT_FACTORY = !System.getProperty("java.version").startsWith("1.");
	private static final char TAG_ARG = '\u0001';
	private static final char TAG_CONST = '\u0002';
	private static final String TEXT_ARG1 = "\u00011";
	private static final String TEXT_ARG2 = "\u00012";
	private static final String TEXT_ARG3 = "\u00013";
	private static final String TEXT_ARG4 = "\u00014";
	private static final String TEXT_ARG5 = "\u00015";
	private static final String TEXT_ARG6 = "\u00016";
	private static final String TEXT_ARG7 = "\u00017";
	private static final String TEXT_ARG8 = "\u00018";
	private static final String TEXT_ARG9 = "\u00019";
	private static final String TEXT_ARG10 = "\u000110";
	private static final String TEXT_ARG11 = "\u000111";
	private static final String TEXT_ARG12 = "\u000112";
	private static final String TEXT_ARG13 = "\u000113";
	private static final String TEXT_ARG14 = "\u000114";
	private static final String TEXT_ARG15 = "\u000115";
	private static final String TEXT_ARG16 = "\u000116";
	private static final MethodType SIMPLE1 = MethodType.methodType(String.class,
			Object.class);
	private static final MethodType SIMPLE2 = MethodType.methodType(String.class,
			Object.class, Object.class);
	private static final MethodType SIMPLE3 = MethodType.methodType(String.class,
			Object.class, Object.class, Object.class);
	private static final MethodType SIMPLE4 = MethodType.methodType(String.class,
			Object.class, Object.class, Object.class, Object.class);
	private static final MethodType SIMPLE5 = MethodType.methodType(String.class,
			Object.class, Object.class, Object.class, Object.class, Object.class);
	private static final MethodType SIMPLE6 = MethodType.methodType(String.class,
			Object.class, Object.class, Object.class, Object.class, Object.class, Object.class);
	private static final MethodType SIMPLE7 = MethodType.methodType(String.class,
			Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class);
	private static final MethodType SIMPLE8 = MethodType.methodType(String.class,
			Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class);
	private static final MethodType SIMPLE9 = MethodType.methodType(String.class,
			Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class,
			Object.class);
	private static final MethodType SIMPLE10 = MethodType.methodType(String.class,
			Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class,
			Object.class, Object.class);
	private static final MethodType SIMPLE11 = MethodType.methodType(String.class,
			Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class,
			Object.class, Object.class, Object.class);
	private static final MethodType SIMPLE12 = MethodType.methodType(String.class,
			Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class,
			Object.class, Object.class, Object.class, Object.class);
	private static final MethodType SIMPLE13 = MethodType.methodType(String.class,
			Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class,
			Object.class, Object.class, Object.class, Object.class, Object.class);
	private static final MethodType SIMPLE14 = MethodType.methodType(String.class,
			Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class,
			Object.class, Object.class, Object.class, Object.class, Object.class, Object.class);
	private static final MethodType SIMPLE15 = MethodType.methodType(String.class,
			Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class,
			Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class);
	private static final MethodType SIMPLE16 = MethodType.methodType(String.class,
			Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class,
			Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class);
	private static final MethodHandles.Lookup selfLookup = MethodHandles.lookup();

	private CompatStringConcatFactory() {}

	public static CallSite makeConcat(MethodHandles.Lookup lookup,
	                                  String name,
	                                  MethodType concatType) {
		if (HAS_CONCAT_FACTORY) {
			try {
				return StringConcatFactory.makeConcat(lookup, name, concatType);
			} catch (Exception e) {
				sneakyThrow(e);
			}
		}
		throw new UnsupportedOperationException("Not yet implemented");
	}

	public static CallSite makeConcatWithConstants(
			MethodHandles.Lookup lookup, String name, MethodType concatType,
			String recipe, Object... constants) throws NoSuchMethodException, IllegalAccessException {
		if (HAS_CONCAT_FACTORY) {
			try {
				return StringConcatFactory.makeConcatWithConstants(lookup, name, concatType, recipe, constants);
			} catch (Exception e) {
				sneakyThrow(e);
			}
		}

		StringBuilder stringBuilder = new StringBuilder();
		int i;
		int prev = 0;
		int args = 0;
		int cst = 0;
		while ((i = nextArg(recipe, prev)) != -1) {
			stringBuilder.append(recipe, prev, i);
			if (recipe.charAt(i) == TAG_CONST) {
				stringBuilder.append(constants[cst]);
				cst++;
			} else {
				args++;
				stringBuilder.append("\u0001").append(args);
			}
			prev = i + 1;
		}
		stringBuilder.append(recipe, prev, recipe.length());
		MethodType methodType;
		switch (args) {
			case 0 -> {
				return new ConstantCallSite(MethodHandles.constant(String.class, stringBuilder.toString()));
			}
			case 1 -> methodType = SIMPLE1;
			case 2 -> methodType = SIMPLE2;
			case 3 -> methodType = SIMPLE3;
			case 4 -> methodType = SIMPLE4;
			case 5 -> methodType = SIMPLE5;
			case 6 -> methodType = SIMPLE6;
			case 7 -> methodType = SIMPLE7;
			case 8 -> methodType = SIMPLE8;
			case 9 -> methodType = SIMPLE9;
			case 10 -> methodType = SIMPLE10;
			case 11 -> methodType = SIMPLE11;
			case 12 -> methodType = SIMPLE12;
			case 13 -> methodType = SIMPLE13;
			case 14 -> methodType = SIMPLE14;
			case 15 -> methodType = SIMPLE15;
			case 16 -> methodType = SIMPLE16;
			default -> throw new Error("Only support 16 inputs maximum, failed to make recipe \"" + recipe + "\"");
		}
		try {
			return new ConstantCallSite(selfLookup.bind(new SimpleConcatHelper(
					stringBuilder.toString()), "apply", methodType).asType(concatType));
		} catch (WrongMethodTypeException t) {
			throw new RuntimeException("Failed to bake: \"" + recipe + "\"", t);
		}
	}

	private static int nextArg(String text, int from) {
		int i = text.indexOf(TAG_ARG, from);
		int i2 = text.indexOf(TAG_CONST, from);
		return i == -1 ? i2 : i2 == -1 ? i : Math.min(i, i2);
	}

	@MakeJVMCompat
	public static final class SimpleConcatHelper {
		private final String pattern;

		public SimpleConcatHelper(String pattern) {
			this.pattern = pattern;
		}

		public String apply(Object o) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o));
		}

		public String apply(Object o1, Object o2) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o1)).replace(TEXT_ARG2, String.valueOf(o2));
		}

		public String apply(Object o1, Object o2, Object o3) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o1)).replace(TEXT_ARG2, String.valueOf(o2))
					.replace(TEXT_ARG3, String.valueOf(o3));
		}

		public String apply(Object o1, Object o2, Object o3, Object o4) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o1)).replace(TEXT_ARG2, String.valueOf(o2))
					.replace(TEXT_ARG3, String.valueOf(o3)).replace(TEXT_ARG4, String.valueOf(o4));
		}

		public String apply(Object o1, Object o2, Object o3, Object o4, Object o5) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o1)).replace(TEXT_ARG2, String.valueOf(o2))
					.replace(TEXT_ARG3, String.valueOf(o3)).replace(TEXT_ARG4, String.valueOf(o4))
					.replace(TEXT_ARG5, String.valueOf(o5));
		}

		public String apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o1)).replace(TEXT_ARG2, String.valueOf(o2))
					.replace(TEXT_ARG3, String.valueOf(o3)).replace(TEXT_ARG4, String.valueOf(o4))
					.replace(TEXT_ARG5, String.valueOf(o5)).replace(TEXT_ARG6, String.valueOf(o6));
		}

		public String apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o1)).replace(TEXT_ARG2, String.valueOf(o2))
					.replace(TEXT_ARG3, String.valueOf(o3)).replace(TEXT_ARG4, String.valueOf(o4))
					.replace(TEXT_ARG5, String.valueOf(o5)).replace(TEXT_ARG6, String.valueOf(o6))
					.replace(TEXT_ARG7, String.valueOf(o7));
		}

		public String apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o1)).replace(TEXT_ARG2, String.valueOf(o2))
					.replace(TEXT_ARG3, String.valueOf(o3)).replace(TEXT_ARG4, String.valueOf(o4))
					.replace(TEXT_ARG5, String.valueOf(o5)).replace(TEXT_ARG6, String.valueOf(o6))
					.replace(TEXT_ARG7, String.valueOf(o7)).replace(TEXT_ARG8, String.valueOf(o8));
		}

		public String apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8,
		                    Object o9) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o1)).replace(TEXT_ARG2, String.valueOf(o2))
					.replace(TEXT_ARG3, String.valueOf(o3)).replace(TEXT_ARG4, String.valueOf(o4))
					.replace(TEXT_ARG5, String.valueOf(o5)).replace(TEXT_ARG6, String.valueOf(o6))
					.replace(TEXT_ARG7, String.valueOf(o7)).replace(TEXT_ARG8, String.valueOf(o8))
					.replace(TEXT_ARG9, String.valueOf(o9));
		}

		public String apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8,
		                    Object o9, Object o10) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o1)).replace(TEXT_ARG2, String.valueOf(o2))
					.replace(TEXT_ARG3, String.valueOf(o3)).replace(TEXT_ARG4, String.valueOf(o4))
					.replace(TEXT_ARG5, String.valueOf(o5)).replace(TEXT_ARG6, String.valueOf(o6))
					.replace(TEXT_ARG7, String.valueOf(o7)).replace(TEXT_ARG8, String.valueOf(o8))
					.replace(TEXT_ARG9, String.valueOf(o9)).replace(TEXT_ARG10, String.valueOf(o10));
		}

		public String apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8,
		                    Object o9, Object o10, Object o11) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o1)).replace(TEXT_ARG2, String.valueOf(o2))
					.replace(TEXT_ARG3, String.valueOf(o3)).replace(TEXT_ARG4, String.valueOf(o4))
					.replace(TEXT_ARG5, String.valueOf(o5)).replace(TEXT_ARG6, String.valueOf(o6))
					.replace(TEXT_ARG7, String.valueOf(o7)).replace(TEXT_ARG8, String.valueOf(o8))
					.replace(TEXT_ARG9, String.valueOf(o9)).replace(TEXT_ARG10, String.valueOf(o10))
					.replace(TEXT_ARG11, String.valueOf(o11));
		}

		public String apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8,
		                    Object o9, Object o10, Object o11, Object o12) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o1)).replace(TEXT_ARG2, String.valueOf(o2))
					.replace(TEXT_ARG3, String.valueOf(o3)).replace(TEXT_ARG4, String.valueOf(o4))
					.replace(TEXT_ARG5, String.valueOf(o5)).replace(TEXT_ARG6, String.valueOf(o6))
					.replace(TEXT_ARG7, String.valueOf(o7)).replace(TEXT_ARG8, String.valueOf(o8))
					.replace(TEXT_ARG9, String.valueOf(o9)).replace(TEXT_ARG10, String.valueOf(o10))
					.replace(TEXT_ARG11, String.valueOf(o11)).replace(TEXT_ARG12, String.valueOf(o12));
		}

		public String apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8,
		                    Object o9, Object o10, Object o11, Object o12, Object o13) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o1)).replace(TEXT_ARG2, String.valueOf(o2))
					.replace(TEXT_ARG3, String.valueOf(o3)).replace(TEXT_ARG4, String.valueOf(o4))
					.replace(TEXT_ARG5, String.valueOf(o5)).replace(TEXT_ARG6, String.valueOf(o6))
					.replace(TEXT_ARG7, String.valueOf(o7)).replace(TEXT_ARG8, String.valueOf(o8))
					.replace(TEXT_ARG9, String.valueOf(o9)).replace(TEXT_ARG10, String.valueOf(o10))
					.replace(TEXT_ARG11, String.valueOf(o11)).replace(TEXT_ARG12, String.valueOf(o12))
					.replace(TEXT_ARG13, String.valueOf(o13));
		}

		public String apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8,
		                    Object o9, Object o10, Object o11, Object o12, Object o13, Object o14) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o1)).replace(TEXT_ARG2, String.valueOf(o2))
					.replace(TEXT_ARG3, String.valueOf(o3)).replace(TEXT_ARG4, String.valueOf(o4))
					.replace(TEXT_ARG5, String.valueOf(o5)).replace(TEXT_ARG6, String.valueOf(o6))
					.replace(TEXT_ARG7, String.valueOf(o7)).replace(TEXT_ARG8, String.valueOf(o8))
					.replace(TEXT_ARG9, String.valueOf(o9)).replace(TEXT_ARG10, String.valueOf(o10))
					.replace(TEXT_ARG11, String.valueOf(o11)).replace(TEXT_ARG12, String.valueOf(o12))
					.replace(TEXT_ARG13, String.valueOf(o13)).replace(TEXT_ARG14, String.valueOf(o14));
		}

		public String apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8,
		                    Object o9, Object o10, Object o11, Object o12, Object o13, Object o14, Object o15) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o1)).replace(TEXT_ARG2, String.valueOf(o2))
					.replace(TEXT_ARG3, String.valueOf(o3)).replace(TEXT_ARG4, String.valueOf(o4))
					.replace(TEXT_ARG5, String.valueOf(o5)).replace(TEXT_ARG6, String.valueOf(o6))
					.replace(TEXT_ARG7, String.valueOf(o7)).replace(TEXT_ARG8, String.valueOf(o8))
					.replace(TEXT_ARG9, String.valueOf(o9)).replace(TEXT_ARG10, String.valueOf(o10))
					.replace(TEXT_ARG11, String.valueOf(o11)).replace(TEXT_ARG12, String.valueOf(o12))
					.replace(TEXT_ARG13, String.valueOf(o13)).replace(TEXT_ARG14, String.valueOf(o14))
					.replace(TEXT_ARG15, String.valueOf(o15));
		}

		public String apply(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8,
		                    Object o9, Object o10, Object o11, Object o12, Object o13, Object o14, Object o15, Object o16) {
			return this.pattern.replace(TEXT_ARG1, String.valueOf(o1)).replace(TEXT_ARG2, String.valueOf(o2))
					.replace(TEXT_ARG3, String.valueOf(o3)).replace(TEXT_ARG4, String.valueOf(o4))
					.replace(TEXT_ARG5, String.valueOf(o5)).replace(TEXT_ARG6, String.valueOf(o6))
					.replace(TEXT_ARG7, String.valueOf(o7)).replace(TEXT_ARG8, String.valueOf(o8))
					.replace(TEXT_ARG9, String.valueOf(o9)).replace(TEXT_ARG10, String.valueOf(o10))
					.replace(TEXT_ARG11, String.valueOf(o11)).replace(TEXT_ARG12, String.valueOf(o12))
					.replace(TEXT_ARG13, String.valueOf(o13)).replace(TEXT_ARG14, String.valueOf(o14))
					.replace(TEXT_ARG15, String.valueOf(o15)).replace(TEXT_ARG16, String.valueOf(o16));
		}
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
		throw (E) e;
	}
}
