package paxos;

import server.ServerLogger;

/**
 * Manages the lifecycle of threads, particularly for the Acceptor role, by monitoring and restarting threads that simulate failure
 */
public class ThreadManager implements Runnable {
  private Thread acceptorThread;
  private Acceptor acceptor;
  private volatile boolean running = true;



  public ThreadManager(Acceptor acceptor) {
    this.acceptorThread = new Thread(acceptor);
    this.acceptor = acceptor;
    this.acceptorThread.start();
  }

  @Override
  public void run() {
    while (running) {
      if (acceptorThread != null && !acceptorThread.isAlive()) {
        try {
          // Deliberate delay to simulate recovery time
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          running = false;
          Thread.currentThread().interrupt();
          return;
        }
        // Restart the Acceptor by creating a new instance and new thread
        restartAcceptor();
        ServerLogger.warn("Acceptor" + this.acceptor.getServerId() + " thread restarted after failure.");
      }
    }
  }

  private void restartAcceptor() {
    acceptor.clearStates();
    acceptorThread = new Thread(acceptor);
    acceptorThread.start();
  }

  public void stop() {
    running = false;
    if (acceptorThread != null && acceptorThread.isAlive()) {
      acceptorThread.interrupt();
    }
  }
}
