package cs451.lattice.shot;

import cs451.lattice.manager.LatticeManager;
import cs451.lattice.messageTypes.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@link LatticeShot}.
 */
public class LatticeShotImpl implements LatticeShot {

    private int ackCount;
    private int nackCount;
    private final int myId;
    private boolean active;
    private int decisionCount;
    private final int numHosts;
    private int proposalNumber;
    private final int shotNumber;
    private final LatticeManager manager;
    private Set<Integer> currentProposal;
    private Set<Integer> acceptedValues;
    private final int[] lastProposal;

    /**
     * Constructor of {@link LatticeShotImpl}.
     *
     * @param myId:        the id of the current host.
     * @param shotNumber: the shot number of the lattice instance.
     * @param numHosts:   the number of hosts in the system.
     * @param manager:    the {@link LatticeManager} to use.
     */
    public LatticeShotImpl(final int myId, final int shotNumber,
                           final int numHosts, final LatticeManager manager) {
        this.myId = myId;
        this.ackCount = 0;
        this.nackCount = 0;
        this.active = false;
        this.decisionCount = 0;
        this.shotNumber = shotNumber;
        this.proposalNumber = 0;
        this.numHosts = numHosts;
        this.manager = manager;
        this.lastProposal = new int[numHosts];
        this.acceptedValues = new HashSet<>();
        this.currentProposal = new HashSet<>();
    }

    @Override
    public void receive(final LatticeMessage message) {
        synchronized (this) {
            if (message instanceof AckMessageImpl) {
                this.handleLatticeAck((AckMessageImpl) message);
            } else if (message instanceof NackMessageImpl) {
                this.handleLatticeNack((NackMessageImpl) message);
            } else if (message instanceof ProposalImpl) {
                this.handleProposal((ProposalImpl) message);
            } else if (message instanceof DecisionImpl) {
                this.decisionCount++;
            } else {
                throw new IllegalArgumentException("Unknown message type: " + message.getClass().getName());
            }
        }
    }

    @Override
    public void propose(final Set<Integer> proposal) {
        synchronized (this) {
            this.active = true;
            this.proposalNumber++;
            this.currentProposal.addAll(proposal);
            this.currentProposal.addAll(this.acceptedValues);
            this.sendProposal(proposal);
        }
    }

    @Override
    public boolean canDie() {
        synchronized (this) {
            return !this.active && this.decisionCount >= this.numHosts;
        }
    }

    private void handleLatticeAck(final AckMessageImpl message) {
        if (this.active && message.getProposalNumber() == this.proposalNumber) {
            this.ackCount++;
            this.DecideOrPropose();
        }
    }

    private void handleLatticeNack(final NackMessageImpl message) {
        if (this.active && message.getProposalNumber() == this.proposalNumber) {
            this.nackCount++;
            this.currentProposal.addAll(message.getProposalNumbers());
            this.DecideOrPropose();
        }
    }

    private void DecideOrPropose() {
        if (this.nackCount > 0 && (this.ackCount + this.nackCount) > this.numHosts / 2) {
            this.proposalNumber++;
            this.ackCount = 0;
            this.nackCount = 0;

            this.sendProposal(this.currentProposal);
        } else if (this.ackCount > this.numHosts / 2) {
            this.manager.decide(this.shotNumber, this.currentProposal);
            this.active = false;

            // decision time
            var message = new DecisionImpl(this.shotNumber, this.currentProposal);
            var payload = LatticeMessageSerializationUtils.serializeLatticeMessage(message);
            this.manager.broadcast(payload, message.getShotNumber());
            this.decisionCount++;
        }
    }

    private void handleProposal(final ProposalImpl message) {
        if (message.getProposalNumber() >= this.lastProposal[message.getSenderId()-1]) {
            this.lastProposal[message.getSenderId()-1] = message.getProposalNumber();
            var proposal = message.getProposalNumbers();
            if (proposal.containsAll(this.acceptedValues)) {
                this.acceptedValues = proposal;
                var ackMessage = new AckMessageImpl(
                        this.shotNumber,
                        message.getProposalNumber()
                );
                var payload = LatticeMessageSerializationUtils.serializeLatticeMessage(ackMessage);
                this.manager.singleSend(payload, ackMessage.getShotNumber(), message.getSenderId());
            } else {
                this.acceptedValues.addAll(proposal);
                var nackMessage = new NackMessageImpl(
                        this.shotNumber,
                        message.getProposalNumber(),
                        this.acceptedValues
                );
                var payload = LatticeMessageSerializationUtils.serializeLatticeMessage(nackMessage);
                this.manager.singleSend(payload, nackMessage.getShotNumber(), message.getSenderId());
            }
        }
    }

    private void sendProposal(final Set<Integer> proposal) {
        Set<Integer> toSend = Set.copyOf(proposal);
        var message = new ProposalImpl(
                this.shotNumber,
                this.proposalNumber,
                this.myId,
                toSend
        );
        var payload = LatticeMessageSerializationUtils.serializeLatticeMessage(message);

        this.acceptedValues.addAll(proposal);

        if (proposal.containsAll(this.acceptedValues)) {
            this.ackCount++;
        } else {
            this.nackCount++;
            this.currentProposal.addAll(this.acceptedValues);
        }

        this.manager.broadcast(payload, message.getShotNumber());
    }

}
