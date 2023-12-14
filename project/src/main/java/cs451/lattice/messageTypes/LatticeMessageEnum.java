package cs451.lattice.messageTypes;

/**
 * Enum for multi-shot lattice agreement messages.
 */
public enum LatticeMessageEnum {

    /**
     * Ack message.
     */
    ACK(0),

    /**
     * Nack message.
     */
    NACK(1),

    /**
     * Proposal message.
     */
    PROPOSAL(2),

    /**
     * Decision message.
     */
    DECISION(3);

    private final int value;

    /**
     * Constructor of {@link LatticeMessageEnum}.
     *
     * @param value: the value of the enum entry.
     */
    LatticeMessageEnum(int value) {
        this.value = value;
    }

    /**
     * Get the value of the enum entry.
     *
     * @return the value of the enum entry.
     */
    public int getValue() {
        return this.value;
    }
}
