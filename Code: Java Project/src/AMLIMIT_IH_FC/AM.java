package AMLIMIT_IH_FC;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

public class AM {

	InputExcel_ForIISE inputExcel;
	GeneralData generalData;
	Board[] board;
	Plate[] plate;
	Product[] product;
	CuttingNorms cuttingNorms;
	ChuLiao chuLiao;
	GenerateQ generateQ;

	public AM(InputExcel_ForIISE inputExcel, GenerateQ generateQ) {
		this.inputExcel = inputExcel;
		this.generalData = inputExcel.generalData;
		this.board = inputExcel.board;
		this.plate = inputExcel.plate;
		this.product = inputExcel.product;
		this.cuttingNorms = inputExcel.cuttingNorms;
		this.chuLiao = inputExcel.chuLiao;
		this.generateQ = generateQ;
	}

	public IloCplex KantoModel;
	public IloObjective ObjCost;
	public double squareProvided;
	public double squareUsed;

	public IloNumVar[] X;
	public IloNumVar[] Y;
	public IloNumVar[][] Z;
	public int[] Y1;
	public int[][] Z1;

	public void establishVar() throws IloException {

		KantoModel = new IloCplex();
		X = new IloNumVar[generalData.getNumOfPlates()];
		Y = new IloNumVar[generalData.getNumOfBoards()];
		Z = new IloNumVar[generalData.getNumOfCuttingNorms()][generalData.getNumOfBoards()];

		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			X[p] = KantoModel.numVar(0, 1, IloNumVarType.Int, "X" + p);
		}

		for (int b = 0; b < generalData.getNumOfBoards(); b++) {
			Y[b] = KantoModel.numVar(0, 1, IloNumVarType.Int, "Y" + b);
		}

		for (int b = 0; b < generalData.getNumOfBoards(); b++) {
			for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
				Z[n][b] = KantoModel.numVar(0, Integer.MAX_VALUE, IloNumVarType.Int, "Z" + n + "," + b);
			}
		}

		ObjCost = KantoModel.addMinimize();

	}

	public void establishConstraints() throws IloException {

		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			for (int b = 0; b < generalData.getNumOfBoards(); b++) {
				if (plate[p].getPlateSn().equals(board[b].getPlateSn())) {
					KantoModel.addLe(Y[b], X[p]);
				}
			}
		}

		for (int b = 0; b < generalData.getNumOfBoards(); b++) {
			IloNumExpr expr = KantoModel.numExpr();
			for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
				expr = KantoModel.sum(expr, KantoModel.prod(cuttingNorms.getNorm()[n], Z[n][b]));
			}
			KantoModel.addLe(expr, KantoModel.prod(board[b].getWidth(), Y[b]));
		}

		for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
			for (int g = 0; g < generalData.getNumOfProductGrades(); g++) {
				double needed = 0;
				for (int g1 = 0; g1 <= g; g1++) {
					needed += generateQ.q[n][g1];
				}

				IloNumExpr provide = KantoModel.numExpr();

				for (int b = 0; b < generalData.getNumOfBoards(); b++) {
					for (int g1 = 0; g1 <= g; g1++) {
						provide = KantoModel.sum(provide, KantoModel
								.prod(board[b].getLength() * chuLiao.getRate()[board[b].getGrade() - 1][g1], Z[n][b]));
					}
				}

				KantoModel.addGe(provide, needed);
			}
		}

	}

	public void establishObjFunction() throws IloException {

		IloNumExpr obj1 = KantoModel.numExpr();
		IloNumExpr obj2 = KantoModel.numExpr();

		// obj1
		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			obj1 = KantoModel.sum(obj1, KantoModel.prod(plate[p].getPrice(), X[p]));
		}

		// obj2
		for (int b = 0; b < generalData.getNumOfBoards(); b++) {
			obj2 = KantoModel.sum(obj2, KantoModel.prod(board[b].getPrice(), Y[b]));
		}

		ObjCost.setExpr(KantoModel.sum(obj1, obj2));
		// System.out.println("objOfSubProblem" + ReducedCost);
	}

	public void setParam(double maxRunTime, double gap) throws IloException {
		KantoModel.setParam(IloCplex.DoubleParam.TiLim, maxRunTime);
		KantoModel.setParam(IloCplex.DoubleParam.EpGap, gap);
	}

	public int countX, countY;
	public double plateCost, boardCost;
	public double totalAreaSelected, chuLiaoArea, inventoryArea;
	public double[][] produce;
	public double[][][] Qngg;
	public double[][] left;

	public void resultOutput() throws Exception {
		Y1 = new int[inputExcel.boardSet.size()];
		Z1 = new int[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.boardSet.size()];
		countX = 0;
		countY = 0;
		plateCost = 0;
		boardCost = 0;
		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			if (Math.round(KantoModel.getValue(X[p])) > 0) {
				//System.out.println("X [" + plate[p].getPlateSn() + "] =" + KantoModel.getValue(X[p]));
				plateCost += inputExcel.plate[p].getPrice();
				countX++;
			}
		}
		for (int b = 0; b < generalData.getNumOfBoards(); b++) {
			if (Math.round(KantoModel.getValue(Y[b])) > 0) {
				Y1[inputExcel.boardSetNo[b]]++;
				countY++;
				boardCost += inputExcel.board[b].getPrice();
				squareProvided += board[b].getMianJi();
				for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
					if (Math.round(KantoModel.getValue(Z[n][b])) != 0) {
						Z1[n][inputExcel.boardSetNo[b]] += (int) Math.round(KantoModel.getValue(Z[n][b]));
					}
				}
			}
		}

		produce = new double[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.generalData
				.getNumOfProductGrades()];
		Qngg = new double[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.generalData
				.getNumOfProductGrades()][inputExcel.generalData.getNumOfProductGrades()];

		left = new double[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.generalData
				.getNumOfProductGrades()];

		totalAreaSelected = 0;
		chuLiaoArea = 0;
		inventoryArea = 0;
		double[][] unmetQ = new double[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.generalData
				.getNumOfProductGrades()];
		for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
			for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
				unmetQ[n][g] = generateQ.q[n][g];
			}
		}

		IfUnmet ifUnmet = new IfUnmet();
		for (int t = 0; t < inputExcel.boardSet.size(); t++) {
			if (Y1[t] > 0) {
				totalAreaSelected += inputExcel.boardSet.get(t).getLength() * inputExcel.boardSet.get(t).getWidth()
						* Y1[t];
				for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
					if (Z1[n][t] > 0) {

						chuLiaoArea += inputExcel.boardSet.get(t).getLength() * inputExcel.cuttingNorms.getNorm()[n]
								* Z1[n][t];
						if (ifUnmet.ifUnmetS(unmetQ, n)) {
							for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
								double pro = inputExcel.boardSet.get(t).getLength() * Z1[n][t]
										* inputExcel.chuLiao.getRate()[inputExcel.boardSet.get(t).getGrade() - 1][g];
								produce[n][g] += pro;
								for (int g1 = g; g1 < inputExcel.generalData.getNumOfProductGrades(); g1++) {
									if (pro > 0) {
										if (unmetQ[n][g1] > pro) {
											Qngg[n][g][g1] += pro;
											unmetQ[n][g1] -= pro;
											pro = 0;
											break;
										} else if (unmetQ[n][g1] <= pro) {
											Qngg[n][g][g1] += unmetQ[n][g1];
											pro -= unmetQ[n][g1];
											unmetQ[n][g1] = 0;
										}
									} else {
										break;
									}
								}
								left[n][g] += pro;
							}
						} else {
							for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
								double pro = inputExcel.boardSet.get(t).getLength() * Z1[n][t]
										* inputExcel.chuLiao.getRate()[inputExcel.boardSet.get(t).getGrade() - 1][g];
								produce[n][g] += pro;
								left[n][g] += pro;

							}
						}

					}
				}
			}

		}

		for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
			for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades() - 1; g++) {
				inventoryArea += left[n][g] * inputExcel.cuttingNorms.getNorm()[n];
			}
		}
	}

}
