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
package com.fox2code.hypertale.launcher;

import com.fox2code.hypertale.init.Agent;

import javax.annotation.Nullable;
import java.lang.instrument.Instrumentation;

public final class HypertaleAgent {
	private static Instrumentation instrumentation;

	private HypertaleAgent() {}

	public static void premain(final String agentArgs, final Instrumentation instrumentation) {
		if (HypertaleAgent.instrumentation == null && instrumentation != null) {
			HypertaleAgent.instrumentation = instrumentation;
		}
	}

	public static void agentmain(final String agentArgs, final Instrumentation instrumentation) {
		if (HypertaleAgent.instrumentation == null && instrumentation != null) {
			HypertaleAgent.instrumentation = instrumentation;
		}
	}

	/**
	 * Return an instrumentation object if present.
	 *
	 * @return the current instrumentation if present
	 */
	@Nullable public static Instrumentation getInstrumentation() {
		return instrumentation;
	}

	static void tryLoadEarlyAgent() {
		if (instrumentation == null) {
			try {
				// Can fail with NoSuchMethodError
				instrumentation = Agent.getInstrumentation();
			} catch (Throwable _) {}
		}
		if (instrumentation == null) {
			// Try to get ByteBuddy agent for the development environment
			try {
				instrumentation = (Instrumentation)
						Class.forName("net.bytebuddy.agent.ByteBuddyAgent")
								.getMethod("getInstrumentation").invoke(null);
			} catch (Exception _) {}
		}
		if (instrumentation == null) {
			instrumentation = MainPlus.tryGetInstrumentationFallback();
		}
	}
}
