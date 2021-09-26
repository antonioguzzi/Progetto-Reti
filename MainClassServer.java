package progetto_2020_2021;

/**
 * MainClass del server WORTH
 * 
 * @author Antonio Guzzi
 */

public class MainClassServer {
	
	//porta per l'esecuzione della registrazione tramite RMI
	private static final int PORT_DEFAULT_RMI = 5000; 
	
	//porta per la connessione TCP con il server
	private static final int PORT_DEFAULT_TCP = 5001;
	
	//porta per la connessione UDP per la chat
	private static final int PORT_DEFAULT_UDP = 5002;
	
	public static void main(String[] args){
		int port1 = PORT_DEFAULT_RMI;
		int port2 = PORT_DEFAULT_TCP;
		int port3 = PORT_DEFAULT_UDP;
		
		Server serverWorth = new Server(port1,port2, port3);
		serverWorth.registerUser();
		serverWorth.start();
	}
}
