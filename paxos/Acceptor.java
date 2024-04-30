package paxos;

import java.rmi.RemoteException;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import server.ServerLogger;

/**
 * Implements the Acceptor role in the Paxos protocol, responsible for 
 * responding to prepare and propose requests from Proposers, promising to accept values, and accepting proposals.
 */

public class Acceptor implements Runnable {
  private final int serverId;
  private final Messenger messenger;
  private ConcurrentHashMap<String, ProposalID> highestPromised = new ConcurrentHashMap<>();
  private ConcurrentHashMap<String, ProposalID> highestAccepted = new ConcurrentHashMap<>();
  private ConcurrentHashMap<String, String> acceptedValues = new ConcurrentHashMap<>();
  private BlockingQueue<PaxosMessage> messageQueue = new LinkedBlockingQueue<>();
  private volatile boolean running = true;
  private Random random = new Random();

  public Acceptor(int serverId, Messenger messenger) {
    this.serverId = serverId;
    this.messenger = messenger;
  }

  public int getServerId() {
    return this.serverId;
  }

  public Messenger getMessenger() {
    return this.messenger;
  }

  @Override
  public void run() {
    try {
      while (running && !Thread.currentThread().isInterrupted()) {
        // Run for a random duration between 5 seconds to 15s before a possible failure
        long workDuration = random.nextInt(10000) + 5000;
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < workDuration && running) {
          PaxosMessage message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
          if (message != null) {
            processMessage(message);
          }
        }
        // a random chance to fail, 5%
        if (random.nextDouble() < 0.05) {
          running = false;  // stop the thread to simulate failure
          ServerLogger.warn("Acceptor " + this.serverId + " simulated failure.");
        } else {
          running = true;
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      ServerLogger.warn("Acceptor" + serverId + " thread interrupted.");
    } catch (RemoteException e) {
      ServerLogger.error("RemoteException error for Acceptor" + serverId + ": " + e.getMessage());
    } finally {
      clearStates();
      running = false;
    }
  }

  public void stop() {
    running = false;
    Thread.currentThread().interrupt();
  }

  /**
   * a method for message queue for current thread to process
   * @param message
   */
  public void enqueueMessage(PaxosMessage message) {
    messageQueue.offer(message);
  }

  private void processMessage(PaxosMessage message) throws RemoteException {
    if (message instanceof MessagePrepare) {
      promise((MessagePrepare) message);
    } else if (message instanceof MessagePropose) {
      accept((MessagePropose) message);
    }
  }

  /**
   * The message to process proposor's prepare message, compare with previous promised proposalId
   * then decide to promise or not
   * @param prepare a prepare message from proposor
   * @throws RemoteException
   */
  private void promise(MessagePrepare prepare) throws RemoteException {
    String key = prepare.getKey();
    ProposalID proposalID = prepare.getProposalID();

    ProposalID currentPromised = highestPromised.get(key);
    if (currentPromised == null || proposalID.compareTo(currentPromised) > 0) {
      // New highest ID received, update and respond with a promise
      highestPromised.put(key, proposalID);
      // was a proposal already accepted?
      if (highestAccepted.containsKey(key)) {
        // There is an accepted proposal, send details with promise
        ProposalID acceptedId = highestAccepted.get(key);
        String value = acceptedValues.get(key);
        MessagePromise promise = new MessagePromise(serverId, key, proposalID, acceptedId, value);
        messenger.sendPaxosMessage(proposalID.getServerId(), promise);
      } else {
        // Now send promise back to the specific proposer
        MessagePromise promise = new MessagePromise(serverId, key, proposalID);
        messenger.sendPaxosMessage(proposalID.getServerId(), promise); 
      }
    } else {
      ServerLogger.log(prepare.getProposalID() + ": Acceptor" + this.serverId + " does not send PROMISE to Proposer" + prepare.getProposalID().getServerId());
    }
  }

  /**
   * A method to process the proposal from proposer, check if promised the current proposal 
   * the decide to accept it or not
   * @param propose a propose message from proposer
   * @throws RemoteException
   */
  private void accept(MessagePropose propose) throws RemoteException {
    if (highestPromised == null) {
      return;
    }
    String key = propose.getKey();
    String value = propose.getValue();
    String operation = propose.getOperation();
    ProposalID proposalID = propose.getProposalID();
    ProposalID currentPromised = highestPromised.get(key);
    if (proposalID.equals(currentPromised)) {
      highestAccepted.put(key, proposalID);
      if (value != null) {
        acceptedValues.put(key, value);
      }
      MessageAccepted accept = new MessageAccepted(serverId, proposalID, key, value, operation);
      messenger.broadcastMessage(accept);
    } else {
      ServerLogger.log("ProposalId: " + propose.getProposalID() + ": Acceptor" + this.serverId + " does not send ACCEPT to Learners");
    }
  }

	public void clearStates() {
		this.highestPromised = new ConcurrentHashMap<>();
    this.highestAccepted = new ConcurrentHashMap<>();
    this.acceptedValues = new ConcurrentHashMap<>();
    this.messageQueue = new LinkedBlockingQueue<>();
    this.running = true;
	}
}
