package AMLIMIT_IH_FC;

import java.util.Random;

public class GenerateQ {

	InputExcel_ForIISE inputExcel;

	public GenerateQ(InputExcel_ForIISE inputExcel) {
		this.inputExcel = inputExcel;
	}

	public double[][] q;// q_sl
	public int[][] P;//
	public double netSquareNeeded;
	public double yanmiSquareNeeded;

	public int chooseNorm(int k) throws Exception {
		int sNo = 0;
		double square = 0;
		double minSquare = Double.MAX_VALUE;

		for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {

			if (inputExcel.product[k].getWidth() > inputExcel.cuttingNorms.getNorm()[s]) {

				square = ((int) Math.ceil(inputExcel.product[k].getWidth() / inputExcel.cuttingNorms.getNorm()[s]))
						* inputExcel.cuttingNorms.getNorm()[s];
			} else {
				square = inputExcel.cuttingNorms.getNorm()[s]
						/ ((int) Math.floor(inputExcel.cuttingNorms.getNorm()[s] / inputExcel.product[k].getWidth()));
			}

			if (s == 0) {
				minSquare = square;
				sNo = s;
			}

			if (square < minSquare) {
				minSquare = square;
				sNo = s;
			}
		}
		return sNo;
	}

	public void generateQ() throws Exception {
		q = new double[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.generalData.getNumOfProductGrades()];
		P = new int[inputExcel.generalData.getNumOfProducts()][inputExcel.generalData.getNumOfCuttingNorms()];
		netSquareNeeded = 0;
		for (int k = 0; k < inputExcel.generalData.getNumOfProducts(); k++) {
			int s = chooseNorm(k);

			if (inputExcel.product[k].getWidth() > inputExcel.cuttingNorms.getNorm()[s]) {
				P[k][s] = (int) Math.ceil(inputExcel.product[k].getWidth() / inputExcel.cuttingNorms.getNorm()[s]);
				q[s][inputExcel.product[k].getLevel() - 1] += ((int) Math
						.ceil(inputExcel.product[k].getWidth() / inputExcel.cuttingNorms.getNorm()[s]))
						* inputExcel.product[k].getLength() * inputExcel.product[k].getDemand();
			} else {
				P[k][s] = 1;
				q[s][inputExcel.product[k].getLevel() - 1] += Math.ceil(((double) inputExcel.product[k].getDemand())
						/ ((int) Math.floor(inputExcel.cuttingNorms.getNorm()[s] / inputExcel.product[k].getWidth())))
						* inputExcel.product[k].getLength();
			}

			netSquareNeeded += inputExcel.product[k].getLength() * inputExcel.product[k].getWidth()
					* inputExcel.product[k].getDemand();

		}
		yanmiSquareNeeded = 0;
		for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
			for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades(); l++) {
				if (q[s][l] > 0) {
					yanmiSquareNeeded += inputExcel.cuttingNorms.getNorm()[s] * q[s][l];
				}
			}
		}

		double totalSquarePro = 0;
		for (int b = 0; b < inputExcel.board.length; b++) {
			totalSquarePro += inputExcel.board[b].getMianJi();
		}
	}

	public void generateQBasedOnSquare() throws Exception {
		P = new int[inputExcel.generalData.getNumOfProducts()][inputExcel.generalData.getNumOfCuttingNorms()];
		q = new double[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.generalData.getNumOfProductGrades()];
		netSquareNeeded = 0;
		for (int k = 0; k < inputExcel.generalData.getNumOfProducts(); k++) {
			int s = chooseNorm(k);

			if (inputExcel.product[k].getWidth() > inputExcel.cuttingNorms.getNorm()[s]) {
				P[k][s] = (int) Math.ceil(inputExcel.product[k].getWidth() / inputExcel.cuttingNorms.getNorm()[s]);
			} else {
				P[k][s] = 1;
			}

			netSquareNeeded += inputExcel.product[k].getLength() * inputExcel.product[k].getWidth()
					* inputExcel.product[k].getDemand();

		}

		// Define q_sl

		double totalSquarePro = 0;
		for (int b = 0; b < inputExcel.board.length; b++) {
			totalSquarePro += inputExcel.board[b].getMianJi();
		}

		Random r = new Random();
		yanmiSquareNeeded = totalSquarePro / 5;
		for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
			for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades() - 1; l++) {
				q[s][l] = yanmiSquareNeeded / 4 / inputExcel.cuttingNorms.getNorm()[s]
						* (0.20 + 0.1 * (l + 1) * r.nextDouble());
			}
		}
	}
}
