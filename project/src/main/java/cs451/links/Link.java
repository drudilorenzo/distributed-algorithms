package cs451.links;

import cs451.packet.Packet;

/**
 * Interface for the abstraction of a link (used to represent the network components of the
 * distributed system).
 *
 * Every pair of processes is connected by a bidirectional link.
 */
interface Link {

    /**
     * Send a packet.
     *
     * @param packet: the packet to send.
     */
    void send(Packet packet);

    /**
     * Close the link.
     */
    void close();

}
