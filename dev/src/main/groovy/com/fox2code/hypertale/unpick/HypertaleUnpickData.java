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
package com.fox2code.hypertale.unpick;

import com.fox2code.hypertale.patcher.HypertaleASMConstants;
import com.fox2code.hypertale.patcher.TransformerUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Textifier;

import java.util.*;

public final class HypertaleUnpickData implements HypertaleASMConstants {
	private static final HashMap<String, ConstantUnpick> staticConstantUnpicks = new HashMap<>();
	private static final HashMap<String, ConstantUnpick> virtualConstantUnpicks = new HashMap<>();
	private static final HashMap<String, ConstantUnpick> returnStaticConstantUnpicks = new HashMap<>();
	private static final HashMap<String, ConstantUnpick> putStaticConstantUnpicks = new HashMap<>();
	private static final HashMap<String, ConstantUnpick> putVirtualConstantUnpicks = new HashMap<>();

	static {
		// Java Unpicks
		ConstantUnpick longExtremesUnpicks = new LongStaticConstantUnpick("java/lang/Long") {
			@Override
			public String unpick(long value) {
				if (value == Long.MIN_VALUE) {
					return "MIN_VALUE";
				} else if (value == Long.MAX_VALUE) {
					return "MAX_VALUE";
				}
				return null;
			}
		};
		ConstantUnpick threadPriorityUnpick = new IntStaticConstantUnpick("java/lang/Thread") {
			@Override
			public String unpick(int value) {
				return switch (value) {
					case Thread.MIN_PRIORITY -> "MIN_PRIORITY";
					case Thread.NORM_PRIORITY -> "NORM_PRIORITY";
					case Thread.MAX_PRIORITY -> "MAX_PRIORITY";
					default -> null;
				};
			}
		};
		virtualConstantUnpicks.put("java/lang/Thread.setPriority(I)V", threadPriorityUnpick);
		staticConstantUnpicks.put("java/lang/Thread.sleep(J)V", longExtremesUnpicks);
		// Hytale unpicks
		ConstantUnpick entityUnassignedId = new IntStaticConstantUnpick(Entity) {
			@Override
			public String unpick(int value) {
				return value == -1 ? "UNASSIGNED_ID" : null;
			}
		};
		putVirtualConstantUnpicks.put(Entity + "#networkId", entityUnassignedId);
	}

	static void patchForDev(HypertaleUnpickConstants developmentSourceConstantData, ClassNode classNode) {
		final IdentityHashMap<AbstractInsnNode, InsnList> constantPatching = new IdentityHashMap<>();
		final ArrayList<AbstractInsnNode> nodesCache = new ArrayList<>();
		final List<HypertaleUnpickConstants.ConstantCheck> classConstantChecks =
				developmentSourceConstantData.getClassConstantChecks(classNode);
		for (MethodNode methodNode : classNode.methods) {
			final InsnList insnList = methodNode.instructions;
			final int state = developmentSourceConstantData.methodStatus(classNode, methodNode);
			if (!constantPatching.isEmpty()) {
				constantPatching.clear();
			}
			for (AbstractInsnNode abstractInsnNode : insnList) {
				final int opcode = abstractInsnNode.getOpcode();
				if (opcode == Opcodes.INVOKESTATIC) {
					MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
					ConstantUnpick constantUnpick =
							staticConstantUnpicks.get(methodInsnNode.owner + "." +
									methodInsnNode.name + methodInsnNode.desc);
					ConstantUnpick returnConstantUnpick =
							returnStaticConstantUnpicks.get(methodInsnNode.owner + "." +
									methodInsnNode.name + methodInsnNode.desc);
					if (constantUnpick != null) {
						constantUnpick.unpick(insnList, methodInsnNode.getPrevious());
					}
					if (returnConstantUnpick != null) {
						AbstractInsnNode insn = methodInsnNode.getNext().getNext();
						if (insn != null) {
							switch (insn.getOpcode()) {
								case Opcodes.IF_ICMPEQ:
								case Opcodes.IF_ICMPNE:
								case Opcodes.IF_ICMPLE:
								case Opcodes.IF_ICMPGE:
								case Opcodes.IF_ICMPLT:
								case Opcodes.IF_ICMPGT:
									returnConstantUnpick.unpick(insnList, methodInsnNode.getNext());
							}
						}
					}
				} else if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESPECIAL) {
					MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
					ConstantUnpick constantUnpick =
							virtualConstantUnpicks.get(methodInsnNode.owner + "." +
									methodInsnNode.name + methodInsnNode.desc);
					if (constantUnpick != null) {
						constantUnpick.unpick(insnList, methodInsnNode.getPrevious());
					}
				} else if (opcode == Opcodes.PUTSTATIC) {
					FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
					ConstantUnpick constantUnpick =
							putStaticConstantUnpicks.get(fieldInsnNode.owner + "#" + fieldInsnNode.name);
					if (constantUnpick != null) {
						constantUnpick.unpick(insnList, fieldInsnNode.getPrevious());
					}
				} else if (opcode == Opcodes.PUTFIELD) {
					FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
					ConstantUnpick constantUnpick =
							putVirtualConstantUnpicks.get(fieldInsnNode.owner + "#" + fieldInsnNode.name);
					if (constantUnpick != null) {
						constantUnpick.unpick(insnList, fieldInsnNode.getPrevious());
					}
				} else if (opcode == Opcodes.LDC) {
					LdcInsnNode ldcInsnNode = (LdcInsnNode) abstractInsnNode;
					if (ldcInsnNode.cst instanceof String) {
						developmentSourceConstantData.patchStringConstant(
								constantPatching, classConstantChecks, state, ldcInsnNode, nodesCache);
					}
				}
			}
			for (Map.Entry<AbstractInsnNode, InsnList> patching : constantPatching.entrySet()) {
				AbstractInsnNode oldConstant = patching.getKey();
				if (oldConstant.getNext() == null) continue;
				insnList.insert(oldConstant, patching.getValue());
				insnList.remove(oldConstant);
			}
		}
	}

	public static abstract class ConstantUnpick {
		private StringBuilder testDebug;

		public abstract void unpick(InsnList insnList, AbstractInsnNode constant);

		public final ConstantUnpick setTestDebug(StringBuilder testDebug) {
			this.setTestDebugOnChilds(testDebug);
			this.testDebug = testDebug;
			return this;
		}

		void setTestDebugOnChilds(StringBuilder testDebug) {}

		protected final void testDbg(String line) {
			if (this.testDebug != null) {
				this.testDebug.append(line).append("\n");
			}
		}

		protected final void testDbgOpcode(int opcode) {
			if (this.testDebug != null) {
				this.testDebug.append("Opcode: ").append(
						Textifier.OPCODES[opcode]).append("\n");
			}
		}

		protected final void testDbgOpcode(String text, int opcode) {
			if (this.testDebug != null) {
				this.testDebug.append(text).append(
						Textifier.OPCODES[opcode]).append("\n");
			}
		}

		protected final void npeDbg(InsnList insnList, Object obj) {
			if (obj != null) return;
			if (this.testDebug == null) throw new NullPointerException();
			StringBuilder stringBuilder = new StringBuilder()
					.append("\n---\n").append(this.testDebug).append("\n---\n");
			TransformerUtils.printInsnList(insnList, stringBuilder);
			throw new NullPointerException(stringBuilder.append("\n---").toString());
		}
	}

	public static final class MultiConstantUnpick extends ConstantUnpick {
		private final ConstantUnpick[] constantUnpicks;

		public MultiConstantUnpick(ConstantUnpick... constantUnpicks) {
			this.constantUnpicks = constantUnpicks;
		}

		@Override
		public void unpick(InsnList insnList, AbstractInsnNode constant) {
			for (ConstantUnpick constantUnpick : constantUnpicks) {
				if (constant.getNext() == null) break;
				constantUnpick.unpick(insnList, constant);
			}
		}

		@Override
		void setTestDebugOnChilds(StringBuilder testDebug) {
			for (ConstantUnpick constantUnpick : constantUnpicks) {
				constantUnpick.setTestDebug(testDebug);
			}
		}
	}

	public static final class ParamsConstantUnpick extends ConstantUnpick {
		private final ConstantUnpick[] constantUnpicks;

		public ParamsConstantUnpick(ConstantUnpick... constantUnpicks) {
			this.constantUnpicks = constantUnpicks;
		}

		@Override
		public void unpick(InsnList insnList, AbstractInsnNode constant) {
			boolean skipNextUnpick;
			for (int i = constantUnpicks.length - 1; i >= 0; i--) {
				skipNextUnpick = false;
				int loops = 1;
				while (loops-->0) {
					npeDbg(insnList, constant); // <- for test
					testDbgOpcode(constant.getOpcode());
					switch (constant.getOpcode()) {
						default:
							return;
						case Opcodes.INVOKEINTERFACE:
						case Opcodes.INVOKEVIRTUAL:
						case Opcodes.INVOKESPECIAL:
							loops++;
						case Opcodes.INVOKESTATIC: {
							MethodInsnNode methodInsnNode = (MethodInsnNode) constant;
							if (methodInsnNode.desc.endsWith(")V")) return;
							loops += Type.getArgumentTypes(methodInsnNode.desc).length;
							skipNextUnpick = true;
							break;
						}
						case Opcodes.DUP:
							loops--;
							break;
						case Opcodes.IADD:
						case Opcodes.ISUB:
						case Opcodes.IMUL:
						case Opcodes.IDIV:
						case Opcodes.FADD:
						case Opcodes.FSUB:
						case Opcodes.FMUL:
						case Opcodes.FDIV:
						case Opcodes.DADD:
						case Opcodes.DSUB:
						case Opcodes.DMUL:
						case Opcodes.DDIV:
						case Opcodes.IAND:
						case Opcodes.IOR:
						case Opcodes.IXOR:
						case Opcodes.ISHL:
						case Opcodes.ISHR:
						case Opcodes.IUSHR:
							loops++;
						case Opcodes.ARRAYLENGTH:
						case Opcodes.GETFIELD:
						case Opcodes.INEG:
						case Opcodes.FNEG:
						case Opcodes.DNEG:
						case Opcodes.I2D:
						case Opcodes.I2F:
						case Opcodes.F2I:
						case Opcodes.D2I:
						case Opcodes.D2F:
						case Opcodes.L2D:
						case Opcodes.L2I:
						case Opcodes.L2F:
						case Opcodes.I2L:
						case Opcodes.D2L:
						case Opcodes.F2L:
							skipNextUnpick = true;
						case Opcodes.CHECKCAST:
						case Opcodes.I2B:
						case Opcodes.I2C:
						case Opcodes.I2S:
							loops++;
						case Opcodes.GETSTATIC:
						case Opcodes.ACONST_NULL:
						case Opcodes.ICONST_M1:
						case Opcodes.ICONST_0:
						case Opcodes.ICONST_1:
						case Opcodes.ICONST_2:
						case Opcodes.ICONST_3:
						case Opcodes.ICONST_4:
						case Opcodes.ICONST_5:
						case Opcodes.FCONST_0:
						case Opcodes.FCONST_1:
						case Opcodes.FCONST_2:
						case Opcodes.DCONST_0:
						case Opcodes.DCONST_1:
						case Opcodes.LCONST_0:
						case Opcodes.LCONST_1:
						case Opcodes.BIPUSH:
						case Opcodes.SIPUSH:
						case Opcodes.LDC:
						case Opcodes.ALOAD:
						case Opcodes.ILOAD:
						case Opcodes.FLOAD:
						case Opcodes.DLOAD:
					}
					if (loops > 0) {
						constant = constant.getPrevious();
					}
					testDbg("Loops left: " + loops + " skip next: " + skipNextUnpick);
				}
				loops++;
				if (loops < 0) {
					testDbg("Neg loop: " + loops);
					i += loops;
					skipNextUnpick = true;
				}
				ConstantUnpick constantUnpick = constantUnpicks[i];
				testDbg("Unpick: " + i + "/" + constantUnpicks.length + " opcode: " + Textifier.OPCODES[constant.getOpcode()]);
				if (constantUnpick != null && !skipNextUnpick) {
					testDbg("Processing...");
					AbstractInsnNode previous = constant.getPrevious();
					constantUnpick.unpick(insnList, constant);
					if (previous == null ||
							(constant = previous.getNext()) == null) {
						return;
					}
				}
				constant = constant.getPrevious();
			}
		}

		@Override
		void setTestDebugOnChilds(StringBuilder testDebug) {
			for (ConstantUnpick constantUnpick : constantUnpicks) {
				if (constantUnpick != null) {
					constantUnpick.setTestDebug(testDebug);
				}
			}
		}
	}

	public static abstract class LongConstantUnpick extends ConstantUnpick {
		@Override
		public final void unpick(InsnList insnList, AbstractInsnNode constant) {
			switch (constant.getOpcode()) {
				case Opcodes.LCONST_0:
					unpick(insnList, constant, 0);
					break;
				case Opcodes.LCONST_1:
					unpick(insnList, constant, 1);
					break;
				case Opcodes.LDC:
					Object ldc = ((LdcInsnNode) constant).cst;
					if (ldc instanceof Long) {
						unpick(insnList, constant, (Long) ldc);
					}
			}
		}

		public abstract void unpick(InsnList insnList, AbstractInsnNode constant, long value);
	}

	public static abstract class IntConstantUnpick extends ConstantUnpick {
		@Override
		public final void unpick(InsnList insnList, AbstractInsnNode constant) {
			switch (constant.getOpcode()) {
				case Opcodes.ICONST_M1:
					unpick(insnList, constant, -1);
					break;
				case Opcodes.ICONST_0:
					unpick(insnList, constant, 0);
					break;
				case Opcodes.ICONST_1:
					unpick(insnList, constant, 1);
					break;
				case Opcodes.ICONST_2:
					unpick(insnList, constant, 2);
					break;
				case Opcodes.ICONST_3:
					unpick(insnList, constant, 3);
					break;
				case Opcodes.ICONST_4:
					unpick(insnList, constant, 4);
					break;
				case Opcodes.ICONST_5:
					unpick(insnList, constant, 5);
					break;
				case Opcodes.BIPUSH:
				case Opcodes.SIPUSH:
					unpick(insnList, constant, ((IntInsnNode) constant).operand);
					break;
				case Opcodes.LDC:
					Object ldc = ((LdcInsnNode) constant).cst;
					if (ldc instanceof Integer) {
						unpick(insnList, constant, (Integer) ldc);
					}
			}
		}

		public abstract void unpick(InsnList insnList, AbstractInsnNode constant, int value);
	}

	private static abstract class StringConstantUnpick extends ConstantUnpick {
		@Override
		public void unpick(InsnList insnList, AbstractInsnNode constant) {
			if (constant.getOpcode() == Opcodes.LDC) {
				Object ldc = ((LdcInsnNode) constant).cst;
				if (ldc instanceof String) {
					unpick(insnList, constant, (String) ldc);
				}
			}
		}

		public abstract void unpick(InsnList insnList, AbstractInsnNode constant, String value);
	}

	public static final class CheckCastConstantUnpick extends ConstantUnpick {
		private final String type;

		private CheckCastConstantUnpick(String type) {
			this.type = type;
		}

		@Override
		public void unpick(InsnList insnList, AbstractInsnNode constant) {
			if (constant.getOpcode() == Opcodes.ACONST_NULL &&
					// For ParamsConstantUnpick compatibility we must
					// also check if next instruction is not a check-cast
					constant.getNext().getOpcode() != Opcodes.CHECKCAST) {
				insnList.insert(constant, new TypeInsnNode(Opcodes.CHECKCAST, this.type));
			}
		}
	}

	public static abstract class GeneratedStaticConstantUnpick extends IntConstantUnpick {
		public GeneratedStaticConstantUnpick() {}

		@Override
		public void unpick(InsnList insnList, AbstractInsnNode constant, int value) {
			FieldInsnNode fieldInsnNode = unpick(value);
			if (fieldInsnNode != null) {
				insnList.insert(constant, fieldInsnNode);
				insnList.remove(constant);
				testDbg("Found field " + fieldInsnNode.name + " for value " + value);
			} else {
				testDbg("Failed to find field for value " + value);
			}
		}

		public abstract FieldInsnNode unpick(int value);
	}

	public static abstract class IntStaticConstantUnpick extends IntConstantUnpick {
		private final String owner;

		public IntStaticConstantUnpick(String owner) {
			this.owner = owner;
		}

		@Override
		public void unpick(InsnList insnList, AbstractInsnNode constant, int value) {
			String fieldName = unpick(value);
			if (fieldName != null) {
				insnList.insert(constant,
						new FieldInsnNode(Opcodes.GETSTATIC, this.owner, fieldName, "I"));
				insnList.remove(constant);
			}
		}

		public abstract String unpick(int value);
	}

	public static abstract class LongStaticConstantUnpick extends LongConstantUnpick {
		private final String owner;

		public LongStaticConstantUnpick(String owner) {
			this.owner = owner;
		}

		@Override
		public void unpick(InsnList insnList, AbstractInsnNode constant, long value) {
			String fieldName = unpick(value);
			if (fieldName != null) {
				insnList.insert(constant,
						new FieldInsnNode(Opcodes.GETSTATIC, this.owner, fieldName, "J"));
				insnList.remove(constant);
			}
		}

		public abstract String unpick(long value);
	}

	public static class FlagIntStaticConstantUnpick extends IntConstantUnpick {
		private final String defaultOwner;
		private final Flag[] flags;

		public FlagIntStaticConstantUnpick(String defaultOwner, Flag... flags) {
			this.defaultOwner = defaultOwner;
			this.flags = flags;
		}

		@Override
		public void unpick(InsnList insnList, AbstractInsnNode constant, int value) {
			if (value == 0) return;
			LinkedList<Flag> flags = new LinkedList<>();
			for (Flag flag : this.flags) {
				if ((flag.value & value) == flag.value) {
					value &= ~flag.value;
					flags.add(flag);
					if (value == 0)
						break;
				}
			}
			if (flags.isEmpty()) return;
			boolean first = true;
			for (Flag flag : flags) {
				String owner = this.defaultOwner;
				if (flag.owner != null) owner = flag.owner;
				insnList.insertBefore(constant, new FieldInsnNode(Opcodes.GETSTATIC, owner, flag.name, "I"));
				if (first) {
					first = false;
				} else {
					insnList.insertBefore(constant, new InsnNode(Opcodes.IOR));
				}
			}
			if (value != 0) {
				insnList.insertBefore(constant, TransformerUtils.getNumberInsn(value));
				insnList.insertBefore(constant, new InsnNode(Opcodes.IOR));
			}
			insnList.remove(constant);
		}

	}

	public static class Flag {
		public final int value;
		public final String owner;
		public final String name;

		Flag(int value, String owner, String name) {
			this.value = value;
			this.owner = owner;
			this.name = name;
		}

		Flag(int value, String name) {
			this.value = value;
			this.owner = null;
			this.name = name;
		}
	}

	private static final class DebugConstantUnpick extends ConstantUnpick {
		private final StringBuilder stringBuilder = new StringBuilder();
		private final ConstantUnpick constantUnpick;

		private DebugConstantUnpick(ConstantUnpick constantUnpick) {
			this.constantUnpick = constantUnpick;
			this.constantUnpick.setTestDebug(this.stringBuilder);
		}

		@Override
		public void unpick(InsnList insnList, AbstractInsnNode constant) {
			synchronized (this.stringBuilder) {
				this.stringBuilder.setLength(0);
				this.stringBuilder.append("Unpick debug:\n");
				this.constantUnpick.unpick(insnList, constant);
				if (this.stringBuilder.length() > 0) {
					System.out.println(this.stringBuilder);
				}
			}
		}

		@Override
		void setTestDebugOnChilds(StringBuilder testDebug) {
			throw new UnsupportedOperationException();
		}
	}
}
