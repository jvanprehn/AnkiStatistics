package simulators;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Random;

import dao.Card;

public class EaseClassifier {
	
		Random random;
	
		Connection connection;
		double[][] probabilitiesCumulative;
		
		//# Prior that half of new cards are answered correctly
		int[] prior_new = {5, 0, 5, 0};		//half of new cards are answered correctly
		int[] prior_young = {1, 0, 9, 0};	//90% of young cards get "good" response
		int[] prior_mature = {1, 0, 9, 0};	//90% of mature cards get "good" response
		
		String query_base_new =
				"select " 
			  +   "count() as N, " 
			  +   "sum(case when ease=1 then 1 else 0 end) as repeat, "
			  +   "0 as hard, "	//Doesn't occur in query_new
			  +	  "sum(case when ease=2 then 1 else 0 end) as good, "
			  +	  "sum(case when ease=3 then 1 else 0 end) as easy "
			  + "from revlog ";
		
		String query_base_young_mature =
			"select " 
		  +   "count() as N, " 
		  +   "sum(case when ease=1 then 1 else 0 end) as repeat, "
		  +   "sum(case when ease=2 then 1 else 0 end) as hard, "	//Doesn't occur in query_new
		  +	  "sum(case when ease=3 then 1 else 0 end) as good, "
		  +	  "sum(case when ease=4 then 1 else 0 end) as easy "
		  + "from revlog ";

		String query_new =
				query_base_new
		  + "where type=0;"; 
		
		String query_young = 
				query_base_young_mature
		  + "where type=1 and lastIvl < 21;";
		    
		String query_mature = 
				query_base_young_mature
		  + "where type=1 and lastIvl >= 21;";
		
		public EaseClassifier(Connection connection) throws SQLException {
			this.connection = connection;
			this.probabilitiesCumulative = calculateCumProbabilitiesForNewEasePerCurrentEase();
			
			System.out.println("new\t" + Arrays.toString(this.probabilitiesCumulative[0]));
			System.out.println("young\t" + Arrays.toString(this.probabilitiesCumulative[1]));
			System.out.println("mature\t" + Arrays.toString(this.probabilitiesCumulative[2]));
			
			random = new Random();
		}
		
		private double[] cumsum(double[] p) throws SQLException {
			
			p[1] = p[0] + p[1];
			p[2] = p[1] + p[2];
			p[3] = p[2] + p[3];
			
			return p;
		}
		
		private double[][] calculateCumProbabilitiesForNewEasePerCurrentEase() throws SQLException {
			double[][] p = new double[3][];
			
			p[0] = cumsum(calculateProbabilitiesForNewEaseForCurrentEase(query_new, prior_new));
			p[1] = cumsum(calculateProbabilitiesForNewEaseForCurrentEase(query_young, prior_young));
			p[2] = cumsum(calculateProbabilitiesForNewEaseForCurrentEase(query_mature, prior_mature));
			
			return p;
		}

		private double[] calculateProbabilitiesForNewEaseForCurrentEase(String queryNewEaseCountForCurrentEase, int[] prior) throws SQLException {
		    Statement stmt = connection.createStatement();
		    ResultSet rs = stmt.executeQuery(queryNewEaseCountForCurrentEase);
		    rs.next();
		    
		    int n_query = rs.getInt("N");
		    int n_prior = prior[0] + prior[1] + prior[2] + prior[3];
		    
		    int n = n_query + n_prior;
		    
		    int[] freqs = new int[] {
		    	rs.getInt("repeat") + prior[0],
				rs.getInt("hard") + prior[1],
				rs.getInt("good") + prior[2],
				rs.getInt("easy") + prior[3]
		    };
		    
		    double[] probs = new double[] {		    
			    freqs[0] / (double) n,
			    freqs[1] / (double) n,
			    freqs[2] / (double) n,
			    freqs[3] / (double) n
		    };
		    
		    rs.close();
		    stmt.close();
		    
		    return probs;
		}
		
		private int draw(double[] p) {
			return searchsorted(p, random.nextDouble());
		}
		
		private int searchsorted(double[] p, double random) {
			if(random <= p[0]) return 0;
			if(random <= p[1]) return 1;
			if(random <= p[2]) return 2;
			return 3;
		}
		
		public Card sim_single_review(Card c, boolean preserve_card) {
			if(preserve_card)
				c = c.cloneShallow();
			return sim_single_review(c);
		}
		
		public Card sim_single_review(Card c){
			
			int c_type = c.getC_type();
			
			int outcome = draw(probabilitiesCumulative[c_type]);
				    
			applyOutcomeToCard(c, outcome);
			
			return c;
		}
		
		private void applyOutcomeToCard(Card c, int outcome) {
			
			int c_type = c.getC_type();
			int c_ivl = c.getC_ivl();
			double c_factor = c.getC_factor();
			
			if(c_type == 0) {
				if (outcome <= 2)
		            c_ivl = 1;
	            else
		            c_ivl = 4;
			}
			else {
				switch(outcome) {
					case 0:
						c_ivl = 1;
						break;
					case 1:
						c_ivl *= 1.2;
						break;
					case 2:
						c_ivl *= 1.2 * c_factor;	
						break;
					case 3:
					default:
						c_ivl *= 1.2 * 2. * c_factor;
						break;
				}
			}
				 
			c.setC_ivl(c_ivl);
			c.setCorrect((outcome > 0) ? 1 : 0);	    
			//c.setC_type(c_type);
			//c.setC_ivl(60);
			//c.setC_factor(c_factor);
		}
}
