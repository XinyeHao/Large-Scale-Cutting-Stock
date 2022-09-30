package AMLIMIT_IH_FC;

import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

public class LS_BasedOnLB {

	InputExcel_ForIISE inputExcel;
	int[] X_p_LB;
	int[] Y_t_LB;
	int[][] Z_nt_LB;
	int boardQuantitySet_thisT2;
	public double newObjValue;
	public int[] numOfBoardsCanBeUsed_t;
	public double thisIterationDeltaObj;

	public LS_BasedOnLB(InputExcel_ForIISE inputExcel, int[] X_p_LB, int[] Y_t_LB, int[][] Z_nt_LB) {
		this.inputExcel = inputExcel;
		this.X_p_LB = X_p_LB;
		this.Y_t_LB = Y_t_LB;
		this.Z_nt_LB = Z_nt_LB;
	}

	public void conductLocalSearch(double[][] unmetQ, int[] Y_Final, int[][] Z_nb_Final,
			int[] numOfBoardsFixedInT_Final, double[][][] Qngg, double[][] left) throws Exception {

		thisIterationDeltaObj=0;
		
		numOfBoardsCanBeUsed_t = new int[inputExcel.boardSet.size()];
		for (int b = 0; b < inputExcel.generalData.getNumOfBoards(); b++) {
			if (X_p_LB[inputExcel.board[b].getPlateNo()] == 1 && Y_Final[b] == 0) {
				numOfBoardsCanBeUsed_t[inputExcel.boardSetNo[b]]++;
			}
		}

		double wd_min = -1;
		for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
			for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
				if (unmetQ[n][g] > 0) {
					wd_min = inputExcel.cuttingNorms.getNorm()[n];
					break;
				}
			}
			if (wd_min != -1) {
				break;
			}
		}

		for (int t = 0; t < inputExcel.boardSet.size(); t++) {
			if (Y_t_LB[t] > 0) {

				if (inputExcel.boardSet.get(t).getWidth() < (2 * wd_min)) {// T_1
					for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
						if (Z_nt_LB[n][t] > 0) {
							numOfBoardsFixedInT_Final[t] += Z_nt_LB[n][t];
							int boardNeeded = Z_nt_LB[n][t];
							for (int b = 0; b < inputExcel.generalData.getNumOfBoards(); b++) {
								if (boardNeeded > 0) {
									if (inputExcel.boardSetNo[b] == t && Y_Final[b] == 0
											&& X_p_LB[inputExcel.board[b].getPlateNo()] == 1) {

										boardNeeded--;

										Y_Final[b] = 1;
										thisIterationDeltaObj+=inputExcel.boardSet.get(t).getPrice();
										Z_nb_Final[n][b]++;
										for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
											double LL = inputExcel.boardSet.get(t).getLength() * inputExcel.chuLiao
													.getRate()[inputExcel.boardSet.get(t).getGrade() - 1][g];
											for (int g1 = g; g1 < inputExcel.generalData
													.getNumOfProductGrades(); g1++) {
												if (LL > 0) {
													if (LL > unmetQ[n][g1]) {
														LL -= unmetQ[n][g1];
														Qngg[n][g][g1] += unmetQ[n][g1];
														unmetQ[n][g1] = 0;
													} else {// WL<unmetQ
														unmetQ[n][g1] -= LL;
														Qngg[n][g][g1] += LL;
														LL = 0;
														break;
													}
												} else {
													break;
												}
											}
											left[n][g] += LL;
										}
									}
								} else {
									break;
								}
							}
						}
					}
				} else {// T_2
					IloCplex BoardModel;
					IloObjective Obj;
					IloNumVar[] BM_Y;// Y_b
					IloNumVar[][] BM_Z_nb;// Z_nb

					BoardModel = new IloCplex();

					boardQuantitySet_thisT2 = 2 * Y_t_LB[t] - 1;

					BM_Y = new IloNumVar[boardQuantitySet_thisT2];
					BM_Z_nb = new IloNumVar[inputExcel.generalData.getNumOfCuttingNorms()][boardQuantitySet_thisT2];

					for (int b = 0; b < boardQuantitySet_thisT2; b++) {
						BM_Y[b] = BoardModel.numVar(0, 1, IloNumVarType.Int, "Y" + b);
						for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
							BM_Z_nb[n][b] = BoardModel.numVar(0, Integer.MAX_VALUE, IloNumVarType.Int,
									"Z" + n + "," + b);
						}
					}
					Obj = BoardModel.addMinimize();

					for (int b = 0; b < boardQuantitySet_thisT2; b++) {
						IloNumExpr expr = BoardModel.numExpr();
						for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
							expr = BoardModel.sum(expr,
									BoardModel.prod(inputExcel.cuttingNorms.getNorm()[n], BM_Z_nb[n][b]));
						}
						BoardModel.addLe(expr, BoardModel.prod(inputExcel.boardSet.get(t).getWidth(), BM_Y[b]));// wd*Z<=w*Y[b]
						if (b + 1 < boardQuantitySet_thisT2) {
							BoardModel.addLe(BM_Y[b + 1], BM_Y[b]);// Y[b+1]<=Y[b]
						}
					}

					for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
						if (Z_nt_LB[n][t] > 0) {
							IloNumExpr expr1 = BoardModel.numExpr();
							for (int b = 0; b < boardQuantitySet_thisT2; b++) {
								expr1 = BoardModel.sum(expr1, BM_Z_nb[n][b]);
							}
							BoardModel.addGe(expr1, Z_nt_LB[n][t]);
						}
					}

					IloNumExpr obj = BoardModel.numExpr();
					for (int b = 0; b < boardQuantitySet_thisT2; b++) {
						obj = BoardModel.sum(obj, BM_Y[b]);
					}
					Obj.setExpr(obj);
					BoardModel.setOut(null);
					BoardModel.solve();

					int numOfBoardsNeedToSearch = Math.min((int) BoardModel.getObjValue(), numOfBoardsCanBeUsed_t[t]);
					numOfBoardsFixedInT_Final[t] += numOfBoardsNeedToSearch;
					int numOfBoardsSearched = 0;
					for (int b = 0; b < inputExcel.generalData.getNumOfBoards(); b++) {
						if (numOfBoardsSearched < numOfBoardsNeedToSearch) {
							if (inputExcel.boardSetNo[b] == t && Y_Final[b] == 0
									&& X_p_LB[inputExcel.board[b].getPlateNo()] == 1) {

								Y_Final[b] = 1;
								thisIterationDeltaObj+=inputExcel.boardSet.get(t).getPrice();
								double widthLeft = inputExcel.boardSet.get(t).getWidth();
								for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
									if (Math.round(BoardModel.getValue(BM_Z_nb[n][numOfBoardsSearched])) > 0) {
										Z_nb_Final[n][b] += Math
												.round(BoardModel.getValue(BM_Z_nb[n][numOfBoardsSearched]));
										widthLeft -= Math.round(BoardModel.getValue(BM_Z_nb[n][numOfBoardsSearched]))
												* inputExcel.cuttingNorms.getNorm()[n];

										if (widthLeft < 0) {
											Thread.sleep(99999999);
										}

										for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
											double LL = Math.round(BoardModel.getValue(BM_Z_nb[n][numOfBoardsSearched]))
													* inputExcel.boardSet.get(t).getLength() * inputExcel.chuLiao
															.getRate()[inputExcel.boardSet.get(t).getGrade() - 1][g];

											for (int g1 = g; g1 < inputExcel.generalData
													.getNumOfProductGrades(); g1++) {
												if (LL > 0) {
													if (LL > unmetQ[n][g1]) {
														LL -= unmetQ[n][g1];
														Qngg[n][g][g1] += unmetQ[n][g1];
														unmetQ[n][g1] = 0;
													} else {// WL<unmetQ
														unmetQ[n][g1] -= LL;
														Qngg[n][g][g1] += LL;
														LL = 0;
														break;
													}
												} else {
													break;
												}
											}
											left[n][g] += LL;
										}
									}
								}

								IfUnmet ifUnmet = new IfUnmet();
								for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
									while (ifUnmet.ifUnmetS(unmetQ, n)) {
										if (widthLeft >= inputExcel.cuttingNorms.getNorm()[n]) {
											widthLeft -= inputExcel.cuttingNorms.getNorm()[n];
											Z_nb_Final[n][b]++;

											for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
												double LL = inputExcel.boardSet.get(t).getLength() * inputExcel.chuLiao
														.getRate()[inputExcel.boardSet.get(t).getGrade() - 1][g];
												for (int g1 = g; g1 < inputExcel.generalData
														.getNumOfProductGrades(); g1++) {
													if (LL > unmetQ[n][g1]) {
														Qngg[n][g][g1] += unmetQ[n][g1];
														LL -= unmetQ[n][g1];
														unmetQ[n][g1] = 0;
													} else {
														Qngg[n][g][g1] += LL;
														unmetQ[n][g1] -= LL;
														LL = 0;
														break;
													}
												}
												left[n][g] += LL;
											}
										} else {
											break;
										}
									}
									if (widthLeft < inputExcel.cuttingNorms.getNorm()[n]) {
										break;
									}
								}
								numOfBoardsSearched++;
							}
						} else {
							break;
						}
					}
				}
			}
		}
	}

}
