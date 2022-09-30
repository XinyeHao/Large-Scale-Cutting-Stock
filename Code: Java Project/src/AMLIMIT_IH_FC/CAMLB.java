package AMLIMIT_IH_FC;

import java.util.ArrayList;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

public class CAMLB {

	GeneralData generalData;
	Plate[] plate;
	Product[] product;
	CuttingNorms cuttingNorms;
	ChuLiao chuLiao;
	ArrayList<BoardSet> boardSet;
	double wd_min;

	int[] pSearch;
	int[] numOfBoardsFixedInT_Final;
	double[][] unmetQ;

	public CAMLB(InputExcel_ForIISE inputExcel, int[] pSearch, int[] numOfBoardsFixedInT_Final, double[][] unmetQ) {//
		this.generalData = inputExcel.generalData;
		this.plate = inputExcel.plate;
		this.product = inputExcel.product;
		this.cuttingNorms = inputExcel.cuttingNorms;
		this.chuLiao = inputExcel.chuLiao;
		this.boardSet = inputExcel.boardSet;
		this.pSearch = pSearch;
		this.numOfBoardsFixedInT_Final = numOfBoardsFixedInT_Final;
		this.unmetQ = unmetQ;
	}

	public IloCplex LBModel;
	public IloObjective ObjCost;

	public IloNumVar[] X;
	public IloNumVar[] Y;
	public IloNumVar[][] Z;
	public boolean firstIteration;
	public double SNRatio;

	public void establishVar() throws IloException {

		firstIteration = true;
		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			if (pSearch[p] != 0) {
				firstIteration = false;
				break;
			}
		}

		LBModel = new IloCplex();
		X = new IloNumVar[generalData.getNumOfPlates()];// X_p
		Y = new IloNumVar[boardSet.size()];// Y_t
		Z = new IloNumVar[generalData.getNumOfCuttingNorms()][boardSet.size()];// Z_nt

		if (firstIteration) {
			for (int p = 0; p < generalData.getNumOfPlates(); p++) {
				X[p] = LBModel.numVar(0, 1, IloNumVarType.Int, "X" + p);
			}
		} else {//
			for (int p = 0; p < generalData.getNumOfPlates(); p++) {
				if (pSearch[p] != 0) {
					X[p] = LBModel.numVar(0, 1, IloNumVarType.Int, "X" + p);
				}
			}
		}

		double leftBoardArea = 0, leftDemand = 0;

		for (int t = 0; t < boardSet.size(); t++) {
			if (boardSet.get(t).boardsInSet <= numOfBoardsFixedInT_Final[t]) {
				System.out.println("type [" + t + "] original = " + boardSet.get(t).boardsInSet + " used = "
						+ numOfBoardsFixedInT_Final[t]);
			} else {
				leftBoardArea += (boardSet.get(t).boardsInSet - numOfBoardsFixedInT_Final[t])
						* boardSet.get(t).getMianJi();
				Y[t] = LBModel.numVar(0, boardSet.get(t).boardsInSet - numOfBoardsFixedInT_Final[t], IloNumVarType.Int,
						"Y" + t);
				for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
					Z[n][t] = LBModel.numVar(0, Integer.MAX_VALUE, IloNumVarType.Int, "Z" + n + "," + t);
				}
			}
		}

		for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
			for (int g = 0; g < generalData.getNumOfProductGrades(); g++) {
				if (unmetQ[n][g] > 0) {
					leftDemand += unmetQ[n][g] * cuttingNorms.getNorm()[n];
				}
			}
		}
		
		SNRatio=leftBoardArea / leftDemand;
		System.out.println("  S/N ration this iteration = " + SNRatio);

		ObjCost = LBModel.addMinimize();

		wd_min = -1;
		for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
			for (int g = 0; g < generalData.getNumOfProductGrades(); g++) {
				if (unmetQ[n][g] > 0) {
					wd_min = cuttingNorms.getNorm()[n];
					break;// 
				}
			}
			if (wd_min != -1) {
				break;
			}
		}
		System.out.println("wd_min = " + wd_min);
	}

	public void establishConstraints() throws IloException {

		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			if (pSearch[p] == 1) {
				LBModel.addEq(X[p], 1);
			} // if ==2, leave it <=1, i.e., not deal with
				// if ==0, null, not deal with, too
		}

		for (int t = 0; t < boardSet.size(); t++) {
			if (Y[t] != null) {
				IloNumExpr expr = LBModel.numExpr();
				for (int p = 0; p < generalData.getNumOfPlates(); p++) {
					if (X[p] != null) {
						expr = LBModel.sum(expr, LBModel.prod(X[p], plate[p].setIInPlate[t]));
					}
				}
				expr = LBModel.diff(expr, numOfBoardsFixedInT_Final[t]);// 
				LBModel.addLe(Y[t], expr);
			}
		}

		for (int t = 0; t < boardSet.size(); t++) {
			if (Y[t] != null) {
				IloNumExpr expr = LBModel.numExpr();
				if (boardSet.get(t).getWidth() < 2 * wd_min) {// t_1
					IloNumExpr expr1 = LBModel.numExpr();
					for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
						if (boardSet.get(t).getWidth() >= cuttingNorms.getNorm()[n]) {
							expr = LBModel.sum(expr, Z[n][t]);
						} else {
							expr1 = LBModel.sum(expr1, Z[n][t]);
						}
					}
					LBModel.addLe(expr, Y[t]);
					LBModel.addEq(expr1, 0);
				} else {// t_2
					for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
						expr = LBModel.sum(expr, LBModel.prod(cuttingNorms.getNorm()[n], Z[n][t]));
					}
					LBModel.addLe(expr, LBModel.prod(boardSet.get(t).getWidth(), Y[t]));
				}
			}
		}

		for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
			for (int g = 0; g < generalData.getNumOfProductGrades(); g++) {
				double needed = 0;
				for (int g1 = 0; g1 <= g; g1++) {
					needed += unmetQ[n][g1];
				}

				IloNumExpr exprR = LBModel.numExpr();
				for (int g1 = 0; g1 <= g; g1++) {
					for (int t = 0; t < boardSet.size(); t++) {
						if (Y[t] != null) {
							exprR = LBModel.sum(exprR, LBModel.prod(
									boardSet.get(t).getLength() * chuLiao.getRate()[boardSet.get(t).getGrade() - 1][g1],
									Z[n][t]));
						}
					}
				}

				LBModel.addGe(exprR, needed);
			}
		}

	}

	public void establishObjFunction() throws IloException {

		IloNumExpr obj1 = LBModel.numExpr();
		IloNumExpr obj2 = LBModel.numExpr();

		// obj1
		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			if (X[p] != null) {
				obj1 = LBModel.sum(obj1, LBModel.prod(plate[p].getPrice(), X[p]));
			}
		}
		// obj2
		for (int t = 0; t < boardSet.size(); t++) {
			if (Y[t] != null) {
				obj2 = LBModel.sum(obj2, LBModel.prod(boardSet.get(t).getPrice(), Y[t]));
			}
		}

		ObjCost.setExpr(LBModel.sum(obj1, obj2));
		// System.out.println("objOfSubProblem" + ReducedCost);
	}

	public void setParam(double maxRunTime, double gap) throws IloException {
		LBModel.setParam(IloCplex.DoubleParam.TiLim, maxRunTime);
		LBModel.setParam(IloCplex.DoubleParam.EpGap, gap);
	}

	public void resultOutput(int[] X_p_LB, int[] Y_t_LB, int[][] Z_nt_LB) throws Exception {
		int countBoard = 0;

		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			if (X[p] != null) {
				if (Math.round(LBModel.getValue(X[p])) > 0) {
					X_p_LB[p] = 1;
				}
			}
		}

		for (int t = 0; t < boardSet.size(); t++) {
			if (Y[t] != null) {
				if (Math.round(LBModel.getValue(Y[t])) > 0) {
					Y_t_LB[t] = (int) Math.round(LBModel.getValue(Y[t]));
					countBoard += (int) Math.round(LBModel.getValue(Y[t]));
					for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
						if (Math.round(LBModel.getValue(Z[n][t])) != 0) {
							Z_nt_LB[n][t] = (int) Math.round(LBModel.getValue(Z[n][t]));
						}
					}
				}
			}
		}
	}

}
