package GRH;

import java.util.ArrayList;

import AMLIMIT_IH_FC.IfUnmet;
import AMLIMIT_IH_FC.InputExcel_ForIISE;
import ilog.concert.IloColumn;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class RMP {
	double[][] unmetQ;
	InputExcel_ForIISE inputExcel;
	int[] X_Final;//
	int[] Y_Final;
	int[] numOfBoardsFixedInT;
	int residualIteTime;

	public RMP(InputExcel_ForIISE inputExcel, double[][] unmetQ, int[] X_Final, int[] Y_Final,
			int[] numOfBoardsFixedInT, int residualIteTime) {
		this.unmetQ = unmetQ;
		this.inputExcel = inputExcel;
		this.X_Final = X_Final;
		this.Y_Final = Y_Final;
		this.numOfBoardsFixedInT = numOfBoardsFixedInT;
		this.residualIteTime = residualIteTime;
	}

	public IloCplex RMP;
	public IloObjective RMP_ObjCost;
	public IloNumVar[] X;//
	public ArrayList<IloNumVar>[] W;// W_t...k

	IloRange[] const_b_p;
	IloRange[][] demandConsts;
	IfUnmet ifUnmet;

	public void estab() throws Exception {
		ifUnmet = new IfUnmet();
		RMP = new IloCplex();
		RMP_ObjCost = RMP.addMinimize();
		X = new IloNumVar[inputExcel.generalData.getNumOfPlates()];//
		W = new ArrayList[inputExcel.boardSet.size()];

		for (int p = 0; p < inputExcel.generalData.getNumOfPlates(); p++) {
			X[p] = RMP.numVar(0, 1, IloNumVarType.Float, "X" + p);
		}

		for (int t = 0; t < inputExcel.boardSet.size(); t++) {
			W[t] = new ArrayList<IloNumVar>();
		}

		IloNumExpr objExpr = RMP.numExpr();
		for (int p = 0; p < inputExcel.generalData.getNumOfPlates(); p++) {
			objExpr = RMP.sum(objExpr, RMP.prod(inputExcel.plate[p].getPrice(), X[p]));
		}

		RMP_ObjCost.setExpr(objExpr);

		const_b_p = new IloRange[inputExcel.boardSet.size()];
		demandConsts = new IloRange[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.generalData
				.getNumOfProductGrades()];

		// Const1
		for (int t = 0; t < inputExcel.boardSet.size(); t++) {
			IloNumExpr expr = RMP.numExpr();
			for (int p = 0; p < inputExcel.generalData.getNumOfPlates(); p++) {
				expr = RMP.sum(expr, RMP.prod(X[p], inputExcel.plate[p].setIInPlate[t]));
			}
			expr = RMP.diff(expr, numOfBoardsFixedInT[t]);
			const_b_p[t] = RMP.addRange(0, expr, Double.MAX_VALUE);
		}

		// Const2
		for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
			for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
				if (unmetQ[n][g] > 0) {
					double demand = 0;
					for (int g1 = 0; g1 <= g; g1++) {
						demand += unmetQ[n][g1];
					}
					demandConsts[n][g] = RMP.addRange(0, RMP.diff(RMP.numExpr(), demand), Double.MAX_VALUE);
				}
			}
		}
		if (residualIteTime > 1) {
			for (int p = 0; p < inputExcel.generalData.getNumOfPlates(); p++) {
				if (X_Final[p] == 1) {
					RMP.addEq(X[p], 1);
				}
			}
		}
	}

	double[] dualAlpha;
	double[][] dualBeta;
	double GLB;
	int CGIteTime;
	SubProblem[] subProblem;
	public SolutionPool[] solutionPool;

	public void iterate() throws Exception {
		
		double danciTime=System.nanoTime();
		
		dualAlpha = new double[inputExcel.boardSet.size()];
		dualBeta = new double[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.generalData
				.getNumOfProductGrades()];
		GLB = 0;
		CGIteTime = 0;
		double thresForEachSubP = -0.00001;
		int maxIte = 500;
		double sumSubObj = 0;
		boolean contIte = true;
		subProblem = new SubProblem[inputExcel.boardSet.size()];
		solutionPool = new SolutionPool[inputExcel.boardSet.size()];
		for (int t = 0; t < inputExcel.boardSet.size(); t++) {
			subProblem[t] = new SubProblem(inputExcel, t);
			subProblem[t].establishVarNConsts();
			solutionPool[t] = new SolutionPool(inputExcel, t);
		}

		GenerateInitColumn geneInitCol = new GenerateInitColumn(inputExcel, unmetQ);
		geneInitCol.generate(Y_Final, solutionPool);
		for (int t = 0; t < solutionPool.length; t++) {
			if (solutionPool[t].size > 0) {
				for (int index = 0; index < solutionPool[t].size; index++) {
					IloColumn col = RMP.column(RMP_ObjCost, inputExcel.boardSet.get(t).getPrice());
					col = col.and(RMP.column(const_b_p[t], -1));
					for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
						if (ifUnmet.ifUnmetS(unmetQ, n)) {
							for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
								if (demandConsts[n][g] != null) {
									col = col
											.and(RMP.column(demandConsts[n][g], solutionPool[t].coef[n][g].get(index)));
								}
							}
						}
					}
					W[t].add(RMP.numVar(col, 0, inputExcel.boardSet.get(t).boardsInSet - numOfBoardsFixedInT[t],
							IloNumVarType.Float));
				}

			}
		}

		while (contIte) {
			CGIteTime++;
			sumSubObj = 0;
			System.out.println("CG_iterationTime = " + CGIteTime);
			contIte = false;
			RMP.solve();
			RMP.exportModel("RMP_test_ite1.lp");
			System.out.println("RMP Obj = " + RMP.getObjValue());


			dualAlpha = RMP.getDuals(const_b_p);

			for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
				for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
					if (demandConsts[n][g] != null) {
						dualBeta[n][g] = RMP.getDual(demandConsts[n][g]);
					}
				}
			}
			for (int t = 0; t < inputExcel.boardSet.size(); t++) {
				subProblem[t].establishObjFunction(dualAlpha, dualBeta);
				subProblem[t].solve(thresForEachSubP, solutionPool);
				if (subProblem[t].subModel.getObjValue() < thresForEachSubP) {
					sumSubObj += subProblem[t].subModel.getObjValue();
					contIte = true;
					IloColumn col = RMP.column(RMP_ObjCost, inputExcel.boardSet.get(t).getPrice());
					col = col.and(RMP.column(const_b_p[t], -1));
					for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
						for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
							if (unmetQ[n][g] > 0) {
								col = col.and(RMP.column(demandConsts[n][g],
										solutionPool[t].coef[n][g].get(solutionPool[t].coef[n][g].size() - 1)));
							}
						}
					}
					W[t].add(RMP.numVar(col, 0, inputExcel.boardSet.get(t).boardsInSet - numOfBoardsFixedInT[t],
							IloNumVarType.Float));
				}
			}
			if (CGIteTime == maxIte||(System.nanoTime()-danciTime)/1e9>=3600) {
				contIte = false;
				RMP.solve();
			}

		}

	}

}
