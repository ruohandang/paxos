package paxos;

import java.io.Serializable;

/**
 * Defines the structure of a message that indicates a proposal has been accepted by an Acceptor and should be learned by Learners.
 */
public class MessageAccepted implements PaxosMessage, Serializable {
  private int serverId;
  private String key;
  private ProposalID proposalId;
  private String acceptedValue;
  private String operation;

  public MessageAccepted(int serverId, ProposalID proposalID, String key, String value, String operation) {
    this.serverId = serverId;
    this.key = key;
    this.proposalId = proposalID;
    this.acceptedValue = value;
    this.operation = operation;
  }

  @Override
  public int getServerId() {
    return this.serverId;
  }

  @Override
  public String getKey() {
    return this.key;
  }

  public String getAcceptedValue() {
    return this.acceptedValue;
  }

  public String getOperation() {
    return this.operation;
  }

  @Override
  public ProposalID getProposalID() {
    return proposalId;
  }
}
