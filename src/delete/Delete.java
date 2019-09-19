package delete;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.com.IniParser;
import common.com.Parser;
import common.com.Trace;
import common.com.XmlParser;
import common.util.StringUtil;
import common.wdclient.wdmData;
import index.Index;

public class Delete {
	private Parser			CONF				= null;
	private Parser			INDEX				= null;
	private Parser			DELETE				= null;
	
	private Trace			TRACE				= null;
	
	private String 			ROOT_PATH			= null;
	private String			INDEX_PATH			= null;
	private String			DATA_PATH			= null;
	
	private String 			AGENT_IP			= null;
	private int 			AGENT_PORT			= 0;
	private String 			AGENT_KEY			= null;
	private String 			AGENT_CIPHER_KEY	= null;
	
	public String 			DELETE_UPDATE_QUERY	= null;
	
	public String 			ERR_MSG				= null;
	
	/*
	 * public Delete(Trace trace) { TRACE = trace; }
	 */
	
	public boolean Init_Configure() {
		try {
			CONF		= new IniParser();
			INDEX		= new XmlParser();
			DELETE		= new XmlParser();
			
			if(!CONF.setParser("ini", "OfficeWSE")) {
				//if(TRACE != null)
				if(TRACE == null) {
            		String LOG_PATH = CONF.getString("COMMON","LOG_PATH","log");
                	int LOG_LV 		= CONF.getInt("COMMON", "LOG_LV", 3);
                	TRACE = new Trace(common.com.Path.getProjectPath() + LOG_PATH, LOG_LV);
            	}
				TRACE.TraceLog("Delete.Init_Configure() : Wrong OfficeWSE.ini file.", Trace.LOG_DEBUG);
				return false;
			}
			
			if(!INDEX.setParser("xml", "index")) {
        		if(TRACE != null)
        			TRACE.TraceLog("Wrong index.xml file", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	if(!DELETE.setParser("xml", "delete")) {
        		if(TRACE != null)
        			TRACE.TraceLog("Wrong delete.xml file", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	if(TRACE == null) {
        		String LOG_PATH = CONF.getString("COMMON","LOG_PATH","log");
            	int LOG_LV 		= CONF.getInt("COMMON", "LOG_LV", 1);
        		TRACE = new Trace(common.com.Path.getProjectPath() + LOG_PATH, LOG_LV);
        	}
			
        	ROOT_PATH 	= CONF.getString("COMMON","ROOT_PATH","");
        	DATA_PATH 	= CONF.getString("COMMON","DATA_PATH","");
        	INDEX_PATH 	= CONF.getString("COMMON","INDEX_PATH","");
        	
        	AGENT_IP			= CONF.getString("AGENT","IP","");
        	AGENT_PORT			= Integer.parseInt(CONF.getString("AGENT","PORT","0"));
        	AGENT_KEY			= CONF.getString("AGENT","KEY","");
        	AGENT_CIPHER_KEY	= CONF.getString("AGENT","CIPHER_KEY","");
        	
        	DELETE_UPDATE_QUERY	= DELETE.getValListByNode("//DELETE/DELETE_UPDATE_QUERY").get(0).trim();
        	
        	if(DATA_PATH.equals("") || INDEX_PATH.equals("")) {
        		TRACE.TraceLog("Delete.Init_Configure() : Invalid configure data(COMMON)", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	if(AGENT_IP.equals("") || AGENT_PORT==0 || AGENT_KEY.equals("") || AGENT_CIPHER_KEY.equals("")) {
        		TRACE.TraceLog("Delete.Init_Configure() : Invalid OfficeWSE.ini data(AGENT).", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	//TRACE.TraceBox("Delete.Init_Configure() OK");
        	
			return true;
			
		} catch (Exception e) {
			TRACE.TraceLog("Delete.Init_Configure() Fail : " + e.toString(), Trace.LOG_DEBUG);
			return false;
		}
	}
	
	public HashMap<String, String> Run_Delete(Map<String, String> mParam) {
		
		HashMap<String, String> mapRes = new HashMap<String, String>();
		mapRes.put("RESULT", "F");
		mapRes.put("MSG", "");
		mapRes.put("SE_FLAG", "99");
		mapRes.put("JOB", "DELETE");
		
		try {
			String strQuery		= "";
			
			if(!Init_Configure()) {
				return StringUtil.failWithMsg(TRACE, mapRes, "Delete.Run_Delete() : Failed init configure.");
			}
		
			String strKey		= mParam.get("SDOC_NO");
			
			if(StringUtil.isBlank(strKey)) {
				return StringUtil.failWithMsg(TRACE, mapRes, "Delete.Run_Delete() : Failed to receive SDOC_NO.");
			} else {
				mapRes.put("SDOC_NO", strKey);
				mapRes.put("SE_FLAG", "90");
				
				strQuery = DELETE_UPDATE_QUERY;
				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
				strQuery = strQuery.replaceFirst("\\?", "getDate()");
				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
				strQuery = strQuery.replaceFirst("\\?", "'12'");
				
				if(!Set_Result(strQuery)) {
					return StringUtil.failWithMsg(TRACE, mapRes, "Delete.Run_Delete() Update query failed : " + strQuery);
				} else {
					TRACE.TraceBox("Delete.Run_Delete() Update query : " + strQuery);
				}
			}
			

			mapRes.put("SE_FLAG", "91");
			
			strQuery = DELETE_UPDATE_QUERY;
			strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
			strQuery = strQuery.replaceFirst("\\?", "getDate()");
			strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
			strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
			strQuery = strQuery.replaceFirst("\\?", "'90'");
			
			if(!Set_Result(strQuery)) {
				return StringUtil.failWithMsg(TRACE, mapRes, "Delete.Run_Delete() Update query failed : " + strQuery);
			} else {
				TRACE.TraceBox("Delete.Run_Delete() Update query : " + strQuery);
			}
			
			Index index = new Index();
			if(!index.deleteRealFile(strKey)) {
				return StringUtil.failWithMsg(TRACE, mapRes, "Delete.Run_Delete() : Failed deleting.");
			} else {
		    	TRACE.TraceBox("Delete document with SDOC_NO='" + strKey + "' ok");
				//TRACE.TraceBox("Delete.Run_Delete() Update query : " + strQuery);
				
				mapRes.put("SE_FLAG", "92");
				mapRes.put("RESULT", "T");
				mapRes.put("MSG", "");
				
				strQuery = DELETE_UPDATE_QUERY;
				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
				strQuery = strQuery.replaceFirst("\\?", "getDate()");
				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
				strQuery = strQuery.replaceFirst("\\?", "'91'");
				
				if(!Set_Result(strQuery)) {
					return StringUtil.failWithMsg(TRACE, mapRes, "Delete.Run_Delete() Update query failed : " + strQuery);
				} else {
					TRACE.TraceBox("Delete.Run_Delete() Update query : " + strQuery);
				}
			};
			
//			wdmData DATA = new wdmData(AGENT_IP, AGENT_PORT, AGENT_KEY);
//			
//			if(DATA.ExecutUpdate(strQuery)!=1) {
//				//TRACE.TraceLog("Delete ExcuteUpdate Fail", Trace.LOG_DEBUG);	
//				return StringUtil.failWithMsg(TRACE, mapRes, DATA.ERR_MSG);
//			}
//			
//			
//			while(DATA.next()) {
//				mapRes.put("SE_FLAG", "91");
//				
//				strQuery = DELETE_UPDATE_QUERY;
//				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
//				strQuery = strQuery.replaceFirst("\\?", "getDate()");
//				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
//				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
//				strQuery = strQuery.replaceFirst("\\?", "'90'");
//				
//				if(!Set_Result(strQuery)) {
//					return StringUtil.failWithMsg(TRACE, mapRes, "Failed inserting status query.");
//				}
//				
//				mapRes.put("SE_FLAG", "92");
//				mapRes.put("RESULT", "T");
//				mapRes.put("MSG", "");
//				
//				return mapRes;
//			}
			
			return mapRes;
			
		} catch (Exception e) {
			ERR_MSG = "Delete.Run_Delete() Error : " + e.toString();
			TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
			//return mapRes;
			return StringUtil.failWithMsg(TRACE, mapRes, ERR_MSG);
		}

		
	}
	
	public boolean Set_Result(String query) {
		
		try	{
			
			wdmData DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
			
			if(query==null || query.equals("")) {
				ERR_MSG = "Search.Set_Result() : query is null";
				TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
				return false;
			}
			
			if(DATA.ExecutUpdate(query)!=1) {
				ERR_MSG = DATA.ERR_MSG;
				TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
				return false;
			}
			
			return true;
		
		} catch (Exception e) {
			ERR_MSG = "Delete.Set_Result() Error :  " + e.toString();
			TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
			return false;
		}
	}
	
//	public boolean Run_Delete(Map<String, String> mParam) {
//	
//	HashMap<String, String> mapRes = new HashMap<String, String>();
//	mapRes.put("RESULT", "F");
//	mapRes.put("MSG", "");
//	mapRes.put("SE_FLAG", "99");
//	
//	try {
//		String strQuery		= "";
//		
//		if(!Init_Configure()) {
//			return false;
//		}
//		
//		List<String> deleteList 	= DELETE.getAttValueList("//DELETE/ADD_PARAMETER/PARAM", "index");
//		List<String> keyList		= DELETE.getAttValueList("//DELETE/ADD_PARAMETER/PARAM[@type='key']", "index");
//		
//		if(deleteList.size()<=0 || keyList.size()<=0) {
//			return false;
//		} else {
//			String strKey		= mParam.get("SDOC_NO");
//			if(StringUtil.isBlank(strKey)) {
//				return false;
//			} else {
//				mapRes.put("SDOC_NO", strKey);
//				mapRes.put("SE_FLAG", "90");
//				
//				strQuery = DELETE_UPDATE_QUERY;
//				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
//				strQuery = strQuery.replaceFirst("\\?", "getDate()");
//				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
//				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
//				strQuery = strQuery.replaceFirst("\\?", "'12'");
//				
//				if(!Set_Result(strQuery)) {
//					return false;
//				}
//			}
//		}
//		
//		wdmData DATA = new wdmData(AGENT_IP, AGENT_PORT, AGENT_KEY);
//		
//		if(DATA.ExecutUpdate(strQuery)!=1) {
//			System.out.println(DATA.ExecutUpdate(strQuery));
//			TRACE.TraceLog("Delete ExcuteUpdate Fail", Trace.LOG_DEBUG);	
//			return false;
//		}
//		
//		
//		while(DATA.next()) {
//			Map<String, String> map = new HashMap<>();
//			mapRes.put("SE_FLAG", "91");
//			
//			strQuery = DELETE_UPDATE_QUERY;
//			strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
//			strQuery = strQuery.replaceFirst("\\?", "getDate()");
//			strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
//			strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
//			strQuery = strQuery.replaceFirst("\\?", "'90'");
//			
//			if(!Set_Result(strQuery)) {
//				return false;
//			}
//		}
//		
//	} catch (Exception e) {
//		ERR_MSG = e.toString();
//		return false;
//	}
//	
//	return true;
//}
	
}
