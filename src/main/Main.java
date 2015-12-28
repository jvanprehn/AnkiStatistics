package main;
import java.sql.Connection;

import chart.NInStateChartWindow;
import chart.NReviewsChartWindow;
import indexmappers.IndexToLabelMapper;
import indexmappers.IndexToLabelMapperImpDefault;
import simulators.EaseClassifier;
import simulators.ReviewSimulator;
import simulators.SimulationResult;
import utils.DBUtils;

public class Main {
	
	public static void main(String[] args) throws Exception {
	    
		Connection c = DBUtils.openDBConnection();
		
		EaseClassifier classifier = new EaseClassifier(c);
		ReviewSimulator reviewSimulator = new ReviewSimulator(c, classifier);
		
		SimulationResult simulationResult = reviewSimulator.sim_n_reviews();
		IndexToLabelMapper indexToLabelMapperDefault = new IndexToLabelMapperImpDefault();
		
		int[][] n_reviews = simulationResult.getNReviews();
		int[][] n_in_state = simulationResult.getNInState();
	    
	    NReviewsChartWindow chart = new NReviewsChartWindow("Predicted # reviews", "Predicted # reviews", n_reviews);
	    NInStateChartWindow chartNInState = new NInStateChartWindow("Predicted # cards in state", "Predicted # cards in state", n_in_state, indexToLabelMapperDefault);   
	    
	    c.close();
	}
}