package AMLIMIT_IH_FC;

import java.io.File;

public class GetFileName {
	
	 public String[] getFileName(String path)
	    {
	        File file = new File(path);
	        String [] fileName = file.list();
	        return fileName;
	    }
}
