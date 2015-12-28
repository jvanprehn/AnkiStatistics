package simulators;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Stack;

import dao.Card;
import dao.Deck;
import main.Settings;
import utils.ArrayUtils;
import utils.Utils;

public class ReviewSimulator {
	
	Connection c;
	EaseClassifier classifier;
	
	//TODO: also exists in Review
	int n_time_bins = Settings.n_time_bins();
    int time_bin_length = Settings.time_bin_length();
	
    int t_max = n_time_bins * time_bin_length;
    
    NewCardSimulator newCardSimulator = new NewCardSimulator();
	
	public ReviewSimulator(Connection c, EaseClassifier classifier) throws ClassNotFoundException, SQLException {
		this.c = c;
		this.classifier = classifier;
	}
	
	public SimulationResult sim_n_reviews() throws ClassNotFoundException, SQLException {
		
		SimulationResult simulationResult = new SimulationResult(n_time_bins);
		
		//n_smooth=1
		
		//TODO:
		//Forecasted final state of deck
		//final_ivl = np.empty((n_smooth, n_cards), dtype='f8')
	    
	    int currentTime = (int) (System.currentTimeMillis() / 1000);
	    
	    int today = (int) ((currentTime - Utils.deckCreationTimeStamp()) / Utils.secsPerDay());
	    int mDayCutoff = Utils.deckCreationTimeStamp() + ((today + 1) * Utils.secsPerDay());
	    
	    System.out.println("today: " + today);
	    System.out.println("todayCutoff: " + mDayCutoff);
	    
	    List<Card> deck = Deck.deckFromDB(c, today);
	    
	    Stack<Review> reviews = new Stack<Review>();
	    
	    //TODO: by having simulateReview add future reviews depending on which simulation of this card this is (the nth) we can:
	    //1. Do monte carlo simulation if we add k future reviews if n = 1
	    //2. Do a complete traversal of the future reviews tree if we add k future reviews for all n
	    //3. Do any combination of these
	    for(Card card: deck) {
	    	
	    	Review review = new Review(card, simulationResult, newCardSimulator, classifier, reviews);
	    	
	    	if(review.getT() < t_max)
	    		reviews.push(review);
	    	
	    	while(!reviews.isEmpty()) {
	    		review = reviews.pop();
	    		review.simulateReview();
	    	}
	    	
	    }
	    
	    ArrayUtils.formatMatrix(simulationResult.getNReviews(), System.out, "%02d ");
	    ArrayUtils.formatMatrix(simulationResult.getNInState(), System.out, "%02d ");
	    
	    return simulationResult;
	}
}
