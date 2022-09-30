package AMLIMIT_IH_FC;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HeuSatisfUnmetQ {
	InputExcel_ForIISE inputExcel;

	public HeuSatisfUnmetQ(InputExcel_ForIISE inputExcel) {
		this.inputExcel = inputExcel;
	}

	public void satisf(double[][] unmetQ, boolean[] fixedPlateLKM, boolean[] fixedBoardLKM, int[][] fixedStripLKM,
			Map<Integer, Double> board_value, double[][] stripSolution, double[][][] QnggLKM, double[][] leftLKM)
			throws Exception {

		for (int m = 0; m < unmetQ.length; m++) {
			for (int n = 0; n < unmetQ[0].length; n++) {
				if (unmetQ[m][n] > 0) {
					System.out.println("HeuSatis Remain  unmetQ [" + m + "][" + n + "] = " + unmetQ[m][n]);
				}
			}
		}

		board_value = sortByValueDescending(board_value);

		IfUnmet ifUnmet = new IfUnmet();

		Iterator<Entry<Integer, Double>> entries = board_value.entrySet().iterator();
		while (entries.hasNext()) {

			Entry<Integer, Double> entry = entries.next();
			int key = entry.getKey();
			double value = entry.getValue();

			if (ifUnmet.ifUnmet(unmetQ)) {
				double widthLeft = inputExcel.board[entry.getKey()].getWidth();
				for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
					if (stripSolution[entry.getKey()][n] > 0 && ifUnmet.ifUnmetS(unmetQ, n)) {
						if (widthLeft >= inputExcel.cuttingNorms.getNorm()[n]) {// 
							fixedPlateLKM[inputExcel.board[entry.getKey()].getPlateNo()] = true;
							fixedBoardLKM[entry.getKey()] = true;
							for (int time = 0; time < Math.max(1,
									(int) Math.floor(stripSolution[entry.getKey()][n])); time++) {// 一次一次地满足
								if (ifUnmet.ifUnmetS(unmetQ, n) && widthLeft >= inputExcel.cuttingNorms.getNorm()[n]) {
									widthLeft -= inputExcel.cuttingNorms.getNorm()[n];
									fixedStripLKM[entry.getKey()][n]++;
									for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
										double WL = inputExcel.board[entry.getKey()].getLength() * inputExcel.chuLiao
												.getRate()[inputExcel.board[entry.getKey()].getGrade() - 1][g];
										for (int g1 = g; g1 < inputExcel.generalData.getNumOfProductGrades(); g1++) {
											if (WL > 0 && unmetQ[n][g1] > 0) {
												if (WL > unmetQ[n][g1]) {
													WL -= unmetQ[n][g1];
													QnggLKM[n][n][g1] += unmetQ[n][g1];
													unmetQ[n][g1] = 0;
												} else {// WL<unmetQ
													unmetQ[n][g1] -= WL;
													QnggLKM[n][g][g1] += WL;
													WL = 0;
													break;
												}
											}
										}
										leftLKM[n][g] += WL;
									}
								}
							}
						}
					}
				}

				for (int n = 0; n < inputExcel.generalData.getNumOfCuttingNorms(); n++) {
					while (widthLeft >= inputExcel.cuttingNorms.getNorm()[n] && ifUnmet.ifUnmetS(unmetQ, n)) {

						widthLeft -= inputExcel.cuttingNorms.getNorm()[n]; //
						fixedStripLKM[entry.getKey()][n]++; //
						for (int g = 0; g < inputExcel.generalData.getNumOfProductGrades(); g++) {
							double WL = inputExcel.board[entry.getKey()].getLength()
									* inputExcel.chuLiao.getRate()[inputExcel.board[entry.getKey()].getGrade() - 1][g];
							for (int g1 = g; g1 < inputExcel.generalData.getNumOfProductGrades(); g1++) {
								if (WL > 0 && unmetQ[n][g1] > 0) {
									if (WL > unmetQ[n][g1]) {
										WL -= unmetQ[n][g1];
										QnggLKM[n][n][g1] += unmetQ[n][g1];
										unmetQ[n][g1] = 0;
									} else {// WL<unmetQ
										unmetQ[n][g1] -= WL;
										QnggLKM[n][g][g1] += WL;
										WL = 0;
										break;
									}
								}
							}
							leftLKM[n][g] += WL;
						}
					}
				}

			} else {
				break;// 
			}
		}

	}

	public <K, V extends Comparable<? super V>> Map<K, V> sortByValueDescending(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				int compare = (o1.getValue()).compareTo(o2.getValue());
				return -compare;
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
}
