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
package com.fox2code.hypertale.universe;

import com.fox2code.hypertale.annotations.AsyncSafe;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class HypertalePlayer {
	private final Player player;
	private final Object playerRefLock;
	private WeakReference<PlayerRef> playerRefWeakReference;

	public HypertalePlayer(Player player) {
		Objects.requireNonNull(player);
		this.player = player;
		this.playerRefLock = new Object();
	}

	@Nonnull
	public Player getPlayer() {
		return this.player;
	}

	@AsyncSafe
	@Nullable public PlayerRef getPlayerRef() {
		WeakReference<PlayerRef> playerRefWeakReference = this.playerRefWeakReference;
		PlayerRef playerRef;
		if (playerRefWeakReference != null && (playerRef = playerRefWeakReference.get()) != null &&
				playerRef.isValid()) {
			return playerRef;
		}
		synchronized (this.playerRefLock) {
			Ref<EntityStore> ref = this.player.getReference();
			World playerWorld = player.getWorld();
			if (playerWorld == null || ref == null || !ref.isValid()) {
				return null;
			}
			if (ref.getStore().isInThread()) {
				playerRef = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
				this.playerRefWeakReference = new WeakReference<>(playerRef);
			} else {
				playerRef = CompletableFuture.supplyAsync(this::getPlayerRef).join();
			}
		}
		return playerRef;
	}

	public boolean isRealPlayer() {
		return this.getPlayerRef() != null;
	}
}
