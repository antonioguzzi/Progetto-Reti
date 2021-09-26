package progetto_2020_2021;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Server è la classe che modella il server WORTH
 * 
 * @author Antonio Guzzi
 */

public class Server {
	
	private final static int BUFFER_DIMENSION = 1024;
	private static String MULTICAST_IP = "239.0.0.0";
	private final int RMIPort;
	private final int TCPport;
	private final int UDPport;
	private List<User> users;
	private ArrayList<Project> projects;
	private ArrayList<String> reusableAddresses;
	EventManager eventManager;
	
	
	// ---------------------------------- METODO COSTRUTTORE ---------------------------------- //
	
	/**
	 * @param port1: porta utilizzata per RMI
	 * @param port2: porta utilizzata per TCP
	 * @param port3: porta utilizzata per UDP
	 */
	public Server(int port1, int port2, int port3) {
        this.RMIPort = port1;
        this.TCPport = port2;
        this.UDPport = port3;
        this.users = Collections.synchronizedList(new ArrayList<User>());
        this.projects = new ArrayList<Project>();
        this.reusableAddresses = new ArrayList<String>();
       
        try {
			eventManager = new EventManager(this.users);
		} catch (RemoteException e) {e.printStackTrace();}
    }
	
	
	// ---------------------------------- METODI DI AVVIO DEL SERVER  ---------------------------------- //
	
	/**
	 * permette la registrazione dell'utente tramite la registrazione dei metodi presenti in eventManager
	 */
	public void registerUser() {
		try {
			
			//il server esporta l'oggetto
			EventManagerInterface stub = (EventManagerInterface) UnicastRemoteObject.exportObject(eventManager, this.RMIPort);
			
			//creo il registro di operazioni
			LocateRegistry.createRegistry(this.RMIPort);
            Registry register = LocateRegistry.getRegistry(this.RMIPort);
            
            //eseguo il binding delle operazioni possibili
            register.rebind("EVENT_MANAGER", stub);
		}catch(RemoteException e) {e.printStackTrace();}
	}
	
	/**
	 * permette l'avvio del server
	 */
	public void start() {
		
		//creo la cartella di recovery del server
		String path = "." + File.separator + "recoveryDir";
		File recoveryDir = new File(path);
		if(!recoveryDir.exists()) recoveryDir.mkdir();
		
		//reading del file json
		this.readerFromJson(recoveryDir);
		
		//creo un channel per la connessione TCP byte-oriented
		try (ServerSocketChannel serverChannel = ServerSocketChannel.open();){
			serverChannel.socket().bind(new InetSocketAddress(TCPport));
			serverChannel.configureBlocking(false);
		            
			//creo e registro il selettore per accettare le connessioni da parte dei client
			Selector sel = Selector.open();
			serverChannel.register(sel, SelectionKey.OP_ACCEPT);
			System.out.printf("Server WORTH: in attesa di connessioni sulla porta %d\n", TCPport);
			
	        while(true) {
		            	
            	//se non ci sono chiavi pronte salto un'iterazione
		        if(sel.select() == 0) continue;
		            	
		        //insiemi delle chiavi associate a dei canali pronti
		        Set<SelectionKey> selectedKeys = sel.selectedKeys();
		            	
		        // definisco un iteratore per l'insieme delle chiavi
		        Iterator<SelectionKey> iter = selectedKeys.iterator();
		        while(iter.hasNext()) {
		        	SelectionKey key = iter.next();
		        	iter.remove();
		        
		        	// utilizzo la try-catch per gestire la terminazione improvvisa del client
		        	try {
		        		// ------- ACCETTABLE ------ //
		        		if(key.isAcceptable()) {
		        			// accetto la connessione con il client e creao una socketchannel
		        			ServerSocketChannel server = (ServerSocketChannel) key.channel();
		        			SocketChannel clientChannel = server.accept();
		        			clientChannel.configureBlocking(false);
		        			System.out.println("Server WORTH: accettata nuova connessione dal client: " + clientChannel.getRemoteAddress());
		        			registerRead(sel, clientChannel);
		        		}
		        		// ------- READABLE ------ //
		        		else if(key.isReadable()) {
		        			readClientMessage(sel, key, recoveryDir);
		        		}
		        		// ------ WRITABLE ------ //
		        		else if(key.isWritable()) {
		        			answer(sel,key);
		        		}
		        	}catch (IOException e) {
		        		e.printStackTrace();
		        		key.channel().close();
		        		key.cancel();
		        	}
		        }
	        }
		} catch (IOException e) {e.printStackTrace();}
	}

	/**
	 * permette di registrare il messaggio inviato dal client con interesse sull'operazione di read
	 * 
	 * @param sel: selettore
	 * @param clientChannel: SocketChannel del client
	 * @throws IOException
	 */
	private void registerRead(Selector sel, SocketChannel clientChannel) throws IOException{
		
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
        ByteBuffer message = ByteBuffer.allocate(BUFFER_DIMENSION);
        ByteBuffer[] buffers = {length, message};
        
        //aggiungo il canale del client al selettore settando la preferenza sull'operazione di read
        clientChannel.register(sel, SelectionKey.OP_READ, buffers);
        return;
	}
	
	/**
	 * permette di registrare il messaggio inviato dal client con interesse sull'operazione di write per la risposta, quest'ultima cambia
	 * in base a ciò che il client ha inviato
	 * 
	 * @param sel: selettore
	 * @param key: chiave contenente il SocketChannel del client
	 * @param recoveryDir: directory per il backUp del server
	 * @throws IOException
	 */
	private void readClientMessage(Selector sel, SelectionKey key, File recoveryDir) throws IOException{
		
		SocketChannel clientChannel = (SocketChannel) key.channel();
		String usersMap;
		 
		//recupero l'attachment del messaggio
		ByteBuffer[] buffers = (ByteBuffer[]) key.attachment();
		clientChannel.read(buffers);
		
		if(!buffers[0].hasRemaining()) {
			 buffers[0].flip();
			 int length = buffers[0].getInt();
			 if(buffers[1].position() == length) {
				 buffers[1].flip();
				 
				 String msg = new String(buffers[1].array()).trim();
				 String[] parameters = msg.split(" ");
				 System.out.println("Server WORTH: ricevuta operazione " + parameters[0]);
				 
				 switch(parameters[0]) {
				 case "login":
					 boolean logged = this.login(parameters[1],parameters[2],clientChannel,sel, clientChannel.socket().getInetAddress(), clientChannel.socket().getPort());
					 //notifica evento
					 if(logged) {
						 usersMap = this.usersToString();
						 eventManager.update(usersMap);
					 }
					 break;
				case "logout":
					this.logout(clientChannel, sel, clientChannel.socket().getInetAddress(), clientChannel.socket().getPort());
					//notifica evento
					usersMap = this.usersToString();
					eventManager.update(usersMap);
					break;			
				case "list_projects":
					this.listProjects(clientChannel, sel, clientChannel.socket().getInetAddress(), clientChannel.socket().getPort());
					break;				
				case "create_project":
					this.createProject(clientChannel, sel, clientChannel.socket().getInetAddress(), clientChannel.socket().getPort(), parameters[1]);
					break;
				case "join_chat":
					this.joinChat(clientChannel, sel, parameters[1], clientChannel.socket().getInetAddress(), clientChannel.socket().getPort());
					break;
				case "add_member":
					this.addMember(clientChannel, sel, parameters[1], parameters[2], clientChannel.socket().getInetAddress(), clientChannel.socket().getPort());
					break;
				case "show_members":
					this.showMembers(clientChannel, sel, parameters[1], clientChannel.socket().getInetAddress(), clientChannel.socket().getPort());
					break;		
				case "show_cards":
					this.showCards(clientChannel, sel, parameters[1], clientChannel.socket().getInetAddress(), clientChannel.socket().getPort());
					break;	
				case "show_card":
					this.showCard(clientChannel, sel, parameters[1], parameters[2],clientChannel.socket().getInetAddress(), clientChannel.socket().getPort());
					break;
				case "add_card":
					StringBuilder description = new StringBuilder();
					for(int i = 3; i < parameters.length; i++) {
						description.append(parameters[i] + " ");
					}
					this.addCard(clientChannel, sel, parameters[1], parameters[2], description.toString().trim(), clientChannel.socket().getInetAddress(), clientChannel.socket().getPort());
					break;
				case "move_card":
					this.moveCard(clientChannel, sel, parameters[1], parameters[2], parameters[3], parameters[4], clientChannel.socket().getInetAddress(), clientChannel.socket().getPort());
					break;	
				case "get_card_history":
					this.getCardHistory(clientChannel, sel, parameters[1], parameters[2], clientChannel.socket().getInetAddress(), clientChannel.socket().getPort());
					break;						
				case "cancel_project":
					this.cancelProject(clientChannel, sel, parameters[1], clientChannel.socket().getInetAddress(), clientChannel.socket().getPort(), recoveryDir);
					break;
				}
			 }
		}
	}
	
	/**
	 * permette di scrivere sul canale in entrare del client in modo che questo riceva la risposta del server
	 * 
	 * @param sel: selettore
	 * @param key: chiave contenente il SocketChannel del client
	 * @throws IOException
	 */
	private void answer(Selector sel, SelectionKey key) throws IOException{
		
		//estraggo il socketchannel del client dalla chiave che ha espresso l'interessa per la scrittura sul buffer di output
		SocketChannel clientChannel = (SocketChannel) key.channel();
		
		//estraggo il messaggio da mandare al client
		String answ = (String) key.attachment();
		
		//scrivo il messaggio sul buffer di output
		ByteBuffer serverAnsw = ByteBuffer.wrap(answ.getBytes("UTF-8"));
		clientChannel.write(serverAnsw);
		if (!serverAnsw.hasRemaining()) {
			serverAnsw.clear();
            this.registerRead(sel, clientChannel);
        }
	}
	
	
	//  ---------------------------------- METODI DI APPOGGIO  ----------------------------------  //
	
	/**
	 * permette di cercare un utente in base al nome
	 * 
	 * @param name: nome dell'utente
	 * @return l'oggeto User associato al nome se la ricerca va a buon fine, null altrimenti
	 */
	private User searchUser(String name) {
		for(int i = 0; i < this.users.size(); i++) {
			if(this.users.get(i).getNickName().equals(name)) {
				return this.users.get(i);
			}
		}
		return null;
	}
	
	/**
	 * permette di cercare un progetto in base al suo nome
	 * 
	 * @param projectName: nome del progetto
	 * @return l'oggetto Project associato al nome, null altrimenti
	 */
	private Project searchProject(String projectName) {
		for(int i = 0; i < this.projects.size(); i++) {
			if(this.projects.get(i).getProjectName().equals(projectName)) {
				return this.projects.get(i);
			}
		}
		return null;
	}
	
	/**
	 * permette di tramutare la struttura dati che mantiene le informazioni degli utenti in una stringa da inviare al client
	 * 
	 * @return stringa contenete gli utenti registrati al servizio
	 */
	private String usersToString() {
		StringBuilder str = new StringBuilder();
		for(User tmp : this.users) {
			 str.append(tmp.getNickName()+ ";" +tmp.getState() + " ");
		 }
		return str.toString().trim();
	}

	/**
	 * permette di cercare un utente in base al suo indirzzo IP e alla sua porta
	 * 
	 * @param IP: indirizzo IP dell'utente
	 * @param port: porta dell'utente
	 * @return l'oggeto User associato al nome se la ricerca va a buon fine, null altrimenti
	 */
	private User getUserFormIp(InetAddress IP, int port) {
		for(User tmp : this.users) {
			if(tmp.getState().equals("Online")) {
				if(tmp.getIp().equals(IP) && tmp.getPort() == port) return tmp;
			}
		}
		return null;
	}
	
	/**
	 * permette di generare un indirizzo IP di multicast 
	 * 
	 * @return
	 */
	private String generateIP() {
		
		//se ci sono indirizzi riutilizzabili li utilizzo
		if(!this.reusableAddresses.isEmpty()) {
			String newIP = this.reusableAddresses.get(0);
			this.reusableAddresses.remove(0);
			return newIP;
		}
		
		String[] values = MULTICAST_IP.split("\\.");
		int[] intValues = new int[4];
       
		//trasformo in interi i valori dell'indirizzo IP
		for (int i = 0; i < values.length; i++) intValues[i] = Integer.parseInt(values[i]);

		//scorroo i valori dell'indirizzo IP partendo dall'ultimo valore
        boolean stop = false;
        for (int i = intValues.length-1; !stop; i--) {
        	//se il valore è minore di 255 lo incremento e fermo il ciclo
            if(intValues[i] < 255){
            	intValues[i]++;
                stop = true;
            }
            //altrimenti lo setto a zero e inizio ad incrementare il prossimo valore
            else intValues[i] = 0;
        }

        String newIP = String.valueOf(intValues[0]) + "." + String.valueOf(intValues[1]) + "." + String.valueOf(intValues[2]) + "." + String.valueOf(intValues[3]);
        MULTICAST_IP = newIP;
        return newIP;
	}
	
	
	// ---------------------------------- HANDLERS DELLE RICHIESTE ---------------------------------- //
	
	/**
	 * implementa l'operazione di login richeista dal client
	 * 
	 * @param name: nome utente
	 * @param psw: password associata all'utente
	 * @param clientChannel: SocketChannel del client
	 * @param sel: selettore 
	 * @param IP: indirizzo IP dell'utente
	 * @param port: porta dell'utente
	 * @throws IOException
	 */
	private boolean login(String name, String psw, SocketChannel clientChannel, Selector sel, InetAddress IP, int port) throws IOException{
		
		StringBuilder str = new StringBuilder();
		User user = searchUser(name);
		boolean logged = false;
		
		//l'utente ha effettuato il login con successo
		 if (user != null && user.getPsw().equals(psw)) {
			 
			 //setto l'indirizzo IP da cui l'utente ha eseguito il login
			 user.setIp(IP);
			 
			 //setto la porta associata allo user
			 user.setPort(port);
			 
			 //setto lo stato dell'utente a Online
			 user.setState("Online");
			 
			 //costruisco il messaggio e la struttura dati da inviare al client
			 str.append("< " + name + " logged in\n");
			 str.append(this.usersToString());
			 logged = true;
		 }
		 //l'utente non è riuscito ad effettuare il login
		 else {
			 str.append("< Errore. utente " + name + " non esistente o password errata");
		 }
		 clientChannel.register(sel, SelectionKey.OP_WRITE, str.toString());
		 return logged;
	}
	
	/**
	 * implementa l'operazione di logout richiesta dal client
	 * 
	 * @param clientChannel: SocketChannel del client
	 * @param sel: selettore
	 * @param IP: indrizzo IP dell'utente richiedente
	 * @param port: porta dell'utente richiedente
	 * @throws IOException
	 */
	private void logout(SocketChannel clientChannel, Selector sel, InetAddress IP, int port) throws IOException {

		User user = this.getUserFormIp(IP,port);
		
		if(user != null) user.setState("Offline"); 
		String answer = "< " + user.getNickName() + " logged out";
		clientChannel.register(sel, SelectionKey.OP_WRITE, answer);
	}
	
	/**
	 * permette al richiedente di visualizzare i progetti di cui fa parte
	 * 
	 * @param clientChannel: SocketChannel del client
	 * @param sel: selettore
	 * @param IP: indrizzo IP dell'utente richiedente
	 * @param port: porta dell'utente richiedente
	 * @throws IOException
	 */
	private void listProjects(SocketChannel clientChannel, Selector sel, InetAddress IP, int port)throws IOException {
		
		StringBuilder str = new StringBuilder();
		
		//in base a IP e porta si identifica il client, quest'ultimo viene cercato all'interno delle liste dei membri di tutti i progetti
		//in modo tale da poter identificare i progetti di cui fa parte
		for(Project p : this.projects) {
			String projectMembers = p.showMembers();
			User client = this.getUserFormIp(IP, port);
			if(projectMembers.contains(client.getNickName()))  str.append(p.getProjectName() + "\n");
		}
		if(str.length() == 0) str.append("l'utente " + getUserFormIp(IP,port).getNickName() + " non fa parte di nessun progetto\n");
		clientChannel.register(sel, SelectionKey.OP_WRITE, str.toString().trim());
	}
	
	/**
	 *permette al richiedente di creare un nuovo progetto con un nome unico
	 * 
	 * @param clientChannel: SocketChannel del client
	 * @param sel: selettore
	 * @param IP: indrizzo IP dell'utente richiedente
	 * @param port: porta dell'utente richiedente
	 * @param projectName: nome del progetto
	 * @throws IOException
	 */
	private void createProject(SocketChannel clientChannel, Selector sel, InetAddress IP, int port, String projectName) throws IOException {
		
		StringBuilder str = new StringBuilder();
		
		//verifico se il nome proposto dall'utente è già in utilizzo
		if(this.searchProject(projectName) != null) {
			str.append("< Errore. nome '" + projectName + "' già in uso");
			clientChannel.register(sel, SelectionKey.OP_WRITE, str.toString().trim());
			return;
		}
		
		//recupero l'utente che ha fatto richiesta della creazione del progetto
		User projectAdmin = this.getUserFormIp(IP, port);
		
		//creo il progetto e aggiungo l'utente che ne ha chiesto la creazione
		Project newProject = new Project(projectName);
		newProject.setIP(this.generateIP());
		newProject.setPort(UDPport);
		newProject.addMember(projectAdmin);
		
		//aggiungo il nuovo progetto all'interno della lista dei progetti del server
		this.projects.add(newProject);
		
		//creo la cartella dedicata al progetto all'interno del File System
		this.createDirProject(projectName);
		//creao il file contenente i membri del progetto
		this.createMembersFile(newProject);
		str.append("< " + projectName + " creato correttamente");
		clientChannel.register(sel, SelectionKey.OP_WRITE, str.toString().trim());
	}
	
	/**
	 *  permette all'utente richiedente di aggiungere un nuovo membro al progetto specificato
	 * se quest'ultimo non è già presente
	 * 
	 * @param clientChannel: SocketChannel del client
	 * @param sel: selettore
	 * @param IP: indrizzo IP dell'utente richiedente
	 * @param port: porta dell'utente richiedente
	 * @param projectName: nome del progetto
	 * @param nickName: nome dell'utente da aggiungere
	 * @throws IOException
	 */
	private void addMember(SocketChannel clientChannel, Selector sel, String projectName, String nickName , InetAddress IP, int port) throws IOException {
		
		String answer;
		
		//recupero il progetto con nome projectName
		Project project = this.searchProject(projectName);
		
		//se il progetto non è stato trovato
		if(project == null) {
			answer = "< Errore. Progetto " + projectName + " non trovato";
			clientChannel.register(sel, SelectionKey.OP_WRITE, answer);
			return;
		}
		
		//recupero i membri del progetto e verifico che il chiamate abbia i diritti per aggiungere un membro 
		String projectMembers = project.showMembers();
		User client = this.getUserFormIp(IP, port);
		if(projectMembers.contains(client.getNickName())) {
			//recupero l'utente associato al nickname che si vuole aggiungere al progetto
			User newMember = this.searchUser(nickName);
			//se l'utente da aggiungere è registrato lo aggiungo al progetto
			if(newMember != null) {
				//se l'utente da aggiungere è gia presente nel progetto
				if(!project.addMember(newMember)) answer = "< " + newMember.getNickName() + " già presente nel progetto " + projectName;
				else {
					//creo il file dedicato ai memebri del progetto all'interno del file system
					this.createMembersFile(project);
					answer = "< " + newMember.getNickName() + " aggiunto correttamente al progetto " + projectName;
					//notifica
					project.sendMessage(client.getNickName() + " ha aggiunto " + newMember.getNickName() + " al progetto");
				}
			}
			//se l'utente da aggiungere NON è registrato restituisco un messaggio di errore
			else answer = "< Errore. " + newMember + " non registrato";
		}else answer = "< Errore. chiamante non appartiene al progetto";
		clientChannel.register(sel, SelectionKey.OP_WRITE, answer);
	}
	
	/**
	 * permette la visualizzazione dei membri di un determinato progetto se questo esiste
	 * 
	 * @param clientChannel: SocketChannel del client
	 * @param sel: selettore
	 * @param IP: indrizzo IP dell'utente richiedente
	 * @param port: porta dell'utente richiedente
	 * @param projectName: nome del progetto
	 * @throws IOException
	 */
	private void showMembers(SocketChannel clientChannel, Selector sel, String projectName, InetAddress IP, int port) throws IOException {
		
		StringBuilder str = new StringBuilder();
		
		//recupero il progetto con nome projectName
		Project project = this.searchProject(projectName);
		
		//se il progetto projectName non esiste
		if(project == null) {
			str.append("Progetto " + projectName + " non trovato\n");
			clientChannel.register(sel, SelectionKey.OP_WRITE, str.toString().trim());
			return;
		}
		
		//recupero i membri del progetto e verifico che il chiamate abbia i diritti per visualizzare i membri
		String projectMembers = project.showMembers();
		User client = this.getUserFormIp(IP, port);
		if(projectMembers.contains(client.getNickName())) {
			String[] users = projectMembers.split(" ");
			for(String user : users) {
				str.append(user + "\n");
			}
		}
		else str.append("chiamante non appartiene al progetto\n");
		
		clientChannel.register(sel, SelectionKey.OP_WRITE, str.toString().trim());
	}
	
	/**
	 * permette di visualizzare le carte di un determinato progetto se questo esiste
	 * 
	 * @param clientChannel: SocketChannel del client
	 * @param sel: selettore
	 * @param IP: indrizzo IP dell'utente richiedente
	 * @param port: porta dell'utente richiedente
	 * @param projectName: nome del progetto
	 * @throws IOException
	 */
	private void showCards(SocketChannel clientChannel, Selector sel, String projectName, InetAddress IP, int port) throws IOException {
		StringBuilder str = new StringBuilder();
		
		//recupero il progetto con nome projectName
		Project project = this.searchProject(projectName);
		
		//se il progetto projectName non esiste 
		if(project == null) {
			str.append("Progetto " + projectName + " non trovato\n");
			clientChannel.register(sel, SelectionKey.OP_WRITE, str.toString().trim());
			return;
		}
		
		//recupero i membri del progetto e verifico che il chiamate abbia i diritti per visualizzare i membri
		String projectMembers = project.showMembers();
		User client = this.getUserFormIp(IP, port);
		if(projectMembers.contains(client.getNickName())) str.append(project.showCards());
		else str.append("chiamante non appartiene al progetto\n");
		clientChannel.register(sel, SelectionKey.OP_WRITE, str.toString().trim());
	}
	
	/**
	 * permette di visualizzare le informazioni di una determinata carta
	 * 
	 * @param clientChannel: SocketChannel del client
	 * @param sel: selettore
	 * @param IP: indrizzo IP dell'utente richiedente
	 * @param port: porta dell'utente richiedente
	 * @param projectName: nome del progetto
	 * @param cardName: nome della carta
	 * @throws IOException
	 */
	private void showCard(SocketChannel clientChannel, Selector sel, String projectName, String cardName, InetAddress IP, int port) throws IOException{
		
		StringBuilder str = new StringBuilder();
		
		//recupero il progetto con nome projectName
		Project project = this.searchProject(projectName);
	
		//se il progetto projectName non esiste
		if(project == null) {
			str.append("Progetto " + projectName + " non trovato");
			clientChannel.register(sel, SelectionKey.OP_WRITE, str.toString().trim());
			return;
		}

		//recupero i membri del progetto e verifico che il chiamate abbia i diritti per visualizzare i membri
		String projectMembers = project.showMembers();
		User client = this.getUserFormIp(IP, port);
		if(projectMembers.contains(client.getNickName())) str.append(project.showCard(cardName));
		else str.append("chiamante non appartiene al progetto");
		clientChannel.register(sel, SelectionKey.OP_WRITE, str.toString().trim());
	}
	
	/**
	 * permette l'aggiunta di una nuova carta ad un progetto specifico
	 * 
	 * @param clientChannel: SocketChannel del client
	 * @param sel: selettore
	 * @param IP: indrizzo IP dell'utente richiedente
	 * @param port: porta dell'utente richiedente
	 * @param projectName: nome del progetto
	 * @param cardName: nome della carta
	 * @param description: descrizione della carta
	 * @throws IOException
	 */
	private void addCard(SocketChannel clientChannel, Selector sel, String projectName, String cardName, String description, InetAddress IP, int port) throws IOException{
		
		//recupero il progetto con nome projectName
		Project project = this.searchProject(projectName);
		String answer;

		//se il progetto projectName non esiste
		if(project == null) {
			answer = "< Progetto " + projectName + " non trovato";
			clientChannel.register(sel, SelectionKey.OP_WRITE, answer);
			return;
		}
		
		//recupero i membri del progetto e verifico che il chiamate abbia i diritti per aggiungere una carta
		String projectMembers = project.showMembers();
		User client = this.getUserFormIp(IP, port);
		if(projectMembers.contains(client.getNickName())) {
			//se la carta non è stata aggiunta
			Card newCard = new Card(cardName, description);
			if(project.addCardToProject(newCard)) {
				this.createCardFile(projectName, newCard);
				answer = "< la card " + cardName + " è stata aggiunta correttamente al progetto " + projectName;
				//notifica
				project.sendMessage(client.getNickName() + " ha aggiunto una nuova card al progetto");
			}
			else answer = "< la card " + cardName + " è gia presente nel progetto " + projectName;
		}else answer = "< chiamante non appartiene al progetto";
		clientChannel.register(sel, SelectionKey.OP_WRITE, answer);
		
	}
	
	/**
	 * permette di muovere una carta all'interno di un progetto, da una lista sorgente ad una di destinazione
	 * 
	 * @param clientChannel: SocketChannel del client
	 * @param sel: selettore
	 * @param IP: indrizzo IP dell'utente richiedente
	 * @param port: porta dell'utente richiedente
	 * @param projectName: nome del progetto
	 * @param cardName: nome della carta che si vuole muovere
	 * @param src: lista sorgente
	 * @param dest: lista destinazione
	 * @throws IOException
	 */
	private void moveCard(SocketChannel clientChannel, Selector sel, String projectName, String cardName, String src, String dest, InetAddress IP, int port) throws IOException{
		
		//recupero il progetto con nome projectName
		Project project = this.searchProject(projectName);
		String answer;
		
		
		//se il progetto projectName non esiste
		if(project == null) {
			answer = "< Progetto " + projectName + " non trovato";
			clientChannel.register(sel, SelectionKey.OP_WRITE, answer);
			return;
		}
		
		//recupero i membri del progetto e verifico che il chiamate abbia i diritti per muovere una carta
		String projectMembers = project.showMembers();
		User client = this.getUserFormIp(IP, port);
		if(projectMembers.contains(client.getNickName())) {
			//se il movimento è consentito e la carta esiste
			Card cardMoved = project.moveCard(cardName, src, dest);
			if(cardMoved != null) {
				this.createCardFile(projectName, cardMoved);
				answer = "< la card " + cardName + " è stata spostata da " + src + " a " + dest;
				project.sendMessage(client.getNickName() + " ha spostato la carta " + cardMoved.getCardName() + " da " + src + " a " + dest);
			}
			else answer = "< Impossibile spostare la card " + cardName + " del progetto " + projectName + ": spostamento non consentito o carta non trovata";
		}
		else answer = "< chiamante non appartiene al progetto";
		clientChannel.register(sel, SelectionKey.OP_WRITE, answer);
	}
	
	/**
	 * permette ad un utente di unirsi alla chat di un progetto
	 * 
	 * @param clientChannel: SocketChannel del client
	 * @param sel: selettore
	 * @param IP: indrizzo IP dell'utente richiedente
	 * @param port: porta dell'utente richiedente
	 * @param projectName: nome del progetto
	 * @throws IOException
	 */
	private void joinChat(SocketChannel clientChannel, Selector sel, String projectName, InetAddress IP, int port) throws IOException{
		
		//recupero il progetto con nome projectName
		Project project = this.searchProject(projectName);
		String answer;
		
		//se il progetto projectName non esiste
		if(project == null) {
			answer = "< Errore. Progetto " + projectName + " non trovato";
			clientChannel.register(sel, SelectionKey.OP_WRITE, answer);
			return;
		}
		
		//recupero i membri del progetto e verifico che il chiamate abbia i diritti per unirsi alla chat
		String projectMembers = project.showMembers();
		User client = this.getUserFormIp(IP, port);
		if(projectMembers.contains(client.getNickName())) {
			answer = project.getIP() + " " + project.getPort() + " " + client.getNickName();
		}else answer = "< Errore. chiamante non appartenente al progetto";
		clientChannel.register(sel, SelectionKey.OP_WRITE, answer);
	}
	
	/**
	 * permette di visualizzare lo storico degli spostamenti di una determinata carta di un progetto
	 * 
	 * @param clientChannel: SocketChannel del client
	 * @param sel: selettore
	 * @param IP: indrizzo IP dell'utente richiedente
	 * @param port: porta dell'utente richiedente
	 * @param projectName: nome del progetto nel quale è contenuta la carta
	 * @param cardName: nome della carta di cui si vuole conoscere la storia
	 * @throws IOException
	 */
	private void getCardHistory(SocketChannel clientChannel, Selector sel, String projectName, String cardName, InetAddress IP, int port) throws IOException{
		
		StringBuilder str = new StringBuilder();
		str.append("< ");
		
		//recupero il progetto con nome projectName
		Project project = this.searchProject(projectName);
		
		//se il progetto projectName non esiste
		if(project == null) {
				str.append("Progetto " + projectName + " non trovato");
				clientChannel.register(sel, SelectionKey.OP_WRITE, str.toString().trim());
				return;
		}
		
		//recupero i membri del progetto e verifico che il chiamate abbia i diritti per vedere la storia della carta
		String projectMembers = project.showMembers();
		User client = this.getUserFormIp(IP, port);
		if(projectMembers.contains(client.getNickName())) {
			//recupero la carta in base al nome
			Card card = project.searchCardInProject(cardName);
			//se la carta esiste
			if(card != null) str.append(card.getHistory());
			else str.append("Impossibile visualizzare la storia di " + cardName + " del progetto " + projectName);
		}
		clientChannel.register(sel, SelectionKey.OP_WRITE, str.toString());
	}

	/**
	 * permette di elinimare un progetto dal database del server
	 * 
	 * @param clientChannel: SocketChannel del client
	 * @param sel: selettore
	 * @param IP: indrizzo IP dell'utente richiedente
	 * @param port: porta dell'utente richiedente
	 * @param projectName: nome del progetto da eliminare
	 * @param recoveryDir: cartella di backUp dalla quale dovrà essere eliminato il progetto
	 * @throws IOException
	 */
	private void cancelProject(SocketChannel clientChannel, Selector sel, String projectName, InetAddress IP, int port,  File recoveryDir) throws IOException{
		
		//recupero il progetto con nome projectName
		Project project = this.searchProject(projectName);
		String answer;
		
		//se il progetto projectName non esiste
		if(project == null) {
			answer = "< Progetto " + projectName + " non trovato";
			clientChannel.register(sel, SelectionKey.OP_WRITE, answer);
			return;
		}
		//recupero i membri del progetto e verifico che il chiamate abbia i diritti per eliminare un progetto
		String projectMembers = project.showMembers();
		User client = this.getUserFormIp(IP, port);
		if(projectMembers.contains(client.getNickName())) {
			//se tutte le card sono in stato DONE
			if(project.getList("TODO").isEmpty() && project.getList("INPROGRESS").isEmpty() && project.getList("TOBEREVISED").isEmpty()) {
				answer = "< Progetto " + project.getProjectName() + " rimosso";
				project.sendMessage("close");
				
				//recupero l'indirizzo IP del progetto in modo da poterlo riutilizzare
				this.reusableAddresses.add(project.getIP());
				this.projects.remove(project);
				this.deleteProjectDir(projectName, recoveryDir);
			}
			else answer = "< Impossibile eliminare il progetto " + projectName + ": progetto non terminato";
		}
		else answer = "< chiamante non appartiene al progetto";
		clientChannel.register(sel, SelectionKey.OP_WRITE, answer);
		
	}
	
	
	// ---------------------------------- METODI PER LA CREAZIONE .JSON ---------------------------------- //
	
	/**
	 * permette di creare una cartella con il medesimo nome del progetto specificato
	 * 
	 * @param projectName: nome del progetto
	 */
	private void createDirProject(String projectName) {
		String path =  "." + File.separator + "recoveryDir" + File.separator + projectName;
		File projectDir = new File(path);
		if(projectDir.mkdir()) System.out.println("server WORTH: Directory " + projectName + " creata correttamente");
		else System.out.println("server WORTH: Impossibile creare la direcotory " + projectName);
		return;
	}
	
	/**
	 * permette di cercare un determinato file all'interno di una cartella
	 * 
	 * @param fileName: nome del file da cercare
	 * @param dir: cartella nella quale si vuole cercare il file
	 * @return File cercato se la ricerca a successo, null altrimenti
	 */
	private File searchFile(String fileName, File dir) {
		File[] projects = dir.listFiles();
		for(int i = 0; i < projects.length; i++) {
			if(projects[i].getName().equals(fileName)) return projects[i];
		}
		return null;
	}
	
	/**
	 * permette la cancellazione di una cartella dal FileSystem
	 * 
	 * @param projectName: nome della cartella associata al progetto
	 * @param recoveryDir: cartella di backUp nella quale cercare la cartella associata al progetto
	 */
	private void  deleteProjectDir(String projectName, File recoveryDir) {
		
		//cerco la direcotory del progetto
		File projectFile = this.searchFile(projectName, recoveryDir);
		
		//elimino tutti i file della direcotory
		String[] files = projectFile.list();
		for(String file : files) {
			File currentFile = new File(projectFile.getPath(),file);
		    currentFile.delete();
		}
		
		//elimino la direcotory
		if(projectFile.delete()) {
			System.out.println("server WORTH: Directory " + projectName + " eliminata correttamente");
		}else System.out.println("server WORTH: Impossibile eliminare la direcotory " + projectName);
		return;
	}
	
	/**
	 * permette la creazione di un file contenente i nomi dei membri di un progetto
	 * 
	 * @param project: progetto dal quale si vogliono estrarre i nomi dei membri
	 * @throws IOException
	 */
	private void createMembersFile(Project project) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		String path = "." + File.separator + "recoveryDir" + File.separator + project.getProjectName() + File.separator + "projectMembers.json";
		File file = new File(path); 
		if(!file.exists()) file.createNewFile();
		mapper.writeValue(file, project.getMembers());
		return;
	}
	
	/**
	 * permette di creare un file associato ad una carta di un progetto
	 * 
	 * @param projectName: nome del progetto
	 * @param card: carta della quale si vogliono salvare le informazioni
	 * @throws IOException
	 */
	private void createCardFile(String projectName, Card card) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		String path = "." + File.separator + "recoveryDir" + File.separator + projectName + File.separator + card.getCardName() + ".json";
		File file = new File(path); 
		mapper.writeValue(file, card);
		return;
	}
	
	/**
	 * permette la lettura all'interno della cartella di backUp
	 * 
	 * @param recoveryDir: caretella di backUp
	 */
	private void readerFromJson(File recoveryDir) {
		
		String registeredUserFilePath = "." + File.separator + "recoveryDir" + File.separator + "registeredUsers.json";
		File registeredUserFile = new File(registeredUserFilePath);
		
		//se ancora non si è registrato nessun utente il server non necessita di salvare nessun dato
		if(!registeredUserFile.exists()) return;
		
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_DIMENSION);
		ObjectMapper objectMapper = new ObjectMapper();
		
		//apro un canale per la lettura nel file json degli utenti registrati
		try (FileChannel inChannel = FileChannel.open(Paths.get(registeredUserFile.getPath()), StandardOpenOption.READ) ){
			boolean stop = false;
			while (!stop) {
				if (inChannel.read(buffer) == -1) stop = true;
			}
		} catch (IOException e) {e.printStackTrace();}
		try {
			this.users.addAll(objectMapper.reader().forType(new TypeReference<ArrayList<User>>() {}).readValue(buffer.array()));
			for(int i = 0; i < this.users.size(); i++) this.users.get(i).setState("Offline");
		}catch (IOException e){e.printStackTrace();}
		
		File[] files = recoveryDir.listFiles();
		//scorro i file nella directory di recovery
		for(File dir : files) {
			//se il file è una directory allora conterrà le informazioni del progetto con il medesimo nome
			if(dir.isDirectory()) {
				//creo il nuovo progetto e lo aggiungo alla lista dei progetti mantenuta dal server
				Project project = new Project(dir.getName());
				project.setIP(this.generateIP());
				project.setPort(UDPport);
				this.projects.add(project);
				
				//recupero i file conteneti all'interno della direcotory del progetto
				File[] dirFiles = dir.listFiles();
				//scorro i file nella directory del progetto
				for(File file : dirFiles) {
					String dirFilePath = "." + File.separator + "recoveryDir" + File.separator + dir.getName() + File.separator +file.getName();
					File dirFile = new File(dirFilePath); 
					//apro un canale per la lettura del file
					try (FileChannel inChannel = FileChannel.open(Paths.get(dirFile.getPath()), StandardOpenOption.READ) ){
						boolean stop = false;
						buffer.clear();
						while (!stop) {
							if (inChannel.read(buffer) == -1) stop = true;
						}
					} catch (IOException e) {e.printStackTrace();}
					try {
						//se il file è projectMembers.json
						if(file.getName().equals("projectMembers.json")) {
							project.setMembers(objectMapper.reader().forType(new TypeReference<ArrayList<String>>() {}).readValue(buffer.array()));
						}
						//se è un file riferito ad una carta
						else {
							Card newCard = objectMapper.readValue(file, Card.class);
							//aggiungo la carta all'interno dell'ultima lista presente nella storia
							project.addCardToList(newCard.getStory().get(newCard.getStory().size()-1), newCard);
						}
					}catch (IOException e) {e.printStackTrace();}
				}
			}
		}
	}
}
