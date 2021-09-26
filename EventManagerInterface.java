package progetto_2020_2021;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * EventManagerInterface è l'interfaccia che permette l'implementazione dei metodi realizzati dal server per il client
 * 
 * @author Antonio Guzzi
 */

public interface EventManagerInterface extends Remote {
	
	/**
	 * permette la registrazione dell'utente sulla struttura dati del server
	 * 
	 * @param nickName: nome dell'utente da registrare
	 * @param password: password associata all'utente
	 * @return true se la registrazione va a buon fine, false altrimenti
	 * @throws RemoteException
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	boolean register(String nickName, String password) throws RemoteException, JsonGenerationException, JsonMappingException, IOException;
	
	/**
	 * permette ad un client di registrarsi al sistema di notifiche
	 * 
	 * @param ClientInterface
	 * @throws RemoteException
	 */
	public void registerForCallback (NotifyEventInterface ClientInterface) throws RemoteException;
	
	/**
	 * permette ad un client di eliminare la registrazione dal sistema di notifiche
	 * 
	 * @param ClientInterface
	 * @throws RemoteException
	 */
	public void unRegisterForCallback (NotifyEventInterface ClientInterface) throws RemoteException;
}
