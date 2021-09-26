package progetto_2020_2021;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * la classe Card modella una carta all'interno del progetto
 * 
 * @author Antonio Guzzi
 */
public class Card {
	private String cardName;
	private String description;
	public ArrayList<String> history;
	
	
	// ---------------------------------- METODI COSTRUTTORE ---------------------------------- //
	
	/**
	 * @param name: nome della carta
	 * @param description: descrizione della carta
	 * @throws IllegalArgumentException se uno dei due paramentri risulta essere uguale a null
	 */
	public Card(String name, String description) throws IllegalArgumentException {
		
		if(name == null) throw new IllegalArgumentException("nome null");
		if( description == null) throw new IllegalArgumentException("descrizione null");
		
		this.cardName = name;
		this.description = description;
		this.history = new ArrayList<String>();
	}
	
	public Card() {
		
	}
	
	// ---------------------------------- METODI GET ---------------------------------- //
	
	/**
	 * @return nome della carta 
	 */
	public String getCardName() {
		return this.cardName;
	}
	
	/**
	 * @return descrizione della carta
	 */
	public String getDescription() {
		return this.description;
	}
	
	/**
	 * @return nomi delle liste visitate dalla carta
	 */
	public ArrayList<String> getStory() {
		return this.history;
	}
	
	/**
	 * @return la storia della carta sotto forma di stringa
	 */
	@JsonIgnore
	public String getHistory() {
		StringBuilder str = new StringBuilder();
		for(String list : this.history) str.append(list + " ");
		return str.toString().trim();
	}
	
	/**
	 * @return nome della lista corrente nel quale si trova la carta
	 */
	@JsonIgnore
	public String getCurrentState() {
		return this.history.get(this.history.size()-1);
	}
	
	// ---------------------------------- METODI SET ---------------------------------- //
	
	/**
	 * permette di assegnare una storia a questa carta
	 * 
	 * @param history: array di stringhe contenente la storia che si desidera attribuire alla carta
	 */
	public void setStory(ArrayList<String> history) {
		this.history = history;
	}
	
	// ---------------------------------- METODI DI APPOGGIO ---------------------------------- //
	
	/**
	 * permette di aggiungere alla storia della carta una lista visitata
	 * 
	 * @param listName: nome della lista che si desidera aggiungere alla storia della carta
	 */
	public void addToHistory(String listName) {
		this.history.add(listName);
		return;
	}
}
