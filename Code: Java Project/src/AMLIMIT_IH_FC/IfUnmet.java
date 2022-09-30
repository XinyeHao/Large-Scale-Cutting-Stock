package AMLIMIT_IH_FC;

public class IfUnmet {
	public boolean ifUnmet(double[][] unmetQ) throws Exception{
		boolean unmet=false;
		for (int m = 0; m < unmetQ.length; m++) {
			for (int n = 0; n < unmetQ[0].length; n++) {
				if(unmetQ[m][n]>0.00001) {
					unmet=true;
				}
			}
		}
		
		return unmet;
	}
	
	public boolean ifUnmetS(double[][] unmetQ,int s) throws Exception{
		boolean unmet=false;
		for (int n = 0; n < unmetQ[0].length; n++) {
			if(unmetQ[s][n]>0.00001) {
				unmet=true;
			}
		}
		return unmet;
	}
}
