package GRH;

import AMLIMIT_IH_FC.IfUnmet;
import AMLIMIT_IH_FC.InputExcel_ForIISE;

public class GenerateInitColumn {

	InputExcel_ForIISE inputExcel;
	double[][] unmetQ;

	public GenerateInitColumn(InputExcel_ForIISE inputExcel, double[][] unmetQ) {
		this.inputExcel = inputExcel;
		this.unmetQ = unmetQ;
	}

	public void generate(int[] Y_Final, SolutionPool[] solutionPool) throws Exception {
		double[][] Q = new double[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.generalData
				.getNumOfProductGrades()];
		for (int n = 0; n < Q.length; n++) {
			for (int g = 0; g < Q[0].length; g++) {
				Q[n][g] = unmetQ[n][g];
			}
		}
		IfUnmet ifUnmet = new IfUnmet();
		boolean useThisB = false;
		int usedB = 0;
		double widthLeftThisB = 0;
		for (int b = 0; b < inputExcel.board.length; b++) {
			if (Y_Final[b] == 0) {
				useThisB = false;
				widthLeftThisB = inputExcel.board[b].getWidth();
				if (ifUnmet.ifUnmet(Q)) {
					solutionPool[inputExcel.boardSetNo[b]].add();
					usedB++;

					for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
						if (ifUnmet.ifUnmetS(Q, n)) {
							if (widthLeftThisB >= inputExcel.cuttingNorms.getNorm()[n]) {
								useThisB = true;
								int quantityS = (int) Math
										.floor(inputExcel.board[b].getWidth() / inputExcel.cuttingNorms.getNorm()[n]);
								
								solutionPool[inputExcel.boardSetNo[b]].Z[n]
										.set(solutionPool[inputExcel.boardSetNo[b]].Z[n].size() - 1, quantityS);
								widthLeftThisB -= quantityS * inputExcel.cuttingNorms.getNorm()[n];
								for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades(); l++) {
									double lengthLeft = inputExcel.board[b].getLength()
											* inputExcel.chuLiao.getRate()[inputExcel.board[b].getGrade() - 1][l]
											* quantityS;
									if (lengthLeft > 0) {
										for (int l1 = l; l1 < inputExcel.generalData.getNumOfProductGrades(); l1++) {
											if (Q[n][l1] > 0) {
												if (Q[n][l1] < lengthLeft) {
													lengthLeft -= Q[n][l1];
													Q[n][l1] = 0;
												} else {
													Q[n][l1] -= lengthLeft;
													lengthLeft = 0;
													break;
												}
											}
										}
									}
								}
							} else {
								break;
							}
						}
					}
					if (useThisB) {
						solutionPool[inputExcel.boardSetNo[b]].calcuCoef();
					} else {
						solutionPool[inputExcel.boardSetNo[b]].delete();
						usedB--;
					}
				} else {
					break;
				}
			}
		}
	}
}
