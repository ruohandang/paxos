package paxos;


import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import common.ServerConfig;
import server.KeyValueStore;
import server.ServerLogger;

/**
 * Represents the Learner role in Paxos, responsible for learning the value chosen 
 * by the Paxos protocol once consensus is reached and applying it to the key-value store.
 * Or ignore the message without consensus.
 */
public class Learner {
  private int serverId;
  private KeyValueStore store;
  private Map<ProposalID, Integer> acceptedCounts = new ConcurrentHashMap<>();
  private Set<ProposalID> finalizedProposals = Collections.newSetFromMap(new ConcurrentHashMap<ProposalID, Boolean>());
  private ConcurrentHashMap<ProposalID, CompletableFuture<Boolean>> completionFutures = new ConcurrentHashMap<>();
  private static final int QUORUM_SIZE = ServerConfig.ALL_SERVERS.length / 2 + 1;


  public Learner(int serverId, KeyValueStore store) {
    this.serverId = serverId;
    this.store = store;
  }

  /**
   * The method to handle accepted message from all acceptors and
   * once consensus is reached and applying it to the key-value store.
   * @param accepted
   */
  public void handleAccepted(MessageAccepted accepted) {
    ServerLogger.log(accepted.getProposalID() + ": Learner" + this.serverId + " received ACCEPT message from server: " + accepted.getServerId());
    if (finalizedProposals.contains(accepted.getProposalID())) {
      return;
    }
    // Atomic update and check within compute method to prevent concurrent issues
    acceptedCounts.compute(accepted.getProposalID(), (proposalID, count) -> {
        // Initialize the count if it is null
      if (count == null) {
        count = 0;
      }
      count++;

      // Check if the count reaches the quorum and the proposal has not been finalized yet
      if (count >= QUORUM_SIZE && !finalizedProposals.contains(proposalID)) {
        ServerLogger.log(proposalID + ": Learner" + this.serverId + " has reached the majority of accept messages");
        
        commit(proposalID, accepted.getOperation(), accepted.getKey(), accepted.getAcceptedValue());
        
        // Add to finalized proposals to prevent reprocessing
        finalizedProposals.add(proposalID);
        
        // Completing the future only if this server initiated the operation
        if (proposalID.getServerId() == this.serverId) {
          CompletableFuture<Boolean> future = completionFutures.get(proposalID);
          if (future != null) {
            future.complete(true); // Notify that the operation is successfully committed
          }
        }
      }
      return count; // Return the new count to update the map
    });
  }

  private void commit(ProposalID proposalID, String operation, String key, String value) {
    switch (operation) {
      case "PUT":
        ServerLogger.log(proposalID + ": Learner" + this.serverId + " is committing PUT to keyValueStore");
        store.put(key, value);
        break;
      case "DELETE":
        ServerLogger.log(proposalID + ": Learner" + this.serverId + " is committing DELETE to keyValueStore");
        store.delete(key);
        break;
    }
  }

  public void registerCompletionFuture(ProposalID proposalID, CompletableFuture<Boolean> future) {
    completionFutures.put(proposalID, future);
  }
}
