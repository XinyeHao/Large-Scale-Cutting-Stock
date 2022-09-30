package PropValidation;

import AMLIMIT_IH_FC.Board;
import AMLIMIT_IH_FC.BoardSet;
import AMLIMIT_IH_FC.ChuLiao;
import AMLIMIT_IH_FC.CuttingNorms;
import AMLIMIT_IH_FC.GeneralData;
import AMLIMIT_IH_FC.GenerateQ;
import AMLIMIT_IH_FC.IfUnmet;
import AMLIMIT_IH_FC.InputExcel_ForIISE;
import AMLIMIT_IH_FC.Plate;
import AMLIMIT_IH_FC.Product;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

public class LAM {

	InputExcel_ForIISE inputExcel;
	GeneralData generalData;
	Board[] board;
	Plate[] plate;
	Product[] product;
	CuttingNorms cuttingNorms;
	ChuLiao chuLiao;
	GenerateQ generateQ;

	public LAM(InputExcel_ForIISE inputExcel, GenerateQ generateQ) {
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

	public IloNumVar[] X;
	public IloNumVar[] Y;
	public IloNumVar[][] Z;

	public void establishVar() throws IloException {

		KantoModel = new IloCplex();
		X = new IloNumVar[generalData.getNumOfPlates()];
		Y = new IloNumVar[generalData.getNumOfBoards()];
		Z = new IloNumVar[generalData.getNumOfCuttingNorms()][generalData.getNumOfBoards()];

		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			X[p] = KantoModel.numVar(0, 1, IloNumVarType.Float, "X" + p);
		}

		for (int b = 0; b < generalData.getNumOfBoards(); b++) {
			Y[b] = KantoModel.numVar(0, 1, IloNumVarType.Float, "Y" + b);
		}

		for (int b = 0; b < generalData.getNumOfBoards(); b++) {
			for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
				Z[n][b] = KantoModel.numVar(0, Double.MAX_VALUE, IloNumVarType.Float, "Z" + n + "," + b);
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
	}

	public void setParam(double maxRunTime, double gap) throws IloException {
		KantoModel.setParam(IloCplex.DoubleParam.TiLim, maxRunTime);
		KantoModel.setParam(IloCplex.DoubleParam.EpGap, gap);
	}

}
