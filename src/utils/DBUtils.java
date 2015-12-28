package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import main.Settings;

public class DBUtils {
	public static Connection openDBConnection() throws ClassNotFoundException, SQLException {
		Class.forName("org.sqlite.JDBC");
		Connection c = DriverManager.getConnection("jdbc:sqlite:" + Settings.db_location());
	    c.setAutoCommit(false);
	    return c;
	}
}
