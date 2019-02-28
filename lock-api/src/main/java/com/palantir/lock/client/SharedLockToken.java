/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.lock.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;
import com.palantir.lock.v2.LockToken;

final class SharedLockToken implements LockToken {
    private final UUID requestId;
    private final LockToken referencedToken;
    private final ReferenceCounter referenceCounter;

    private boolean unlocked;

    private SharedLockToken(ReferenceCounter referenceCounter, LockToken token) {
        this.referenceCounter = referenceCounter;
        this.requestId = UUID.randomUUID();
        this.referencedToken = token;
        this.unlocked = false;
    }

    public static List<LockToken> share(LockToken token, int referenceCount) {
        Preconditions.checkArgument(referenceCount > 0, "Reference count should be more than zero");
        Preconditions.checkArgument(!(token instanceof SharedLockToken), "Can not share a shared lock token");
        ReferenceCounter referenceCounter = new ReferenceCounter(referenceCount);
        return IntStream.range(0, referenceCount)
                .mapToObj(unused -> new SharedLockToken(referenceCounter, token))
                .collect(Collectors.toList());
    }

    /**
     * Unlocks shared token on client side - does not guarantee underlying token to be unlocked on server side.
     *
     * @return referenced lock token if all shared lock tokens are unlocked; that is lock token on server side is good
     * to be unlocked.
     */
    public synchronized Optional<LockToken> unlock() {
        if (!unlocked) {
            unlocked = true;
            referenceCounter.unmark();
        }

        return referenceCounter.dereferenced() ? Optional.of(referencedToken) : Optional.empty();
    }

    public LockToken referencedToken() {
        return referencedToken;
    }

    @Override
    public UUID getRequestId() {
        return requestId;
    }

    private static final class ReferenceCounter {
        private int referenceCount;

        private ReferenceCounter(int referenceCount) {
            this.referenceCount = referenceCount;
        }

        synchronized void unmark() {
            Preconditions.checkState(referenceCount >= 0, "Reference count can not go below zero!");
            referenceCount--;
        }

        synchronized boolean dereferenced() {
            return referenceCount == 0;
        }
    }
}
