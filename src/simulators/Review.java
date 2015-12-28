package simulators;

import java.util.Stack;

import dao.Card;
import main.Settings;

public class Review {
	
	int max_reviews_per_day = Settings.max_reviews_per_day();
	
	// Time bins for "Month" view. Change for "Year" or "Deck Life"
    //int n_time_bins = 30;
	//TODO: also exists in Review
	int n_time_bins = Settings.n_time_bins();
    int time_bin_length = Settings.time_bin_length();
	
    int t_max = n_time_bins * time_bin_length;
	
	int t_elapsed;
	Card card;
	SimulationResult simulationResult;
	EaseClassifier classifier;
	Stack<Review> reviews;
	
	public Review (Card card, SimulationResult simulationResult, EaseClassifier classifier, Stack<Review> reviews, int t_elapsed) {
		this.card = card;
		this.simulationResult = simulationResult;
		this.classifier = classifier;
		this.reviews = reviews;
		
		this.t_elapsed = t_elapsed;
	}
	
	public Review (Card card, SimulationResult simulationResult, NewCardSimulator newCardSimulator, EaseClassifier classifier, Stack<Review> reviews) {
		this.card = card;
		this.simulationResult = simulationResult;
		this.classifier = classifier;
		this.reviews = reviews;
		
		//# Rate-limit new cards by shifting starting time
        if (card.getC_type() == 0)
        	t_elapsed = newCardSimulator.simulateNewCard(card);
        else
        	t_elapsed = card.getC_due();
        
        // Set state of card between start and first review
        this.simulationResult.updateNInState(card, 0, t_elapsed);
	}

	public void simulateReview() {
		//Set state of card for current review
    	simulationResult.incrementNInState(card.getC_type(), t_elapsed / time_bin_length);
        
		if(card.getC_type() == 0 || simulationResult.nReviewsDoneToday(t_elapsed) < max_reviews_per_day) {
            // Update the forecasted number of reviews
			simulationResult.incrementNReviews(card.getC_type(), t_elapsed / time_bin_length);
            
            // Simulate response
            card = classifier.sim_single_review(card, true);
            
            // If card failed, update "relearn" count
            if(card.getCorrect() == 0)
            	simulationResult.incrementNReviews(3, t_elapsed / time_bin_length);
            
            // Set state of card between current and next review
            simulationResult.updateNInState(card, t_elapsed + 1, t_elapsed + card.getC_ivl());
            
            // Advance time to next review
            t_elapsed += card.getC_ivl();
		}
        else {
        	// Advance time to next review (max. #reviews reached for this day)
            t_elapsed += 1;
        }
		
		if (t_elapsed < t_max) {
			Review review = new Review(card, simulationResult, classifier, reviews, t_elapsed);
			this.reviews.push(review);
		}
	}
	
	public int getT() {
		return t_elapsed;
	}
	
}
