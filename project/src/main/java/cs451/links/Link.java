package cs451.links;

import cs451.message.Message;

/**
 * Interface for the abstraction of a link (used to represent the network components of the
 * distributed system).
 */
public interface Link {

    /**
     * Send a message.
     *
     * @param message: the message to send.
     */
    void send(Message message);

    /**
     * Close the link.
     */
    void close();

}
