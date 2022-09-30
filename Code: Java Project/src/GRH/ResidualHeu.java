package GRH;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import AMLIMIT_IH_FC.FCForInvoking;
import AMLIMIT_IH_FC.GenerateQ;
import AMLIMIT_IH_FC.GetFileName;
import AMLIMIT_IH_FC.IfUnmet;
import AMLIMIT_IH_FC.InputExcel_ForIISE;

public class ResidualHeu {

	public static void main(String[] args) throws Exception {

		String path = System.getProperty("user.dir");
		System.out.println(path);
		String suanliPath = path + "\\";
		GetFileName getFileName = new GetFileName();
		String[] fileName = getFileName.getFileName(suanliPath);
		double maxRunTime = 3600;

		for (int fileNo = 0; fileNo < fileName.length; fileNo++) {
			try {

				InputExcel_ForIISE inputExcel = new InputExcel_ForIISE();
				String suanLiName = suanliPath + fileName[fileNo];
				inputExcel.input(suanLiName);

				try {

					GenerateQ generateQ = new GenerateQ(inputExcel);
					generateQ.generateQ();

					IfUnmet ifUnmet = new IfUnmet();

					double[][] unmetQ = new double[generateQ.q.length][generateQ.q[0].length];
					for (int m = 0; m < unmetQ.length; m++) {
						for (int n = 0; n < unmetQ[0].length; n++) {
							unmetQ[m][n] = generateQ.q[m][n];
						}
					}

					System.out.println("residual heuristic");
					double CGHTime1 = System.nanoTime();

					// searchSpace
					int[] X_Final = new int[inputExcel.generalData.getNumOfPlates()];//
					// finalSolution Y* Z*
					int[] Y_Final = new int[inputExcel.generalData.getNumOfBoards()];// Y*_b
					int[][] Z_nt = new int[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.boardSet.size()];// Z*_nt

					int[] numOfBoardsFixedInT = new int[inputExcel.boardSet.size()];

					double[][][] CGHQngg = new double[inputExcel.generalData
							.getNumOfCuttingNorms()][inputExcel.generalData
									.getNumOfProductGrades()][inputExcel.generalData.getNumOfProductGrades()];// ngg
					double[][] CGHLeft = new double[inputExcel.generalData
							.getNumOfCuttingNorms()][inputExcel.generalData.getNumOfProductGrades()];// n-g

					ArrayList<Double> CGHIterationTime = new ArrayList<Double>();
					double sTimeThisCGHIte;
					double LBGivenByCG = 0;

					int residualIteTime = 0;

					while (ifUnmet.ifUnmet(unmetQ)) {

						sTimeThisCGHIte = System.nanoTime();

						RMP rmp = new RMP(inputExcel, unmetQ, X_Final, Y_Final, numOfBoardsFixedInT, residualIteTime);
						rmp.estab();
						
						rmp.iterate();
						
						if (residualIteTime == 1) {
							LBGivenByCG = rmp.RMP.getObjValue();
						}

						for (int p = 0; p < inputExcel.generalData.getNumOfPlates(); p++) {
							if (rmp.RMP.getValue(rmp.X[p]) > 0) {
								X_Final[p] = 1;
							}
						}
						int needToFix_T = 0;

						ArrayList<Pattern> sortedPattern = new ArrayList();
						for (int t = 0; t < inputExcel.boardSet.size(); t++) {
							for (int index = 0; index < rmp.W[t].size(); index++) {
								if (rmp.RMP.getValue(rmp.W[t].get(index)) > 0) {
									sortedPattern.add(new Pattern(inputExcel, t));
									sortedPattern.get(sortedPattern.size() - 1).t = t;
									sortedPattern.get(sortedPattern.size() - 1).RMP_Value = rmp.RMP
											.getValue(rmp.W[t].get(index));
									for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
										if (rmp.solutionPool[t].Z[n].get(index) > 0) {
											sortedPattern.get(sortedPattern.size() - 1).Z[n] = rmp.solutionPool[t].Z[n]
													.get(index);
										}
									}
								}
							}
						}
						Collections.sort(sortedPattern, new Comparator<Pattern>() {
							@Override
							public int compare(Pattern o1, Pattern o2) {
								int r;
								if (o2.RMP_Value > o1.RMP_Value) {
									r = 1;
								} else if (o2.RMP_Value == o1.RMP_Value) {
									r = 0;
								} else {
									r = -1;
								}
								return r;
							}
						});

						boolean stopThisPattern = false;
						for (int index = 0; index < sortedPattern.size(); index++) {

							for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
								if (sortedPattern.get(index).Z[n] > 0 && !ifUnmet.ifUnmetS(unmetQ, n)) {
									stopThisPattern = true;
									break;
								}
							}

							needToFix_T = Math.max(1, (int) Math.ceil(sortedPattern.get(index).RMP_Value));

							for (int b = 0; b < inputExcel.generalData.getNumOfBoards(); b++) {
								if (needToFix_T > 0 && !stopThisPattern) {
									if (inputExcel.boardSetNo[b] == sortedPattern.get(index).t && Y_Final[b] == 0) {
										Y_Final[b] = 1;
										needToFix_T--;

										numOfBoardsFixedInT[sortedPattern.get(index).t]++;

										for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
											if (sortedPattern.get(index).Z[n] > 0) {

												Z_nt[n][sortedPattern.get(index).t] += sortedPattern.get(index).Z[n]
														* 1;

												for (int g = 0; g < inputExcel.generalData
														.getNumOfProductGrades(); g++) {
													double lengthLeft = inputExcel.boardSet
															.get(sortedPattern.get(index).t).getLength()
															* inputExcel.chuLiao.getRate()[inputExcel.boardSet
																	.get(sortedPattern.get(index).t).getGrade() - 1][g]
															* sortedPattern.get(index).Z[n] * 1;
													if (lengthLeft > 0) {
														for (int g1 = g; g1 < inputExcel.generalData
																.getNumOfProductGrades(); g1++) {
															if (unmetQ[n][g1] > 0) {
																if (unmetQ[n][g1] < lengthLeft) {
																	lengthLeft -= unmetQ[n][g1];
																	CGHQngg[n][g][g1] += unmetQ[n][g1];
																	unmetQ[n][g1] = 0;
																} else {
																	unmetQ[n][g1] -= lengthLeft;
																	CGHQngg[n][g][g1] += lengthLeft;
																	lengthLeft = 0;
																	break;
																}
															}
														}
													}
													CGHLeft[n][g] += lengthLeft;
												}
											}
										}

										for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
											if (sortedPattern.get(index).Z[n] > 0 && !ifUnmet.ifUnmetS(unmetQ, n)) {
												stopThisPattern = true;
												break;
											}
										}
										if (stopThisPattern) {
											break;
										}

									}
								} else {
									break;
								}
							}
							if (stopThisPattern) {
								break;
							}
						}

						CGHIterationTime.add((System.nanoTime() - sTimeThisCGHIte) / 1e9);
					}

					double IHTime = (System.nanoTime() - CGHTime1) / 1e9;

					double IHCostB = 0, IHCostP = 0;
					int IHNumOfB = 0, IHNumOfP = 0;
					for (int p = 0; p < inputExcel.generalData.getNumOfPlates(); p++) {
						if (X_Final[p] == 1) {
							IHCostP += inputExcel.plate[p].getPrice();
							IHNumOfP++;
						}
					}
					double[][] IHsuppQng = new double[inputExcel.generalData
							.getNumOfCuttingNorms()][inputExcel.generalData.getNumOfProductGrades()];
					for (int t = 0; t < inputExcel.boardSet.size(); t++) {
						if (numOfBoardsFixedInT[t] > 0) {
							IHCostB += numOfBoardsFixedInT[t] * inputExcel.boardSet.get(t).getPrice();
							IHNumOfB += numOfBoardsFixedInT[t];
							for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
								for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
									IHsuppQng[n][g] += Z_nt[n][t] * inputExcel.boardSet.get(t).getLength()
											* inputExcel.chuLiao.getRate()[inputExcel.boardSet.get(t).getGrade()
													- 1][g];
								}
							}
						}
					}

					double FCTime = System.nanoTime();
					FCForInvoking fc = new FCForInvoking(inputExcel, generateQ.q);
					fc.cal();
					FCTime = (System.nanoTime() - FCTime) / 1e9;

					Workbook NewModelResult = new HSSFWorkbook();

					Sheet SheetResult = NewModelResult.createSheet("Result");
					String[] titleResult = { "CPU Time", "TotalCost", "PlateCost", "BoardCost", "NumOfPlates", "NumOfBoards", "SW", "CW",
							"IV", "RelativeGap", "Valid LB", "Iteration Time" };
					double[] runTime = new double[2];
					double[] totalCost = new double[2];
					double[] plateCost = new double[2];
					double[] boardCost = new double[2];
					int[] numOfPlate = new int[2];
					int[] numOfBoard = new int[2];
					double spliceWasteRate;
					double[] cuttingWasteRate = new double[2];
					double[] inventoryRate = new double[2];

					runTime[0] = IHTime;
					runTime[1] = FCTime;

					totalCost[0] = IHCostP + IHCostB;
					totalCost[1] = fc.FCCostB + fc.FCCostP;

					plateCost[0] = IHCostP;
					plateCost[1] = fc.FCCostP;

					boardCost[0] = IHCostB;
					boardCost[1] = fc.FCCostB;

					numOfPlate[0] = IHNumOfP;
					numOfPlate[1] = fc.FCYquantt;

					numOfBoard[0] = IHNumOfB;
					numOfBoard[1] = fc.FCXquantt;

					spliceWasteRate = 1 - generateQ.netSquareNeeded / generateQ.yanmiSquareNeeded;

					double totalAreaSelected = 0, chuLiaoArea = 0, inventoryArea = 0;
					for (int t = 0; t < inputExcel.boardSet.size(); t++) {
						if (numOfBoardsFixedInT[t] > 0) {
							totalAreaSelected += inputExcel.boardSet.get(t).getLength()
									* inputExcel.boardSet.get(t).getWidth() * numOfBoardsFixedInT[t];
						}

					}
					for (int t = 0; t < inputExcel.boardSet.size(); t++) {
						for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
							if (Z_nt[n][t] > 0) {
								chuLiaoArea += inputExcel.boardSet.get(t).getLength()
										* inputExcel.cuttingNorms.getNorm()[n] * Z_nt[n][t];
							}
						}
					}
					for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
						for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades() - 1; g++) {
							inventoryArea += CGHLeft[n][g] * inputExcel.cuttingNorms.getNorm()[n];
						}
					}
					cuttingWasteRate[0] = 1 - chuLiaoArea / totalAreaSelected;
					cuttingWasteRate[1] = 1 - fc.chuLiaoArea / fc.totalAreaSelected;


					inventoryRate[0] = inventoryArea / totalAreaSelected;
					inventoryRate[1] = fc.inventoryArea / fc.totalAreaSelected;

					Row RowR = SheetResult.createRow(0);
					Cell CellR = null;
					for (int biaoTou = 0; biaoTou < titleResult.length; biaoTou++) {
						CellR = RowR.createCell(biaoTou);
						CellR.setCellValue(titleResult[biaoTou]);
					}
					for (int r = 0; r < 2; r++) {
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
						Write.createCell(9).setCellValue(1 - LBGivenByCG / totalCost[r]);
						if (r == 0) {
							Write.createCell(10).setCellValue(LBGivenByCG);
							for (int it = 0; it < CGHIterationTime.size(); it++) {
								Write.createCell(11 + it).setCellValue(CGHIterationTime.get(it));
							}
						}
					}

					Sheet SplicePlan = NewModelResult.createSheet("SplicingPlan");
					String[] TitleSpli = { "ProductSN", "MT", "PLevel", "pl", "pw", "ph", "demand", "52", "62", "81",
							"89" };
					Row RowS = SplicePlan.createRow(0);
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

					Sheet YanmiNeed = NewModelResult.createSheet("LS Demands");
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
						}
					}

					Sheet YanmiSuppIH = NewModelResult.createSheet("GRH Produce");
					Row RowYanmiSuppIH = YanmiSuppIH.createRow(0);
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

					Sheet YanmiAllocationIH = NewModelResult.createSheet("GRH LS-DemandsSatis");
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
								CellYAIH.setCellValue(CGHQngg[s][l][l1]);
							}
						}
					}

					Sheet YanmiLeftIH = NewModelResult.createSheet("GRH LS Left");
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
							Write.createCell(l + 1).setCellValue(CGHLeft[s][l]);
						}
					}

					Sheet YanmiSuppFC = NewModelResult.createSheet("FC LS Produce");
					Row RowYanmiSuppFC = YanmiSuppFC.createRow(0);
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

					Sheet YanmiAllocationFC = NewModelResult.createSheet("FC LS-DemandsSatis");
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
						String NMOutPut = path + "\\" + fileName[fileNo] + ".xls";
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
