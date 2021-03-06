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

package io.smilo.commons.peer;

import io.smilo.commons.block.BlockStore;
import io.smilo.commons.ledger.AddressManager;
import io.smilo.commons.peer.network.Network;
import io.smilo.commons.peer.network.NetworkStatus;
import io.smilo.commons.peer.payloadhandler.PayloadType;
import io.smilo.commons.peer.sport.INetworkState;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Core implementation of networkState
 */
@Component
public class CoreNetworkState implements INetworkState {

    private boolean catchupMode = false;
    private long topBlock = 0;
    private Set<Network> networks = new HashSet<>();

    private final BlockStore blockStore;
    private final PeerSender peerSender;
    private final AddressManager addressManager;

    public CoreNetworkState(BlockStore blockStore, PeerSender peerSender, AddressManager addressManager) {
        this.blockStore = blockStore;
        this.peerSender = peerSender;
        this.addressManager = addressManager;
    }

    /**
     * Checks if the chain in the database has the same length as the top block. If the top block is higher than the database, set catchupMode to true
     */
    @Override
    public void updateCatchupMode() {
        /*
         * Current chain is shorter than peer chains.
         * Chain starts counting at 0, so a chain height of 100, for example, means there are 100 blocks, and the top block's index is 99.
         * So we need to catch up!
         *
         */
        if (topBlock > blockStore.getBlockchainLength()) {
            catchupMode = true;
        } else {
            catchupMode = false;
        }
    }

    /**
     * Checks if the current network block is higher than the local database block.
     *
     * @return true if the current network block is higher than the local database block
     */
    @Override
    public boolean getCatchupMode() {
        return catchupMode;
    }

    /**
     * Finds the highest blocknum from the network
     *
     * @return the highest blocknum from the network
     */
    @Override
    public long getTopBlock() {
        return topBlock;
    }

    @Override
    public void setTopBlock(long topBlock) {
        this.topBlock = topBlock;
        updateCatchupMode();
    }

    @Override
    public Set<Network> getNetworks() {
        return networks;
    }

    @Override
    public Optional<Network> getNetworkByIdentifier(String networkIdentifier) {
        return this.getNetworks().stream()
                .filter(network -> network.getIdentifier().equals(networkIdentifier))
                .findFirst();
    }

    @Override
    public void addNetwork(Network network) {
        networks.add(network);
        peerSender.broadcastToNetwork(network, PayloadType.LINK_NETWORK, network.getIdentifier());

        // if it's linked, it means we created the network ourselves. In that case, we can assume that we're confirmed.
        if (network.getNetworkStatus() == NetworkStatus.LINKED) {
            network.getPeerIdentifiers().add(addressManager.getDefaultAddress());
        } else {
            network.getUnconfirmedPeerIdentifiers().add(addressManager.getDefaultAddress());
        }

    }

    @Override
    public void removeNetwork(Network network) {
        // Todo: disconnect
        networks.remove(network);
    }
}
