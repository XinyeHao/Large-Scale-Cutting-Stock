package AMLIMIT_IH_FC;

import java.util.ArrayList;

public class Plate {

	private String PlateSn;
	private int[] plateToProductLevel;
	public ArrayList<Board> board = new ArrayList<Board>();
	public ArrayList<Integer> boardTypeNo = new ArrayList<Integer>();
	public double price;

	public int[] setIInPlate;

	public String getPlateSn() {
		return PlateSn;
	}

	public void setPlateSn(String plateSn) {
		PlateSn = plateSn;
	}

	public int[] getPlateToProductLevel() {
		return plateToProductLevel;
	}

	public void setPlateToProductLevel(int[] plateToProductLevel) {
		this.plateToProductLevel = plateToProductLevel;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}
}
