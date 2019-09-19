package common.com;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.w3c.dom.NodeList;

public class IniParser implements Parser {
	
	private String[] m_arrStrInput;
	
	public IniParser() {}
	
	public IniParser(String type, String name) {
		setParser(type, name);
	}
	
	public boolean setParser(String type, String name) {
    	byte[] lineBuffer;
		Vector<String> vector = new Vector<String>(20, 10);	  
	
		try {
			FileInputStream inputStream = null;
			inputStream = new FileInputStream(new parserPath().getPath(type,name));           
			byte[] lineData;
			boolean bReadLine = true;
			while(bReadLine) {
				lineBuffer = new byte[1024];                 
				lineData = readLine(inputStream, lineBuffer);
				if(lineData != null && lineData.length>0) {
					String strLine = new String(lineData);
					strLine.trim();
					if(strLine.length() == 0) { 
						bReadLine = false;  
						continue; 
					}
					if(!strLine.startsWith("'")) 
						vector.addElement(strLine); 
				} 
				else
					bReadLine = false;
			}
			inputStream.close();
		} catch(Exception e) {
			return false;
		}

		int vSize = vector.size();
		if (vSize > 1) { 		
			m_arrStrInput = new String[vSize];
			for(int i=0 ; i<vSize ; i++) {
				m_arrStrInput[i] = (String)vector.elementAt(i);
			}
			return true;
		}
		return false;
    }
    
	public int getInt(String strSection, String strKey) {
		int nRet = 0;
		try {
			String strRet = getString(strSection, strKey, Integer.toString(0));
			nRet = Integer.parseInt(strRet);
		} catch(Exception e) {
			return 0;
		}
		return nRet;
	}
	
	public int getInt(String strSection, String strKey, int nDefault) {
		int nRet = 0;
		try {
			String strRet = getString(strSection, strKey, Integer.toString(nDefault));
			nRet = Integer.parseInt(strRet);
		} catch(Exception e) {
			return nDefault;
		}
		return nRet;
	}
	
	public String getString(String strSection, String strKey) {
		return getString(strSection, strKey, "");
	}
	
	public String getString(String strSection, String strKey, String strDefault) {
        String 	strLine			="";
        String 	strKeyValue 	= "";
		int 		nSize 		= m_arrStrInput.length;
        for(int nIndex = 0; nIndex < nSize ; nIndex++) 
        {
			strLine = m_arrStrInput[nIndex];
			if (strLine.indexOf("[") == 0)
			{
				int nLoc = strLine.indexOf("]"); 
				if(nLoc >= 0 && strSection.equals(strLine.substring(1, nLoc).trim()))	  
				{
					for( int i=nIndex+1; i< nSize; i++)
					{
						strLine = m_arrStrInput[i];
						nLoc = strLine.indexOf("[");
						if ( nLoc == 0 )		break;
						
						nLoc = strLine.indexOf("=");
						if(nLoc >= 0 && strKey.equals(strLine.substring(0, nLoc).trim()))  
						{
							strKeyValue = strLine.substring(nLoc+1).trim();    
							strKeyValue.trim();	
							return strKeyValue; 
						}
					}
				}
			}
        }
		return (strKeyValue==null||strKeyValue.length()==0)?strDefault:strKeyValue;
    }
	 
    private static byte[] readLine(InputStream is, byte[] t_buf) throws IOException {
    	byte[] ret;
    	
    	int c = is.read();
	    if (c == -1)  return null; //c=10;
	 	t_buf[0] = (byte)c;
    	
    	int i = 1, len=1;
    	try {
    	    for (; i < 1024 ; i++) {
    	        c = is.read();
    	        if (c == -1)  {
    	            c = 10;  
    	        }
    	        switch(c) {
    	            case 0 : c = ' ';
    	                     t_buf[i] = (byte) c;
    	                     len++;
    	                     break;
    	            case 13 : break;
    	            case 10 :
    	                     ret = new byte[len];
    	                     System.arraycopy(t_buf, 0, ret, 0, len);
    	                     return ret;    	                     
    	            default :          
    	                     t_buf[i] = (byte) c;
    	                    len++;
    	                    break;
    	        }    	        
    	    }
    	    ret = new byte[len];
    	    System.arraycopy(t_buf, 0, ret, 0, len);
    	    return ret;  
	    } catch (IOException ee) {
	        return null;
	    }
    }
    
    public boolean setStreamXml(InputStreamReader xml)									{return false;}
	public boolean setStringXml(String xml)											{return false;}
	public boolean setByteXml(byte[] xml)												{return false;}
	public boolean setXml(String xml)													{return false;}
	
	public boolean getNodeList(String node, ArrayList<Map<String, String>> arrInput)	{return false;}
	public NodeList getNodeList(String exp)												{return null;}
	public List<String> getValListByNode(String exp)									{return null;}
	public List<String> getAttValueList(String exp, String attId)						{return null;}
	public ArrayList<Map<String,String>> getAttList(String exp)							{return null;}
}
