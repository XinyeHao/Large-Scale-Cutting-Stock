package AMLIMIT_IH_FC;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class Frame_AM_IH {
	public static void main(String[] args) throws Exception {

		String path = System.getProperty("user.dir");
		System.out.println(path);
		String suanliPath = path + "\\";
		GetFileName getFileName = new GetFileName();
		String[] fileName = getFileName.getFileName(suanliPath);
		double maxRunTime = 3600;
		double gap = 0.01;


		for (int fileNo = 0; fileNo < fileName.length; fileNo++) {
			try {

				System.out.println("Inputing" + fileName[fileNo]);
				InputExcel_ForIISE inputExcel = new InputExcel_ForIISE();
				String suanLiName = suanliPath + fileName[fileNo];
				inputExcel.input(suanLiName);
				

				try {

					GenerateQ generateQ = new GenerateQ(inputExcel);
					// generateQ.generateQBasedOnSquare();
					generateQ.generateQ();// 生成了q_sl

					IfUnmet ifUnmet = new IfUnmet();

					double[][] unmetQ = new double[generateQ.q.length][generateQ.q[0].length];
					for (int m = 0; m < unmetQ.length; m++) {
						for (int n = 0; n < unmetQ[0].length; n++) {
							unmetQ[m][n] = generateQ.q[m][n];
						}
					}


					double KMTime = System.nanoTime();
					AM km = new AM(inputExcel, generateQ);// 建立Model类的一个对象
					km.establishVar();
					km.establishConstraints();
					km.establishObjFunction();
					km.setParam(maxRunTime - (System.nanoTime() - KMTime) / 1e9, 0.01);
					if (km.KantoModel.solve()) {
						KMTime = (System.nanoTime() - KMTime) / 1e9;
						km.resultOutput();
					} else {
						System.out.println("Infeasible!!!");
					}
					

					// IH求解
					double cplexTime1 = System.nanoTime();

					// searchSpace
					int[] pSearch = new int[inputExcel.generalData.getNumOfPlates()];// 0: fix to 0; 1: fix to 1; 2:\le
					// finalSolution Y* Z*
					int[] Y_Final = new int[inputExcel.generalData.getNumOfBoards()];// Y*_b
					int[][] Z_nb_Final = new int[inputExcel.generalData
							.getNumOfCuttingNorms()][inputExcel.board.length];// Z*_nb
					int[] numOfBoardsFixedInT_Final = new int[inputExcel.boardSet.size()];// 

					double[][][] IHQngg = new double[inputExcel.generalData
							.getNumOfCuttingNorms()][inputExcel.generalData
									.getNumOfProductGrades()][inputExcel.generalData.getNumOfProductGrades()];// ngg
					double[][] IHLeft = new double[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.generalData
							.getNumOfProductGrades()];// n-g

					ArrayList<Double> IH_IterationTime = new ArrayList<Double>();
					ArrayList<Double> IH_DeltaObj = new ArrayList<Double>();
					ArrayList<Double> IH_SNRatio = new ArrayList<Double>();
					double deltaObjCalcu = 0;

					double sTime;
					double LBGivenByCAMLB = 0;

					int ite = 0;
					while (ifUnmet.ifUnmet(unmetQ)) {// 若还有未满足的延米，就继续迭代

						deltaObjCalcu = 0;

						sTime = System.nanoTime();

						// CAMUB
						int[] X_p_UB = new int[inputExcel.generalData.getNumOfPlates()];
						CAMUB HMUB = new CAMUB(inputExcel, pSearch, numOfBoardsFixedInT_Final, unmetQ);// M-II-UB
						HMUB.establishVar();
						HMUB.establishConstraints();
						HMUB.establishObjFunction();
						HMUB.setParam(maxRunTime, gap);
						if (HMUB.UBModel.solve()) {
							HMUB.resultOutput(X_p_UB);
						} else {
							HMUB.UBModel.exportModel("UBModel_inf.lp");
							Thread.sleep(99999999);
						}

						// CAMLB
						int[] X_p_LB = new int[inputExcel.generalData.getNumOfPlates()];// X_p
						int[] Y_t_LB = new int[inputExcel.boardSet.size()];// Y_t
						int[][] Z_nt_LB = new int[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.boardSet
								.size()];// Z_nt

						CAMLB HMLB = new CAMLB(inputExcel, pSearch, numOfBoardsFixedInT_Final, unmetQ);// CAMLB
						HMLB.establishVar();
						HMLB.establishConstraints();
						HMLB.establishObjFunction();
						HMLB.setParam(maxRunTime, gap);
						IH_SNRatio.add(HMLB.SNRatio);
						HMLB.LBModel.solve();
						if (ite == 1) {
							LBGivenByCAMLB = HMLB.LBModel.getObjValue() * (1 - HMLB.LBModel.getMIPRelativeGap());
						}
						HMLB.resultOutput(X_p_LB, Y_t_LB, Z_nt_LB);// generate these results, which are used to local
																	// search

						// local search based on LB
						LS_BasedOnLB ls_BasedOnLB = new LS_BasedOnLB(inputExcel, X_p_LB, Y_t_LB, Z_nt_LB);
						ls_BasedOnLB.conductLocalSearch(unmetQ, Y_Final, Z_nb_Final, numOfBoardsFixedInT_Final, IHQngg,
								IHLeft);
						deltaObjCalcu += ls_BasedOnLB.thisIterationDeltaObj;

						for (int p = 0; p < inputExcel.generalData.getNumOfPlates(); p++) {
							if (X_p_LB[p] == 1 && pSearch[p] == 0) {
								deltaObjCalcu += inputExcel.plate[p].getPrice();
								pSearch[p] = 1;
							}
							if (X_p_UB[p] > X_p_LB[p]) {
								pSearch[p] = 2;
							}
						}

						IH_DeltaObj.add(deltaObjCalcu);

						IH_IterationTime.add((System.nanoTime() - sTime) / 1e9);
					}

					double IHTime = (System.nanoTime() - cplexTime1) / 1e9;

					double IHCostB = 0, IHCostP = 0;
					int IHNumOfB = 0, IHNumOfP = 0;
					for (int p = 0; p < inputExcel.generalData.getNumOfPlates(); p++) {
						if (pSearch[p] == 1) {
							IHCostP += inputExcel.plate[p].getPrice();
							System.out.println(" COST_p [" + p + "] = " + inputExcel.plate[p].getPrice());
							IHNumOfP++;
						}
					}
					double[][] IHsuppQng = new double[inputExcel.generalData
							.getNumOfCuttingNorms()][inputExcel.generalData.getNumOfProductGrades()];
					for (int b = 0; b < inputExcel.board.length; b++) {
						if (Y_Final[b] == 1) {
							IHCostB += inputExcel.board[b].getPrice();
							IHNumOfB++;
							for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
								if (Z_nb_Final[n][b] > 0) {

									for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
										IHsuppQng[n][g] += Z_nb_Final[n][b] * inputExcel.board[b].getLength()
												* inputExcel.chuLiao.getRate()[inputExcel.board[b].getGrade() - 1][g];
									}
								}
							}
						}
					}
					System.out
							.println("IHCostP=" + IHCostP + ", IHCostB=" + IHCostB + "...total=" + (IHCostB + IHCostP));
					System.out.println("IH_TIME = " + IHTime);

					double FCTime = System.nanoTime();
					FCForInvoking fc = new FCForInvoking(inputExcel, generateQ.q);
					fc.cal();
					FCTime = (System.nanoTime() - FCTime) / 1e9;

					Workbook NewModelResult = new HSSFWorkbook();// 

					Sheet SheetResult = NewModelResult.createSheet("Result");
					String[] titleResult = { "CPU Time", "TotalCost", "PlateCost", "BoardCost", "NumOfPlates", "NumOfBoards", "SW", "CW",
							"IV", "AM_Gap", "CAMLB LB", "IH Iteration Information" };
					double[] runTime = new double[3];
					double[] totalCost = new double[3];
					double[] plateCost = new double[3];
					double[] boardCost = new double[3];
					int[] numOfPlate = new int[3];
					int[] numOfBoard = new int[3];
					double spliceWasteRate;
					double[] cuttingWasteRate = new double[3];
					double[] inventoryRate = new double[3];

					runTime[1] = IHTime;
					runTime[2] = FCTime;
					runTime[0] = KMTime;
					
					totalCost[1] = IHCostP + IHCostB;
					totalCost[2] = fc.FCCostB + fc.FCCostP;
					totalCost[0] = km.KantoModel.getObjValue();
					
					plateCost[1] = IHCostP;
					plateCost[2] = fc.FCCostP;
					plateCost[0] = km.plateCost;

					boardCost[1] = IHCostB;
					boardCost[2] = fc.FCCostB;
					boardCost[0] = km.boardCost;

					numOfPlate[1] = IHNumOfP;
					numOfPlate[2] = fc.FCYquantt;
					numOfPlate[0] = km.countX;

					numOfBoard[1] = IHNumOfB;
					numOfBoard[2] = fc.FCXquantt;
					numOfBoard[0] = km.countY;

					spliceWasteRate = 1 - generateQ.netSquareNeeded / generateQ.yanmiSquareNeeded;

					double totalAreaSelected = 0, chuLiaoArea = 0, inventoryArea = 0;
					for (int t = 0; t < inputExcel.boardSet.size(); t++) {
						if (numOfBoardsFixedInT_Final[t] > 0) {
							totalAreaSelected += inputExcel.boardSet.get(t).getLength()
									* inputExcel.boardSet.get(t).getWidth() * numOfBoardsFixedInT_Final[t];
						}

					}
					for (int b = 0; b < inputExcel.board.length; b++) {
						for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
							if (Z_nb_Final[n][b] > 0) {
								chuLiaoArea += inputExcel.board[b].getLength() * inputExcel.cuttingNorms.getNorm()[n]
										* Z_nb_Final[n][b];
							}
						}
					}
					for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
						for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades() - 1; g++) {
							inventoryArea += IHLeft[n][g] * inputExcel.cuttingNorms.getNorm()[n];
						}
					}
					cuttingWasteRate[1] = 1 - chuLiaoArea / totalAreaSelected;
					cuttingWasteRate[2] = 1 - fc.chuLiaoArea / fc.totalAreaSelected;
					cuttingWasteRate[0] = 1 - km.chuLiaoArea / km.totalAreaSelected;

					inventoryRate[1] = inventoryArea / totalAreaSelected;
					inventoryRate[2] = fc.inventoryArea / fc.totalAreaSelected;
					System.out.println(" problem invenRate = " + km.inventoryArea + " ///// " + km.totalAreaSelected);
					inventoryRate[0] = km.inventoryArea / km.totalAreaSelected;

					Row RowR = SheetResult.createRow(0);//
					Cell CellR = null;
					for (int biaoTou = 0; biaoTou < titleResult.length; biaoTou++) {
						CellR = RowR.createCell(biaoTou);
						CellR.setCellValue(titleResult[biaoTou]);
					}
					for (int r = 0; r < 3; r++) {
						Row Write = SheetResult.createRow(SheetResult.getPhysicalNumberOfRows());
						Write.createCell(0).setCellValue(runTime[r]);
						Write.createCell(1).setCellValue(totalCost[r]);
						Write.createCell(2).setCellValue(plateCost[r]);
						Write.createCell(3).setCellValue(boardCost[r]);
						Write.createCell(4).setCellValue(numOfPlate[r]);
						Write.createCell(5).setCellValue(numOfBoard[r]);
						Write.createCell(6).setCellValue(spliceWasteRate);
						Write.createCell(7).setCellValue(cuttingWasteRate[r]);
						Write.createCell(8).setCellValue(inventoryRate[r]);
						if (r == 0) {
							Write.createCell(9).setCellValue(km.KantoModel.getMIPRelativeGap());
						}else if (r == 1) {
							Write.createCell(10).setCellValue(LBGivenByCAMLB);
							for (int it = 0; it < IH_IterationTime.size(); it++) {
								Write.createCell(11 + it).setCellValue(IH_IterationTime.get(it));
							}
						}else {
							for (int it = 0; it < IH_DeltaObj.size(); it++) {
								Write.createCell(11 + it).setCellValue(IH_DeltaObj.get(it));
							}
						}
					}
					
					//addIHIteration deltaObj and sn ratio
					Row Write1 = SheetResult.createRow(SheetResult.getPhysicalNumberOfRows());
					for (int it = 0; it < IH_SNRatio.size(); it++) {
						Write1.createCell(11 + it).setCellValue(IH_SNRatio.get(it));
					}

					Sheet SplicePlan = NewModelResult.createSheet("SplicingPlan");
					String[] TitleSpli = { "ProductSN", "MT", "PLevel", "pl", "pw", "ph", "demand", "52", "62", "81",
							"89" };// 
					Row RowS = SplicePlan.createRow(0);// 
					Cell CellS = null;
					for (int biaoTou = 0; biaoTou < TitleSpli.length; biaoTou++) {
						CellS = RowS.createCell(biaoTou);
						CellS.setCellValue(TitleSpli[biaoTou]);
					}
					for (int p = 0; p < inputExcel.generalData.getNumOfProducts(); p++) {
						Row Write = SplicePlan.createRow(SplicePlan.getPhysicalNumberOfRows());
						Write.createCell(0).setCellValue(inputExcel.product[p].getPsn());
						Write.createCell(1).setCellValue(inputExcel.product[p].getMT());
						Write.createCell(2).setCellValue(inputExcel.product[p].getLevel());
						Write.createCell(3).setCellValue(inputExcel.product[p].getLength());
						Write.createCell(4).setCellValue(inputExcel.product[p].getWidth());
						Write.createCell(5).setCellValue(inputExcel.product[p].getHeight());
						Write.createCell(6).setCellValue(inputExcel.product[p].getDemand());
						Write.createCell(7).setCellValue(generateQ.P[p][0]);
						Write.createCell(8).setCellValue(generateQ.P[p][1]);
						Write.createCell(9).setCellValue(generateQ.P[p][2]);
						Write.createCell(10).setCellValue(generateQ.P[p][3]);
					}

					Sheet YanmiNeed = NewModelResult.createSheet("LongStripDemand");
					String[] Qsl_Title = { "Norm", "G1", "G2", "G3", "G4", "G5" };
					Row RowYanmiNeed = YanmiNeed.createRow(0);// 
					Cell CellYanmiNeed = null;
					for (int biaoTou = 0; biaoTou < Qsl_Title.length; biaoTou++) {
						CellYanmiNeed = RowYanmiNeed.createCell(biaoTou);
						CellYanmiNeed.setCellValue(Qsl_Title[biaoTou]);
					}
					for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
						Row Write = YanmiNeed.createRow(YanmiNeed.getPhysicalNumberOfRows());
						Write.createCell(0).setCellValue(inputExcel.cuttingNorms.getNorm()[s]);
						for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades(); l++) {
							Write.createCell(l + 1).setCellValue(generateQ.q[s][l]);
							// System.out.println("L278" + generateQ.q[s][l]);
						}
					}

					Sheet YanmiSuppM1 = NewModelResult.createSheet("AM Produce LongStrip");
					Row RowYanmiSuppM1 = YanmiSuppM1.createRow(0);//
					Cell CellYanmiSuppM1 = null;
					for (int biaoTou = 0; biaoTou < Qsl_Title.length; biaoTou++) {
						CellYanmiSuppM1 = RowYanmiSuppM1.createCell(biaoTou);
						CellYanmiSuppM1.setCellValue(Qsl_Title[biaoTou]);
					}
					for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
						Row Write = YanmiSuppM1.createRow(YanmiSuppM1.getPhysicalNumberOfRows());
						Write.createCell(0).setCellValue(inputExcel.cuttingNorms.getNorm()[s]);
						for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades(); l++) {
							Write.createCell(l + 1).setCellValue(km.produce[s][l]);
						}
					}

					Sheet YanmiAllocationM1 = NewModelResult.createSheet("AM LS-DemandSatis");
					String[] NormTitle = { "52", "62", "81", "89" };
					Row RowYanmiAlloM1 = YanmiAllocationM1.createRow(0);
					Cell CellYAM1 = null;
					for (int c = 0; c < 25; c++) {
						CellYAM1 = RowYanmiAlloM1.createCell(c + 1);
						if (c >= 0 && c < 5) {
							CellYAM1.setCellValue("1");
						} else if (c >= 5 && c < 10) {
							CellYAM1.setCellValue("2");
						} else if (c >= 10 && c < 15) {
							CellYAM1.setCellValue("3");
						} else if (c >= 15 && c < 20) {
							CellYAM1.setCellValue("4");
						} else {
							CellYAM1.setCellValue("5");
						}
					}
					RowYanmiAlloM1 = YanmiAllocationM1.createRow(1);
					for (int n = 1; n <= 5; n++) {
						for (int n1 = 1; n1 <= 5; n1++) {
							CellYAM1 = RowYanmiAlloM1.createCell((n - 1) * 5 + n1);
							CellYAM1.setCellValue(String.valueOf(n1));
						}
					}
					for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
						RowYanmiAlloM1 = YanmiAllocationM1.createRow(s + 2);
						CellYAM1 = RowYanmiAlloM1.createCell(0);
						CellYAM1.setCellValue(String.valueOf(inputExcel.cuttingNorms.getNorm()[s]));
						for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades(); l++) {
							for (int l1 = 0; l1 < inputExcel.generalData.getNumOfProductGrades(); l1++) {
								CellYAM1 = RowYanmiAlloM1.createCell(l * 5 + l1 + 1);
								CellYAM1.setCellValue(km.Qngg[s][l][l1]);
							}
						}
					}

					Sheet YanmiLeftM1 = NewModelResult.createSheet("AM LS Left");
					Row RowYanmiLeftM1 = YanmiLeftM1.createRow(0);
					Cell CellYLM1 = null;
					for (int biaoTou = 0; biaoTou < Qsl_Title.length; biaoTou++) {
						CellYLM1 = RowYanmiLeftM1.createCell(biaoTou);
						CellYLM1.setCellValue(Qsl_Title[biaoTou]);
					}
					for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
						Row Write = YanmiLeftM1.createRow(YanmiLeftM1.getPhysicalNumberOfRows());
						Write.createCell(0).setCellValue(inputExcel.cuttingNorms.getNorm()[s]);
						for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades(); l++) {
							Write.createCell(l + 1).setCellValue(km.left[s][l]);
						}
					}

					Sheet YanmiSuppIH = NewModelResult.createSheet("IH LS Produce");
					Row RowYanmiSuppIH = YanmiSuppIH.createRow(0);// 
					Cell CellYanmiSuppIH = null;
					for (int biaoTou = 0; biaoTou < Qsl_Title.length; biaoTou++) {
						CellYanmiSuppIH = RowYanmiSuppIH.createCell(biaoTou);
						CellYanmiSuppIH.setCellValue(Qsl_Title[biaoTou]);
					}
					for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
						Row Write = YanmiSuppIH.createRow(YanmiSuppIH.getPhysicalNumberOfRows());
						Write.createCell(0).setCellValue(inputExcel.cuttingNorms.getNorm()[s]);
						for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades(); l++) {
							Write.createCell(l + 1).setCellValue(IHsuppQng[s][l]);
						}
					}

					Sheet YanmiAllocationIH = NewModelResult.createSheet("IH LS-DemandSatis");
					Row RowYanmiAlloIH = YanmiAllocationIH.createRow(0);
					Cell CellYAIH = null;
					for (int c = 0; c < 25; c++) {
						CellYAIH = RowYanmiAlloIH.createCell(c + 1);
						if (c >= 0 && c < 5) {
							CellYAIH.setCellValue("1");
						} else if (c >= 5 && c < 10) {
							CellYAIH.setCellValue("2");
						} else if (c >= 10 && c < 15) {
							CellYAIH.setCellValue("3");
						} else if (c >= 15 && c < 20) {
							CellYAIH.setCellValue("4");
						} else {
							CellYAIH.setCellValue("5");
						}
					}
					RowYanmiAlloIH = YanmiAllocationIH.createRow(1);
					for (int n = 1; n <= 5; n++) {
						for (int n1 = 1; n1 <= 5; n1++) {
							CellYAIH = RowYanmiAlloIH.createCell((n - 1) * 5 + n1);
							CellYAIH.setCellValue(String.valueOf(n1));
						}
					}
					for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
						RowYanmiAlloIH = YanmiAllocationIH.createRow(s + 2);
						CellYAIH = RowYanmiAlloIH.createCell(0);
						CellYAIH.setCellValue(String.valueOf(inputExcel.cuttingNorms.getNorm()[s]));
						for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades(); l++) {
							for (int l1 = 0; l1 < inputExcel.generalData.getNumOfProductGrades(); l1++) {
								CellYAIH = RowYanmiAlloIH.createCell(l * 5 + l1 + 1);
								CellYAIH.setCellValue(IHQngg[s][l][l1]);
							}
						}
					}

					Sheet YanmiLeftIH = NewModelResult.createSheet("IH LS Left");
					Row RowYanmiLeftIH = YanmiLeftIH.createRow(0);
					Cell CellYLIH = null;
					for (int biaoTou = 0; biaoTou < Qsl_Title.length; biaoTou++) {
						CellYLIH = RowYanmiLeftIH.createCell(biaoTou);
						CellYLIH.setCellValue(Qsl_Title[biaoTou]);
					}
					for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
						Row Write = YanmiLeftIH.createRow(YanmiLeftIH.getPhysicalNumberOfRows());
						Write.createCell(0).setCellValue(inputExcel.cuttingNorms.getNorm()[s]);
						for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades(); l++) {
							Write.createCell(l + 1).setCellValue(IHLeft[s][l]);
						}
					}

					Sheet YanmiSuppFC = NewModelResult.createSheet("FC LS Produce");
					Row RowYanmiSuppFC = YanmiSuppFC.createRow(0);// 
					Cell CellYanmiSuppFC = null;
					for (int biaoTou = 0; biaoTou < Qsl_Title.length; biaoTou++) {
						CellYanmiSuppFC = RowYanmiSuppFC.createCell(biaoTou);
						CellYanmiSuppFC.setCellValue(Qsl_Title[biaoTou]);
					}
					for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
						Row Write = YanmiSuppFC.createRow(YanmiSuppFC.getPhysicalNumberOfRows());
						Write.createCell(0).setCellValue(inputExcel.cuttingNorms.getNorm()[s]);
						for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades(); l++) {
							Write.createCell(l + 1).setCellValue(fc.FCproduceQsl[s][l]);
						}
					}

					Sheet YanmiAllocationFC = NewModelResult.createSheet("FC LS-DemandSatis");
					Row RowYanmiAlloFC = YanmiAllocationFC.createRow(0);
					Cell CellYAFC = null;
					for (int c = 0; c < 25; c++) {
						CellYAFC = RowYanmiAlloFC.createCell(c + 1);
						if (c >= 0 && c < 5) {
							CellYAFC.setCellValue("1");
						} else if (c >= 5 && c < 10) {
							CellYAFC.setCellValue("2");
						} else if (c >= 10 && c < 15) {
							CellYAFC.setCellValue("3");
						} else if (c >= 15 && c < 20) {
							CellYAFC.setCellValue("4");
						} else {
							CellYAFC.setCellValue("5");
						}
					}
					RowYanmiAlloFC = YanmiAllocationFC.createRow(1);
					for (int n = 1; n <= 5; n++) {
						for (int n1 = 1; n1 <= 5; n1++) {
							CellYAFC = RowYanmiAlloFC.createCell((n - 1) * 5 + n1);
							CellYAFC.setCellValue(String.valueOf(n1));
						}
					}
					for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
						RowYanmiAlloFC = YanmiAllocationFC.createRow(s + 2);
						CellYAFC = RowYanmiAlloFC.createCell(0);
						CellYAFC.setCellValue(String.valueOf(inputExcel.cuttingNorms.getNorm()[s]));
						for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades(); l++) {
							for (int l1 = 0; l1 < inputExcel.generalData.getNumOfProductGrades(); l1++) {
								CellYAFC = RowYanmiAlloFC.createCell(l * 5 + l1 + 1);
								CellYAFC.setCellValue(fc.FCQsll[s][l][l1]);
							}
						}
					}

					Sheet YanmiLeftFC = NewModelResult.createSheet("FC LS Left");
					Row RowYanmiLeftFC = YanmiLeftFC.createRow(0);
					Cell CellYLFC = null;
					for (int biaoTou = 0; biaoTou < Qsl_Title.length; biaoTou++) {
						CellYLFC = RowYanmiLeftFC.createCell(biaoTou);
						CellYLFC.setCellValue(Qsl_Title[biaoTou]);
					}
					for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
						Row Write = YanmiLeftFC.createRow(YanmiLeftFC.getPhysicalNumberOfRows());
						Write.createCell(0).setCellValue(inputExcel.cuttingNorms.getNorm()[s]);
						for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades(); l++) {
							Write.createCell(l + 1).setCellValue(fc.FCleft[s][l]);
						}
					}

					try {
						String NMOutPut = path + fileName[fileNo] + ".xls";
						FileOutputStream fos = new FileOutputStream(NMOutPut);
						NewModelResult.write(fos);
						fos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (Exception in) {
				in.printStackTrace();
			}
		}

	}
}
