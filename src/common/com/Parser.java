package common.com;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.NodeList;

public interface Parser {
	
	public boolean setParser(String type, String name);
	
	public boolean setStreamXml(InputStreamReader xml);
	public boolean setStringXml(String xml);
	public boolean setByteXml(byte[] xml);
	public boolean setXml(String xml);
	
	public String getString(String strSection, String strKey);
	public String getString(String strSection, String strKey, String strDefault);
	public int getInt(String strSection, String strKey);
	public int getInt(String strSection, String strKey, int nDefault);
	
	public boolean getNodeList(String node, ArrayList<Map<String, String>> arrInput);
	public NodeList getNodeList(String exp);
	public List<String> getValListByNode(String exp);
	public List<String> getAttValueList(String exp, String attId);
	public ArrayList<Map<String,String>> getAttList(String exp);
}

class parserPath {
	
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
	
	private String ProjectName() {
		
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
			path = path.substring(path.lastIndexOf(bs)+1,path.length());
		} catch(Exception e) {
			return "";
		}
		return path;
	}
	
	public static String getProjectPath() {
		return new parserPath().ProjectPath();
	}
	
	public static String getProjectName() {
		return new parserPath().ProjectName();
	}
	
	public static String getPath(String type, String name) {
		String filepath = getProjectPath();
		String sep 		= filepath.indexOf("/")==-1?"\\":"/";
		return (filepath+type.toLowerCase()+sep+name+"."+type.toLowerCase());
	}
}
