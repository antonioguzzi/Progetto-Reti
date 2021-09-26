package progetto_2020_2021;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

/**
 * Project è la classe che modella un progetto
 * 
 * @author Antonio Guzzi
 */
public class Project {
	
	private String projectName;
	private ArrayList<Card> TODO;
	private ArrayList<Card> INPROGRESS;
	private ArrayList<Card> TOBEREVISED;
	private ArrayList<Card> DONE;
	private ArrayList<String> pojectUsers;
	private String chatIP;
	private int port;
	private DatagramSocket datagramSocket;
	
	// ---------------------------------- METODO COSTRUTTORE ---------------------------------- //
	
	/**
	 * @param projectName: nome da assegnare al progetto
	 * @throws IllegalArgumentException se il nome del progetto è nullo
	 */
	public Project (String projectName) throws IllegalArgumentException{
		if(projectName == null) throw new IllegalArgumentException("projectName null");
		this.projectName = projectName;
		this.TODO = new ArrayList<Card>();
		this.DONE = new ArrayList<Card>();
		this.INPROGRESS = new ArrayList<Card>();
		this.TOBEREVISED = new ArrayList<Card>();
		this.pojectUsers = new ArrayList<String>();
		
		try{
            this.datagramSocket = new DatagramSocket();
        }catch(IOException e){e.printStackTrace();}
	}

	// ---------------------------------- METODI GET ---------------------------------- //
    
	/**
	 * @return porta associata al progetto e alla sua chat
	 */
	public int getPort(){
        return port;
    }
    
	/**
	 * @return indirizzo IP associato al progetto e alla sua chat
	 */
    public String getIP(){
		 return chatIP;
	}
    
    /**
     * @return il nome del progetto 
     */
    public String getProjectName() {
		return this.projectName;
	}
	
    /**
     * @return i membri associati al progetto
     */
    public ArrayList<String> getMembers() {
		return this.pojectUsers;
	}
	
    /**
     * ritorna la lista di carte specificata da cardListName
     * 
     * @param cardListName: nome della lista 
     * @return lista di carte
     */
    public ArrayList<Card> getList(String cardListName){
		switch(cardListName) {
		case "TODO":
			return this.TODO;
		case "INPROGRESS":
			return this.INPROGRESS;
		case "TOBEREVISED":
			return this.TOBEREVISED;
		case "DONE":
			return this.DONE;
		}
		return null;
	}
	
    // ---------------------------------- METODI SET ---------------------------------- //
    
    /**
     * permette di settare l'indirizzo IP del progetto e della chat a lui associata
     * 
     * @param ip: indirizzo IP che si desidera associare al progetto e alla sua chat
     */
    public void setIP(String ip){
		 chatIP = ip;
	}
	
    /**
     * permette di settare la porta del progetto e della chat a lui associata
     * 
     * @param p: porta che si desideta associare al progetto e alla sua chat
     */
    public void setPort(int p){
        port = p;
    }
	
    /**
     * permette di settare la lista di utenti membri del progetto
     * 
     * @param projectUsers
     */
	public void setMembers(ArrayList<String> projectUsers) {
		this.pojectUsers = projectUsers;
	}
	
	// ---------------------------------- METODI DI APPOGGIO ---------------------------------- //
	
	/**
	 * permette l'aggiunta di un membro all'interno della lista dei membri del progetto
	 * 
	 * @param member: nuovo membro da aggiungere
	 * @return true se l'aggiunta va a buon fine, false altrimenti
	 */
	public boolean addMember(User member) {
		for(String tmp : this.pojectUsers) {
			if(tmp.equals(member.getNickName())) return false;
		}
		this.pojectUsers.add(member.getNickName());
		return true;
	}
	
	/**
	 * permette di visualizzare i membri del progetto
	 * 
	 * @return stringa contenente la lista di membri del progetto
	 */
	public String showMembers() {
		StringBuilder str = new StringBuilder();
		for(String tmp : this.pojectUsers) str.append(tmp + " ");
		return str.toString();
	}
	
	/**
	 * permette di visualizzare le carte del progetto
	 * 
	 * @return stringa contenente le liste e le carte al loro intero
	 */
	public String showCards() {
		
		StringBuilder str = new StringBuilder();
		str.append("TODO:\n");
		for(Card projectCard : this.TODO) {
			str.append(projectCard.getCardName() + "\n");
		}
		str.append("INPROGRESS:\n");
		for(Card projectCard : this.INPROGRESS) {
			str.append(projectCard.getCardName() + "\n");
		}
		str.append("TOBEREVISED:\n");
		for(Card projectCard : this.TOBEREVISED) {
			str.append(projectCard.getCardName() + "\n");
		}
		str.append("DONE:\n");
		for(Card projectCard : this.DONE) {
			str.append(projectCard.getCardName() + "\n");
		}
		return str.toString();
	}
	
	/**
	 * permette di cercare una carta all'interno del progetto
	 * 
	 * @param cardName: nome della carta che si desidera cercare
	 * @return l'oggetto associato al nome della carta se quest'ultima viene trovata, null altrimenti
	 */
	public Card searchCardInProject(String cardName) {
		
		for(int i = 0; i < this.TODO.size(); i++) {
			if(this.TODO.get(i).getCardName().equals(cardName)) return this.TODO.get(i);
		}
		for(int i = 0; i < this.INPROGRESS.size(); i++) {
			if(this.INPROGRESS.get(i).getCardName().equals(cardName)) return this.INPROGRESS.get(i);
		}
		for(int i = 0; i < this.TOBEREVISED.size(); i++) {
			if(this.TOBEREVISED.get(i).getCardName().equals(cardName)) return this.TOBEREVISED.get(i);
		}
		for(int i = 0; i < this.DONE.size(); i++) {
			if(this.DONE.get(i).getCardName().equals(cardName)) return this.DONE.get(i);
		}
		return null;
	}
	
	/**
	 * permette la ricerca di una carta all'interno di una lista specifica
	 * 
	 * @param cardList: nome della lista nella quale si desidera cercare la carta
	 * @param cardName: nome della carta da cercare
	 * @return l'oggetto associato al nome della carta se quest'ultima viene trovata, null altrimenti
	 */
	public Card searchCard(ArrayList<Card> cardList, String cardName) {
		for(int i = 0; i < cardList.size(); i++) {
			if(cardList.get(i).getCardName().equals(cardName)) return cardList.get(i);
		}
		return null;
	}
	
	/**
	 * permette la rimozione di una carta all'interno di una lista specifica
	 * 
	 * @param cardList: nome della lista nella quale si vuole rimuovere la carta
	 * @param cardName: nome della carta da rimuovere
	 * @return true se la rimozione va a buon fine, false altrimenti
	 */
	public boolean removeCard(ArrayList<Card> cardList, String cardName) {
		for(int i = 0; i < cardList.size(); i++) {
			if(cardList.get(i).getCardName().equals(cardName)) {
				cardList.remove(i);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * permette di visualizzare le informazioni di una determinata carta
	 * 
	 * @param cardName: nome della carta della quale si vogliono conoscere le informazioni
	 * @return informazioni della carta sottoforma di stringa
	 */
	public String showCard(String cardName) {
		
		StringBuilder str = new StringBuilder();
		Card projectCard = this.searchCardInProject(cardName);
		
		//se la carta non è presente nel progetto
		if(projectCard == null) {
			str.append("Errore. carta non presente nel progetto " + this.projectName);
			return str.toString().trim();
		}
		
		str.append("nome: " + projectCard.getCardName() + "\n");
		str.append("descrizione: " + projectCard.getDescription() + "\n");
		str.append("stato corrente: " + projectCard.getCurrentState());
		
		return str.toString();
	}
	
	/**
	 * permette di muovere una carta da una lista sorgente ad una di destinazione
	 * 
	 * @param cardName: nome della carta da spostare
	 * @param src: lista sorgente
	 * @param dst: lista destinazione
	 * @return la carta appena spostata se l'operazione di move ha avuto successo, null altrimenti
	 */
	public Card moveCard(String cardName, String src, String dst) {
		
		if((src.equals("TODO") && dst.equals("INPROGRESS"))||(src.equals("INPROGRESS") && dst.equals("TOBEREVISED")) || (src.equals("INPROGRESS") && dst.equals("DONE")) || (src.equals("TOBEREVISED") && dst.equals("INPROGRESS"))) {
			Card tmp = this.searchCard(this.getList(src), cardName);
			
			//se la carta è presente nella lista sorgente
			if(tmp != null) {
				
				//rimuovo la carta dalla lista sorgente 
				if(!this.removeCard(this.getList(src), tmp.getCardName())) return null;
				
				//aggiungo al suo storico la nuova lista in cui vado ad inserirla
				tmp.addToHistory(dst);
				ArrayList<Card> dest = this.getList(dst);
				if(dest != null) {
					dest.add(tmp);
					return tmp;
				}else return null;
			}
		}
		return null;
	}
	
	/**
	 * permette l'aggiunta di una nuova carta al progetto
	 * 
	 * @param newCard: carta da aggiungere
	 * @return true se l'aggiunta va a buon fine, false altrimenti
	 */
	public boolean addCardToProject(Card newCard) {
		if(this.searchCardInProject(newCard.getCardName()) == null) {
			newCard.addToHistory("TODO");
			this.TODO.add(newCard);
			return true;
		}
		return false;
	}
	
	/**
	 * permette l'aggiunta di una carta in una lista specifica
	 * 
	 * @param cardListName: nome della lista nella quale si vuole aggiungere la carta
	 * @param newCard: carta da aggiungere
	 * @return true se l'aggiunta va a buon fine, false altrimenti
	 */
	public boolean addCardToList(String cardListName, Card newCard) {
		switch(cardListName) {
		case "TODO":
			if(this.searchCard(this.TODO, newCard.getCardName()) == null) {
				this.TODO.add(newCard);
				return true;
			}
			break;
		case "INPROGRESS":
			if(this.searchCard(this.INPROGRESS, newCard.getCardName()) == null) {
				this.INPROGRESS.add(newCard);
				return true;
			}
			break;
		case "TOBEREVISED":
			if(this.searchCard(this.TOBEREVISED, newCard.getCardName()) == null) {
				this.TOBEREVISED.add(newCard);
				return true;
			}
			break;
		case "DONE":
			if(this.searchCard(this.DONE, newCard.getCardName()) == null) {
				this.DONE.add(newCard);
				return true;
			}
			break;
		}
		return false;
	}
	
	/**
	 * permette di inviare un messaggio sulla chat associata al progetto
	 * 
	 * @param msg: messaggio da inviare sulla chat del progetto
	 */
	public void sendMessage(String msg){
		String message = "Server WORTH: "+ msg;
		byte[] buffer = message.getBytes();
		try{
				//preparazione del datagramma da inviare contenente il messaggio passato al metodo
				DatagramPacket datagram = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.chatIP), this.port);
				this.datagramSocket.send(datagram);
	        }catch(Exception e){e.printStackTrace();}
	}
}
