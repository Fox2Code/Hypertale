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
package com.fox2code.hypertale.patcher.mixin;

import com.fox2code.hypertale.launcher.EarlyLogger;
import com.fox2code.hypertale.utils.StackTraceStringifier;
import com.google.errorprone.annotations.FormatMethod;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.LoggerAdapterAbstract;

public final class MixinLoggerAdapter extends LoggerAdapterAbstract {
	private HytaleLoggerBridge hytaleLoggerBridge;

	MixinLoggerAdapter(String id) {
		super(id);
	}

	HytaleLoggerBridge getHytaleLoggerBridge() {
		if (this.hytaleLoggerBridge != null) {
			return this.hytaleLoggerBridge;
		}
		synchronized (this) {
			if (this.hytaleLoggerBridge == null) {
				this.hytaleLoggerBridge = HytaleLoggerAdapter.getLoggerBridge(this.getId());
			}
		}
		return this.hytaleLoggerBridge;
	}

	@Override
	public String getType() {
		return "Hypertale log adapter";
	}

	@Override
	public void catching(Level level, Throwable t) {
		if (MixinLoader.isPostInitialized()) {
			this.getHytaleLoggerBridge().catching(level, t);
			return;
		}
		if (level == Level.DEBUG || level == Level.TRACE) return;
		EarlyLogger.logRaw("[Mixin] " + level.name() + ": \n" +
				StackTraceStringifier.stringifyStackTrace(t));
	}

	@Override
	@FormatMethod
	public void log(Level level, String message, Object... params) {
		if (MixinLoader.isPostInitialized()) {
			this.getHytaleLoggerBridge().log(level, message, params);
			return;
		}
		if (level == Level.DEBUG || level == Level.TRACE) return;
		EarlyLogger.logRaw("[Mixin] " + level.name() + ": \n" +
				String.format(message, params));

	}

	@Override
	public void log(Level level, String message, Throwable t) {
		if (MixinLoader.isPostInitialized()) {
			this.getHytaleLoggerBridge().log(level, message, t);
			return;
		}
		if (level == Level.DEBUG || level == Level.TRACE) return;
		EarlyLogger.logRaw("[Mixin] " + level.name() + ": " + message + "\n" +
				StackTraceStringifier.stringifyStackTrace(t));
	}

	@Override
	public <T extends Throwable> T throwing(T t) {
		if (MixinLoader.isPostInitialized()) {
			this.getHytaleLoggerBridge().throwing(t);
			return t;
		}
		EarlyLogger.logRaw("[Mixin] Throwing:\n" +
				StackTraceStringifier.stringifyStackTrace(t));
		return t;
	}

	static abstract class HytaleLoggerBridge {

		public abstract void catching(Level level, Throwable t);

		public abstract void log(Level level, String message, Object... params);

		public abstract void log(Level level, String message, Throwable t);

		public abstract void throwing(Throwable t);
	}
}
