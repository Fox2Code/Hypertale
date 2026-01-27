package com.fox2code.hypertale.commands;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

final class HypertaleStatusCommand extends AbstractCommand {
	HypertaleStatusCommand() {
		super("status", "Show hypertale status information!");
	}

	@Override
	protected @NonNull CompletableFuture<Void> execute(@NonNull CommandContext commandContext) {
		commandContext.sendMessage(HypertaleVersionCommand.VERSION_MESSAGE);
		commandContext.sendMessage(HypertaleSystemCommand.SYSTEM_MESSAGE);
		return CompletableFuture.completedFuture(null);
	}
}
