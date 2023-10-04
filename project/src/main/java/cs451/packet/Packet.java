package cs451.packet;

public interface Packet {

    public static final int MAX_PAYLOAD_SIZE = 65507;

    byte[] getData();

    int getPacketId();

    int getTimestamp();

    boolean isAck();

}
