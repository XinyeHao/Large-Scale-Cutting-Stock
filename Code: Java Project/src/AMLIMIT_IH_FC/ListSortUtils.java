package AMLIMIT_IH_FC;

import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
 
public class ListSortUtils {
	
	public static <T> void sort(List<T> list, final boolean isAsc, final String... sortName) {
		Collections.sort(list, new Comparator<T>() { 
			public int compare(T a, T b) {
				int ret = 0;
				try {
					for (int i = 0; i < sortName.length; i++) {
						ret = ListSortUtils.compareObject(sortName[i], isAsc, a, b);
						if (0 != ret) {
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return ret;
			}
		});
	}
	
	public static <T> void sort(List<T> list, final String[] sortNameArray, final boolean[] sortDirectionArray) {
		if (sortNameArray.length != sortDirectionArray.length) {
			throw new RuntimeException("XXXXXX");
		}
		Collections.sort(list, new Comparator<T>() {
			public int compare(T t1, T t2) {
				int ret = 0;
				try {
					for (int i = 0; i < sortNameArray.length; i++) {
						ret = compareObject(sortNameArray[i], sortDirectionArray[i], t1, t2);
						if (0 != ret) {
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return ret;
			}
		});
	}
	
	private static <T> int compareObject(final String sortName, final boolean isAsc, T t1, T t2) throws Exception {
		int ret;
		Object value1 = forceGetFieldValue(t1, sortName);
		Object value2 = forceGetFieldValue(t2, sortName);
		String str1 = value1.toString();
		String str2 = value2.toString();
		if (value1 instanceof Number && value2 instanceof Number) {
			int maxlen = Math.max(str1.length(), str2.length());
			str1 = addZero2Str((Number) value1, maxlen);
			str2 = addZero2Str((Number) value2, maxlen);
		} else if (value1 instanceof Date && value2 instanceof Date) {
			long time1 = ((Date) value1).getTime();
			long time2 = ((Date) value2).getTime();
			int maxlen = Long.toString(Math.max(time1, time2)).length();
			str1 = addZero2Str(time1, maxlen);
			str2 = addZero2Str(time2, maxlen);
		}
		if (isAsc) {
			ret = str1.compareTo(str2);
		} else {
			ret = str2.compareTo(str1);
		}
		return ret;
	}
 
	public static String addZero2Str(Number numObj, int length) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(false);
		nf.setMaximumIntegerDigits(length);
		nf.setMinimumIntegerDigits(length);
		return nf.format(numObj);
	}
 
	public static Object forceGetFieldValue(Object obj, String fieldName) throws Exception {
		Field field = obj.getClass().getDeclaredField(fieldName);
		Object object = null;
		boolean accessible = field.isAccessible();
		if (!accessible) {
			field.setAccessible(true);
			object = field.get(obj);
			field.setAccessible(accessible);
			return object;
		}
		object = field.get(obj);
		return object;
	}
}

