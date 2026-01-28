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
package com.fox2code.hypertale.commands;

import com.fox2code.hypertale.launcher.BuildConfig;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import org.jspecify.annotations.NonNull;

public final class HypertaleCommand extends AbstractCommandCollection {
	public HypertaleCommand() {
		super("hypertale", """
		Hypertale is a mod for the HytaleServer software bringing extra APIs and optimizations!
		
		Current Hypertale version: %version%
		Source code: https://github.com/Fox2Code/Hypertale
		""".replace("%version%", BuildConfig.HYPERTALE_VERSION));
		this.addSubCommand(new HypertaleVersionCommand());
		this.addSubCommand(new HypertaleSystemCommand());
		this.addSubCommand(new HypertaleStatusCommand());
		this.addSubCommand(new HypertalePyroCommand());
		this.addAliases("hyper");
	}

	@Override
	public boolean hasPermission(@NonNull CommandSender sender) {
		return true;
	}
}
