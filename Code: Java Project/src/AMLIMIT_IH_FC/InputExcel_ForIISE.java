package AMLIMIT_IH_FC;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class InputExcel_ForIISE {

	public GeneralData generalData;
	public Board[] board;
	public Plate[] plate;
	public Product[] product;
	public CuttingNorms cuttingNorms;
	public ChuLiao chuLiao;

	public ArrayList<BoardSet> boardSet;
	public int[] boardSetNo;
	// public double mostExpens;

	public double[] plateCost;

	public void input(String filePath) throws Exception {

		generalData = new GeneralData();
		Workbook wb = null;
		Sheet[] sheet = new Sheet[5];//
		Row row = null;
		Set plateSet = new HashSet();//

		List<Map<String, String>>[] list = new List[5];
		for (int st = 0; st < 5; st++) {
			list[st] = new ArrayList<Map<String, String>>();//
		}

		String[] cellData = new String[5];
		for (int st = 0; st < 5; st++) {
			sheet[st] = null;
			cellData[st] = null;
		}

		
		wb = readExcel(filePath);

		generalData.setNumOfMaterlGrades(4);// 
		generalData.setNumOfProductGrades(5);// 

		double[][] rate = new double[generalData.getNumOfMaterlGrades()][generalData.getNumOfProductGrades()];
		double[] boardPrice = new double[generalData.getNumOfMaterlGrades()];
		double[] weight = new double[3];// 
		double maxArea = 0;
		double aveBoardCost = 0;

		if (wb != null) {

			cuttingNorms = new CuttingNorms();
			chuLiao = new ChuLiao();

			boardSet = new ArrayList<BoardSet>();

			generalData.setNumOfProducts(wb.getSheetAt(0).getPhysicalNumberOfRows() - 1);
			generalData.setNumOfBoards(wb.getSheetAt(1).getPhysicalNumberOfRows() - 1);

			board = new Board[generalData.getNumOfBoards()];
			for (int i = 0; i < generalData.getNumOfBoards(); i++) {
				board[i] = new Board();
			}
			product = new Product[generalData.getNumOfProducts()];
			for (int k = 0; k < generalData.getNumOfProducts(); k++) {
				product[k] = new Product();
			}

			for (int st = 0; st < 4; st++) {

				sheet[st] = wb.getSheetAt(st);// 

				row = sheet[st].getRow(0);
				int rowNum = sheet[st].getPhysicalNumberOfRows();// 
				int columnNum = row.getPhysicalNumberOfCells();// 

				for (int i = 1; i < rowNum || (i == 1 && st == 2); i++) {
					row = sheet[st].getRow(i);
					if (row != null || (i == 1 && st == 2)) {

						switch (st) {

						case 0: 
							product[i - 1].setPsn((String) getCellFormatValue(row.getCell(0)));
							product[i - 1].setMT((String) getCellFormatValue(row.getCell(1)));
							product[i - 1].setLevel((int) Double.parseDouble(getCellFormatValue(row.getCell(2))));
							product[i - 1].setLength(Double.valueOf(getCellFormatValue(row.getCell(3))) / 1);// cm
							product[i - 1].setWidth(Double.valueOf(getCellFormatValue(row.getCell(4))) / 1);// cm
							product[i - 1].setHeight(Double.valueOf(getCellFormatValue(row.getCell(5))) / 1);// cm
							product[i - 1].setDemand(Double.valueOf(getCellFormatValue(row.getCell(6))));
							product[i - 1].setProductType((String) getCellFormatValue(row.getCell(7)));
							break;
						case 1:
							board[i - 1].setMsn((String) getCellFormatValue(row.getCell(0)));
							board[i - 1].setPlateSn((String) getCellFormatValue(row.getCell(1)));
							board[i - 1].setMT((String) getCellFormatValue(row.getCell(2)));
							board[i - 1].setGrade((int) Double.parseDouble(getCellFormatValue(row.getCell(3))));
							board[i - 1].setLength(Double.valueOf(getCellFormatValue(row.getCell(4))) / 1);// cm
							board[i - 1].setWidth(Double.valueOf(getCellFormatValue(row.getCell(5))) / 1);
							board[i - 1].setHeight(Double.valueOf(getCellFormatValue(row.getCell(6))) / 1);
							board[i - 1].setMianJi(board[i - 1].getLength() * board[i - 1].getWidth());
							maxArea = Math.max(maxArea, board[i - 1].getMianJi());
							plateSet.add((String) getCellFormatValue(row.getCell(1)));
							break;
						case 2:
							row = sheet[st].getRow(0);
							generalData.setNumOfCuttingNorms(row.getPhysicalNumberOfCells() - 1);
							double[] norm = new double[generalData.getNumOfCuttingNorms()];

							for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
								norm[n] = Double.valueOf(getCellFormatValue(row.getCell(n + 1))) / 1;// cm
							}
							cuttingNorms.setNorm(norm);
							break;
						case 3:
							for (int l = 0; l < generalData.getNumOfProductGrades(); l++) {
								rate[i - 1][l] = Double.valueOf(getCellFormatValue(row.getCell(l + 1))) / 100;// %
							}
							break;
						}
					} else {
						break;
					}
				}


				if (st == 1) {
					boardSetNo = new int[board.length];
					for (int i = 0; i < board.length; i++) {//
						board[i].setPrice(1 * (board[i].getMianJi()/* /board[i].getGrade() */) / maxArea * 100);
						aveBoardCost += board[i].getPrice();
						for (int set = 0; set < boardSet.size() || boardSet.size() == 0; set++) {
							if (boardSet.size() == 0) {// 
								boardSetNo[i] = 0;
								boardSet.add(new BoardSet());
								boardSet.get(boardSet.size() - 1).setMT(board[i].getMT());
								boardSet.get(boardSet.size() - 1).setGrade(board[i].getGrade());
								boardSet.get(boardSet.size() - 1).setLength(board[i].getLength());
								boardSet.get(boardSet.size() - 1).setWidth(board[i].getWidth());
								boardSet.get(boardSet.size() - 1).setHeight(board[i].getHeight());
								boardSet.get(boardSet.size() - 1).setMianJi(board[i].getMianJi());
								boardSet.get(boardSet.size() - 1).setPrice(board[i].getPrice());
								boardSet.get(boardSet.size() - 1).boardsInSet = 1;
								boardSet.get(boardSet.size() - 1).msn = new ArrayList<String>();
								boardSet.get(boardSet.size() - 1).msn.add(board[i].getMsn());
								boardSet.get(boardSet.size() - 1).psn = new ArrayList<String>();
								boardSet.get(boardSet.size() - 1).psn.add(board[i].getPlateSn());
								break;
							} else if ((boardSet.get(set).getGrade() == board[i].getGrade()
									&& boardSet.get(set).getLength() == board[i].getLength()
									&& boardSet.get(set).getWidth() == board[i].getWidth())) {// 
								boardSetNo[i] = set;
								boardSet.get(set).msn.add(board[i].getMsn());
								boardSet.get(set).psn.add(board[i].getPlateSn());
								boardSet.get(set).boardsInSet++;

								break;
							} else if (set == boardSet.size() - 1) {
								boardSet.add(new BoardSet());
								boardSetNo[i] = boardSet.size() - 1;
								boardSet.get(boardSet.size() - 1).setMT(board[i].getMT());
								boardSet.get(boardSet.size() - 1).setGrade(board[i].getGrade());
								boardSet.get(boardSet.size() - 1).setLength(board[i].getLength());
								boardSet.get(boardSet.size() - 1).setWidth(board[i].getWidth());
								boardSet.get(boardSet.size() - 1).setHeight(board[i].getHeight());
								boardSet.get(boardSet.size() - 1).setMianJi(board[i].getMianJi());
								boardSet.get(boardSet.size() - 1).setPrice(board[i].getPrice());
								boardSet.get(boardSet.size() - 1).boardsInSet = 1;
								boardSet.get(boardSet.size() - 1).msn = new ArrayList<String>();
								boardSet.get(boardSet.size() - 1).msn.add(board[i].getMsn());
								boardSet.get(boardSet.size() - 1).psn = new ArrayList<String>();
								boardSet.get(boardSet.size() - 1).psn.add(board[i].getPlateSn());
								break;
							}

						}
					}

					int totalBoard = 0;
					for (int set = 0; set < boardSet.size(); set++) {
						totalBoard += boardSet.get(set).boardsInSet;
					}
				}

				switch (st) {
				case 1:
					generalData.setNumOfPlates(plateSet.size());
					break;
				case 3:
					chuLiao.setRate(rate);
					break;
				}

			}
			System.out.println("maxArea ==== " + maxArea);
			plate = new Plate[generalData.getNumOfPlates()];
			for (int pt = 0; pt < generalData.getNumOfPlates(); pt++) {
				plate[pt] = new Plate();
			}
			Iterator iterator = plateSet.iterator();
			int pt = 0;
			while (iterator.hasNext()) {
				plate[pt].setPlateSn((String) iterator.next());
				pt++;
			}

			int[][] plateToProductLevel = new int[generalData.getNumOfPlates()][generalData.getNumOfProductGrades()];
			for (int j = 0; j < generalData.getNumOfPlates(); j++) {
				for (int i = 0; i < generalData.getNumOfBoards(); i++) {
					if (plate[j].getPlateSn().equals(board[i].getPlateSn())) {
						board[i].setPlateNo(j);
						plate[j].board.add(board[i]);
						plate[j].boardTypeNo.add(boardSetNo[i]);
						for (int l = 0; l < generalData.getNumOfProductGrades(); l++) {
							if (chuLiao.getRate()[board[i].getGrade() - 1][l] > 0) {

								plateToProductLevel[j][l] = 1;
							}
						}
					}
				}
				plate[j].setPlateToProductLevel(plateToProductLevel[j]);

				List<Board> listforSort = new ArrayList<Board>();
				for (int i = 0; i < plate[j].board.size(); i++) {
					listforSort.add(plate[j].board.get(i));
				}

				String[] sortNameArr = { "grade", "length", "width" };
				boolean[] isAscArr = { true, true, true };


				ListSortUtils.sort(listforSort, sortNameArr, isAscArr);

				for (int i = 0; i < plate[j].board.size(); i++) {
					plate[j].board.set(i, listforSort.get(i));
				}

			}

			aveBoardCost /= board.length;
			System.out.println("aveCost = " + aveBoardCost);

			double[] priceJ = new double[generalData.getNumOfPlates()];
			for (int j = 0; j < generalData.getNumOfPlates(); j++) {
				priceJ[j] = aveBoardCost * 10;

				plate[j].setPrice(priceJ[j]);
			}

			for (int j = 0; j < generalData.getNumOfPlates(); j++) {
				int[] setIInPlate = new int[boardSet.size()];
				for (int set = 0; set < boardSet.size(); set++) {
					for (int i = 0; i < boardSet.get(set).boardsInSet; i++) {
						if (boardSet.get(set).psn.get(i).equals(plate[j].getPlateSn())) {
							setIInPlate[set]++;
						}
					}
				}
				plate[j].setIInPlate = setIInPlate;
			}

			double numOfBoards = 0;
			for (int i = 0; i < boardSet.size(); i++) {
				numOfBoards += boardSet.get(i).boardsInSet;
			}

			double netSquareNeeded = 0;
			for (int k = 0; k < generalData.getNumOfProducts(); k++) {
				netSquareNeeded += product[k].getLength() * product[k].getWidth() * product[k].getDemand();
			}
			double totalSquarePro = 0;
			for (int b = 0; b < board.length; b++) {
				totalSquarePro += board[b].getMianJi();
			}
		}
	}

	public int numOfNewInstances = 100;

	public void generateNewInstances(String filePath) throws Exception {
		for (int time = 0; time < numOfNewInstances; time++) {
			Random rand = new Random();
			for (int set = 0; set < boardSet.size(); set++) {
				if (rand.nextDouble() > 0.50) {

					double newLength;
					double newWidth;
					int newQuality = 1 + rand.nextInt(4);

					if (rand.nextDouble() < 0.80) {
						newLength = 200 + rand.nextInt(7) * 50;

					} else {
						newLength = 500 + rand.nextInt(5) * 50;
					}

					if (rand.nextDouble() < 0.90) {
						newWidth = Double.valueOf(110 + 10 * rand.nextInt(10));
					} else {
						newWidth = Double.valueOf(200 + 50 * rand.nextInt(5));
					}

					for (int b = 0; b < generalData.getNumOfBoards(); b++) {
						if (boardSetNo[b] == set) {
							if (rand.nextDouble() < 0.90) {
								board[b].setGrade(newQuality);
								board[b].setLength(newLength);// cm
								board[b].setWidth(newWidth);
								if (rand.nextDouble() > 0.80) {
									board[b].setPlateSn(plate[rand.nextInt(plate.length)].getPlateSn());
								}
							}
						}
					}
				}
			}

			double netSquareNeeded = 0;
			for (int k = 0; k < generalData.getNumOfProducts(); k++) {
				netSquareNeeded += product[k].getLength() * product[k].getWidth() * product[k].getDemand();
			}
			double totalSquarePro = 0;
			for (int b = 0; b < board.length; b++) {
				totalSquarePro += board[b].getLength() * board[b].getWidth();
			}
			Workbook NewModelResult = new HSSFWorkbook();
			Sheet Input_Product = NewModelResult.createSheet("Input_Product");
			String[] Title_Input_Product = { "ProductSN", "MT", "PLevel", "pl", "pw", "ph", "demand", "PT" };
			Row Row_Input_Product = Input_Product.createRow(0);
			Cell Cell_Input_Product = null;
			for (int biaoTou = 0; biaoTou < Title_Input_Product.length; biaoTou++) {
				Cell_Input_Product = Row_Input_Product.createCell(biaoTou);
				Cell_Input_Product.setCellValue(Title_Input_Product[biaoTou]);
			}
			for (int p = 0; p < generalData.getNumOfProducts(); p++) {
				Row Write = Input_Product.createRow(Input_Product.getPhysicalNumberOfRows());
				Write.createCell(0).setCellValue(product[p].getPsn());
				Write.createCell(1).setCellValue(product[p].getMT());
				Write.createCell(2).setCellValue(product[p].getLevel());
				Write.createCell(3).setCellValue(product[p].getLength());
				Write.createCell(4).setCellValue(product[p].getWidth());
				Write.createCell(5).setCellValue(product[p].getHeight());
				Write.createCell(6).setCellValue(product[p].getDemand());
				Write.createCell(7).setCellValue(product[p].getMT());
			}

			// SHEET2: Input_Board
			Sheet Input_Board = NewModelResult.createSheet("Input_Board");
			String[] Title_Input_Board = { "BoardSN", "PSN", "MT", "MLevel", "ml", "md", "mh" };
			Row Row_Input_Board = Input_Board.createRow(0);
			Cell Cell_Input_Board = null;
			for (int biaoTou = 0; biaoTou < Title_Input_Board.length; biaoTou++) {
				Cell_Input_Board = Row_Input_Board.createCell(biaoTou);
				Cell_Input_Board.setCellValue(Title_Input_Board[biaoTou]);
			}
			for (int b = 0; b < generalData.getNumOfBoards(); b++) {
				Row Write = Input_Board.createRow(Input_Board.getPhysicalNumberOfRows());
				Write.createCell(0).setCellValue(board[b].getMsn());
				Write.createCell(1).setCellValue(board[b].getPlateSn());
				Write.createCell(2).setCellValue(board[b].getMT());
				Write.createCell(3).setCellValue(board[b].getGrade());
				Write.createCell(4).setCellValue(board[b].getLength());
				Write.createCell(5).setCellValue(board[b].getWidth());
				Write.createCell(6).setCellValue(board[b].getHeight());
			}

			// SHEET3: CuttingNorm
			Sheet CuttingNorm = NewModelResult.createSheet("CuttingNorm");

			for (int r = 0; r < 1; r++) {
				Row Write = CuttingNorm.createRow(0);
				Write.createCell(0).setCellValue("cutting norm£¨mm)");
				Write.createCell(1).setCellValue(52);
				Write.createCell(2).setCellValue(62);
				Write.createCell(3).setCellValue(81);
				Write.createCell(4).setCellValue(89);
			}

			// SHEET4: OutputRate
			Sheet Input_OutputRate = NewModelResult.createSheet("OutputRate");
			String[] Title_Input_OutputRate = { "OutputRate", "1", "2", "3", "4", "5", "comments" };
			Row Row_OutputRate = Input_OutputRate.createRow(0);
			Cell Cell_OutputRate = null;
			for (int biaoTou = 0; biaoTou < Title_Input_OutputRate.length; biaoTou++) {
				Cell_OutputRate = Row_OutputRate.createCell(biaoTou);
				Cell_OutputRate.setCellValue(Title_Input_OutputRate[biaoTou]);
			}
			for (int n = 0; n < generalData.getNumOfCuttingNorms(); n++) {
				Row Write = Input_OutputRate.createRow(Input_OutputRate.getPhysicalNumberOfRows());
				String qualityType = String.valueOf((char) (65 + n));
				Write.createCell(0).setCellValue(qualityType);
				for (int g = 0; g < generalData.getNumOfProductGrades(); g++) {
					Write.createCell(g + 1).setCellValue(100 * chuLiao.getRate()[n][g]);
				}
				Write.createCell(6).setCellValue(1);
			}

			try {
				String NMOutPut = filePath + "\\NewInstances\\Case WB-NewInstance" + String.valueOf(1 + time) + ".xls";
				System.out.println("new instances [" + (1 + time) + "] are generated");
				FileOutputStream fos = new FileOutputStream(NMOutPut);
				NewModelResult.write(fos);
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	public void sortMaterial() throws Exception {
		List<Board> listforSort = new ArrayList<Board>();
		for (int i = 0; i < generalData.getNumOfBoards(); i++) {
			listforSort.add(board[i]);
		}

		String[] sortNameArr = { "grade", "length", "width" };
		boolean[] isAscArr = { true, true, true };

		ListSortUtils.sort(listforSort, sortNameArr, isAscArr);

		for (int i = 0; i < generalData.getNumOfBoards(); i++) {
			board[i] = listforSort.get(i);
		}
	}

	public static Workbook readExcel(String filePath) {
		Workbook wb = null;
		if (filePath == null) {
			return null;
		}
		String extString = filePath.substring(filePath.lastIndexOf("."));
		InputStream is = null;
		try {
			is = new FileInputStream(filePath);
			if (".xls".equals(extString)) {
				return wb = new HSSFWorkbook(is);
			} else if (".xlsx".equals(extString)) {
				return wb = new XSSFWorkbook(is);
			} else {
				return wb = null;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return wb;
	}

	public static String getCellFormatValue(Cell cell) {
		String cellValue = null;
		if (cell != null) {
			switch (cell.getCellType()) {
			case Cell.CELL_TYPE_NUMERIC: {
				cellValue = String.valueOf(cell.getNumericCellValue());
				break;
			}
			case Cell.CELL_TYPE_STRING: {
				cellValue = cell.getStringCellValue();
				break;
			}
			default:
				cellValue = "";
			}
		} else {
			cellValue = "";
		}
		return cellValue;
	}

}