package cs451.lattice.shot;

import cs451.lattice.messageTypes.LatticeMessage;

import java.util.Set;

/**
 * Interface for multi-shot lattice agreement messages.
 *
 *  It is characterized by the following properties:
 *  Let Ii be the proposal of process Pi.
 *  - Validity: Let a process Pi decide a set Oi. Then Ii ⊆ Oi and Oi ⊆ U Ij (for all j).
 *  - Consistency: Let a process Pi decide a set Oi, and let a process Pj decide a set Oj.
 *                 Then Oi and Oj are comparable.
 *  - Termination: Every correct process eventually decides.
 *
 *  Implementation based on the Pseudo-code from the slides of the course Concurrent Computing at EPFL.
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
