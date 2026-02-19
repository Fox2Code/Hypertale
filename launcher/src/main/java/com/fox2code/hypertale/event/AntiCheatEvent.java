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
package com.fox2code.hypertale.event;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Event triggered by anti-cheat plugins to communicate with other plugins anti-cheat events!
 * <p>
 * Note: Can also include events from item duplication checkers and anti-exploit plugins!
 */
public class AntiCheatEvent implements IEvent<Void> {
	@Nonnull
	private final PlayerRef playerRef;
	@Nonnull
	private final AntiCheatCertainty certainty;
	@Nullable private final IEvent<?> cause;
	@Nonnull
	private final AntiCheatAction action;
	@Nonnull
	private final Message message;
	@Nonnull
	private final String id;
	private final long tempBanLengthTime;
	private final boolean automation;

	public AntiCheatEvent(@Nonnull PlayerRef playerRef, @Nullable AntiCheatCertainty certainty,
						  @Nullable IEvent<?> cause, @Nullable AntiCheatAction action, @Nonnull Message message,
						  @Nonnull String id) {
		this(playerRef, certainty, cause, action, message, id, -1, false);
	}

	public AntiCheatEvent(@Nonnull PlayerRef playerRef, @Nullable AntiCheatCertainty certainty,
						  @Nullable IEvent<?> cause, @Nullable AntiCheatAction action, @Nonnull Message message,
						  @Nonnull String id, long tempBanLengthTime) {
		this(playerRef, certainty, cause, action, message, id, tempBanLengthTime, false);
	}

	public AntiCheatEvent(@Nonnull PlayerRef playerRef, @Nullable AntiCheatCertainty certainty,
						  @Nullable IEvent<?> cause, @Nullable AntiCheatAction action, @Nonnull Message message,
						  @Nonnull String id, long tempBanLengthTime, boolean automation) {
		if ((certainty == null || certainty == AntiCheatCertainty.INFORMATIVE) &&
				(action != null && action.willDisconnect)) {
			throw new IllegalArgumentException("Informative certainty should not disconnect the player!");
		}
		if (certainty == AntiCheatCertainty.EXPLOIT && (action != null && !action.willDisconnect)) {
			throw new IllegalArgumentException("Exploit certainty should always disconnect the player!");
		}
		this.playerRef = Objects.requireNonNull(playerRef);
		this.certainty = certainty == null ? AntiCheatCertainty.INFORMATIVE : certainty;
		this.cause = cause;
		this.action = action == null ? this.certainty.defaultResponse : action;
		this.message = Objects.requireNonNull(message);
		this.id = Objects.requireNonNull(id);
		this.tempBanLengthTime = tempBanLengthTime <= 0 ? -1 : tempBanLengthTime;
		this.automation = automation;
	}

	@Nonnull
	public PlayerRef getPlayerRef() {
		return this.playerRef;
	}

	@Nonnull
	public AntiCheatCertainty getCertainty() {
		return this.certainty;
	}

	@Nullable public IEvent<?> getCause() {
		return this.cause;
	}

	@Nonnull
	public AntiCheatAction getAction() {
		return this.action;
	}

	/**
	 * User-readable message provided by the anti-cheat.
	 */
	@Nonnull
	public Message getMessage() {
		return this.message;
	}

	/**
	 * Identifier of the check that the anti-cheat performed.
	 */
	@Nonnull
	public String getId() {
		return this.id;
	}

	/**
	 * Temp ban length time in milliseconds, recommended ban length, if -1 it means a permanent ban length is undefined.
	 */
	public long getTempBanLengthTime() {
		return this.tempBanLengthTime;
	}

	/**
	 * whenever the anticheat think the action was caused by macro or other from of automations.
	 */
	public boolean isAutomation() {
		return this.automation;
	}

	public final boolean willDisconnect() {
		return this.action.willDisconnect;
	}

	public static Builder builder(AntiCheatCertainty certainty, AntiCheatAction action) {
		return new Builder(certainty, action);
	}

	/**
	 * Type of the action that was performed by the anti-cheat or that the anti-cheat wants performed.
	 * <p>
	 * Note: Receiving plugins are allowed to not follow the wishes of the anti-cheat, but in some cases Hypertale
	 * will still enforce the anticheat action if deemed appropriate.
	 */
	public enum AntiCheatAction {
		/**
		 * Indicate the anticheat has not performed any action and do not expect any!
		 */
		NONE(false),
		/**
		 * Indicate the anticheat has canceled the player action server side and that the action
		 * will have no impact on the client behavior due to the cancellation.
		 */
		NULLIFIED(false),
		/**
		 * Indicate the anticheat has rolled back the player action server side and that the action
		 * may have an impact on the client behavior despite being canceled.
		 */
		ROLLBACK(false),
		/**
		 * Indicate the anticheat has quarantined at some of the player items or data, might also imply that
		 * manual intervention is required to restore the player inventory or data.
		 */
		QUARANTINED(false),
		/**
		 * Indicate the anticheat has kicked the player server side.
		 */
		KICK(true),
		/**
		 * Indicate the anticheat expect a receiving plugin to ban the player.
		 * <p>
		 * Note: Hypertale will autoban the player if the player is still connected to the server after this event
		 * has been processed, so sending this event will always result in a player disconnection from the server.
		 */
		AWAITING_BAN(true);

		private final boolean willDisconnect;

		AntiCheatAction(boolean willDisconnect) {
			this.willDisconnect = willDisconnect;
		}
	}

	public enum AntiCheatCertainty {
		/**
		 * Indicate the anticheat has no suspicion on the player that caused by the event!
		 */
		INFORMATIVE(AntiCheatAction.NONE),
		/**
		 * Indicate that the anti-dupe thinks the player duplicated items!
		 */
		DUPLICATING(AntiCheatAction.NONE),
		/**
		 * Indicate the anticheat has suspicion on the player that caused by the event, but no certainty if
		 * the player is cheating or not! Indicate human review might be required!
		 */
		SUSPICIOUS(AntiCheatAction.NONE),
		/**
		 * Indicate the anticheat has suspicion on the player that caused by the event, and the player is
		 * very likely cheating! Excluding potential unforeseen false positives.
		 */
		VERY_LIKELY(AntiCheatAction.KICK),
		/**
		 * Indicate the anticheat knows the player is cheating, and that appropriate punishment should be applied.
		 * The anticheat if furthermore confident of its certainty.
		 */
		CERTAIN(AntiCheatAction.KICK),
		/**
		 * Indicate the anticheat detected a server exploit that put the server at immediate risk.
		 * <p>
		 * Note: For anticheat bypass exploit, please use another certainty level!
		 */
		EXPLOIT(AntiCheatAction.AWAITING_BAN);

		private final AntiCheatAction defaultResponse;

		AntiCheatCertainty(AntiCheatAction defaultResponse) {
			this.defaultResponse = defaultResponse;
		}
	}

	public static final class Builder {
		private final AntiCheatCertainty certainty;
		private IEvent<?> cause;
		private final AntiCheatAction action;
		private Message message;
		private String id;
		private long tempBanLengthTime;
		private boolean automation;

		public Builder(Builder builder) {
			this.certainty = builder.certainty;
			this.cause = builder.cause;
			this.action = builder.action;
			this.message = builder.message;
			this.id = builder.id;
			this.tempBanLengthTime = builder.tempBanLengthTime;
			this.automation = builder.automation;
		}

		public Builder(AntiCheatCertainty certainty, AntiCheatAction action) {
			if ((certainty == null || certainty == AntiCheatCertainty.INFORMATIVE) &&
					(action != null && action.willDisconnect)) {
				throw new IllegalArgumentException("Informative certainty should not disconnect the player!");
			}
			if (certainty == AntiCheatCertainty.EXPLOIT && (action != null && !action.willDisconnect)) {
				throw new IllegalArgumentException("Exploit certainty should always disconnect the player!");
			}
			this.certainty = certainty;
			this.action = action;
		}

		public AntiCheatEvent build(PlayerRef playerRef) {
			Objects.requireNonNull(playerRef, "playerRef cannot be null!");
			return new AntiCheatEvent(playerRef, this.certainty, this.cause, this.action,
					this.message, this.id, this.tempBanLengthTime, this.automation);
		}

		public void setCause(IEvent<?> cause) {
			this.cause = cause;
		}

		public void setMessage(Message message) {
			this.message = message;
		}

		public void setId(String id) {
			this.id = id;
		}

		public void setTempBanLengthTime(long tempBanLengthTime) {
			this.tempBanLengthTime = tempBanLengthTime;
		}

		public void setAutomation(boolean automation) {
			this.automation = automation;
		}
	}
}
