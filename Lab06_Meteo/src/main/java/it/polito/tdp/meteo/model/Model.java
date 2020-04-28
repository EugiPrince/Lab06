package it.polito.tdp.meteo.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import it.polito.tdp.meteo.DAO.MeteoDAO;

public class Model {
	
	private final static int COST = 100;
	private final static int NUMERO_GIORNI_CITTA_CONSECUTIVI_MIN = 3;
	private final static int NUMERO_GIORNI_CITTA_MAX = 6;
	private final static int NUMERO_GIORNI_TOTALI = 15;
	
	private MeteoDAO meteoDAO;
	private List<Rilevamento> rilevamentiGenova;
	private List<Rilevamento> rilevamentiMilano;
	private List<Rilevamento> rilevamentiTorino;
	
	private List<Citta> leCitta;
	private List<Citta> best;

	public Model() {
		this.meteoDAO = new MeteoDAO();
		this.leCitta = meteoDAO.getAllCitta();
	}
	
	public List<Citta> getLeCitta() {
		return leCitta;
	}
	
	private List<Rilevamento> rilGenova(int mese, String localita) {
		return this.meteoDAO.getAllRilevamentiLocalitaMese(mese, "Genova");
	}
	
	private List<Rilevamento> rilMilano(int mese, String localita) {
		return this.meteoDAO.getAllRilevamentiLocalitaMese(mese, "Milano");
	}
	
	private List<Rilevamento> rilTorino(int mese, String localita) {
		return this.meteoDAO.getAllRilevamentiLocalitaMese(mese, "Torino");
	}

	// of course you can change the String output with what you think works best
	public String getUmiditaMedia(int mese) {
		//SI POTEVA FARE TUTTO TRAMITE SQL COME NELLA SOLUZIONE ED ERA MOLTO MEGLIO
		
		String s ="";
		double media = 0.0;
		double somma = 0.0;
		
		this.rilevamentiGenova = this.rilGenova(mese, "Genova");
		this.rilevamentiMilano = this.rilMilano(mese, "Milano");
		this.rilevamentiTorino = this.rilTorino(mese, "Torino");
		
		for(Rilevamento r : rilevamentiGenova) {
			somma += r.getUmidita();
		}
		media = (double) (somma/rilevamentiGenova.size());
		s = s + "Media umidita Genova: " + media + "\n";
		
		for(Rilevamento r : rilevamentiMilano) {
			somma += r.getUmidita();
		}
		media = (double) (somma/rilevamentiMilano.size());
		s = s + "Media umidita Milano: " + media + "\n";
		
		for(Rilevamento r : rilevamentiTorino) {
			somma += r.getUmidita();
		}
		media = (double) (somma/rilevamentiTorino.size());
		s = s + "Media umidita Torino: " + media + "\n";
		
		return s;
	}
	
	// of course you can change the String output with what you think works best
	/**
	 * Calcola la sequenza ottimale di visita delle citta' nel mese specificato
	 * @param mese il mese da analizzare
	 * @return la lista di citta da visitare nei primi 15 giorni
	 */
	public List<Citta> trovaSequenza(int mese) {
		
		List<Citta> parziale = new ArrayList<>();
		this.best = null;
		
		MeteoDAO dao = new MeteoDAO();
		
		for(Citta c : leCitta)
			c.setRilevamenti(dao.getAllRilevamentiLocalitaMese(mese, c.getNome()));
		
		//inizia la ricorsione
		cerca(parziale, 0);
		return best;
	}
	
	/**
	 * Procedura ricorsiva per il calcolo della sequenza ottima di citta.
	 * @param parziale
	 * @param livello
	 */
	private void cerca(List<Citta> parziale, int livello) {
		//Caso terminale quando soluzione e' lunga 15 giorni, quando livello e' 15
		if(livello == NUMERO_GIORNI_TOTALI) {
			//Una soluzione l'abbiamo, pero' dobbiamo verificare se sia la migliore o no.. quindi:
			Double costo = calcolaCosto(parziale);
			
			//Se best e' vuota oopure e' meglio di quella che gia' c'e' allora aggiorniamo
			if(best==null || costo < calcolaCosto(best)) {
				best = new ArrayList<>(parziale);
			}
			return;
		}
		
		//Se siamo qua non e' caso terminale, dobbiamo costruire la soluzione
		
		for(Citta prova : leCitta) {
			if(aggiuntaValida(prova, parziale)) { //Se e' valida la citta di prova allora aggiungiamola
				//Controlliamo gia' qui se e' valida perche' cosi' scremiamo gia' un sacco di sequenze che iniziano
				//in modo sbagliato, se facessimo il controllo alla fine la ricorsione durerebbe troppo
				parziale.add(prova);
				cerca(parziale, livello+1);
				parziale.remove(parziale.size()-1);
			}
		}
	}

	private boolean aggiuntaValida(Citta prova, List<Citta> parziale) {
		//1) Quando parziale e' size = 0 e' sempre true, posso inserire qualsiasi citta
		//2)Quando la size e' 1 o 2, se vogliamo aggiungere una citta' possiamo farlo solo se la citta' che vogliamo
		//3)aggiungere e' gia' quella che c'e' per avere almeno 3 occorrenze di questa
		
		//Da qui la size e' sicuramente >=3, allora se mantengo la citta' -> true; se cambiamo citta dobbiamo vedere
		//che le ultime 3 citta siano uguali tra loro e poi aggiungiamo
		
		int conta = 0;
		
		for(Citta precedente : parziale) {
			if(precedente.equals(prova)) {
				conta++;
			}
		}
		
		//Sono gia' stato troppe volte in quella citta
		if(conta >= NUMERO_GIORNI_CITTA_MAX)
			return false;
		
		if(parziale.size()==0) //1
			return true;
		
		//Verifichiamo se l'ultimo elemento della lista e' uguale a quello che vorremmo mettere, se e' uguale dara' true
		if(parziale.size() < NUMERO_GIORNI_CITTA_CONSECUTIVI_MIN) //2, 3
			return parziale.get(parziale.size()-1).equals(prova);
		
		//Se non vogliamo cambiare citta' sicuramente true
		if(parziale.get(parziale.size()-1).equals(prova))
			return true;
		
		//Controlliamo gli ultimi giorni.. cioe' se voglio cambiare citta' devo verificare che i giorni precedenti
		//io sia rimasto nella stessa citta'
		
		//Quindi se non e' vero che la citta in quei giorni e' la stessa, allora non va bene e quindi -> false, perche'
		//non sono statoo abbastanza giorni in una citta e non posso ancora cambiare
		for(int i=0; i<NUMERO_GIORNI_CITTA_CONSECUTIVI_MIN-1; i++) {
			if(!parziale.get(parziale.size()-(i+1)).equals(parziale.get(parziale.size()-(i+2)))) {
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Calcola il costo di una determinata soluzione (totale)
	 * @param parziale la soluzione (totale) proposta
	 * @return valore del costo
	 */
	private Double calcolaCosto(List<Citta> parziale) {
		Double costo = 0.0;
		
		for(int giorno=1; giorno <= NUMERO_GIORNI_TOTALI; giorno++) {
			Citta c = parziale.get(giorno-1);
			double umid = c.getRilevamenti().get(giorno-1).getUmidita(); //umidita' per una data citta'
			
			costo += umid;
		}
		
		for(int giorno=2; giorno <= NUMERO_GIORNI_TOTALI; giorno++) {
			//Se il giorno -1 ha una citta diversa da quella in -2 devo mettere la cost in costo
			if(!parziale.get(giorno-1).equals(parziale.get(giorno-2))) {
				costo += COST;
			}
		
		}
		return costo;
	}
	
}
