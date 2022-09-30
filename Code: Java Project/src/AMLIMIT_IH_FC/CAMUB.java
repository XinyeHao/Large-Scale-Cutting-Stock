package AMLIMIT_IH_FC;

import java.util.ArrayList;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

public class CAMUB {

	GeneralData generalData;
	Plate[] plate;
	Product[] product;
	CuttingNorms cuttingNorms;
	ChuLiao chuLiao;
	ArrayList<BoardSet> boardSet;
	double wd_min, wd_max;

	int[] pSearch;
	int[] numOfBoardsFixedInT_Final;
	double[][] unmetQ;

	public CAMUB(InputExcel_ForIISE inputExcel, int[] pSearch, int[] numOfBoardsFixedInT_Final, double[][] unmetQ) {// 
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

	public IloCplex UBModel;
	public IloObjective ObjCost;

	public IloNumVar[] X;
	public IloNumVar[] Y;
	public IloNumVar[][] Z;
	public boolean firstIteration;

	public void establishVar() throws IloException {

		firstIteration = true;
		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			if (pSearch[p] != 0) {
				firstIteration = false;
				break;
			}
		}

		UBModel = new IloCplex();
		X = new IloNumVar[generalData.getNumOfPlates()];// X_p
		Y = new IloNumVar[boardSet.size()];// Y_t
		Z = new IloNumVar[generalData.getNumOfCuttingNorms()][boardSet.size()];// Z_nt

		if (firstIteration) {
			for (int p = 0; p < generalData.getNumOfPlates(); p++) {
				X[p] = UBModel.numVar(0, 1, IloNumVarType.Int, "X" + p);
			}
		} else {// 
			for (int p = 0; p < generalData.getNumOfPlates(); p++) {
				if (pSearch[p] != 0) {
					X[p] = UBModel.numVar(0, 1, IloNumVarType.Int, "X" + p);
				} 
			}
		}

		for (int t = 0; t < boardSet.size(); t++) {
			Y[t] = UBModel.numVar(0, boardSet.get(t).boardsInSet - numOfBoardsFixedInT_Final[t], IloNumVarType.Int,
					"Y" + t);
		}

		for (int t = 0; t < boardSet.size(); t++) {
			for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
				Z[n][t] = UBModel.numVar(0, Integer.MAX_VALUE, IloNumVarType.Int, "Z" + n + "," + t);
			}
		}

		ObjCost = UBModel.addMinimize();

		wd_min = -1;
		for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
			for (int g = 0; g < generalData.getNumOfProductGrades(); g++) {
				if (unmetQ[n][g] > 0) {
					wd_min = cuttingNorms.getNorm()[n];
					break;// 
				}
			}
			if (wd_min != -1) {//
				break;
			}
		}
		System.out.println("wd_min = " + wd_min);

		wd_max = -1;
		for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
			for (int g = 0; g < generalData.getNumOfProductGrades(); g++) {
				if (unmetQ[n][g] > 0) {
					wd_max = cuttingNorms.getNorm()[n];
					break;// 
				}
			}
		}
		System.out.println("wd_max = " + wd_max);
	}

	public void establishConstraints() throws IloException {

		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			if (pSearch[p] == 1) {// 
				UBModel.addEq(X[p], 1);
			} // if ==2, leave it <=1, i.e., not deal with
				// if ==0, null, not deal with, too
		}

		for (int t = 0; t < boardSet.size(); t++) {
			IloNumExpr expr = UBModel.numExpr();
			for (int p = 0; p < generalData.getNumOfPlates(); p++) {
				if (X[p] != null) {
					expr = UBModel.sum(expr, UBModel.prod(X[p], plate[p].setIInPlate[t]));
				}
			}
			expr = UBModel.diff(expr, numOfBoardsFixedInT_Final[t]);// 
			UBModel.addLe(Y[t], expr);
		}

		for (int t = 0; t < boardSet.size(); t++) {
			IloNumExpr expr = UBModel.numExpr();
			if (boardSet.get(t).getWidth() < 2 * wd_min) {// t_1
				IloNumExpr expr1 = UBModel.numExpr();
				for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
					if (boardSet.get(t).getWidth() >= cuttingNorms.getNorm()[n]) {
						expr = UBModel.sum(expr, Z[n][t]);
					} else {
						expr1 = UBModel.sum(expr1, Z[n][t]);
					}
				}
				UBModel.addLe(expr, Y[t]);
				UBModel.addEq(expr1, 0);
			} else {// t_2
				for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
					expr = UBModel.sum(expr, UBModel.prod(cuttingNorms.getNorm()[n], Z[n][t]));
				}
				UBModel.addLe(expr, UBModel.prod(Math.max(0, boardSet.get(t).getWidth() - wd_max), Y[t]));
			}
		}

		for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
			for (int g = 0; g < generalData.getNumOfProductGrades(); g++) {
				double needed = 0;
				for (int g1 = 0; g1 <= g; g1++) {
					needed += unmetQ[n][g1];
				}

				IloNumExpr exprR = UBModel.numExpr();
				for (int g1 = 0; g1 <= g; g1++) {
					for (int t = 0; t < boardSet.size(); t++) {
						exprR = UBModel.sum(exprR, UBModel.prod(
								boardSet.get(t).getLength() * chuLiao.getRate()[boardSet.get(t).getGrade() - 1][g1],
								Z[n][t]));
					}
				}

				UBModel.addGe(exprR, needed);
			}
		}

	}

	public void establishObjFunction() throws IloException {

		IloNumExpr obj1 = UBModel.numExpr();
		IloNumExpr obj2 = UBModel.numExpr();

		// obj1
		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			if (X[p] != null) {
				obj1 = UBModel.sum(obj1, UBModel.prod(plate[p].getPrice(), X[p]));
			}
		}
		// obj2
		for (int t = 0; t < boardSet.size(); t++) {
			obj2 = UBModel.sum(obj2, UBModel.prod(boardSet.get(t).getPrice(), Y[t]));
		}

		ObjCost.setExpr(UBModel.sum(obj1, obj2));
		// System.out.println("objOfSubProblem" + ReducedCost);
	}

	public void setParam(double maxRunTime, double gap) throws IloException {
		UBModel.setParam(IloCplex.DoubleParam.TiLim, maxRunTime);
		UBModel.setParam(IloCplex.DoubleParam.EpGap, gap);
		UBModel.setOut(null);
	}

	public void resultOutput(int[] X_p_UB) throws Exception {

		int countBoard = 0;

		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			if (X[p] != null) {
				if (Math.round(UBModel.getValue(X[p])) > 0) {
					X_p_UB[p] = 1;
				}
			}
		}

		for (int t = 0; t < boardSet.size(); t++) {
			if (Math.round(UBModel.getValue(Y[t])) > 0) {
				countBoard += (int) Math.round(UBModel.getValue(Y[t]));
			}
		}
	}

}
