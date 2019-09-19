package common.com;

public class Path {
	
	private String ProjectPath() {
		
		String path = "";
		char bs		= 0;
		int idx		= 0;
		try {
			
			path 	= this.getClass().getResource("/").getPath();
			path 	= path==null?System.getProperty("user.dir"):path;
			bs 		= path.indexOf("/")==-1?'\\':'/';	
			
			if(path==null||path.equals(""))
				return "";
			
			if(path.charAt(0)==bs)	
				path = path.substring(1,path.length());
			if(path.substring(0,4).equalsIgnoreCase("file"))	
				path = path.substring(6,path.length());
			
			idx = path.indexOf("WEB-INF");
			if(idx<0) {
				if(path.indexOf(bs+"bin"+bs)>-1)
					idx = path.indexOf("bin");
				else if(path.indexOf(bs+"target"+bs)>-1)
					idx = path.indexOf("target");
				else
					idx = path.length();
			} else {
				path = path.replaceAll("%20", " ");
				idx = path.indexOf("WEB-INF");
			}
			path = path.substring(0,idx);
		} catch(Exception e) {
			return "";
		}
		return path;
	}
	
	public String ProjectName() {
		
		String path = "";
		char bs		= 0;
		int idx		= 0;
		try {
			
			path 	= this.getClass().getResource("/").getPath();
			path 	= path==null?System.getProperty("user.dir"):path;
			bs 		= path.indexOf("/")==-1?'\\':'/';	
			
			if(path==null||path.equals(""))
				return "";
			
			if(path.charAt(0)==bs)	
				path = path.substring(1,path.length());
			if(path.substring(0,4).equalsIgnoreCase("file"))	
				path = path.substring(6,path.length());
			
			idx = path.indexOf("WEB-INF");
			if(idx<0) {
				if(path.indexOf(bs+"bin"+bs)>-1)
					idx = path.indexOf("bin")-1;
				else if(path.indexOf(bs+"target"+bs)>-1)
					idx = path.indexOf("target")-1;
				else
					idx = path.length();
			} else {
				path = path.replaceAll("%20", " ");
				idx = path.indexOf("WEB-INF");
				idx--;
			}
			path = path.substring(0,idx);
			if(path.lastIndexOf(bs)+1 == path.length()) {
				path = path.substring(0, path.length()-1);	
			}
			path = path.substring(path.lastIndexOf(bs)+1,path.length());
		} catch(Exception e) {
			return "";
		}
		return path;
	}
	
	public static String getProjectPath() {
		return new Path().ProjectPath();
	}
	
	public static String getProjectName() {
		return new Path().ProjectName();
	}
	
	public static String getPath(String type, String name) {
		String filepath = getProjectPath();
		String sep 		= filepath.indexOf("/")==-1?"\\":"/";
		return (filepath+type.toLowerCase()+sep+name+"."+type.toLowerCase());
	}
	
	public static void main(String[] args) {
		System.out.println(getProjectPath());
		System.out.println(getProjectName());
	}
}
