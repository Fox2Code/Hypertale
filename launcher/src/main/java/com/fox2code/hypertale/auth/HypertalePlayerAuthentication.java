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
package com.fox2code.hypertale.auth;

import com.fox2code.hypertale.utils.EmptyArrays;
import com.hypixel.hytale.server.core.auth.JWTValidator;
import com.hypixel.hytale.server.core.auth.PlayerAuthentication;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.UUID;

/**
 * HypertalePlayerAuthentication is a class that extends PlayerAuthentication and provides additional
 * identifying elements for Hypertale players to be used by Hypertale plugins.
 */
public final class HypertalePlayerAuthentication extends PlayerAuthentication {
	private final JWTValidator.IdentityTokenClaims identityTokenClaims;

	public HypertalePlayerAuthentication(@Nonnull UUID uuid, @Nonnull String username,
										 @Nonnull JWTValidator.IdentityTokenClaims identityTokenClaims) {
		super(uuid, username);
		this.identityTokenClaims = identityTokenClaims;
	}

	public @Nonnull String getIssuer() {
		return this.identityTokenClaims.issuer;
	}

	public @Nonnull String getScope() {
		return this.identityTokenClaims.scope;
	}

	public @Nonnull String[] getEntitlements() {
		String[] entitlements = this.identityTokenClaims.entitlements;
		return entitlements == null || entitlements.length == 0 ?
				EmptyArrays.EMPTY_STRING_ARRAY :
				Arrays.copyOf(entitlements, entitlements.length);
	}

	public boolean hasEntitlement(String entitlement) {
		String[] entitlements = this.identityTokenClaims.entitlements;
		if (entitlements == null) {
			return false;
		}
		String[] strings = this.identityTokenClaims.entitlements;
		for (String string : strings) {
			if (entitlement.equals(string)) {
				return true;
			}
		}
		return false;
	}

	public @Nonnull String getSkin() {
		String skin = this.identityTokenClaims.skin;
		return skin == null || skin.isEmpty() ? "{}" : skin;
	}
}
