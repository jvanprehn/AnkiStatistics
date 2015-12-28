package simulators;

import dao.Card;

public class NewCardSimulator {
	
	int max_add_per_day=20;
	
	int n_added_today = 0;
    int t_add = 0;
	
	public int simulateNewCard(Card card) {
        n_added_today++;
        int t_elapsed = t_add;	//differs from online
        if (n_added_today >= max_add_per_day) {
        	t_add++;
            n_added_today = 0;
        }
        return t_elapsed;
	}
}
