package paxos;

import java.io.Serializable;

/**
 * Represents a message containing an actual proposal sent by a Proposer to Acceptors after receiving promises.
 */
public class MessagePropose implements PaxosMessage, Serializable {
  private String key;
  private ProposalID proposalId;
  private String value;
  private String operation;

  public MessagePropose(ProposalID proposalID, String key, String value, String operation) {
    this.key = key;
    this.proposalId = proposalID;
    this.value = value;
    this.operation = operation;
}

  public String getValue() {
    return value;
  }
  @Override
  public String getKey() {
      return this.key;
  }

  public String getOperation() {
    return this.operation;
  }

  @Override
  public ProposalID getProposalID() {
    return proposalId;
  }

  @Override
  public int getServerId() {
    return this.getProposalID().getServerId();
  }
}
