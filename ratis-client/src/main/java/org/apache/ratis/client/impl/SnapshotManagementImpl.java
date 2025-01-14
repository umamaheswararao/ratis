/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ratis.client.impl;

import org.apache.ratis.client.api.SnapshotManagementApi;
import org.apache.ratis.protocol.RaftClientReply;
import org.apache.ratis.protocol.SnapshotManagementRequest;
import org.apache.ratis.rpc.CallId;

import java.io.IOException;
import java.util.Objects;

class SnapshotManagementImpl implements SnapshotManagementApi {
  private final RaftClientImpl client;

  SnapshotManagementImpl(RaftClientImpl client) {
    this.client = Objects.requireNonNull(client, "client == null");
  }

  @Override
  public RaftClientReply create(long timeoutMs) throws IOException {
    final long callId = CallId.getAndIncrement();
    return client.io().sendRequestWithRetry(() -> SnapshotManagementRequest.newCreate(
        client.getId(), client.getLeaderId(), client.getGroupId(), callId, timeoutMs));
  }
}
