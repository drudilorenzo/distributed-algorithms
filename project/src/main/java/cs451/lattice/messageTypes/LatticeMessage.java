package cs451.lattice.messageTypes;

/**
 * Interface for multi-shot lattice agreement messages.
 *
 * You can see the list of all the possible types in {@link LatticeMessageEnum}.
 */
public interface LatticeMessage {

    /**
     * Get the shot number of the message (lattice instance).
     *
     * @return the shot number.
     */
    int getShotNumber();

}
