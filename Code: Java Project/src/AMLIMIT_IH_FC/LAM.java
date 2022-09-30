package AMLIMIT_IH_FC;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

public class LAM {

	GeneralData generalData;
	Board[] board;
	Plate[] plate;
	Product[] product;
	CuttingNorms cuttingNorms;
	ChuLiao chuLiao;
	double[][] unmetQ;
	boolean[] fixedPlateLKM;
	boolean[] boardIsUsedLKM;

	public LAM(InputExcel_ForIISE inputExcel, boolean[] fixedPlateLKM, boolean[] boardIsUsedLKM, double[][] unmetQ) {
		this.generalData = inputExcel.generalData;
		this.board = inputExcel.board;
		this.plate = inputExcel.plate;
		this.product = inputExcel.product;
		this.cuttingNorms = inputExcel.cuttingNorms;
		this.chuLiao = inputExcel.chuLiao;
		this.unmetQ = unmetQ;
		this.fixedPlateLKM = fixedPlateLKM;
		this.boardIsUsedLKM = boardIsUsedLKM;
	}

	public IloCplex KantoModel;
	public IloObjective ObjCost;
	public double squareProvided;
	public double squareUsed;

	public IloNumVar[] rX;
	public IloNumVar[] rY;
	public IloNumVar[][] rZ;

	public void establishVar() throws IloException {

		int minN = -1;
		for (int n = 0; n < unmetQ.length; n++) {
			for (int g = 0; g < unmetQ[0].length; g++) {
				if (unmetQ[n][g] > 0) {
					minN = n;
					break;
				}
			}
			if (minN > -1) {
				break;
			}
		}

		KantoModel = new IloCplex();
		rX = new IloNumVar[generalData.getNumOfPlates()];// Xp
		rY = new IloNumVar[generalData.getNumOfBoards()];// Yb
		rZ = new IloNumVar[generalData.getNumOfCuttingNorms()][generalData.getNumOfBoards()];// Znb

		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			rX[p] = KantoModel.numVar(0, 1, IloNumVarType.Float, "X" + p);
		}

		for (int b = 0; b < generalData.getNumOfBoards(); b++) {
			if (!boardIsUsedLKM[b]) {
				if (board[b].getWidth() >= cuttingNorms.getNorm()[minN]) {
					rY[b] = KantoModel.numVar(0, 1, IloNumVarType.Float, "Y" + b);
				} else {
					System.out
							.println(" width = " + board[b].getWidth() + " minNorm = " + cuttingNorms.getNorm()[minN]);
				}
			}
		}

		for (int b = 0; b < generalData.getNumOfBoards(); b++) {
			for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
				rZ[n][b] = KantoModel.numVar(0, Double.MAX_VALUE, IloNumVarType.Float, "Z" + n + "," + b);
			}
		}

		ObjCost = KantoModel.addMinimize();

	}

	public void establishConstraints() throws IloException {

		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			if (fixedPlateLKM[p]) {
				KantoModel.addEq(rX[p], 1);
			}
		}

		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			for (int b = 0; b < generalData.getNumOfBoards(); b++) {
				if (plate[p].getPlateSn().equals(board[b].getPlateSn()) && rY[b] != null) {
					KantoModel.addLe(rY[b], rX[p]);
				}
			}
		}

		for (int b = 0; b < generalData.getNumOfBoards(); b++) {
			if (rY[b] != null) {
				IloNumExpr expr = KantoModel.numExpr();
				IloNumExpr expr1 = KantoModel.numExpr();

				for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
					expr = KantoModel.sum(expr, KantoModel.prod(cuttingNorms.getNorm()[n], rZ[n][b]));
				}
				KantoModel.addLe(expr, KantoModel.prod(board[b].getWidth(), rY[b]));
			}
		}

		for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
			for (int g = 0; g < generalData.getNumOfProductGrades(); g++) {
				double needed = 0;
				for (int g1 = 0; g1 <= g; g1++) {
					needed += unmetQ[n][g1];
				}

				IloNumExpr provide = KantoModel.numExpr();

				for (int b = 0; b < generalData.getNumOfBoards(); b++) {
					if (rY[b] != null) {
						for (int g1 = 0; g1 <= g; g1++) {
							provide = KantoModel.sum(provide, KantoModel.prod(
									board[b].getLength() * chuLiao.getRate()[board[b].getGrade() - 1][g1], rZ[n][b]));
						}
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
			obj1 = KantoModel.sum(obj1, KantoModel.prod(plate[p].getPrice(), rX[p]));
		}

		// obj2
		for (int b = 0; b < generalData.getNumOfBoards(); b++) {
			if (rY[b] != null) {
				obj2 = KantoModel.sum(obj2, KantoModel.prod(board[b].getPrice(), rY[b]));
			}
		}

		ObjCost.setExpr(KantoModel.sum(obj1, obj2));
	}

	public void resultOutput(Map<Integer, Double> board_value, double[][] stripSolution) throws Exception {

		double totalLength = 0;
		for (int b = 0; b < generalData.getNumOfBoards(); b++) {
			if (rY[b] != null) {
				if (KantoModel.getValue(rY[b]) > 0.00) {
					board_value.put(b, KantoModel.getValue(rY[b]));
					for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
						totalLength += KantoModel.getValue(rZ[n][b]) * board[b].getLength()
								* (1 - chuLiao.getRate()[board[b].getGrade() - 1][4]);
						stripSolution[b][n] = KantoModel.getValue(rZ[n][b]);
					}
				}
			}
		}
	}

}
