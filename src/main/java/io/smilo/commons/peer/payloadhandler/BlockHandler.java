/*
 * Copyright (c) 2018 Smilo Platform B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smilo.commons.peer.payloadhandler;

import io.smilo.commons.HashUtility;
import io.smilo.commons.block.Block;
import io.smilo.commons.block.BlockParser;
import io.smilo.commons.block.SmiloChainService;
import io.smilo.commons.peer.IPeer;
import io.smilo.commons.peer.sport.INetworkState;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BlockHandler implements PayloadHandler {

    private SmiloChainService smiloChainService;
    private BlockParser blockParser;
    private INetworkState networkState;

    public BlockHandler(SmiloChainService smiloChainService, BlockParser blockParser, INetworkState networkState) {
        this.smiloChainService = smiloChainService;
        this.blockParser = blockParser;
        this.networkState = networkState;
    }

    @Override
    public void handlePeerPayload(List<String> parts, IPeer peer) {
        if(networkState.getCatchupMode()){ // Prevents blocks to get injected by malicious nodes (instead of COMMIT message)
            byte[] byteArray = HashUtility.decodeFromBase64(parts.get(1));
            Block block = blockParser.deserialize(byteArray);
            smiloChainService.addBlockToSmiloChain(block);
        }
    }

    @Override
    public PayloadType supports() {
        return PayloadType.BLOCK;
    }
}
