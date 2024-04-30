package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import paxos.PaxosMessage;
/**
 * Defines the remote interface for Paxos nodes, outlining the methods that can be remotely invoked via RMI.
 */
public interface IPaxosNode extends Remote {
  String get(String clientId, String key) throws RemoteException;
  String put(String clientId, String key, String value) throws RemoteException;
  String delete(String clientId, String key) throws RemoteException;
  void handlePaxosMessage(PaxosMessage message) throws RemoteException;
}
