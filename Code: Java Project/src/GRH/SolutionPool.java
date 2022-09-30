package GRH;

import java.util.ArrayList;

import AMLIMIT_IH_FC.InputExcel_ForIISE;

public class SolutionPool {
	InputExcel_ForIISE inputExcel;
	int t;

	public ArrayList<Integer>[] Z;
	public ArrayList<Double>[][] coef;
	public int size;

	public SolutionPool(InputExcel_ForIISE inputExcel, int t) {
		this.inputExcel = inputExcel;
		this.t = t;
		Z = new ArrayList[inputExcel.generalData.getNumOfCuttingNorms()];
		coef = new ArrayList[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.generalData
				.getNumOfProductGrades()];
		for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
			Z[n] = new ArrayList();
			for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
				coef[n][g] = new ArrayList();
			}
		}
		size = 0;
	}

	public void add() {
		size++;
		for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
			Z[n].add(0);
			for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
				coef[n][g].add(0.0);
			}
		}
	}

	public void delete() {
		size--;
		for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
			Z[n].remove(Z[n].size() - 1);
			for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
				coef[n][g].remove(coef[n][g].size() - 1);
			}
		}
	}

	public void calcuCoef() {
		double cof = 0;
		for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
			for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
				cof = 0;
				for (int g1 = 0; g1 <= g; g1++) {
					cof += Z[n].get(Z[n].size() - 1) * inputExcel.boardSet.get(t).getLength()
							* inputExcel.chuLiao.getRate()[inputExcel.boardSet.get(t).getGrade() - 1][g1];
				}
				coef[n][g].set(coef[n][g].size() - 1, cof);
			}
		}
	}
}
