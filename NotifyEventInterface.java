package progetto_2020_2021;

import java.rmi.*;

public interface NotifyEventInterface extends Remote{
	
	/**
	 * permette l'aggiornamento della struttura dati del client utilizzanto le informazioni passate dal server
	 * 
	 * @param userMap: struttura dati del client 
	 * @throws RemoteException
	 */
	public void notifyEvent(String userMap) throws RemoteException;
}
