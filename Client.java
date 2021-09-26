package progetto_2020_2021;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Client è la classe che modella un client richiedente i servizi del server WORTH
 * 
 * @author Antonio Guzzi
 */
public class Client {
	
	private final static int BUFFER_DIMENSION = 1024;
	private final int RMIport;
	private final int TCPport;
	private Scanner scanner;
	private HashMap<String, String> map; //coppie user - state
	private HashMap<String, Chat> chats; //coppie progetto - chat
	
	
	// ---------------------------------- METODO COSTRUTTORE ---------------------------------- //
	
	/**
	 * 
	 * @param port1: porta utilizzata per RMI
	 * @param port2: porta utilizzata per TCP
	 */
	public Client(int port1, int port2) {
		this.RMIport = port1;
        this.TCPport = port2;
        this.scanner = new Scanner(System.in);
        this.map = new HashMap<String, String>();
        this.chats = new HashMap<String, Chat>();
	}
	
	// ---------------------------------- METODO DI AVVIO DEL CLIENT  ---------------------------------- //
	
	/**
	 * permette l'avvio del client
	 */
	public void start() {
		NotifyEventInterface callbackObj = null;
		NotifyEventInterface stub = null;
		boolean logged = false;
		boolean connected = false;
		SocketChannel client = null;
		
		Registry r;
		EventManagerInterface remoteEventManager = null;
		try {
			//il client ottiene un riferimento al registro delle operazioni
			r = LocateRegistry.getRegistry(this.RMIport);
			//il client ottiene un riferimento all'event manager con il nome specificato
			remoteEventManager = (EventManagerInterface) r.lookup("EVENT_MANAGER");
		} catch (RemoteException | NotBoundException e1) {e1.printStackTrace();}
		
		System.out.println("---- WELCOME TO SERVER WORTH ----");
		System.out.println("Digitare 'help' per visualizzare la lista dei comandi");
		
		while(true) {
			//fino a quando l'utente non è loggato può fare solo poche operazioni
			while(!logged) {
				System.out.print("> ");
				String input = this.scanner.nextLine();
				String[] parameters = input.split(" ");
				
				//controllo il primo parametro passato in input
				switch(parameters[0]) {
				case "register":
					//controllo che il numero di argomenti sia corretto
					if(parameters.length == 3) {
						try {
							//il client effettua la registrazione di un utente
							this.register(parameters[1], parameters[2], remoteEventManager);
						}catch (NotBoundException | IOException e) { 
							e.printStackTrace();
							return;
						}
					}
					//se il numero di argomenti è errato stampo un messaggio di errore
					else System.out.println("register [nome dell'utente] [psw]");
					break;
				
				case "login":
					//controllo che il numero di argomenti sia corretto
					if(parameters.length == 3) {
						try {
							//apro la connessione verso il server WORTH
							if (!connected) {
								client = SocketChannel.open(new InetSocketAddress(InetAddress.getLocalHost(), this.TCPport));
								connected = true;
							}
							
							//il client effettua il login
							logged = this.login(parameters[0],parameters[1],parameters[2],client);
							
							//se il login è andato a buon fine
							if(logged) {
								//il client attiva il sistema di notifiche
								callbackObj = new NotifyEvent(this.map);
								stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
								this.turnUpNotifications(remoteEventManager,stub);
							}
						}catch (IOException e) { e.printStackTrace();}
					}
					//se il numero di argomenti è errato stampo un messaggio di errore
					else System.out.println("login [nome dell'utente] [psw]");
					break;
				
				case "quit":
					return;
				
				case "help":
					try {
						this.help();
					} catch (IOException e) {e.printStackTrace();}
					break;
				
				default:
					System.out.println("comando non disponibile");
				}
			}
			
			while(logged) {
				System.out.print("> ");
				String input = this.scanner.nextLine();
				String[] parameters = input.split(" ");
				
				switch(parameters[0]) {
				case "help":
					try {
						this.help();
					} catch (IOException e) {e.printStackTrace();}
					break;
				case "logout":
					try {
						
						//eseguo il logout
						this.logout(parameters[0], client, callbackObj);
						
						//modifico i flag che segnalano il cambio di stato del client
						logged = false;
						connected = false;
						
						//elimino il client dal registro per le notifiche
						this.turnOffNotification(remoteEventManager, stub);
					} catch (IOException e) {e.printStackTrace();}
					break;
				
				case "quit":
					try {
						
						//eseguo il logout
						this.logout("logout", client, callbackObj);
						
						//elimino il client dal registro per le notifiche
						this.turnOffNotification(remoteEventManager, stub);
						
						//termino l'ascolto su tutte le chat
						for (Chat c : chats.values()) c.close();
					} catch (IOException e) {e.printStackTrace();}
					return;
				
				case "list_users":
					this.listUser();
					break;
				
				case "list_online_users":
					this.listOnlineUsers();
					break;
				
				case "list_projects":
					try {
						this.listProjects(parameters[0], client);
					} catch (IOException e) {e.printStackTrace();}
					break;
				
				case "create_project":
					//controllo che il numero di argomenti sia corretto
					if(parameters.length == 2) {
						try {
							this.createProject(parameters[0], parameters[1], client);
						} catch (IOException e) {e.printStackTrace();}
					}else System.out.println("create_project [nome del progetto]");
					break;
					
				case "add_member":
					//controllo che il numero di argomenti sia corretto
					if(parameters.length == 3) {
						try {
							this.addMember(parameters[0], parameters[1], parameters[2], client);
						} catch (IOException e) {e.printStackTrace();}
					}else System.out.println("add_member [nome del progetto] [nome dell'utente]");
					break;
				
				case "show_members":
					//controllo che il numero di argometi si acorretto
					if(parameters.length == 2) {
						try {
							this.showMembers(parameters[0], parameters[1], client);
						} catch (IOException e) {e.printStackTrace();}
					}else System.out.println("show_members [nome del progetto]");
					break;
				
				case "show_cards":
					//controllo che il numero di argometi si acorretto
					if(parameters.length == 2) {
						try {
							this.showCards(parameters[0], parameters[1], client);
						} catch (IOException e) {e.printStackTrace();}
					}else System.out.println("show_cards [nome del progetto]");
					break;
				
				case "show_card":
					//controllo che il numero di argometi si acorretto
					if(parameters.length == 3) {
						try {
							this.showCard(parameters[0], parameters[1], parameters[2], client);
						} catch (IOException e) {e.printStackTrace();}
					}else System.out.println("show_card [nome del progetto] [nome della carta]");
					break;
					
				case "add_card":
					//controllo che il numero di argometi si acorretto
					if(parameters.length >= 4) {
						try {
							this.addCard(parameters, client);
						} catch (IOException e) {e.printStackTrace();}
					}else System.out.println("add_card [nome del progetto] [nome della carta] [descrizione della carta]");
					break;
					
				case "move_card":
					//controllo che il numero di argometi si acorretto
					if(parameters.length == 5) {
						try {
							this.moveCard(parameters[0], parameters[1], parameters[2], parameters[3], parameters[4], client);
						} catch (IOException e) {e.printStackTrace();}
					}else System.out.println("move_card [nome del progetto] [nome della carta] [lista di partenza] [lista di destinazione]");
					break;
				
				case "get_card_history":
					//controllo che il numero di argometi si acorretto
					if(parameters.length == 3) {
						try {
							this.getCardHistory(parameters[0], parameters[1], parameters[2],client);
						} catch (IOException e) {e.printStackTrace();}
					}else System.out.println("get_card_history [nome del progetto] [nome della carta]");
					break;
				
				case "join_chat":
					//controllo che il numero di argometi si acorretto
					if(parameters.length == 2) {
						try {
							this.joinChat(parameters[0], parameters[1], client);
						} catch (IOException e) {e.printStackTrace();}
					}else System.out.println("join_chat [nome del progetto]");
					break;
					
				case "read_chat":
					//controllo che il numero di argometi si acorretto
					if(parameters.length == 2) this.readChat(parameters[1]);
					else System.out.println("read_chat [nome del progetto]");
					break;
				
				case "send_chat_msg":
					//controllo che il numero di argometi si acorretto
					if(parameters.length >= 3) this.sendChatMsg(parameters);
					else System.out.println("read_chat [nome del progetto] [msg]");
					break;
				
				case "cancel_project":
					//controllo che il numero di argometi si acorretto
					if(parameters.length == 2) {
						try {
							this.cancelProject(parameters[0], parameters[1], client);
						} catch (IOException e) {e.printStackTrace();}
					}else  System.out.println("cancel_project [nome del progetto]");
					break;
					
				default:
					System.out.println("comando non disponibile");
				}
			}
		}
	}
	
	// ---------------------------------- HANDLERS DELLE RICHIESTE ---------------------------------- //
	
	/**
	 * permette al client di registrarsi al servizio WORTH tramite RMI
	 * 
	 * @param nickName: nome dell'utente da registrare
	 * @param psw: password associata all'utente
	 * @param remoteEventManager: riferimento all'event manager con il quale è possibile eseguire il metodo di register
	 * @throws NotBoundException
	 * @throws IOException
	 */
	private void register(String nickName, String psw, EventManagerInterface remoteEventManager) throws NotBoundException, IOException{
		//effettuo l'operazione in remoto
		if(remoteEventManager.register(nickName, psw)) System.out.println("< ok");
		else System.out.printf("< User %s già registrato\n", nickName);
		return;
	}
	
	/**
	 * permette al client di visualizzare i comandi disponibili per comunicare con il server worth
	 * 
	 * @throws IOException
	 */
	private void help() throws IOException {
		String path = "." + File.separator + "help.txt";
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			   String line;
			   while ((line = br.readLine()) != null) System.out.println(line);
		}
		return;
	}
	
	/**
	 * permette l'invio di un messaggio al server woth
	 * 
	 * @param msg: messaggio generato dal client che verrà recapitato dal server
	 * @param client: socketChannel del client
	 * @return risposta del server
	 * @throws IOException
	 */
	private String sendMessage(String msg,  SocketChannel client) throws IOException {
		// la prima parte del messaggio consiste nella sua lunghezza
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		length.putInt(msg.length());
		length.flip();
		client.write(length);
		length.clear();
		        
		//la seconda parte del messaggio sono i parametri su cui viene fatto il login
		ByteBuffer readBuffer = ByteBuffer.wrap(msg.trim().getBytes("UTF-8"));
		client.write(readBuffer);
		readBuffer.clear();
		        
		//ricezione della risposta da parte del server
		ByteBuffer reply = ByteBuffer.allocate(BUFFER_DIMENSION);
		client.read(reply);
		reply.flip();
		String serverAnsw = new String(reply.array(), "UTF-8");
		return serverAnsw;
	}
	
	/**
	 * 
	 * @param op: identificatore dell'operazione
	 * @param nickName: nome utente
	 * @param psw: password associata all'utente
	 * @param client: socketchannel del client
	 * @return true se il login va a buon fine, false altrimenti
	 * @throws IOException
	 */
	private boolean login(String op, String nickName, String psw,  SocketChannel client) throws IOException {
		String msg = op + " " + nickName + " " + psw;
		String serverAnsw = this.sendMessage(msg, client);
		
       //il login è stato effettuato, il server risponde con la lista di utenti registrati e il client crea la propria struttura dati
       if(!serverAnsw.contains("Errore")) {
        	String[] serverMsg = serverAnsw.split("\n");
        	String registered = serverMsg[1].trim();
       	 	String[] users = registered.split(" ");
       	 	for(String user: users) {
       	 		String[] data = user.split(";");
       	 		if(!this.map.containsKey(data[0]))this.map.put(data[0], data[1]);
       	 	}
       	 	System.out.println(serverMsg[0]);
       	 	return true;
        }
        //il login non è andato a buon fine, il server risponde con un messaggio di errore
        System.out.println(serverAnsw);
        return false;
	}
	
	/**
	 * permette di effettuare il logout dell'utente attualmente loggato
	 * 
	 * @param op: identificatore dell'operazione
	 * @param client: socketChannel del client
	 * @param callbackObj: oggetto per l'esecuzione dei metodi di notifica
	 * @throws IOException
	 */
	private void logout(String op, SocketChannel client, NotifyEventInterface callbackObj) throws IOException {
		String serverAnsw = this.sendMessage(op, client);
		UnicastRemoteObject.unexportObject(callbackObj, true);
		System.out.println(serverAnsw);
		client.close();
		return;
	}
	
	/**
	 * permette la visualizzazione degli utenti registrati al servizio WORTH
	 */
	private void listUser() {
		System.out.println("Utenti registrati al servizio:");
		for(String key : this.map.keySet()) System.out.println(key + " " + this.map.get(key));
		return;
	}
	
	/**
	 * permette la visualizzazione degli utenti
	 */
	private void listOnlineUsers(){
		System.out.println("Utenti ONLINE registrati al servizio:");
		for(String key : this.map.keySet()) {
			if (this.map.get(key).equals("Online")) System.out.println(key + " " + this.map.get(key));
		}
		return;
	}
	
	/**
	 * permette di visualizzare i progetti di cui il chiamante è membro
	 * 
	 * @param op: identificatore dell'operazione
	 * @param client: socketChannel del client
	 * @throws IOException
	 */
	private void listProjects(String op, SocketChannel client) throws IOException {
		String serverAnsw = this.sendMessage(op, client);
		System.out.println("lista dei progetti di cui sei membro:");
		System.out.println(serverAnsw);
		return;
	}
	
	/**
	 * permette di richiedere la creazione di un nuovo progetto
	 * 
	 * @param op: identificatore dell'operazione
	 * @param projectName: nome del progetto
	 * @param client: socketChannel del client
	 * @throws IOException
	 */
	private void createProject(String op, String projectName, SocketChannel client) throws IOException {
		String msg = op + " " + projectName;
		String serverAnsw = this.sendMessage(msg, client);
		System.out.println(serverAnsw);
		return;
	}
	
	/**
	 * permette di richiedere l'aggiunta di un membro ad un progettp
	 * 
	 * @param op: identificare dell'operazione
	 * @param projectName: nome del progetto
	 * @param userNick: nome dell'utente da aggiungere al progetto
	 * @param client: SocketChannel del client
	 * @throws IOException
	 */
	private void addMember(String op, String projectName, String userNick, SocketChannel client) throws IOException {
		String msg = op + " " + projectName + " " + userNick;
		String serverAnsw = this.sendMessage(msg, client);
		System.out.println(serverAnsw);
		return;
	}
	
	/**
	 * permette di richiedere la visualizzazione dei membri di un determinato progetto
	 * 
	 * @param op: identificatore dell'operazione
	 * @param projectName: nome del progetto di cui si vogliono vedere i membri
	 * @param client: SocketChannel del client
	 * @throws IOException
	 */
	private void showMembers(String op, String projectName, SocketChannel client) throws IOException {
		String msg = op + " " + projectName;
		String serverAnsw = this.sendMessage(msg, client);
		System.out.println(serverAnsw);
		return;
	}
	
	/**
	 * permette di richiedere la visualizzazione delle carte di un determinato progetto
	 * 
	 * @param op: identificatore dell'operazione
	 * @param projectName: nome del progetto di cui si vogliono visualizzare le carte
	 * @param client: SocketChannel del client
	 * @throws IOException
	 */
	private void showCards(String op, String projectName, SocketChannel client) throws IOException {
		String msg = op + " " + projectName;
		String serverAnsw = this.sendMessage(msg, client);
		System.out.println(serverAnsw);
		return;
	}
	
	/**
	 * permette di richiedere la visualizzazione di una determinata carta all'interno del progetto
	 * 
	 * @param op: identificatore dell'operazione
	 * @param projectName: nome del progetto nel quale è contenuta la carta
	 * @param cardName: nome della carta della quale si vogliono le informazioni
	 * @param client:  SocketChannel del client
	 * @throws IOException
	 */
	private void showCard(String op, String projectName, String cardName,SocketChannel client) throws IOException {
		String msg = op + " " + projectName + " " + cardName;
		String serverAnsw = this.sendMessage(msg, client);
		System.out.println(serverAnsw);
		return;
	}
	
	/**
	 * permette di richiedere l'aggiunta di una carta all'interno di un progetto
	 * 
	 * @param parameters: argomenti passati da linea di comendo
	 * @param client: SocketChannel del client
	 * @throws IOException
	 */
	private void addCard(String[] parameters, SocketChannel client) throws IOException {
		StringBuilder msg = new StringBuilder();
		
		msg.append(parameters[0] + " ");
		msg.append(parameters[1] + " ");
		msg.append(parameters[2] + " ");
		for(int i = 3; i < parameters.length; i++) {
			msg.append(parameters[i] + " ");
		}
		
		String serverAnsw = this.sendMessage(msg.toString().toString().trim(), client);
		System.out.println(serverAnsw);
		return;
	}
	
	/**
	 * permette di richiedere lo spostamento di una carta da una lista sorgente ad una di destinazione 
	 * 
	 * @param op: identificatore dell'operazione
	 * @param projectName: nome del progetto nel quale è contenuta la carta da spostare
	 * @param cardName: nome della carta che si desidera spostare
	 * @param src: lista sorgente
	 * @param dest: lista destinazione
	 * @param client: SocketChannel del client
	 * @throws IOException
	 */
	private void moveCard(String op, String projectName, String cardName, String src, String dest, SocketChannel client) throws IOException {
		
		//l'utente può anche scrivere i nomi delle liste in maiuscolo, la cosa verrà corretta dall'handler del clients
		String msg = op + " " + projectName + " " + cardName + " " + src.toUpperCase() + " " + dest.toUpperCase();
		String serverAnsw = this.sendMessage(msg, client);
		System.out.println(serverAnsw);
		return;
	}
	
	/**
	 * permette di richiedere la visualizzazione della storia di una carta
	 * 
	 * @param op: identificatore dell'operazione
	 * @param projectName: nome del progetto nel quale è presente la carta
	 * @param cardName: nome della carta della quale di vuole conoscere la storia
	 * @param client: SocketChannel del client
	 * @throws IOException
	 */
	private void getCardHistory (String op, String projectName, String cardName, SocketChannel client) throws IOException {
		String msg = op + " " + projectName + " " + cardName;
		String serverAnsw = this.sendMessage(msg, client);
		System.out.println(serverAnsw);
		return;
	}
	
	/**
	 * permette di unirsi ad una chat di un progetto
	 * 
	 * @param op: identificatore dell'operazione
	 * @param projectName: nome del progetto nel quale è presente la chat a cui l'utente si vuole unire
	 * @param client: SocketChannel del client
	 * @throws IOException
	 */
	private void joinChat(String op, String projectName, SocketChannel client) throws IOException {
		
		String msg = op + " " + projectName;
		String serverAnsw = this.sendMessage(msg, client);
		
		if(!serverAnsw.contains("Errore")) {
			
			String[] data = serverAnsw.trim().split(" ");
			
			//memorizzo le informazioni mandatemi dal server WORTH
			String ip = data[0];
	        int port = Integer.parseInt(data[1]);
	        String nickName = data[2];
	        
	        //se l'utente si è già unito alla chat
	        if(this.chats.containsKey(projectName) && this.chats.get(projectName).getState()) {
	        	System.out.println("< Errore. l'utente si è già unito a questa chat");
	        	return;
	        }
	        
	        Chat newChat = new Chat(nickName, ip, port);
	        new Thread(newChat).start();
	        chats.put(projectName, newChat);
	        System.out.println("< L'utente " + nickName + " si è unito alla chat del progetto " + projectName);
		}else System.out.println(serverAnsw);
	}
	
	/**
	 * permette all'untente di inviare un messaggio sulla chat del progetto
	 * 
	 * @param parameters: argomenti passati da input
	 */
	private void sendChatMsg(String[] parameters) {
		
		Chat chat = chats.get(parameters[1]); //nome del progetto
		if(chat == null) return;
		
		//controllo che la chat che sto provando a leggere sia ancora valida (il progetto potrebbe essere stato eliminato)
		if(!chat.getState()) {
			//se il progetto risulta eliminato, modifico la struttura dati
			chats.remove(parameters[1]);
			System.out.println("< Errore. questa chat è stata eliminata");
			return;
		}
		
		StringBuilder msg = new StringBuilder();
		for(int i = 2; i < parameters.length; i++) msg.append(parameters[i] + " ");
		chat.sendMsg(msg.toString().trim());
		System.out.println("< Messaggio inviato");
	}
	
	/**
	 * permette di leggere la chat di una chat associata ad un progetto
	 * 
	 * @param projectName: nome del progetto al quale è associata la chat che si desidera leggere
	 */
	private void readChat(String projectName) {
		
		Chat chat = chats.get(projectName);
		if(chat == null) return;
		
		//controllo che la chat che sto provando a leggere sia ancora valida (il progetto potrebbe essere stato eliminato)
		if(!chat.getState()) {
			//se il progetto risulta eliminato, modifico la struttura dati
			chats.remove(projectName);
			System.out.println("< Errore. questa chat è stata eliminata");
			return;
		}
		
		ArrayList<String> unreadMessages = new ArrayList<String>();
		unreadMessages = chat.getUnreadMessages();
		for(String msg : unreadMessages) System.out.println(msg);
		return;
	}
	
	/**
	 * permette di richiedere l'eliminazione di un progetto
	 * 
	 * @param op: identificatore dell'operazione
	 * @param projectName: nome del progetto da liminare
	 * @param client: SocketChannel del client
	 * @throws IOException
	 */
	private void cancelProject (String op, String projectName, SocketChannel client) throws IOException{
		String msg = op + " " + projectName;
		String serverAnsw = this.sendMessage(msg, client);
		System.out.println(serverAnsw);
		return;
	}
	
	/**
	 * permette di attivare il sistema di notifiche 
	 * 
	 * @param remoteEventManager: manager per la registrazione al sistema di notifiche
	 * @param stub: stub del client
	 */
	private void turnUpNotifications(EventManagerInterface remoteEventManager, NotifyEventInterface stub) {
		try {
			//effettuo l'operazione di registrazione al sistema di notifiche
			remoteEventManager.registerForCallback(stub);
		} catch (RemoteException e) {e.printStackTrace();}
		return;
	}
	
	/**
	 * permette di disattivare il sistema di notifiche
	 * 
	 * @param remoteEventManager: manager per la registrazione al sistema di notifiche
	 * @param stub: stub del client
	 */
	private void turnOffNotification(EventManagerInterface remoteEventManager, NotifyEventInterface stub) {
		try {
			//rimuovo la registrazione dal sistema di notifiche
			remoteEventManager.unRegisterForCallback(stub);
		} catch (RemoteException e) {e.printStackTrace();}
		return;
	}
}
