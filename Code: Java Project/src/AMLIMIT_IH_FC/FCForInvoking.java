package AMLIMIT_IH_FC;

public class FCForInvoking {

	InputExcel_ForIISE inputExcel;
	double[][] unmetQ1;
	IfUnmet ifUmet;

	public double FCCostB, FCCostP;
	public int FCXquantt, FCYquantt;
	public int[] FCusePlate;
	public int[] FCuseBoard;
	public int[][] FCproduceZ;
	public double[][] FCproduceQsl;
	public double[][][] FCQsll;
	public double[][] FCleft;
	public double totalAreaSelected;
	public double chuLiaoArea;
	public double inventoryArea;

	public FCForInvoking(InputExcel_ForIISE inputExcel, double[][] unmetQ) {
		this.inputExcel = inputExcel;
		this.unmetQ1 = unmetQ;
	}

	public void cal() throws Exception {

		double[][] unmetQ = new double[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.generalData
				.getNumOfProductGrades()];
		for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
			for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades(); l++) {
				unmetQ[s][l] = unmetQ1[s][l];
			}
		}

		IfUnmet ifUnmet = new IfUnmet();
		FCusePlate = new int[inputExcel.generalData.getNumOfPlates()];
		FCuseBoard = new int[inputExcel.boardSet.size()];
		FCproduceZ = new int[inputExcel.boardSet.size()][inputExcel.generalData.getNumOfCuttingNorms()];
		FCproduceQsl = new double[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.generalData
				.getNumOfProductGrades()];
		FCQsll = new double[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.generalData
				.getNumOfProductGrades()][inputExcel.generalData.getNumOfProductGrades()];
		FCleft = new double[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.generalData
				.getNumOfProductGrades()];

		for (int j = 0; j < inputExcel.generalData.getNumOfPlates(); j++) {
			for (int i = 0; i < inputExcel.plate[j].board.size(); i++) {
				System.out.println("FC board " + inputExcel.plate[j].board.get(i).getLength() + " "
						+ inputExcel.plate[j].board.get(i).getWidth() + " "
						+ inputExcel.plate[j].board.get(i).getGrade());
				double widthLeft = inputExcel.plate[j].board.get(i).getWidth();
				if (ifUnmet.ifUnmet(unmetQ)) {
					for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
						if (widthLeft >= inputExcel.cuttingNorms.getNorm()[s]) {
							if (ifUnmet.ifUnmetS(unmetQ, s)) {
								int quantityS = (int) Math.floor(inputExcel.plate[j].board.get(i).getWidth()
										/ inputExcel.cuttingNorms.getNorm()[s]);
								FCproduceZ[inputExcel.plate[j].boardTypeNo.get(i)][s] += quantityS;

								widthLeft -= quantityS * inputExcel.cuttingNorms.getNorm()[s];

								System.out.println(" Z [" + s + "] +" + quantityS);
								FCusePlate[j] = 1;
								FCuseBoard[inputExcel.plate[j].boardTypeNo.get(i)]++;

								for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades(); l++) {
									double lengthLeft = inputExcel.plate[j].board.get(i).getLength()
											* inputExcel.chuLiao.getRate()[inputExcel.plate[j].board.get(i).getGrade()
													- 1][l]
											* quantityS;
									FCproduceQsl[s][l] += lengthLeft;
									if (lengthLeft > 0) {
										for (int l1 = l; l1 < inputExcel.generalData.getNumOfProductGrades(); l1++) {
											if (unmetQ[s][l1] > 0) {
												if (unmetQ[s][l1] < lengthLeft) {
													FCQsll[s][l][l1] += unmetQ[s][l1];
													lengthLeft -= unmetQ[s][l1];
													unmetQ[s][l1] = 0;
												} else {
													FCQsll[s][l][l1] += lengthLeft;
													unmetQ[s][l1] -= lengthLeft;
													lengthLeft = 0;
													break;
												}
											}
										}
									}
									FCleft[s][l] += lengthLeft;
								}
							}
						} else {
							break;
						}

					}

				}
			}
		}
		double time2 = System.nanoTime();
		FCCostB = 0;
		FCCostP = 0;
		FCXquantt = 0;
		FCYquantt = 0;
		totalAreaSelected = 0;
		chuLiaoArea = 0;
		inventoryArea = 0;
		for (int j = 0; j < inputExcel.generalData.getNumOfPlates(); j++) {
			if (FCusePlate[j] == 1) {
				FCCostP += (inputExcel.plate[j].getPrice() * FCusePlate[j]);
				FCYquantt++;
			}
		}
		for (int t = 0; t < inputExcel.boardSet.size(); t++) {
			if (FCuseBoard[t] > 0) {
				FCXquantt += FCuseBoard[t];
				FCCostB += (inputExcel.boardSet.get(t).getPrice() * FCuseBoard[t]);
				System.out.println(" FC  t_ length = " + inputExcel.boardSet.get(t).getLength() + " width = "
						+ inputExcel.boardSet.get(t).getWidth() + " grade = " + inputExcel.boardSet.get(t).getGrade()
						+ " price = " + inputExcel.boardSet.get(t).getPrice() + "  usedQuantty = " + FCuseBoard[t]);


				totalAreaSelected += FCuseBoard[t] * inputExcel.boardSet.get(t).getMianJi();

			}
			for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {

				chuLiaoArea += FCproduceZ[t][s] * inputExcel.boardSet.get(t).getLength()
						* inputExcel.cuttingNorms.getNorm()[s];
			}
		}
		for (int s = 0; s < inputExcel.generalData.getNumOfCuttingNorms(); s++) {
			for (int l = 0; l < inputExcel.generalData.getNumOfProductGrades() - 1; l++) {
				inventoryArea += FCleft[s][l] * inputExcel.cuttingNorms.getNorm()[s];
			}
		}
	}
}
