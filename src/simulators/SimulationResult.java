package simulators;

import dao.Card;
import main.Settings;
import utils.ArrayUtils;

public class SimulationResult {
	
	//TODO: Setting also accessed elsewhere
	int time_bin_length = Settings.time_bin_length();
	int t_max;
	
	// Forecasted number of reviews
    //   0 = Learn
    //   1 = Young
    //   2 = Mature
    //   3 = Relearn
	int[][] n_reviews;

    // Forecasted number of cards per state
    //   0 = New
    //   1 = Young
    //   2 = Mature
	int[][] n_in_state;
	
	public SimulationResult(int n_time_bins) {
	    n_reviews = ArrayUtils.createIntMatrix(4, n_time_bins);
	    n_in_state = ArrayUtils.createIntMatrix(3, n_time_bins);
	    
	    t_max = n_time_bins * time_bin_length;
	}
	
	public SimulationResult(int[][] n_reviews, int[][] n_in_state) {
		this.n_reviews = n_reviews;
		this.n_in_state = n_in_state;
	}
	
	public int[][] getNReviews() {
		return n_reviews;
	}

	public int[][] getNInState() {
		return n_in_state;
	}
	
	public int nReviewsDoneToday(int t_elapsed) {
		//This excludes new cards and relearns
		return n_reviews[1][t_elapsed / time_bin_length] +
			   n_reviews[2][t_elapsed / time_bin_length];
	}
	
	public void incrementNInState(int cardType, int t) {
		n_in_state[cardType][t]++;
	}
	
	public void incrementNReviews(int cardType, int t) {
		n_reviews[cardType][t]++;
	}
	
	public void updateNInState(Card card, int t_from, int t_to) {
		for(int t = t_from / time_bin_length; t < t_to / time_bin_length; t++)
        	if(t < t_max) {
        		n_in_state[card.getC_type()][t]++;
        	}
	}
}
