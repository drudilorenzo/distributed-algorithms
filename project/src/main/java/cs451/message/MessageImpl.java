package cs451.message;

public class MessageImpl implements Message {

    private final int messageId;
    private final int originId;
    private final int lastHop;

    public MessageImpl(final int messageId, final int originId, final int lastHop) {
        this.messageId = messageId;
        this.originId = originId;
        this.lastHop = lastHop;
    }

    @Override
    public int getMessageId() {
        return this.messageId;
    }

    @Override
    public int getOriginId() {
        return this.originId;
    }

    @Override
    public int getlastHopId() {
        return this.lastHop;
    }
}
