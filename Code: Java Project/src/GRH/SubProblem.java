package GRH;

import AMLIMIT_IH_FC.InputExcel_ForIISE;
import ilog.concert.*;
import ilog.cplex.IloCplex;

public class SubProblem {

	InputExcel_ForIISE inputExcel;
	int t;

	public SubProblem(InputExcel_ForIISE inputExcel, int t) {
		this.inputExcel = inputExcel;
		this.t = t;
	}

	IloCplex subModel;
	IloObjective ReducedCost;

	public IloNumVar[] Z;

	public void establishVarNConsts() throws IloException {

		subModel = new IloCplex();
		subModel.setOut(null);
		Z = new IloNumVar[inputExcel.generalData.getNumOfCuttingNorms()];

		for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
			Z[n] = subModel.numVar(0,
					(int) Math.floor(inputExcel.boardSet.get(t).getWidth() / inputExcel.cuttingNorms.getNorm()[n]),
					IloNumVarType.Int, "Z" + n);
		}
		ReducedCost = subModel.addMinimize();

		// Z
		IloNumExpr expr = subModel.numExpr();
		for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
			expr = subModel.sum(expr, subModel.prod(Z[n], inputExcel.cuttingNorms.getNorm()[n]));
		}
		subModel.addLe(expr, inputExcel.boardSet.get(t).getWidth());
	}

	public void establishObjFunction(double[] dualAlpha, double[][] dualBeta) throws IloException {
		double[] coefZn = new double[inputExcel.generalData.getNumOfCuttingNorms()];
		for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
			for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
				for (int g1 = 0; g1 <= g; g1++) {
					coefZn[n] -= dualBeta[n][g] * inputExcel.boardSet.get(t).getLength()
							* inputExcel.chuLiao.getRate()[inputExcel.boardSet.get(t).getGrade() - 1][g1];
				}
			}
		}

		IloNumExpr obj = subModel.numExpr();
		for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
			obj = subModel.sum(obj, subModel.prod(coefZn[n], Z[n]));
		}

		obj = subModel.sum(obj, dualAlpha[t]);
		obj = subModel.sum(obj, inputExcel.boardSet.get(t).getPrice());
		ReducedCost.setExpr(obj);
	}


	public void solve(double threshold, SolutionPool[] solutionPool) throws Exception {

		if (subModel.solve() == false) {
			Thread.sleep(99999999);
		} else {
			if (subModel.getObjValue() < threshold) {
				solutionPool[t].add();
				for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
					if ((int) Math.round(subModel.getValue(Z[n])) > 0) {
						solutionPool[t].Z[n].set(solutionPool[t].Z[n].size() - 1,
								(int) Math.round(subModel.getValue(Z[n])));
					}
				}
				solutionPool[t].calcuCoef();
			}
		}
	}

}
