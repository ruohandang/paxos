package paxos;

import java.io.Serializable;

/**
 * Represents a unique identifier for a proposal in the Paxos protocol, which may include a sequence number and the identifier of the Proposer node.
 */
public class ProposalID implements Serializable, Comparable<ProposalID> {
  private static final long serialVersionUID = 1L;
  private final long number;
  private int serverId;

  public ProposalID(int serverId) {
    this.serverId = serverId;
    this.number = generateUniqueNumber(serverId);
}

private long generateUniqueNumber(int serverId) {
    // Assuming this generates a unique number based on server ID and timestamp
    long timestamp = System.currentTimeMillis();
    return timestamp * 10 + serverId; // Example implementation
}

  public long getNumber() {
    return this.number;
  } 

  public int getServerId() {
    return this.serverId;
  } 
  
  @Override
  public int compareTo(ProposalID other) {
    return Long.compare(this.number, other.number);
  }

  @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      ProposalID that = (ProposalID) obj;
      return number == that.number;
    }

  @Override
  public int hashCode() {
    return Long.hashCode(number);
  }

  @Override
    public String toString() {
      return "ProposalID{" +
              "number=" + number +
              ", serverId='" + serverId + '\'' +
              '}';
    }
}
