package progetto_2020_2021;

import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;

/**
 * NotifyEvent è implementata grazie all'interfaccia NotifyEventInterface e permette la notifica di determinati eventi al client
 * aggiornando la struttura dati che esso mantiene 
 * 
 * @author Antonio Guzzi
 */

public class NotifyEvent extends RemoteObject implements NotifyEventInterface{
	
	private static final long serialVersionUID = 7794944609520090159L;
	HashMap<String, String> map;
	
	// ---------------------------------- METODO COSTRUTTORE ---------------------------------- //
	
	/**
	 * @param map: struttura dati del client che necessita di essere aggiornata
	 * @throws RemoteException
	 */
	public NotifyEvent(HashMap<String, String> map) throws RemoteException{
		super();
		if(map == null) throw new IllegalArgumentException("struttura dati 'map' null");
		this.map = map;
	}
	
	// ---------------------------------- METODI IMPLEMENTATI DALL'INTERFACCIA ---------------------------------- //
	@Override
	public void notifyEvent(String userMap) throws RemoteException {
		//System.out.println(userMap);
		String[] users = userMap.split(" ");
   	 	for(String user: users) {
   	 		String[] data = user.split(";");
   	 		this.map.put(data[0], data[1]);
   	 	}
   	 	return;
	}
}
