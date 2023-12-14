package cs451.lattice.manager;

import cs451.Host;
import cs451.broadcast.BebBroadcast;
import cs451.lattice.messageTypes.DecisionImpl;
import cs451.lattice.shot.LatticeShot;
import cs451.lattice.shot.LatticeShotImpl;
import cs451.lattice.messageTypes.LatticeMessage;
import cs451.utils.Utils;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Implementation of {@link LatticeManager}.
 */
public class LatticeManagerImpl implements LatticeManager {

    private static final int WINDOW_SIZE = 10; // shots handled at once

    private final int myId;
    private int shotsCounter;
    private final int numHosts;
    private int lastDecidedShot;
    private final BebBroadcast bebBroadcast;
    private final PriorityQueue<DecisionImpl> decisionQueue;
    private final Consumer<Set<Integer>> deliverCallback;
    private final Map<Integer, LatticeShot> currentShots;

    // need to use a lock to protect access to shared resources (decisionQueue, currentShots)
    private final ReentrantLock lock;
    private final Condition handleMore;

    /**
     * Constructor of {@link LatticeManagerImpl}.
     *
     * @param hosts: the list of hosts in the system.
     * @param myId:  the id of the current host.
     * @param port:  the port to use for the perfect link.
     */
    public LatticeManagerImpl(final List<Host> hosts, final int myId, final int port, final Consumer<Set<Integer>> deliverCallback) {
        this.myId = myId;
        this.shotsCounter = 0;
        this.lastDecidedShot = -1;
        this.numHosts = hosts.size();
        this.lock = new ReentrantLock();
        this.currentShots = new HashMap<>();
        this.deliverCallback = deliverCallback;
        this.handleMore = this.lock.newCondition();
        this.bebBroadcast = new BebBroadcast(hosts, myId, port, this::deliver);
        this.decisionQueue = new PriorityQueue<>(Comparator.comparingInt(DecisionImpl::getShotNumber));
    }

    @Override
    public void propose(final Set<Integer> proposal) {
        this.lock.lock();
        LatticeShot shot;
        try {
            while (this.shotsCounter - this.lastDecidedShot > WINDOW_SIZE) {
                // if the window is full, wait until a shot is decided (decision signal)
                try {
                    this.handleMore.await();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            shot = this.currentShots.get(this.shotsCounter);
            if (shot == null)  {
                shot = new LatticeShotImpl(this.myId, this.shotsCounter, this.numHosts, this);
                this.currentShots.put(this.shotsCounter, shot);
            }
            this.shotsCounter++;
        } finally {
            this.lock.unlock();
        }
        shot.propose(proposal);
    }

    @Override
    public void broadcast(final byte[] payload, final int messageId) {
        this.bebBroadcast.broadcast(payload, messageId);
    }

    @Override
    public void singleSend(final byte[] payload, final int messageId, final int destination) {
        this.bebBroadcast.singleSend(payload, messageId, destination);
    }

    @Override
    public void decide(final int shotNumber, final Set<Integer> decision) {
        this.lock.lock();
        try {
            // Add the decision to the queue and deliver all the decisions that can be delivered.
            this.decisionQueue.add(new DecisionImpl(shotNumber, decision));
            boolean doSignal = false;
            while (!this.decisionQueue.isEmpty() && this.decisionQueue.peek().getShotNumber() == this.lastDecidedShot + 1) {
                final DecisionImpl decisionToDeliver = this.decisionQueue.poll();
                this.deliverCallback.accept(decisionToDeliver.getDecision());
                this.lastDecidedShot++;
                doSignal = true;
            }
            if (doSignal) {
                this.handleMore.signal();
            }
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void close() {
        this.bebBroadcast.close();
    }

    private void deliver(final LatticeMessage message) {
        this.lock.lock();
        LatticeShot shot;
        try {
            var shotNumber = message.getShotNumber();
            shot = this.currentShots.get(shotNumber);
            if (shot == null) {
                shot = new LatticeShotImpl(this.myId, shotNumber, this.numHosts, this);
                this.currentShots.put(shotNumber, shot);
            }
        } finally {
            this.lock.unlock();
        }

        shot.receive(message);

        if (shot.canDie()) {
            this.lock.lock();
            try {
                this.currentShots.remove(message.getShotNumber());
            } finally {
                this.lock.unlock();
            }

            Utils.checkGc();

        }

    }

}
