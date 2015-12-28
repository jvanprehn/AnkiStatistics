package dao;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Deck {
	
	public static List<Card> deckFromDB(Connection c, int today) throws ClassNotFoundException, SQLException {
		List<Card> deck = new ArrayList<Card>();
	    
	    Statement stmt = c.createStatement();
	    
	    String sql = 	"SELECT id, due, ivl, factor, type, reps " +
	    				"FROM cards " +
	    				"order by id;";
	    
	    ResultSet rs = stmt.executeQuery( sql );
	    
	    while ( rs.next() ) {
	    	
	    	Card card = new Card(rs.getLong("id"),
	    			             rs.getInt("reps") == 0 ? 0 : rs.getInt("ivl"),  		//# card interval
	    						 rs.getInt("factor") > 0 ? rs.getInt("factor") :  2500,
	    							Math.max(rs.getInt("due") - today, 0),
	    							1);
	    	deck.add(card);
	    }
	    
	    rs.close();
	    stmt.close();
	    
	    return deck;
	}
}
