package cs451.lattice.messageTypes;

/**
 * Interface for multi-shot lattice agreement messages.
 */
public interface LatticeMessage {

    /**
     * Get the shot number of the message (lattice instance).
     *
     * @return the shot number.
     */
    int getShotNumber();

}
