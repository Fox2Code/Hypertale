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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public final class OpcodesUtils {
	private OpcodesUtils() {}

	public static int getStackConsume(AbstractInsnNode insn) {
		int opcode = insn.getOpcode();
		if (opcode == -1) return 0;

		return switch (opcode) {
			case Opcodes.NOP, Opcodes.IINC, Opcodes.LDC, Opcodes.ALOAD, Opcodes.ILOAD, Opcodes.FLOAD, Opcodes.DLOAD,
				 Opcodes.LLOAD, Opcodes.ACONST_NULL, Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1,
				 Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5, Opcodes.FCONST_0,
				 Opcodes.FCONST_1, Opcodes.FCONST_2, Opcodes.DCONST_0, Opcodes.DCONST_1, Opcodes.LCONST_0,
				 Opcodes.LCONST_1, Opcodes.BIPUSH, Opcodes.SIPUSH, Opcodes.RETURN, Opcodes.GETSTATIC, Opcodes.GOTO,
				 Opcodes.NEW -> 0;
			case Opcodes.POP, Opcodes.DUP, Opcodes.ASTORE, Opcodes.ISTORE, Opcodes.FSTORE, Opcodes.ATHROW,
				 Opcodes.ARETURN, Opcodes.FRETURN, Opcodes.IRETURN, Opcodes.GETFIELD, Opcodes.IFNONNULL, Opcodes.IFNULL,
				 Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE, Opcodes.NEWARRAY,
				 Opcodes.ANEWARRAY, Opcodes.ARRAYLENGTH, Opcodes.INSTANCEOF, Opcodes.CHECKCAST, Opcodes.I2L,
				 Opcodes.I2F, Opcodes.I2D, Opcodes.F2I, Opcodes.F2L, Opcodes.F2D, Opcodes.I2B, Opcodes.I2C, Opcodes.I2S,
				 Opcodes.INEG, Opcodes.FNEG, Opcodes.TABLESWITCH, Opcodes.LOOKUPSWITCH, Opcodes.MONITORENTER,
				 Opcodes.MONITOREXIT -> 1;
			case Opcodes.POP2, Opcodes.SWAP, Opcodes.DUP2, Opcodes.DUP_X1, Opcodes.IADD, Opcodes.ISUB, Opcodes.IMUL,
				 Opcodes.IDIV, Opcodes.IREM, Opcodes.IOR, Opcodes.IXOR, Opcodes.IAND, Opcodes.FADD, Opcodes.FSUB,
				 Opcodes.FMUL, Opcodes.FDIV, Opcodes.FREM, Opcodes.ISHL, Opcodes.ISHR, Opcodes.IUSHR, Opcodes.AALOAD,
				 Opcodes.IALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD, Opcodes.FALOAD, Opcodes.DSTORE,
				 Opcodes.LSTORE, Opcodes.DRETURN, Opcodes.LRETURN, Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE,
				 Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE, Opcodes.IF_ACMPEQ,
				 Opcodes.IF_ACMPNE, Opcodes.L2I, Opcodes.L2F, Opcodes.L2D, Opcodes.D2I, Opcodes.D2L, Opcodes.D2F,
				 Opcodes.LNEG, Opcodes.DNEG, Opcodes.FCMPL, Opcodes.FCMPG -> 2;
			case Opcodes.DUP_X2, Opcodes.DUP2_X1, Opcodes.DALOAD, Opcodes.LALOAD, Opcodes.IASTORE, Opcodes.FASTORE,
				 Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE -> 3;
			case Opcodes.DUP2_X2, Opcodes.LADD, Opcodes.LSUB, Opcodes.LMUL, Opcodes.LDIV, Opcodes.LREM, Opcodes.LAND,
				 Opcodes.LOR, Opcodes.LXOR, Opcodes.DADD, Opcodes.DSUB, Opcodes.DMUL, Opcodes.DDIV, Opcodes.DREM,
				 Opcodes.LSHL, Opcodes.LSHR, Opcodes.LUSHR, Opcodes.LASTORE, Opcodes.DASTORE, Opcodes.LCMP,
				 Opcodes.DCMPL, Opcodes.DCMPG -> 4;
			case Opcodes.PUTSTATIC -> Type.getType(((FieldInsnNode) insn).desc).getSize();
			case Opcodes.PUTFIELD -> Type.getType(((FieldInsnNode) insn).desc).getSize() + 1;
			case Opcodes.INVOKESTATIC -> (Type.getArgumentsAndReturnSizes(((MethodInsnNode) insn).desc) >> 2) - 1;
			case Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKEINTERFACE ->
					Type.getArgumentsAndReturnSizes(((MethodInsnNode) insn).desc) >> 2;
			case Opcodes.INVOKEDYNAMIC ->
					(Type.getArgumentsAndReturnSizes(((InvokeDynamicInsnNode) insn).desc) >> 2) - 1;
			case Opcodes.MULTIANEWARRAY -> ((MultiANewArrayInsnNode) insn).dims;
			default -> throw new IllegalArgumentException("Unsupported stack opcode: " + opcode);
		};
	}

	public static int getStackProduce(AbstractInsnNode insn) {
		int opcode = insn.getOpcode();
		if (opcode == -1) return 0;

		return switch (opcode) {
			case Opcodes.NOP, Opcodes.IINC, Opcodes.POP, Opcodes.POP2, Opcodes.ASTORE, Opcodes.ISTORE, Opcodes.FSTORE,
				 Opcodes.LSTORE, Opcodes.DSTORE, Opcodes.ATHROW, Opcodes.ARETURN, Opcodes.FRETURN, Opcodes.IRETURN,
				 Opcodes.DRETURN, Opcodes.LRETURN, Opcodes.RETURN, Opcodes.PUTFIELD, Opcodes.PUTSTATIC,
				 Opcodes.IFNONNULL, Opcodes.IFNULL, Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE,
				 Opcodes.IFGT, Opcodes.IFLE, Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE,
				 Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE, Opcodes.IASTORE,
				 Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE,
				 Opcodes.SASTORE, Opcodes.GOTO, Opcodes.TABLESWITCH, Opcodes.LOOKUPSWITCH, Opcodes.MONITORENTER,
				 Opcodes.MONITOREXIT -> 0;
			case Opcodes.ALOAD, Opcodes.ILOAD, Opcodes.FLOAD, Opcodes.ACONST_NULL, Opcodes.ICONST_M1, Opcodes.ICONST_0,
				 Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5,
				 Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2, Opcodes.BIPUSH, Opcodes.SIPUSH, Opcodes.IADD,
				 Opcodes.ISUB, Opcodes.IMUL, Opcodes.IDIV, Opcodes.IREM, Opcodes.IOR, Opcodes.IXOR, Opcodes.IAND,
				 Opcodes.FADD, Opcodes.FSUB, Opcodes.FMUL, Opcodes.FDIV, Opcodes.FREM, Opcodes.ISHL, Opcodes.ISHR,
				 Opcodes.IUSHR, Opcodes.AALOAD, Opcodes.IALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD,
				 Opcodes.FALOAD, Opcodes.NEWARRAY, Opcodes.ANEWARRAY, Opcodes.ARRAYLENGTH, Opcodes.INSTANCEOF,
				 Opcodes.CHECKCAST, Opcodes.NEW, Opcodes.I2F, Opcodes.L2I, Opcodes.L2F, Opcodes.F2I, Opcodes.D2I,
				 Opcodes.D2F, Opcodes.I2B, Opcodes.I2C, Opcodes.I2S, Opcodes.INEG, Opcodes.FNEG, Opcodes.LCMP,
				 Opcodes.FCMPL, Opcodes.FCMPG, Opcodes.DCMPL, Opcodes.DCMPG, Opcodes.MULTIANEWARRAY -> 1;
			case Opcodes.DLOAD, Opcodes.LLOAD, Opcodes.DUP, Opcodes.SWAP, Opcodes.LADD, Opcodes.LSUB, Opcodes.LMUL,
				 Opcodes.LDIV, Opcodes.LREM, Opcodes.LAND, Opcodes.LOR, Opcodes.LXOR, Opcodes.DADD, Opcodes.DSUB,
				 Opcodes.DMUL, Opcodes.DDIV, Opcodes.DREM, Opcodes.LSHL, Opcodes.LSHR, Opcodes.LUSHR, Opcodes.DALOAD,
				 Opcodes.LALOAD, Opcodes.DCONST_0, Opcodes.DCONST_1, Opcodes.LCONST_0, Opcodes.LCONST_1, Opcodes.I2L,
				 Opcodes.I2D, Opcodes.L2D, Opcodes.F2L, Opcodes.F2D, Opcodes.D2L, Opcodes.LNEG, Opcodes.DNEG -> 2;
			case Opcodes.DUP_X1 -> 3;
			case Opcodes.DUP_X2, Opcodes.DUP2 -> 4;
			case Opcodes.DUP2_X1 -> 5;
			case Opcodes.DUP2_X2 -> 6;
			case Opcodes.GETSTATIC, Opcodes.GETFIELD -> Type.getType(((FieldInsnNode) insn).desc).getSize();
			case Opcodes.INVOKESTATIC, Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKEINTERFACE ->
					Type.getArgumentsAndReturnSizes(((MethodInsnNode) insn).desc) & 3;
			case Opcodes.INVOKEDYNAMIC -> Type.getArgumentsAndReturnSizes(((InvokeDynamicInsnNode) insn).desc) & 3;
			case Opcodes.LDC -> {
				Object value = ((LdcInsnNode) insn).cst;
				yield (value instanceof Double || value instanceof Long) ? 2 : 1;
			}
			default -> throw new IllegalArgumentException("Unsupported stack opcode: " + opcode);
		};
	}
}
