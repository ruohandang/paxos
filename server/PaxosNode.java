package server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import common.IPaxosNode;
import paxos.*;

/**
 * Represents a server node in the Paxos cluster. It contains instances of Proposer, Acceptor, and Learner, 
 * and is responsible for handling RMI calls and delegating Paxos protocol operations.
 */
public class PaxosNode extends UnicastRemoteObject implements IPaxosNode{
  private Proposer proposer;
  private Acceptor acceptor;
  private Learner learner;
  private Messenger messenger;
  private KeyValueStore keyValueStore = new KeyValueStore();
  private int serverId;
  private static final int TIMEOUT_WAITING_LEARNER = 60;
  private ConcurrentHashMap<ProposalID, Proposal> activeProposals = new ConcurrentHashMap<>();
  private ThreadManager threadManager;

  /**
     * Constructs a PaxosNode with specific configuration.
     *
     * @param centralRegistryHost The host address of the central RMI registry.
     * @param centralRegistryPort The port number of the central RMI registry.
     * @param serverId The unique identifier for this server node.
     * @throws RemoteException If a network-related exception occurs.
     */
  protected PaxosNode(String centralRegistryHost, int centralRegistryPort, int serverId) throws RemoteException {
    super();
    this.serverId = serverId;
    this.messenger = new Messenger(centralRegistryHost, centralRegistryPort);
    this.proposer = new Proposer(serverId, messenger);
    this.acceptor = new Acceptor(serverId, messenger);
    this.learner = new Learner(serverId, keyValueStore);
    this.threadManager = new ThreadManager(acceptor);
    new Thread(threadManager).start();
  }

  @Override
  public void handlePaxosMessage(PaxosMessage message) throws RemoteException {
    if (message instanceof MessagePrepare || message instanceof MessagePropose) {
      acceptor.enqueueMessage(message);
    } else if (message instanceof MessagePromise) {
      proposer.propose((MessagePromise) message, activeProposals.get(message.getProposalID()));
    } else if (message instanceof MessageAccepted) {
      learner.handleAccepted((MessageAccepted) message);
    }
  }

  @Override
  public String get(String clientId, String key) throws RemoteException {
    ServerLogger.log("Server" + serverId + " received GET request for key: " + key + " from Client ID: " + clientId);
    if (key == null || key.trim().isEmpty()) {
      Response res = new Response(false, "GET", "Key must not be null or empty.");
      ServerLogger.error(res.toString());
      return res.toString();
    }

    String value = keyValueStore.get(key);
    Response res = (value != null)
      ? new Response(true, "GET", "Key found: [key]" + key, value)
      : new Response(false, "GET", "[key]" + key +" not found");
    ServerLogger.log(res.toString());
    return res.toString(); 
  }

  @Override
  public String put(String clientId, String key, String value) throws RemoteException {
    ServerLogger.log("Server" + serverId + " received PUT request for key: " + key + " from Client ID: " + clientId);
    if (key == null || key.trim().isEmpty() || value == null) {
      Response res = new Response(false, "PUT", "Key and value must not be null or empty.");
      ServerLogger.error(res.toString());
      return res.toString();
    }
  
    ProposalID proposalID = new ProposalID(serverId);
    Proposal proposal = new Proposal(proposalID, key, value, "PUT");
    
    CompletableFuture<Boolean> completionFuture = new CompletableFuture<>();
    learner.registerCompletionFuture(proposal.getProposalID(), completionFuture);
    completionFuture.orTimeout(TIMEOUT_WAITING_LEARNER, TimeUnit.SECONDS);

    activeProposals.put(proposalID, proposal);
    proposer.prepare(proposal);

    return completionFuture.handle((result, ex) -> {
      activeProposals.remove(proposalID);
      if (ex != null) {
        if (ex instanceof TimeoutException) {
          return new Response(false, "PUT", "[key]" + key + " timed out").toString();
        } else {
          return new Response(false, "PUT", "Error during operation: " + ex.getCause()).toString();
        }
      }
      return result ? new Response(true, "PUT", "[key]" + key + " added/updated").toString() :
                      new Response(false, "PUT", "[key]" + key + " aborted").toString();
    }).join();
  }


  @Override
  public String delete(String clientId, String key) throws RemoteException {
    ServerLogger.log("Server" + serverId + " received DELETE request for key: " + key + " from Client ID: " + clientId);
    if (key == null || key.trim().isEmpty()) {
      Response res = new Response(false, "DELETE", "Key must not be null or empty.");
      ServerLogger.error(res.toString());
      return res.toString();
    }
    if (keyValueStore.get(key) == null) {
      Response res = new Response(false, "DELETE", "[key]" + key +" not found");
      ServerLogger.error(res.toString());
      return res.toString();
    }
    
    ProposalID proposalID = new ProposalID(serverId);
    Proposal proposal = new Proposal(proposalID, key, null, "DELETE");
    
    CompletableFuture<Boolean> completionFuture = new CompletableFuture<>();
    learner.registerCompletionFuture(proposal.getProposalID(), completionFuture);
    // Register the future with a timeout
    completionFuture.orTimeout(TIMEOUT_WAITING_LEARNER, TimeUnit.SECONDS);
    
    activeProposals.put(proposalID, proposal);
    proposer.prepare(proposal);

    return completionFuture.thenApply(result -> {
      if (activeProposals.contains(proposalID)) {
        activeProposals.remove(proposalID);
      }
      if (result) {
        return new Response(true, "DELETE", "[key]" + key + " added/updated").toString();
      } else {
        return new Response(false, "DELETE", "[key]" + key + " aborted").toString();
      }
    }).exceptionally(ex -> {
      if (activeProposals.contains(proposalID)) {
        activeProposals.remove(proposalID);
      }
      if (ex instanceof TimeoutException) {
        return new Response(false, "DELETE", "[key]" + key + " timed out").toString();
      } else {
        return new Response(false, "DELETE", "[key]" + key + " error during operation: " + ex.getMessage()).toString();
      }
    }).join(); 
  }

  public void shutdown() {
    ServerLogger.log("Initiating shutdown of PaxosNode...");
    proposer.shutdownScheduler();
    if (threadManager != null) {
      threadManager.stop();
    }
  }
}
