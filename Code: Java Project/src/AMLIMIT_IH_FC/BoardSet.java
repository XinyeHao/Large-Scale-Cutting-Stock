package AMLIMIT_IH_FC;

import java.util.ArrayList;

public class BoardSet {
	
	public ArrayList<String> msn ;
	public ArrayList<String> psn ;
	public int boardsInSet;
	
	
	
	private String MT;
	private int grade;
	private double length;
	private double width;
	private double height;
	private double mianJi;
	private double price;
	
	private double tiJi;
	
	
	
	public double getTiJi() {
		return tiJi;
	}
	public void setTiJi(double tiji) {
		this.tiJi = tiji;
	}
	public int getGrade() {
		return grade;
	}
	public void setGrade(int grade) {
		this.grade = grade;
	}
	public double getLength() {
		return length;
	}
	public void setLength(double length) {
		this.length = length;
	}
	public double getWidth() {
		return width;
	}
	public void setWidth(double width) {
		this.width = width;
	}
	public double getHeight() {
		return height;
	}
	public void setHeight(double height) {
		this.height = height;
	}
	public String getMT() {
		return MT;
	}
	public void setMT(String mT) {
		MT = mT;
	}
	public double getMianJi() {
		return mianJi;
	}
	public void setMianJi(double mianJi) {
		this.mianJi = mianJi;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
}
