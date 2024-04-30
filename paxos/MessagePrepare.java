package paxos;

import java.io.Serializable;

/**
 * Represents a message sent by a Proposer to Acceptors to prepare for a new proposal, carrying information such as the proposal number.
 */
public class MessagePrepare implements PaxosMessage, Serializable {
    private String key;
    private ProposalID proposalID;

    public MessagePrepare(ProposalID proposalID, String key) {
        this.key = key;
        this.proposalID = proposalID;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public ProposalID getProposalID() {
        return this.proposalID;    
    }

    @Override
    public int getServerId() {
        return this.proposalID.getServerId();
    }
}
