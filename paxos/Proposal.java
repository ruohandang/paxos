package paxos;

/**
 * Encapsulates a proposal in the Paxos protocol, which includes a unique identifier and the proposed value.
 */
public class Proposal {
  private final ProposalID proposalID;
  private final String key;
  private final String value;
  private final String operation;

  public Proposal(ProposalID proposalID, String key, String value, String operation) {
    this.proposalID = proposalID;
    this.key = key;
    this.value = value;
    this.operation = operation;
  }

  public ProposalID getProposalID() {
    return proposalID;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  public String getOperation() {
    return operation;
  }

  @Override
  public String toString() {
    return "Proposal{" +
            "proposalID=" + proposalID +
            ", key='" + key + '\'' +
            ", value='" + value + '\'' +
            ", operation='" + operation + '\'' +
            '}';
  }
}

