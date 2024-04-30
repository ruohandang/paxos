package paxos;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import common.IPaxosNode;
import common.ServerConfig;
import server.ServerLogger;

/**
 * Handles the sending and receiving of Paxos messages between nodes, acting as a communication layer in the Paxos protocol.
 */

public class Messenger {
  private String centralRegistryHost;
  private int centralRegistryPort;

  public Messenger(String centralRegistryHost, int centralRegistryPort) {
    this.centralRegistryHost = centralRegistryHost;
    this.centralRegistryPort = centralRegistryPort;
  }

  public void broadcastMessage(PaxosMessage message) {
    ExecutorService executor = Executors.newFixedThreadPool(ServerConfig.ALL_SERVER_IDs.length);
    for (int serverId : ServerConfig.ALL_SERVER_IDs) { 
      executor.submit(() -> sendPaxosMessage(serverId, message));
    }
    executor.shutdown();
    try {
      if (!executor.awaitTermination(20, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException ie) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  public void sendPaxosMessage(int serverId, PaxosMessage message) {
    String serverName = "KeyValueService" + serverId;
    try {
      Registry registry = LocateRegistry.getRegistry(centralRegistryHost, centralRegistryPort);
      IPaxosNode remoteNode = (IPaxosNode) registry.lookup(serverName);
      if (message instanceof MessagePrepare) {
        ServerLogger.log(message.getProposalID() + ": is sending PREPARE to Acceptor" + serverId);
      } else if (message instanceof MessagePromise) {
        ServerLogger.log( message.getProposalID() + ": is sending PROMISE message to Proposer by Acceptor" + message.getServerId());
      } else if (message instanceof MessagePropose) {
        ServerLogger.log( message.getProposalID() + ": is sending PROPOSE message to Acceptor" + serverId);
      } else if (message instanceof MessageAccepted) {
        ServerLogger.log(message.getProposalID() + ": is sending ACCEPT message to Learner" + serverId + " by Acceptor" + message.getServerId());
      }
      remoteNode.handlePaxosMessage(message);
    } catch (Exception e) {
      if (message instanceof MessagePrepare) {
        ServerLogger.error(message.getProposalID() + ": Failed to send Paxos PREPARE message to Acceptor" + serverId + ": " + e.getMessage());
      } else if (message instanceof MessagePromise) {
        ServerLogger.error(message.getProposalID() + ": Failed to send Paxos PROMISE message to Proposer by Acceptor" + message.getServerId() + ": " + e.getMessage());
      } else if (message instanceof MessagePropose) {
        ServerLogger.error(message.getProposalID() + ": Failed to send Paxos PROPOSE message to Acceptor" + serverId + ": " + e.getMessage());
      } else if (message instanceof MessageAccepted) {
        ServerLogger.error(message.getProposalID() + ": Failed to send Paxos ACCEPT message to Learner" + serverId + " by Acceptor" + message.getServerId() + ": " + e.getMessage());
      }
      e.printStackTrace();
    }
  }
}
