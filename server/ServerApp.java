package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
/* CS6650 Ruohan Dang */
/**
 * The server's main class that sets up the RMI registry, binds Paxos node instances for remote access, 
 * and initiates the server to be ready to accept client connections.
 */

public class ServerApp {
  public static void main(String[] args) {
    if (args.length < 3) {
      System.err.println("Usage: java ServerApp <Central Registry Host> <RMI Registry Port> <Server ID>");
      System.exit(1);
    }
    String centralRegistryHost = args[0];
    int portNumber = Integer.parseInt(args[1]);
    int serverId = Integer.parseInt(args[2]);
    String serverName = "KeyValueService" + serverId;

    try {
      PaxosNode paxosServer = new PaxosNode(centralRegistryHost, portNumber, serverId);

      // sharing one central registry host
      Registry registry = LocateRegistry.getRegistry(centralRegistryHost, portNumber);
      registry.bind(serverName, paxosServer);
      ServerLogger.log("Server instance identified by Server'" + serverId + "' has been successfully registered with the RMI registry on " + centralRegistryHost + ":" + portNumber);
      addShutdownHook(paxosServer, serverName);
    } catch (Exception e) {
      ServerLogger.error("Server exception: " + e.toString());
    }
  }

  private static void addShutdownHook(PaxosNode keyValueServer, String serverName) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      keyValueServer.shutdown();
      try {
        ServerLogger.log("Server " + serverName + " is shutting down...");
        // Unexport the remote object
        if (keyValueServer != null) {
          UnicastRemoteObject.unexportObject(keyValueServer, true);
          ServerLogger.log("Server " + serverName + " unexported successfully.");
        }
      } catch (Exception e) {
        ServerLogger.error("Error during server shutdown: " + e.getMessage());
      }
    }));
  }
}
