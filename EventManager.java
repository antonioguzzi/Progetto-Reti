package progetto_2020_2021;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.*;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * EventManager implementa l'interfaccia EventManagerInterface e permette al client di eseguire i metodi registrati dal server
 * 
 * @author Antonio Guzzi
 */
public class EventManager extends RemoteServer implements EventManagerInterface {
	
	private static final long serialVersionUID = -7691830863689206817L;
	private List<User> users; //lista degli utenti registrati al servizio
	private List<NotifyEventInterface> clients; //lista dei client registrati al servizio di notifica
	
	
	// ---------------------------------- METODO COSTRUTTORE ---------------------------------- //
	
	/**
	 * @param users: lista degli utenti registrati
	 * @throws RemoteException
	 */
	public EventManager(List<User> users) throws RemoteException{
		if(users == null) throw new IllegalArgumentException("struttura dati 'users' null");
		this.users = users;
		this.clients = new ArrayList<NotifyEventInterface>();
	}
	
	// ---------------------------------- METODI IMPLEMENTATI DALL'INTERFACCIA ---------------------------------- //
	
	@Override
	public synchronized boolean  register(String nickName, String password) throws RemoteException,JsonGenerationException, JsonMappingException, IOException {
		
		//verifico che il nickName sia unico
		for(User tmp : this.users) {
			if(tmp.getNickName().equals(nickName)) return false;
		}
		
		//se il nickName è unico, aggiungo l'utente tra gli utenti registrati
		this.users.add(new User(nickName,password,"Offline"));
		System.out.println("server WORTH: nuovo utente registrato correttamente");
		
		//Serializzazione con Jackson dell'ArrayList di user
		ObjectMapper mapper = new ObjectMapper();
		String path = "." + File.separator + "recoveryDir" + File.separator + "registeredUsers.json";
		File file = new File(path); 
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.writeValue(file, users);
		
		//notifica evento di nuovo utente registrato
		StringBuilder str = new StringBuilder();
		for(User tmp : this.users) {
			 str.append(tmp.getNickName()+ ";" +tmp.getState() + " ");
		 }
		update(str.toString().trim());
		return true;
	}
	
	@Override
	public synchronized void registerForCallback(NotifyEventInterface ClientInterface) throws RemoteException {
		//due client diversi possono eseguire register e unregister, entrambi i metodi vanno a modificare la struttura dati nello stesso momento
		//questo può portare ad uno stato inconsistente, clients necessita quini di essere una struttura sinconizzata
		synchronized(clients) {
			if(!clients.contains(ClientInterface)) {
				clients.add(ClientInterface);
				return;
			}
		}
		return;
	}
	
	@Override
	public synchronized void unRegisterForCallback(NotifyEventInterface ClientInterface) throws RemoteException {
		//due client diversi possono eseguire register e unregister, entrambi i metodi vanno a modificare la struttura dati nello stesso momento
		//questo può portare ad uno stato inconsistente, clients necessita quini di essere una struttura sincronizzata
		synchronized(clients) {
			if (clients.remove(ClientInterface)) System.out.println("server WORTH: logout dell'utente eseguito con successo: Client unregistered");
			else System.out.println("server WORTH: Errore nel logout dell'utente: Unable to unregister client");
		}
	}
	
	//  ---------------------------------- METODI DI APPOGGIO  ----------------------------------  //
	
	/**
	 * permette l'update della struttura dati del client
	 * 
	 * @param map: hashmap dell server utilizzarta per effettuare l'update della struttura dati del client
	 * @throws RemoteException
	 */
	public void update(String map) throws RemoteException {
		System.out.println("server WORTH: Update mandato a tutti i client");
        this.doCallBacks(map);
	}
	
	/**
	 * implementa la ricerca del client all'interno della struttura dati degli utenti registrati al sistema di notifiche
	 * 
	 * @param map  hashmap dell server utilizzarta per effettuare l'update della struttura dati del client
	 * @throws RemoteException
	 */
	public synchronized void  doCallBacks(String map) throws RemoteException { //chiamata dal server, serve sincronizzata?
		NotifyEventInterface tmp = null;
		for(NotifyEventInterface client : this.clients) {
			try {
				client.notifyEvent(map);
			}catch(RemoteException e) {tmp = client;}
		}
		if(tmp != null) this.clients.remove(tmp);
	}
}
