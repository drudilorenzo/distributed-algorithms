package cs451.packet;

import cs451.message.Message;
import cs451.message.MessageUtils;

import java.nio.ByteBuffer;

/**
 * Utility class for {@link Packet}.
 */
public class PacketUtils {

    /**
     * Deserialize a byte array into a {@link Packet}.
     *
     * @param data: the byte array to deserialize.
     * @return The deserialized packet.
     */
    public static Packet deserialize(final byte[] data) {
        final ByteBuffer bb = ByteBuffer.wrap(data);
        final Packet packet = new PacketImpl();
        final int numMessages = bb.getInt();
        Message message;
        int messageLength;
        byte[] messageByte;
        for (int i = 0; i < numMessages; i++) {
            messageLength = bb.getInt();
            messageByte = new byte[messageLength];
            bb.get(messageByte);
            message = MessageUtils.deserialize(messageByte);
            packet.addMessage(message);
        }
        return packet;
    }

}
