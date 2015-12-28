package main;

public class Settings {
	
	public static int deckCreationTimeStamp() {
		return 1445619600;
	}
	
	public static int n_time_bins() {
		return 30;
	}
	
	public static int time_bin_length() {
		return 1;
	}
	
	public static int max_reviews_per_day() {
		return 10000;
	}
	
	public static String db_location() {
		return "d:\\collection_20151227.sqlite";
	}
	
	public static long now() {
		return 1451223980146L;
		//return System.currentTimeMillis();
	}
}
