package paxos;

/**
 * An interface that serves as a base for all message types exchanged in the Paxos protocol.
 */
public interface PaxosMessage {
  ProposalID getProposalID();
  String getKey();
  int getServerId();
}
