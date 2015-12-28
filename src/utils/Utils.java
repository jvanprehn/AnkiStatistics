package utils;

import main.Settings;

public class Utils {
	
	public static int today() {
		int currentTime = (int) (Settings.now() / 1000);
	    int today = (int) ((currentTime - deckCreationTimeStamp()) / secsPerDay());
	    return today;
	}
	
	public static int deckCreationTimeStamp() {
		return Settings.deckCreationTimeStamp();
	}
	
	public static int secsPerDay() {
		return 86400;
	}
	
	public static int todayCutoffSecsFromDeckCreation() {
		int mDayCutoff = Utils.deckCreationTimeStamp() + ((today() + 1) * Utils.secsPerDay());
		return mDayCutoff;
	}
	
}
