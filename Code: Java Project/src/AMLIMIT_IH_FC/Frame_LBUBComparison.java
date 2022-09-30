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

public class Frame_LBUBComparison {
	public static void main(String[] args) throws Exception {

		String path = System.getProperty("user.dir");
		System.out.println(path);
		String suanliPath = path + "\\";
		GetFileName getFileName = new GetFileName();
		String[] fileName = getFileName.getFileName(suanliPath);
		double maxRunTime = 3600;
		double gap = 0.01;


		int[] numOfPlateLB = new int[fileName.length];
		int[] numOfPlateUB = new int[fileName.length];
		int[] numOfBoardTypeLB = new int[fileName.length];
		int[] numOfBoardTypeUB = new int[fileName.length];
		int[] numOfBoardLB = new int[fileName.length];
		int[] numOfBoardUB = new int[fileName.length];
		double[] plateSimilarity = new double[fileName.length];
		double[] boardSimilarity = new double[fileName.length];

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

					int[] pSearch = new int[inputExcel.generalData.getNumOfPlates()];// 0: fix to 0; 1: fix to 1; 2:\le
					int[] numOfBoardsFixedInT_Final = new int[inputExcel.boardSet.size()];// 

					// ÇóCAMLB
					int[] X_p_LB = new int[inputExcel.generalData.getNumOfPlates()];// X_p
					int[] Y_t_LB = new int[inputExcel.boardSet.size()];// Y_t
					int[][] Z_nt_LB = new int[inputExcel.generalData.getNumOfCuttingNorms()][inputExcel.boardSet
							.size()];// Z_nt

					CAMLB HMLB = new CAMLB(inputExcel, pSearch, numOfBoardsFixedInT_Final, unmetQ);// CAMLB
					HMLB.establishVar();
					HMLB.establishConstraints();
					HMLB.establishObjFunction();
					HMLB.setParam(maxRunTime, gap);
					HMLB.LBModel.setOut(null);
					HMLB.LBModel.solve();
					HMLB.resultOutput(X_p_LB, Y_t_LB, Z_nt_LB);//

					// ÇóCAMUB
					int[] X_p_UB = new int[inputExcel.generalData.getNumOfPlates()];
					int[] Y_t_UB = new int[inputExcel.boardSet.size()];// Y_t
					CAMUB HMUB = new CAMUB(inputExcel, pSearch, numOfBoardsFixedInT_Final, unmetQ);// M-II-UB
					HMUB.establishVar();
					HMUB.establishConstraints();
					HMUB.establishObjFunction();
					HMUB.setParam(maxRunTime, gap);
					HMUB.UBModel.setOut(null);
					if (HMUB.UBModel.solve()) {
						// HMUB.UBModel.exportModel("UBModel_feasible" + String.valueOf(ite) + ".lp");
						HMUB.resultOutput(X_p_UB);
						for (int t = 0; t < inputExcel.boardSet.size(); t++) {
							if (Math.round(HMUB.UBModel.getValue(HMUB.Y[t])) > 0) {
								Y_t_UB[t] = (int) Math.round(HMUB.UBModel.getValue(HMUB.Y[t]));
							}
						}
					} else {
						Thread.sleep(99999999);
					}

					double CRP = 0, CRT = 0, CRTFenMu = 0;

					for (int p = 0; p < inputExcel.plate.length; p++) {
						CRP += X_p_LB[p] * X_p_UB[p];
						numOfPlateLB[fileNo] += X_p_LB[p];
						numOfPlateUB[fileNo] += X_p_UB[p];
					}
					for (int t = 0; t < inputExcel.boardSet.size(); t++) {
						CRT += Math.min(1, Y_t_LB[t]) * Math.min(1, Y_t_UB[t]);
						CRTFenMu += Math.min(1, Y_t_LB[t]);
						numOfBoardLB[fileNo] += Y_t_LB[t];
						numOfBoardUB[fileNo] += Y_t_UB[t];
						numOfBoardTypeLB[fileNo] += Math.min(1, Y_t_LB[t]);
						numOfBoardTypeUB[fileNo] += Math.min(1, Y_t_UB[t]);
					}

					plateSimilarity[fileNo] = CRP / numOfPlateLB[fileNo];
					boardSimilarity[fileNo] = CRT / CRTFenMu;

					System.out.println("Instance " + fileName[fileNo] + " plateLB = " + numOfPlateLB[fileNo]
							+ " plateUB = " + numOfPlateUB[fileNo] + "\n boardLB = " + numOfBoardTypeLB[fileNo]
							+ " boardUB = " + numOfBoardTypeUB[fileNo] + " CRP = " + plateSimilarity[fileNo] + " CRB = "
							+ boardSimilarity[fileNo]);

				} catch (Exception e) {
					e.printStackTrace();
				}

			} catch (Exception in) {
				in.printStackTrace();
			}
		}

		Workbook NewModelResult = new HSSFWorkbook();// 

		Sheet SheetResult = NewModelResult.createSheet("SimilarityOutput");
		String[] titleResult = { "InstanceNo", "PlateLB", "PlateUB", "BoardLB", "BoardUB", "BoardTypeLB", "BoardTypeUB",
				"CRP", "CRB" };

		Row RowR = SheetResult.createRow(0);//
		Cell CellR = null;
		for (int biaoTou = 0; biaoTou < titleResult.length; biaoTou++) {
			CellR = RowR.createCell(biaoTou);
			CellR.setCellValue(titleResult[biaoTou]);
		}
		for (int r = 0; r < fileName.length; r++) {
			Row Write = SheetResult.createRow(SheetResult.getPhysicalNumberOfRows());
			Write.createCell(0).setCellValue(r + 1);
			Write.createCell(1).setCellValue(numOfPlateLB[r]);
			Write.createCell(2).setCellValue(numOfPlateUB[r]);
			Write.createCell(3).setCellValue(numOfBoardLB[r]);
			Write.createCell(4).setCellValue(numOfBoardUB[r]);
			Write.createCell(5).setCellValue(numOfBoardTypeLB[r]);
			Write.createCell(6).setCellValue(numOfBoardTypeUB[r]);
			Write.createCell(7).setCellValue(plateSimilarity[r]);
			Write.createCell(8).setCellValue(boardSimilarity[r]);
		}

		try {
			String NMOutPut = path + "\\similarity.xls";
			FileOutputStream fos = new FileOutputStream(NMOutPut);
			NewModelResult.write(fos);
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
