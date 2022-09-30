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

public class CAMUB_N {

	GeneralData generalData;
	Plate[] plate;
	Product[] product;
	CuttingNorms cuttingNorms;
	ChuLiao chuLiao;
	ArrayList<BoardSet> boardSet;
	double wd_max;

	double[][] unmetQ;

	public CAMUB_N(InputExcel_ForIISE inputExcel, double[][] unmetQ) {// 
		this.generalData = inputExcel.generalData;
		this.plate = inputExcel.plate;
		this.product = inputExcel.product;
		this.cuttingNorms = inputExcel.cuttingNorms;
		this.chuLiao = inputExcel.chuLiao;
		this.boardSet = inputExcel.boardSet;
		this.unmetQ = unmetQ;
	}

	public IloCplex UBModel;
	public IloObjective ObjCost;

	public IloNumVar[] X;
	public IloNumVar[] Y;
	public IloNumVar[][] Z;

	public void establishVar() throws IloException {

		UBModel = new IloCplex();
		X = new IloNumVar[generalData.getNumOfPlates()];// X_p
		Y = new IloNumVar[boardSet.size()];// Y_t
		Z = new IloNumVar[generalData.getNumOfCuttingNorms()][boardSet.size()];// Z_nt

		for (int p = 0; p < generalData.getNumOfPlates(); p++) {
			X[p] = UBModel.numVar(0, 1, IloNumVarType.Int, "X" + p);
		}

		for (int t = 0; t < boardSet.size(); t++) {
			Y[t] = UBModel.numVar(0, boardSet.get(t).boardsInSet, IloNumVarType.Int, "Y" + t);
		}

		for (int t = 0; t < boardSet.size(); t++) {
			for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
				Z[n][t] = UBModel.numVar(0, Integer.MAX_VALUE, IloNumVarType.Int, "Z" + n + "," + t);
			}
		}

		ObjCost = UBModel.addMinimize();

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

		for (int t = 0; t < boardSet.size(); t++) {
			IloNumExpr expr = UBModel.numExpr();
			for (int p = 0; p < generalData.getNumOfPlates(); p++) {
				if (X[p] != null) {
					expr = UBModel.sum(expr, UBModel.prod(X[p], plate[p].setIInPlate[t]));
				}
			}
			UBModel.addLe(Y[t], expr);
		}

		for (int t = 0; t < boardSet.size(); t++) {
			IloNumExpr expr = UBModel.numExpr();
			for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
				expr = UBModel.sum(expr, UBModel.prod(cuttingNorms.getNorm()[n], Z[n][t]));
			}
			UBModel.addLe(expr, UBModel.prod(Math.max(0, boardSet.get(t).getWidth() - wd_max), Y[t]));
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
	}

	public void setParam(double maxRunTime, double gap) throws IloException {
		UBModel.setParam(IloCplex.DoubleParam.TiLim, maxRunTime);
		UBModel.setParam(IloCplex.DoubleParam.EpGap, gap);
	}

}
