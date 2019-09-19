package common.wdclient;

import java.util.ArrayList;
import java.util.Map;

import org.json.simple.JSONArray;

import common.com.Parser;
import common.com.XmlParser;
import common.util.JsonUtil;

public class wdmData {
	
	private String 	IP;
	private String 	KEY;
	private int 	PORT;
	
	private int 	seq = -1;
	private int	cnt = 0;
	
	private Parser PARSER = new XmlParser();
	
	public ArrayList<Map<String, String>> arrRs 	= null;
	public String ERR_MSG 							= null;
	
	public wdmData(String ip, int port, String key) {
		IP		= ip;
		PORT	= port;
		KEY		= key;
	}

	private String getResult(String query, String op) {
		try {
			wdmStream wdmstream = new wdmStream();
			
			if(!wdmstream.setKey(KEY)) {
				ERR_MSG = "invalid key";
				return null;
			}
			if(!wdmstream.setOp(op)) {
				ERR_MSG = "invalid op";
				return null;
			}
			if(!wdmstream.query(query)) {
				ERR_MSG = "invalid query";
				return null;
			}
				
			wdmSocket wdmsocket = new wdmSocket(IP, PORT);
			
			String strRet = wdmsocket.requestData(wdmstream);
			
			if(strRet==null) {
				ERR_MSG = wdmsocket.ERR_MSG;
				return null;
			} else {;
				return strRet;
			}	
		} catch(Exception e) {
			ERR_MSG = e.toString();
			return null;
		}
	}
	
	public boolean ExecutQuery(String query) {
    	try {
    		seq		= -1;
    		cnt 	= 0;
    		arrRs 	= new ArrayList<Map<String, String>>();
    		String strRet = getResult(query, wdmStream.SELECT_QUERY);
    		
    		if(strRet==null)
    			return false;
    		
    		strRet = strRet.replace("\"", "&quot;");
    		strRet = strRet.replace("&", "&amp;");
    		strRet = strRet.replace("\'", "&apos;");
    		strRet = strRet.replaceAll("(\r\n|\r|\n|\n\r)", " ");
    		strRet = strRet.trim();
    			
    		PARSER.setStringXml(strRet);
    		PARSER.getNodeList("//ListData/Row", arrRs);
    		cnt = arrRs.size();
       } catch(Exception e) {
    		return false;
    	}
    	return true;
    }
	
	public int ExecutUpdate(String query) {
    	try {
   
    		PARSER.setStringXml(getResult(query, wdmStream.UPDATE_QUERY));
    		String ret = PARSER.getValListByNode("//Return/Code").get(0);
    		if(ret.equalsIgnoreCase("OK")) {
    			return Integer.parseInt(PARSER.getValListByNode("//Return/Query/Cnt").get(0));
    		} else {
    			ERR_MSG = PARSER.getValListByNode("//Return/Query/SQLException").get(0);
    			return -1;
    		}
    	} catch(Exception e) {
    		return -1;
    	}
    }
	
	public String getString(String strKey) {
		if(arrRs.get(seq).containsKey(strKey.toUpperCase()))
			return arrRs.get(seq).get(strKey.toUpperCase());
		else
			return null;
	}
	
	public String getString(String strKey, String strDefault) {
		if(arrRs.get(seq).containsKey(strKey.toUpperCase()))
			return arrRs.get(seq).get(strKey.toUpperCase());
		else
			return strDefault;
	}
	
	public int getInt(String strKey) {
		if(arrRs.get(seq).containsKey(strKey.toUpperCase())) {
			int nRet = 0;
			try { nRet = Integer.parseInt(arrRs.get(seq).get(strKey.toUpperCase())); } catch(Exception e) { nRet = 0; }
			return nRet;	
		} else {
			return 0;
		}
	}
	
	public long getLong(String strKey) {
		if(arrRs.get(seq).containsKey(strKey.toUpperCase())) {
			int nRet = 0;
			try { nRet = Integer.parseInt(arrRs.get(seq).get(strKey.toUpperCase())); } catch(Exception e) { nRet = 0; }
			return nRet;	
		} else {
			return 0;
		}
	}
	
	public boolean next() {
		seq++;
		if(seq<cnt)	return true;
		else		return false;
	}
	
	public void setDefalutIdx() {
		seq = -1;
	}
	
	public int getCount() {
		return cnt;
	}
	
	public String getServerStatus() {
    	String strRet = "";
		try {
    		wdmStream wdmstream = new wdmStream();
			
			if(!wdmstream.setKey(KEY)) {
				ERR_MSG = "invalid key";
				return null;
			}
			if(!wdmstream.setOp("T")) {
				ERR_MSG = "invalid op";
				return null;
			}
			if(!wdmstream.status()) {
				ERR_MSG = "invalid server status";
				return null;
			}	
			wdmSocket wdmsocket = new wdmSocket(IP, PORT);
			strRet = wdmsocket.requestData(wdmstream);
			
       } catch(Exception e) {
    	   strRet = "";
    	}
    	return strRet;
    }
	
	public JSONArray getServerStatusJson() {
		JSONArray jsonRet = null;
		try {
			
			arrRs 	= new ArrayList<Map<String, String>>();
			
    		wdmStream wdmstream = new wdmStream();
			
			if(!wdmstream.setKey(KEY)) {
				ERR_MSG = "invalid key";
				return null;
			}
			if(!wdmstream.setOp("T")) {
				ERR_MSG = "invalid op";
				return null;
			}
			if(!wdmstream.status()) {
				ERR_MSG = "invalid server status";
				return null;
			}	
			wdmSocket wdmsocket = new wdmSocket(IP, PORT);
			String strRet = wdmsocket.requestData(wdmstream);
			PARSER.setStringXml(strRet);
    		PARSER.getNodeList("//StatusData/Row", arrRs);
    		jsonRet = JsonUtil.getJsonArrayFromList(arrRs);
			
       } catch(Exception e) {
    	   jsonRet = null;
    	}
    	return jsonRet;
    }
	
	public String getServerInfo(String op, String job) {
    	String strRet = "";
		try {
    		wdmStream wdmstream = new wdmStream();
			
			if(!wdmstream.setKey(KEY)) {
				ERR_MSG = "invalid key";
				return null;
			}
			if(!wdmstream.setOp("Y")) {
				ERR_MSG = "invalid op";
				return null;
			}
			if(!wdmstream.info(op, job)) {
				ERR_MSG = "invalid server status";
				return null;
			}	
			wdmSocket wdmsocket = new wdmSocket(IP, PORT);
			strRet = wdmsocket.requestData(wdmstream);
			
       } catch(Exception e) {
    	   strRet = "";
    	}
    	return strRet;
    }
}
