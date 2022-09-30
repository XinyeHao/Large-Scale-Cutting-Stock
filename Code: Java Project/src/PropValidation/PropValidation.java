package PropValidation;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import AMLIMIT_IH_FC.GenerateQ;
import AMLIMIT_IH_FC.GetFileName;
import AMLIMIT_IH_FC.IfUnmet;
import AMLIMIT_IH_FC.InputExcel_ForIISE;

public class PropValidation {
	public static void main(String[] args) throws Exception {

		String path = System.getProperty("user.dir");
		System.out.println(path);
		String suanliPath = path + "\\";
		GetFileName getFileName = new GetFileName();
		String[] fileName = getFileName.getFileName(suanliPath);
		double maxRunTime = 3600;
		double gap = 0.01;


		double[] AM_Value = new double[fileName.length];
		double[] CAMLBOPT = new double[fileName.length];
		double[] CAMLB_OPTBar = new double[fileName.length];
		double[] CAMUBOPT = new double[fileName.length];
		double[] CAMUB_OPTBar = new double[fileName.length];
		double[] AMRLX_Value = new double[fileName.length];
		double[] CAMRLX_Value = new double[fileName.length];

		double[] AM_ValueGAP = new double[fileName.length];
		double[] CAMLBOPTGAP = new double[fileName.length];
		double[] CAMLB_OPTBarGAP = new double[fileName.length];
		double[] CAMUBOPTGAP = new double[fileName.length];
		double[] CAMUB_OPTBarGAP = new double[fileName.length];

		for (int fileNo = 0; fileNo < fileName.length; fileNo++) {
			try {

				InputExcel_ForIISE inputExcel = new InputExcel_ForIISE();
				String suanLiName = suanliPath + fileName[fileNo];
				inputExcel.input(suanLiName);

				try {

					GenerateQ generateQ = new GenerateQ(inputExcel);
					generateQ.generateQ();// 

					IfUnmet ifUnmet = new IfUnmet();

					double[][] unmetQ = new double[generateQ.q.length][generateQ.q[0].length];

					// (AM)
					System.out.println("...AM");
					AM km = new AM(inputExcel, generateQ);
					km.establishVar();
					km.establishConstraints();
					km.establishObjFunction();
					km.setParam(86400, gap);
					km.KantoModel.solve();
					AM_Value[fileNo] = km.KantoModel.getObjValue();
					AM_ValueGAP[fileNo] = km.KantoModel.getMIPRelativeGap();

					// (2) CAMLB
					System.out.println("...CAMLB");
					CAMLB camlb = new CAMLB(inputExcel, unmetQ);// CAMLB
					camlb.establishVar();
					camlb.establishConstraints();
					camlb.establishObjFunction();
					camlb.setParam(maxRunTime, gap);
					//camlb.LBModel.setOut(null);
					camlb.LBModel.solve();
					CAMLBOPT[fileNo] = camlb.LBModel.getObjValue();
					CAMLBOPTGAP[fileNo] = camlb.LBModel.getMIPRelativeGap();

					// (3) CAMLB_Bar
					System.out.println("...CAMLB_N");
					CAMLB_N camlb_n = new CAMLB_N(inputExcel, unmetQ);// CAMLB
					camlb_n.establishVar();
					camlb_n.establishConstraints();
					camlb_n.establishObjFunction();
					camlb_n.setParam(maxRunTime, gap);
					camlb_n.LBModel.setOut(null);
					camlb_n.LBModel.solve();
					CAMLB_OPTBar[fileNo] = camlb_n.LBModel.getObjValue();
					CAMLB_OPTBarGAP[fileNo] = camlb_n.LBModel.getMIPRelativeGap();

					// (4) CAMUB
					System.out.println("...CAMUB");
					CAMUB camub = new CAMUB(inputExcel, unmetQ);// CAMUB
					camub.establishVar();
					camub.establishConstraints();
					camub.establishObjFunction();
					camub.setParam(maxRunTime, gap);
					camub.UBModel.setOut(null);
					if (camub.UBModel.solve()) {
						CAMUBOPT[fileNo] = camub.UBModel.getObjValue();
						CAMUBOPTGAP[fileNo] = camub.UBModel.getMIPRelativeGap();
					}

					// (5) CAMUB_Bar
					System.out.println("...CAMUB_N");
					CAMUB_N camub_n = new CAMUB_N(inputExcel, unmetQ);// CAMUB
					camub_n.establishVar();
					camub_n.establishConstraints();
					camub_n.establishObjFunction();
					camub_n.setParam(maxRunTime, gap);
					camub_n.UBModel.setOut(null);
					if (camub_n.UBModel.solve()) {
						CAMUB_OPTBar[fileNo] = camub_n.UBModel.getObjValue();
						CAMUB_OPTBarGAP[fileNo] = camub_n.UBModel.getMIPRelativeGap();
					}

					// (6) AMRLX
					System.out.println("...AMRLX");
					LAM lkm = new LAM(inputExcel, generateQ);
					lkm.establishVar();
					lkm.establishConstraints();
					lkm.establishObjFunction();
					lkm.KantoModel.solve();
					AMRLX_Value[fileNo] = lkm.KantoModel.getObjValue();

					// (7) CAMRLX
					System.out.println("...CAMRLX");
					CAMLB_RLX camlb_rlx = new CAMLB_RLX(inputExcel, unmetQ);
					camlb_rlx.establishVar();
					camlb_rlx.establishConstraints();
					camlb_rlx.establishObjFunction();
					camlb_rlx.LBModel.setOut(null);
					camlb_rlx.LBModel.solve();
					CAMRLX_Value[fileNo] = camlb_rlx.LBModel.getObjValue();

				} catch (Exception e) {
					e.printStackTrace();
				}

			} catch (Exception in) {
				in.printStackTrace();
			}
		}

		Workbook NewModelResult = new HSSFWorkbook();

		Sheet SheetResult = NewModelResult.createSheet("PropValidation");
		String[] titleResult = { "InstanceNo", "AMOPTLB", "UB", "GAP", "CAMLBLB", "UB", "GAP", "CAMLB_N_LB", "UB",
				"GAP", "CAMUB_LB", "UB", "GAP", "CAMUB_N_LB", "UB", "GAP", "AMRLX", "CAMLBRLX" };

		Row RowR = SheetResult.createRow(0);// 
		Cell CellR = null;
		for (int biaoTou = 0; biaoTou < titleResult.length; biaoTou++) {
			CellR = RowR.createCell(biaoTou);
			CellR.setCellValue(titleResult[biaoTou]);
		}
		for (int r = 0; r < fileName.length; r++) {
			Row Write = SheetResult.createRow(SheetResult.getPhysicalNumberOfRows());
			Write.createCell(0).setCellValue(r + 1);
			Write.createCell(1).setCellValue(AM_Value[r] * (1 - AM_ValueGAP[r]));
			Write.createCell(2).setCellValue(AM_Value[r]);
			Write.createCell(3).setCellValue(AM_ValueGAP[r]);
			Write.createCell(4).setCellValue(CAMLBOPT[r] * (1 - CAMLBOPTGAP[r]));
			Write.createCell(5).setCellValue(CAMLBOPT[r]);
			Write.createCell(6).setCellValue(CAMLBOPTGAP[r]);
			Write.createCell(7).setCellValue(CAMLB_OPTBar[r] * (1 - CAMLB_OPTBarGAP[r]));
			Write.createCell(8).setCellValue(CAMLB_OPTBar[r]);
			Write.createCell(9).setCellValue(CAMLB_OPTBarGAP[r]);
			Write.createCell(10).setCellValue(CAMUBOPT[r] * (1 - CAMUBOPTGAP[r]));
			Write.createCell(11).setCellValue(CAMUBOPT[r]);
			Write.createCell(12).setCellValue(CAMUBOPTGAP[r]);
			Write.createCell(13).setCellValue(CAMUB_OPTBar[r] * (1 - CAMUB_OPTBarGAP[r]));
			Write.createCell(14).setCellValue(CAMUB_OPTBar[r]);
			Write.createCell(15).setCellValue(CAMUB_OPTBarGAP[r]);
			Write.createCell(16).setCellValue(AMRLX_Value[r]);
			Write.createCell(17).setCellValue(CAMRLX_Value[r]);
		}

		try {
			String NMOutPut = path + "\\PropValidation.xls";
			FileOutputStream fos = new FileOutputStream(NMOutPut);
			NewModelResult.write(fos);
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
