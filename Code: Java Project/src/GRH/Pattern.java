package GRH;

import AMLIMIT_IH_FC.InputExcel_ForIISE;

public class Pattern {

	InputExcel_ForIISE inputExcel;

	public int t;
	public int[] Z;
	public double RMP_Value;

	public Pattern(InputExcel_ForIISE inputExcel, int t) {
		this.inputExcel = inputExcel;
		this.t = t;
		Z = new int[inputExcel.generalData.getNumOfCuttingNorms()];
	}
}
