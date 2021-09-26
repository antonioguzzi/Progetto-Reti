package progetto_2020_2021;

/**
 * MainClass del client
 * 
 * @author Antonio Guzzi
 */

public class MainClassClient {
	
	//porta per l'esecuzione della registrazione tramite RMI
	private static final int PORT_DEFAULT_RMI = 5000; 
	
	//porta per la connessione TCP con il server
	private static final int PORT_DEFAULT_TCP = 5001; 
	
	public static void main (String[] args) {
		int port1 = PORT_DEFAULT_RMI;
		int port2 = PORT_DEFAULT_TCP;
		
		Client clientWorth = new Client(port1, port2);
		clientWorth.start();
	}
}
