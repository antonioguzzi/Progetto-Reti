package progetto_2020_2021;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * la classe Chat modella una chat associata al progetto
 * 
 * @author Antonio Guzzi
 */
public class Chat implements Runnable {
	
	private final static int DATAGRAM_DIMENSION = 1024;
	private String nickName;                    
    private String ip;
    private int port;
    private InetAddress multicastAddress; 
    private AtomicBoolean listening;                
    private ArrayList<String> unreadMessages; 
    private MulticastSocket group;
    private DatagramSocket datagramSocket;
	
    
    // ---------------------------------- METODO COSTRUTTORE ---------------------------------- //
    
    /**
     * @param nickName: nickName del partecipante alla chat
     * @param ip: indirizzo IP del partecipante alla chat
     * @param port: porta utilizzata dalla chat per la comunicazione
     * @throws IllegalArgumentException se il nickName dell'utente o il suo indizzo ip sono nulli
     */
    public Chat(String nickName, String ip, int port) throws IllegalArgumentException{
    	
    	if(nickName == null) throw new IllegalArgumentException("nickName null");
    	if(ip == null) throw new IllegalArgumentException("ip null");
    	
    	this.nickName = nickName;
    	this.ip = ip;
    	this.port = port;
    	//come la lista dei messaggi non letti, anche la variabile che permette di capire lo stato della chat è modificabile solo tramite
    	//operazioni atomiche, questo per via del numero di utenti che possono accedere contemporaneamente alla chat
    	this.listening = new AtomicBoolean(); 
    	this.listening.set(false);
    	this.unreadMessages = new ArrayList<String>();
    	
    	try {
			this.multicastAddress = InetAddress.getByName(this.ip);
			this.group = new MulticastSocket(this.port);
			this.datagramSocket = new DatagramSocket();
			this.group.joinGroup(multicastAddress);
		} catch (IOException e) {e.printStackTrace();}
    }
    
    // ---------------------------------- METODI GET ---------------------------------- //
    
    /**
     * @return lo stato della chat, il quale permette di capire se la chat è stata eliminata o meno
     */
    public boolean getState() {
    	return this.listening.get();
    }
    
    /**
     * @return i messaggi non letti associati a questa chat
     */
    public ArrayList<String> getUnreadMessages(){
    	ArrayList<String> tmp = new ArrayList<String>();
    	
    	//la lista dei messaggi viene utilizzata attraverso un blocco synchronized dato che viene acceduta contemporaneamente da più utenti
    	synchronized(unreadMessages){
    		tmp.addAll(this.unreadMessages);
    		this.unreadMessages.clear();
    	}
    	tmp.add("messaggi terminati");
    	return tmp;
    }
    
    // ---------------------------------- METODO RUN ---------------------------------- //
    @Override
	public void run() {
		this.listening.set(true);
		
		//fino a quando lo stato della chat è valido
		while(listening.get()) {
			try {
				//preparo il datagramma da ricevere
				DatagramPacket datagram = new DatagramPacket(new byte[DATAGRAM_DIMENSION],DATAGRAM_DIMENSION);
				this.group.receive(datagram);
				//estraggo il messaggio dal datagramma
				String msg = new String(datagram.getData(), "UTF-8");
				//se il messaggio proviene dal server ed è una close
				if(msg.contains("Server WORTH: close")) this.listening.set(false);
				else {
					//accedo in mutua esclusione alla lista di messaggi non letti
					 synchronized(unreadMessages){       
						 unreadMessages.add(msg.trim());
		             }  
				}
			} catch (IOException e) {}
		}
	}
    
    // ---------------------------------- METODI DI APPOGGIO ---------------------------------- //
    
    /**
     * permette di inviare sulla chat del progetto un messaggio 
     * 
     * @param msg: messaggio da inviare sulla chat
     */
    public void sendMsg(String msg) {
    	
    	String send = this.nickName + ": " + msg;
    	byte[] buffer = send.getBytes();
    	try {
    		 DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.multicastAddress, this.port);
             this.datagramSocket.send(packet);
    	}catch (IOException e) {e.printStackTrace();}
    }
    
    /**
     * permette la chiusura della socketMulticast
     */
    public void close() {
    	this.group.close();
    	this.listening.set(false);
    }
}
