package paxos;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import common.ServerConfig;
import server.ServerLogger;

/**
 * Implements the Proposer role in the Paxos protocol, responsible for initiating the proposal of values and driving the consensus process.
 */
public class Proposer{
  private Messenger messenger;
  private int serverId;
  private static final int QUORUM_SIZE = ServerConfig.ALL_SERVERS.length / 2 + 1;
  private final Map<ProposalID, Integer> promiseCounts = new ConcurrentHashMap<>();
  private Map<ProposalID, MessagePromise> receivedPromises = new ConcurrentHashMap<>();
  private Set<ProposalID> abandonedProposals = ConcurrentHashMap.newKeySet();  // Track abandoned proposals
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final Map<ProposalID, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();

  public Proposer(int serverId, Messenger messenger) {
    this.serverId = serverId;
    this.messenger = messenger;
  }

  /**
   * The method start a new propsal, paxos step1
   * @param proposal
   */
  public void prepare(Proposal proposal) {
    MessagePrepare prepare = new MessagePrepare(proposal.getProposalID(), proposal.getKey());
    ServerLogger.log(prepare.getProposalID() + "Proposer" + this.serverId + " is starting PAXOS: preparing");
    messenger.broadcastMessage(prepare);
  }

  /**
   * The method to handle the promises from acceptors, and once get consensus, then start propose
   * @param promise the promise acceptor sends back
   * @param proposal the original proposal
   */
  public void propose(MessagePromise promise, Proposal proposal) {
    // Early exit if the proposal ID has been abandoned
    if (abandonedProposals.contains(promise.getProposalID())) {
      ServerLogger.log("Ignoring promise for abandoned ProposalId: " + promise.getProposalID());
      return;
    }
    Integer count = promiseCounts.compute(promise.getProposalID(), (key, val) -> {
      if (val == null) {
        scheduleTimeout(key);
        val = 0;
      }
      return val + 1;
    });

    // Early exit if a quorum has already been reached and decision made
    if (count > QUORUM_SIZE) {
      ServerLogger.log("Promises quorum already reached for ProposalId: " + promise.getProposalID());
      return;
    }

    if (!receivedPromises.containsKey(promise.getProposalID())) {
      receivedPromises.put(promise.getProposalID(), promise);
      } else {
        ProposalID maxId = receivedPromises.get(promise.getProposalID()).getPreviousAcceptedId();
        if (promise.getPreviousAcceptedId() != null && promise.getPreviousAcceptedId().compareTo(maxId) > 0) {
          receivedPromises.put(promise.getProposalID(), promise);
      }
    }

    if (count == QUORUM_SIZE) {
      cancelTimeout(promise.getProposalID());
      ServerLogger.log(promise.getProposalID() + "Proposer" + this.serverId + " is starting PAXOS: proposing");
      if (receivedPromises.get(promise.getProposalID()) != null && receivedPromises.get(promise.getProposalID()).getPreviousAcceptedId() != null) {
        String acceptedValue = receivedPromises.get(promise.getProposalID()).getPreviousAcceptedValue();
        MessagePropose accept = new MessagePropose(promise.getProposalID(), proposal.getKey(), acceptedValue, proposal.getOperation());
        messenger.broadcastMessage(accept);
      } else {
        MessagePropose accept = new MessagePropose(proposal.getProposalID(), proposal.getKey(), proposal.getValue(), proposal.getOperation());
        messenger.broadcastMessage(accept);
      }
    }
  }

  /**
   * A time out method for collecting promises from acceptors.
   * @param proposalID
   */
  private void scheduleTimeout(ProposalID proposalID) {
    ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
      if (promiseCounts.getOrDefault(proposalID, 0) < QUORUM_SIZE) {
        ServerLogger.log("Timeout without reaching quorum for ProposalId: " + proposalID);
        abandonedProposals.add(proposalID);
      }
    }, 30, TimeUnit.SECONDS);
    timeoutTasks.put(proposalID, timeoutTask);
  }

  private void cancelTimeout(ProposalID proposalID) {
    ScheduledFuture<?> timeoutTask = timeoutTasks.remove(proposalID);
    if (timeoutTask != null) {
      timeoutTask.cancel(false);
    }
  }
  public void shutdownScheduler() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
          scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}

