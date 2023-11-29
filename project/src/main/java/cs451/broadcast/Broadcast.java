package cs451.broadcast;

/**
 * Interface for Broadcast.
 */
public interface Broadcast {

        /**
        * Broadcast a message.
        *
        * @param msgId:   the id of the message.
        * @param payload: the payload of the message.
        */
        void broadcast(int msgId, byte[] payload);

        /**
         * Close the broadcast.
         */
        void close();

}
