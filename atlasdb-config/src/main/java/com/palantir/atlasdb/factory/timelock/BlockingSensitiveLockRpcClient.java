/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.atlasdb.factory.timelock;

import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;

import com.palantir.lock.HeldLocksGrant;
import com.palantir.lock.HeldLocksToken;
import com.palantir.lock.LockClient;
import com.palantir.lock.LockRefreshToken;
import com.palantir.lock.LockRequest;
import com.palantir.lock.LockResponse;
import com.palantir.lock.LockRpcClient;
import com.palantir.lock.LockServerOptions;
import com.palantir.lock.SimpleHeldLocksToken;

/**
 * Given two proxies to the same set of underlying remote lock servers, one configured to expect longer-running
 * operations on the server and one not to, routes calls appropriately.
 */
public class BlockingSensitiveLockRpcClient implements LockRpcClient {
    private final LockRpcClient blocking;
    private final LockRpcClient nonBlocking;

    public BlockingSensitiveLockRpcClient(BlockingAndNonBlockingServices<LockRpcClient> services) {
        this.blocking = services.blocking();
        this.nonBlocking = services.nonBlocking();
    }

    @Override
    public Optional<LockResponse> lockWithFullLockResponse(String namespace, LockClient client, LockRequest request)
            throws InterruptedException {
        return blocking.lockWithFullLockResponse(namespace, client, request);
    }

    @Override
    public boolean unlock(String namespace, HeldLocksToken token) {
        return nonBlocking.unlock(namespace, token);
    }

    @Override
    public boolean unlock(String namespace, LockRefreshToken token) {
        return nonBlocking.unlock(namespace, token);
    }

    @Override
    public boolean unlockSimple(String namespace, SimpleHeldLocksToken token) {
        return nonBlocking.unlockSimple(namespace, token);
    }

    @Override
    public boolean unlockAndFreeze(String namespace, HeldLocksToken token) {
        // TODO (jkong): It feels like this could be non-blocking but not 100% sure so going for the safe option.
        return blocking.unlockAndFreeze(namespace, token);
    }

    @Override
    public Set<HeldLocksToken> getTokens(String namespace, LockClient client) {
        return nonBlocking.getTokens(namespace, client);
    }

    @Override
    public Set<HeldLocksToken> refreshTokens(String namespace, Iterable<HeldLocksToken> tokens) {
        return nonBlocking.refreshTokens(namespace, tokens);
    }

    @Override
    public Optional<HeldLocksGrant> refreshGrant(String namespace, HeldLocksGrant grant) {
        return nonBlocking.refreshGrant(namespace, grant);
    }

    @Override
    public Optional<HeldLocksGrant> refreshGrant(String namespace, BigInteger grantId) {
        return nonBlocking.refreshGrant(namespace, grantId);
    }

    @Override
    public HeldLocksGrant convertToGrant(String namespace, HeldLocksToken token) {
        // TODO (jkong): It feels like this could be non-blocking but not 100% sure so going for the safe option.
        return blocking.convertToGrant(namespace, token);
    }

    @Override
    public HeldLocksToken useGrant(String namespace, LockClient client, HeldLocksGrant grant) {
        // TODO (jkong): It feels like this could be non-blocking but not 100% sure so going for the safe option.
        return blocking.useGrant(namespace, client, grant);
    }

    @Override
    public HeldLocksToken useGrant(String namespace, LockClient client, BigInteger grantId) {
        // TODO (jkong): It feels like this could be non-blocking but not 100% sure so going for the safe option.
        return blocking.useGrant(namespace, client, grantId);
    }

    @Override
    public Optional<Long> getMinLockedInVersionId(String namespace) {
        return nonBlocking.getMinLockedInVersionId(namespace);
    }

    @Override
    public Optional<Long> getMinLockedInVersionId(String namespace, LockClient client) {
        return nonBlocking.getMinLockedInVersionId(namespace, client);
    }

    @Override
    public Optional<Long> getMinLockedInVersionId(String namespace, String client) {
        return nonBlocking.getMinLockedInVersionId(namespace, client);
    }

    @Override
    public LockServerOptions getLockServerOptions(String namespace) {
        return nonBlocking.getLockServerOptions(namespace);
    }

    @Override
    public Optional<LockRefreshToken> lock(String namespace, String client, LockRequest request)
            throws InterruptedException {
        return blocking.lock(namespace, client, request);
    }

    @Override
    public Optional<HeldLocksToken> lockAndGetHeldLocks(String namespace, String client, LockRequest request)
            throws InterruptedException {
        return blocking.lockAndGetHeldLocks(namespace, client, request);
    }

    @Override
    public Set<LockRefreshToken> refreshLockRefreshTokens(String namespace, Iterable<LockRefreshToken> tokens) {
        return nonBlocking.refreshLockRefreshTokens(namespace, tokens);
    }

    @Override
    public long currentTimeMillis(String namespace) {
        return nonBlocking.currentTimeMillis(namespace);
    }

    @Override
    public void logCurrentState(String namespace) {
        // Even if this does take more than the non-blocking timeout, the request will fail while the server will
        // dump its logs out.
        nonBlocking.logCurrentState(namespace);
    }
}
