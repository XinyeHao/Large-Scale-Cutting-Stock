package PropValidation;

import java.util.ArrayList;

import AMLIMIT_IH_FC.BoardSet;
import AMLIMIT_IH_FC.ChuLiao;
import AMLIMIT_IH_FC.CuttingNorms;
import AMLIMIT_IH_FC.GeneralData;
import AMLIMIT_IH_FC.InputExcel_ForIISE;
import AMLIMIT_IH_FC.Plate;
import AMLIMIT_IH_FC.Product;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

public class CAMLB_RLX {

	GeneralData generalData;
	Plate[] plate;
	Product[] product;
	CuttingNorms cuttingNorms;
	ChuLiao chuLiao;
	ArrayList<BoardSet> boardSet;

	double[][] unmetQ;

	public CAMLB_RLX(InputExcel_ForIISE inputExcel, double[][] unmetQ) {//
		this.generalData = inputExcel.generalData;
		this.plate = inputExcel.plate;
		this.product = inputExcel.product;
		this.cuttingNorms = inputExcel.cuttingNorms;
		this.chuLiao = inputExcel.chuLiao;
		this.boardSet = inputExcel.boardSet;
		this.unmetQ = unmetQ;
	}

	public IloCplex LBModel;
	public IloObjective ObjCost;

	public IloNumVar[] X;
	public IloNumVar[] Y;
	public IloNumVar[][] Z;

	public void establishVar() throws IloException {

		LBModel = new IloCplex();
		X = new IloNumVar[generalData.getNumOfPlates()];// X_p
		Y = new IloNumVar[boardSet.size()];// Y_t
		Z = new IloNumVar[generalData.getNumOfCuttingNorms()][boardSet.size()];// Z_nt

		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			X[p] = LBModel.numVar(0, 1, IloNumVarType.Float, "X" + p);
		}

		for (int t = 0; t < boardSet.size(); t++) {
			Y[t] = LBModel.numVar(0, boardSet.get(t).boardsInSet, IloNumVarType.Float, "Y" + t);
		}

		for (int t = 0; t < boardSet.size(); t++) {
			for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
				Z[n][t] = LBModel.numVar(0, Double.MAX_VALUE, IloNumVarType.Float, "Z" + n + "," + t);
			}
		}

		ObjCost = LBModel.addMinimize();

	}

	public void establishConstraints() throws IloException {

		for (int t = 0; t < boardSet.size(); t++) {
			IloNumExpr expr = LBModel.numExpr();
			for (int p = 0; p < generalData.getNumOfPlates(); p++) {
				if (X[p] != null) {
					expr = LBModel.sum(expr, LBModel.prod(X[p], plate[p].setIInPlate[t]));
				}
			}
			LBModel.addLe(Y[t], expr);
		}

		for (int t = 0; t < boardSet.size(); t++) {

			IloNumExpr expr = LBModel.numExpr();
			for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
				expr = LBModel.sum(expr, LBModel.prod(cuttingNorms.getNorm()[n], Z[n][t]));
			}
			LBModel.addLe(expr, LBModel.prod(boardSet.get(t).getWidth(), Y[t]));
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
						exprR = LBModel.sum(exprR, LBModel.prod(
								boardSet.get(t).getLength() * chuLiao.getRate()[boardSet.get(t).getGrade() - 1][g1],
								Z[n][t]));
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
			obj2 = LBModel.sum(obj2, LBModel.prod(boardSet.get(t).getPrice(), Y[t]));
		}

		ObjCost.setExpr(LBModel.sum(obj1, obj2));
	}

	public void setParam(double maxRunTime, double gap) throws IloException {
		LBModel.setParam(IloCplex.DoubleParam.TiLim, maxRunTime);
		LBModel.setParam(IloCplex.DoubleParam.EpGap, gap);
	}

}
