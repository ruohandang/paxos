package paxos;

import java.io.Serializable;
/**
 * Defines the structure of a promise message that an Acceptor sends to a Proposer in response to a prepare request.
 */
public class MessagePromise implements PaxosMessage, Serializable {
  private int serverId;
  private String key;
  private ProposalID proposalId;
  private ProposalID previousAcceptedId;
  private String previousAcceptedValue;

  public MessagePromise(int serverId, String key, ProposalID proposalId) {
    this.serverId = serverId;
    this.key = key;
    this.proposalId = proposalId;
  }

  public MessagePromise(int serverId, String key, ProposalID proposalId, ProposalID previousAcceptedId, String previousAcceptedValue) {
    this.serverId = serverId;
    this.key = key;
    this.proposalId = proposalId;
    this.previousAcceptedId = previousAcceptedId;
    this.previousAcceptedValue = previousAcceptedValue;
  }

  public ProposalID getPreviousAcceptedId() {
    return previousAcceptedId;
  }

  public String getPreviousAcceptedValue() {
    return previousAcceptedValue;
  }
  
  @Override
  public int getServerId() {
    return this.serverId;
  }

  @Override
  public String getKey() {
    return this.key;
  }

  @Override
  public ProposalID getProposalID() {
    return proposalId;  
  }
}

