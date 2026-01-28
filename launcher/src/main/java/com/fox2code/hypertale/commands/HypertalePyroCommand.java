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

import com.fox2code.hypertale.loader.HypertalePlugin;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

final class HypertalePyroCommand extends AbstractCommand {
	static final Message PYRO_MESSAGE = Message.join(HypertalePlugin.HYPERTALE,
			Message.raw(": Pyro has Hytale game servers -> "),
			Message.raw("https://pyro.host/").link("https://pyro.host/?ref=QAIPYBDW")
					.color(new Color(127, 127, 255)));
	static final Message PYRO_MESSAGE_ALT = Message.join(HypertalePlugin.HYPERTALE,
			Message.raw(": Pyro has Hytale game servers -> "),
			Message.raw("https://pyro.host/?ref=QAIPYBDW").link("https://pyro.host/?ref=QAIPYBDW")
					.color(new Color(127, 127, 255)));

	HypertalePyroCommand() {
		super("pyro", "Pyro beans are cool!\n\nExecute the command for clickable link to https://pyro.host/ :3");
		this.setPermissionGroups("Default", GameMode.Adventure.toString());
	}

	@Override
	protected @Nullable CompletableFuture<Void> execute(@NonNull CommandContext commandContext) {
		if (commandContext.isPlayer()) {
			commandContext.sendMessage(PYRO_MESSAGE);
		}
		return null;
	}

	@Override
	public boolean hasPermission(@NonNull CommandSender sender) {
		return true;
	}
}
