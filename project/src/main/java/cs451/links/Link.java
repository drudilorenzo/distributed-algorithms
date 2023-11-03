package cs451.links;

import cs451.message.Message;

/**
 * Interface for the abstraction of a link (used to represent the network components of the
 * distributed system).
 */
interface Link {

    /**
     * Send a packet.
     *
     * @param message: the packet to send.
     */
    void send(Message message);

    /**
     * Close the link.
     */
    void close();

}
