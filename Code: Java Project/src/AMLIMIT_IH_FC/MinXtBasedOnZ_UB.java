package AMLIMIT_IH_FC;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

public class MinXtBasedOnZ_UB {

	InputExcel_ForIISE inputExcel;
	int[] plateSolution;
	int[] boardSetSolution;// x[t]
	int[][] stripSolution;// z[t][s]
	int maxBoardQuantityInEachSet;
	public double newObjValue;

	public MinXtBasedOnZ_UB(InputExcel_ForIISE inputExcel, int[] plateSolution, int[] boardSetSolution,
			int[][] stripSolution) {
		this.inputExcel = inputExcel;
		this.plateSolution = plateSolution;
		this.boardSetSolution = boardSetSolution;
		this.stripSolution = stripSolution;
	}

	public IloCplex HModel;
	public IloObjective Obj;
	public IloNumVar[][] X;// t-i
	public IloNumVar[][][] Z;// t-i-s

	public void establishVar() throws IloException {
		HModel = new IloCplex();

		maxBoardQuantityInEachSet = 0;
		for (int t = 0; t < inputExcel.boardSet.size(); t++) {
			maxBoardQuantityInEachSet = Math.max(maxBoardQuantityInEachSet, boardSetSolution[t]);
		}

		X = new IloNumVar[inputExcel.boardSet.size()][maxBoardQuantityInEachSet];
		Z = new IloNumVar[inputExcel.boardSet.size()][maxBoardQuantityInEachSet][inputExcel.generalData
				.getNumOfCuttingNorms()];

		for (int t = 0; t < inputExcel.boardSet.size(); t++) {
			for (int i = 0; i < maxBoardQuantityInEachSet; i++) {
				if (boardSetSolution[t] > i) {
					X[t][i] = HModel.numVar(0, 1, IloNumVarType.Int, "X" + t + "," + i);
					for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
						Z[t][i][s] = HModel.numVar(0, Integer.MAX_VALUE, IloNumVarType.Int,
								"Z" + t + "," + i + "," + s);
					}
				} else {
					X[t][i] = null;
					for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
						Z[t][i][s] = null;
					}
				}

			}
		}

		Obj = HModel.addMinimize();
	}

	public void establishConstraints() throws IloException {
		for (int t = 0; t < inputExcel.boardSet.size(); t++) {
			if (boardSetSolution[t] > 0) {
				for (int i = 0; i < maxBoardQuantityInEachSet; i++) {
					if (X[t][i] != null) {
						IloNumExpr expr = HModel.numExpr();
						for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
							expr = HModel.sum(expr, HModel.prod(inputExcel.cuttingNorms.getNorm()[s], Z[t][i][s]));
						}
						HModel.addLe(expr, HModel.prod(inputExcel.boardSet.get(t).getWidth(), X[t][i]));
						if (i + 1 < maxBoardQuantityInEachSet && X[t][i + 1] != null) {
							HModel.addLe(X[t][i + 1], X[t][i]);
						}
					}
				}
			}
		}

		for (int t = 0; t < inputExcel.boardSet.size(); t++) {
			if (boardSetSolution[t] > 0) {
				for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
					IloNumExpr expr1 = HModel.numExpr();
					for (int i = 0; i < maxBoardQuantityInEachSet; i++) {
						if (X[t][i] != null) {
							expr1 = HModel.sum(expr1, Z[t][i][s]);
						}
					}
					HModel.addEq(expr1, stripSolution[t][s]);
				}
			}
		}
	}

	public void establishObjFunction() throws IloException {
		IloNumExpr obj = HModel.numExpr();

		for (int t = 0; t < inputExcel.boardSet.size(); t++) {
			if (boardSetSolution[t] > 0) {
				for (int i = 0; i < maxBoardQuantityInEachSet; i++) {
					if (X[t][i] != null) {
						obj = HModel.sum(obj, X[t][i]);
					}
				}
			}

		}
		Obj.setExpr(obj);
	}

	public void resultOutput(int[] plateSolution, int[] boardSetSolution) throws Exception {
		newObjValue = 0;
		for (int t = 0; t < inputExcel.boardSet.size(); t++) {
			if (boardSetSolution[t] > 0) {
				int numX = 0;
				for (int i = 0; i < maxBoardQuantityInEachSet; i++) {
					if (X[t][i] != null && Math.round(HModel.getValue(X[t][i])) > 0) {
						numX++;
					} else {
						break;
					}
				}
				boardSetSolution[t] = numX;
				newObjValue += inputExcel.boardSet.get(t).getPrice() * numX;
				int diff = numX;
				for (int j = 0; j < inputExcel.plate.length; j++) {
					if (plateSolution[j] == 1) {
						if (inputExcel.plate[j].setIInPlate[t] > 0 && inputExcel.plate[j].setIInPlate[t] < diff) {
							diff -= inputExcel.plate[j].setIInPlate[t];
							plateSolution[j] = 1;
						} else if (inputExcel.plate[j].setIInPlate[t] > diff && diff > 0) {
							diff = 0;
							plateSolution[j] = 1;
						} else {
							plateSolution[j] = 0;
						}
					}
				}

			}
		}
		for (int j = 0; j < inputExcel.plate.length; j++) {
			if (plateSolution[j] == 1) {
				newObjValue += inputExcel.plateCost[j];
			}
		}

	}
}
