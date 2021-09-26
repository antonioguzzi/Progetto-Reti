package progetto_2020_2021;

import java.net.InetAddress;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * User è la classe che modella un utente
 * 
 * @author Antonio Guzzi
 */
public class User{
	private String nickName;
	private String psw;
	
	@JsonIgnore
	private String state;
	@JsonIgnore
	private InetAddress ip;
	@JsonIgnore
	private int port;
	
	
	// ---------------------------------- METODI COSTRUTTORE ---------------------------------- //
	
	/**
	 * @param nickName: nome dell'utente
	 * @param psw: passworod associata all'utente
	 * @param state: stringa che esprime lo stato Online/Offline dell'utente
	 * @throws IllegalArgumentException se il nome utente, la password o lo stato sono nulli
	 */
	public User(String nickName, String psw, String state) throws IllegalArgumentException{
		if(nickName == null) throw new IllegalArgumentException("nickName null");
		if(psw == null) throw new IllegalArgumentException("psw null");
		if(state == null) throw new IllegalArgumentException("state null");
		this.nickName = nickName;
		this.psw = psw;
		this.state = state;
		this.ip = null;
	}
	
	public User() {
		
	}
	
	// ---------------------------------- METODI GET ---------------------------------- //
	
	/**
	 * @return il nome associato all'utente
	 */
	public String getNickName () {
		return this.nickName;
	}
	
	/**
	 * @return lo stato dell'utente
	 */
	public String getState() {
		return this.state;
	}
	
	/**
	 * @return la password associata all'utente
	 */
	public String getPsw() { 
		return this.psw;
	}
	
	/**
	 * @return l'indirizzo IP associato allo user
	 */
	public InetAddress getIp() {
		return this.ip;
	}
	
	/**
	 * @return la porta associata allo user
	 */
	public int getPort() {
		return this.port;
	}
	
	// ---------------------------------- METODI SET ---------------------------------- //
	
	/**
	 * permette di settare lo stato dell'utente
	 * 
	 * @param newState: stato da associare all'utente
	 */
	public void setState(String newState) {
		this.state = newState;
	}
	
	/**
	 * permette di settare l'IP dell'utente
	 * 
	 * @param ip: indirizzo IP da associare all'utente
	 */
	public void setIp(InetAddress ip) {
		this.ip = ip;
	}
	
	/**
	 * permette di associare una porta all'utente
	 * 
	 * @param port: prota da associare all'utente
	 */
	public void setPort(int port) {
		this.port = port;
	}
}
