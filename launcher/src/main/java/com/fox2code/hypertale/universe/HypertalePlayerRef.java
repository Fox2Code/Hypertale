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

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class HypertalePlayerRef {
	private final PlayerRef playerRef;
	private final Object playerRefLock;
	private volatile WeakReference<Player> player;

	public HypertalePlayerRef(PlayerRef playerRef) {
		this.playerRef = playerRef;
		this.playerRefLock = new Object();
	}

	public @Nonnull PlayerRef getPlayerRef() {
		return this.playerRef;
	}

	public void invalidateEntity() {
		synchronized (this.playerRefLock) {
			this.player = null;
		}
	}

	public @Nullable World getWorld() {
		WeakReference<Player> playerWeakReference = this.player;
		Player player;
		Ref<EntityStore> entityStoreRef;
		if (playerWeakReference != null &&
				(player = playerWeakReference.get()) != null &&
				(entityStoreRef = player.getReference()) != null &&
				entityStoreRef.isValid()) {
			return player.getWorld();
		}
		UUID worldUUID = this.playerRef.getWorldUuid();
		return worldUUID == null ? null :
				Universe.get().getWorld(worldUUID);
	}

	public @Nullable Player getPlayer() {
		WeakReference<Player> playerWeakReference = this.player;
		Player player = null;
		Ref<EntityStore> entityStoreRef;
		if (playerWeakReference != null &&
				(player = playerWeakReference.get()) != null &&
				(entityStoreRef = player.getReference()) != null &&
				entityStoreRef.isValid()) {
			return player;
		} else if (this.playerRef.getReference() == null) {
			return null;
		}
		synchronized (this.playerRefLock) {
			entityStoreRef = this.playerRef.getReference();
			UUID worldUUID = this.playerRef.getWorldUuid();
			World world;
			if (entityStoreRef != null && worldUUID != null &&
					(world = Universe.get().getWorld(worldUUID)) != null) {
				if (world.isInThread()) {
					player = entityStoreRef.getStore()
							.getComponent(entityStoreRef,
									Player.getComponentType());
					this.player = new WeakReference<>(player);
				} else {
					player = CompletableFuture.supplyAsync(this::getPlayer, world).join();
				}
			}
		}
		return player;
	}
}
