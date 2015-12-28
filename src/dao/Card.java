package dao;

public class Card {
	int c_ivl;
	double c_factor;
	int c_due;
	int correct;
	long id;
	
	@Override
	public String toString() {
		return "Card [c_ivl=" + c_ivl + ", c_factor=" + c_factor + ", c_due=" + c_due + ", correct=" + correct + ", id="
				+ id + "]";
	}
	
	public Card(long id, int c_ivl, int c_factor, int c_due, int correct) {
		super();
		this.id = id;
		this.c_ivl = c_ivl;
		this.c_factor = c_factor / 1000.0;
		this.c_due = c_due;
		this.correct = correct;
	}

	public Card cloneShallow() {
		return new Card(id, c_ivl, (int) (c_factor * 1000), c_due, correct);
	}
	
	public long getId() {
		return id;
	}
	
	public int getC_ivl() {
		return c_ivl;
	}

	public void setC_ivl(int c_ivl) {
		this.c_ivl = c_ivl;
	}

	public double getC_factor() {
		return c_factor;
	}

	public void setC_factor(double c_factor) {
		this.c_factor = c_factor;
	}

	public int getC_due() {
		return c_due;
	}

	public void setC_due(int c_due) {
		this.c_due = c_due;
	}	
	
	public int getC_type() {
		//# 0=new, 1=Young, 2=mature
		if(c_ivl == 0) {
			return 0;
		} else if (c_ivl >= 21) {
			return 2;
		} else {
			return 1;
		}
	}

	public int getCorrect() {
		return correct;
	}

	public void setCorrect(int correct) {
		this.correct = correct;
	}
}
