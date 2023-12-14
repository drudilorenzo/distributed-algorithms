package cs451.lattice.shot;

import cs451.lattice.messageTypes.LatticeMessage;

import java.util.Set;

/**
 * Interface for multi-shot lattice agreement messages.
 */
public interface LatticeShot {

    /**
     * Receive a {@link LatticeMessage}.
     *
     * @param message: the {@link LatticeMessage} to receive.
     */
    void receive(LatticeMessage message);

    /**
     * Propose a new set.
     */
    void propose(Set<Integer> proposal);

    /**
     * True if the instance can die (removed from the current shot sets).
     *
     * @return true if the instance can die.
     */
    boolean canDie();

}
