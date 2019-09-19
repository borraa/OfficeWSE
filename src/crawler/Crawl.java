package crawler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryparser.flexible.core.util.StringUtils;
import org.json.simple.JSONObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import common.chiper.ARIAChiper;
import common.com.HttpParser;
import common.com.IniParser;
import common.com.Parser;
import common.com.Trace;
import common.com.XmlParser;
import common.util.FileUtil;
import common.util.StringUtil;
import common.wdclient.wdmData;
import common.wdclient.wdmFile;
import index.Index;
import io.netty.buffer.ByteBuf;
import main.OfficeWSE;

@DisallowConcurrentExecution
public class Crawl implements Job {
	
	private Parser 		CONF				= null;
	private Parser 		CRAWLER				= null;
	private Parser 		INDEX				= null;
	private Trace		TRACE				= null;
	
	private boolean 	CRAWLER_USE 		= false;
	
	private String 		ROOT_PATH			= null;
	private String 		DATA_PATH			= null;
	private String 		DATA_RETRY_PATH		= null;
	private String 		INDEX_PATH			= null;
	private String 		BASE_ROOT_PATH		= null;
	private String 		BASE_FOLDER_PATH	= null;
	
	private String 		AGENT_IP			= null;
	private int 		AGENT_PORT			= 0;
	private String 		AGENT_KEY			= null;
	private String 		AGENT_CIPHER_KEY	= null;
	
	private String		SLIP_TABLE 			= null;

	public String 		SDOC_CRAWL_QUERY	= null;
	public String 		ADD_QUERY			= null;
	public String 		CRAWL_UPDATE_QUERY  = null;
	public String 		INDEX_RESULT_QUERY  = null;
	private String 		CRAWL_START_QUERY	= null;
	private String 		SDOC_CHK_QUERY		= null;
	private String 		SDOC_SE_QUERY		= null;
	private String 		SDOC_UPDATE_QUERY	= null;

	private String		FILE_FLAG			= null;
	
	private int 		RESULT_CNT			= 0;
	
	public String 		ERR_MSG				= null;


	//###################################//
	//## PUBLIC FUNCTION 
	//###################################//
	    
	public void execute(JobExecutionContext context) throws JobExecutionException {

		try {
	    	
			JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
			TRACE = (Trace) jobDataMap.get("trace");
			
			if(!Run_Crawl(null, true)) {
				return;
			}
			
			if(RESULT_CNT>0) {
				TRACE.TraceLog("Crawling complete...", Trace.LOG_RELEASE);	
			}

			
	    } catch(Exception e) {
	    	TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
	    } 
	}
	
	public boolean Init_Configure() {
		
    	try {

    		CONF 		= new IniParser();
    		INDEX		= new XmlParser();
    		CRAWLER 	= new XmlParser();
    		
        	if(!CONF.setParser("ini", "OfficeWSE")) {
        		if(TRACE == null) {
            		String LOG_PATH = CONF.getString("COMMON","LOG_PATH","log");
                	int LOG_LV 		= CONF.getInt("COMMON", "LOG_LV", 3);
                	TRACE = new Trace(common.com.Path.getProjectPath() + LOG_PATH, LOG_LV);
            	}
        		TRACE.TraceLog("Crawl.Init_Configure() : Wrong OfficeWSE.ini file.", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	if(TRACE == null) {
        		String LOG_PATH = CONF.getString("COMMON","LOG_PATH","log");
            	int LOG_LV 		= CONF.getInt("COMMON", "LOG_LV", 3);
            	TRACE = new Trace(common.com.Path.getProjectPath() + LOG_PATH, LOG_LV);
        	}
        
        	if(!INDEX.setParser("xml", "index")) {
        		TRACE.TraceLog("Crawl.Init_Configure() : Wrong index.xml file.", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	if(!CRAWLER.setParser("xml", "crawler")) {
        		TRACE.TraceLog("Crawl.Init_Configure() : Wrong crawler.xml file.", Trace.LOG_DEBUG);
        		return false;
        	}

        	CRAWLER_USE 		= CONF.getString("CRAWLER","USE").equalsIgnoreCase("YES")?true:false;
        	
        	ROOT_PATH 			= CONF.getString("COMMON","ROOT_PATH","");
        	DATA_PATH 			= CONF.getString("COMMON","DATA_PATH","");
        	DATA_RETRY_PATH 	= CONF.getString("COMMON","DATA_RETRY_PATH","");
        	INDEX_PATH 			= CONF.getString("COMMON","INDEX_PATH","");
        	BASE_ROOT_PATH 		= CONF.getString("COMMON","BASE_ROOT_PATH","");
        	BASE_FOLDER_PATH 	= CONF.getString("COMMON","BASE_FOLDER_PATH","");
        	
        	SLIP_TABLE			= CRAWLER.getValListByNode("//CRAWLER/CRAWL_TABLE/SLIP_TABLE").get(0).trim();
        	
        	SDOC_CRAWL_QUERY	= CRAWLER.getValListByNode("//CRAWLER/CRAWL_QUERY/SDOC_CRAWL_QUERY").get(0).trim();
        	ADD_QUERY			= CRAWLER.getValListByNode("//CRAWLER/ADD_QUERY").get(0).trim();
        	CRAWL_UPDATE_QUERY	= CRAWLER.getValListByNode("//CRAWLER/CRAWL_UPDATE_QUERY").get(0).trim();
        	CRAWL_START_QUERY	= CRAWLER.getValListByNode("//CRAWLER/CRAWL_START_QUERY").get(0).trim();
        	SDOC_CHK_QUERY		= CRAWLER.getValListByNode("//CRAWLER/SDOC_CHK_QUERY").get(0).trim();
        	SDOC_SE_QUERY		= CRAWLER.getValListByNode("//CRAWLER/SDOC_SE_QUERY").get(0).trim();
        	SDOC_UPDATE_QUERY	= CRAWLER.getValListByNode("//CRAWLER/SDOC_UPDATE_QUERY").get(0).trim();
        	
        	AGENT_IP			= CONF.getString("AGENT","IP","");
        	AGENT_PORT			= Integer.parseInt(CONF.getString("AGENT","PORT","0"));
        	AGENT_KEY			= CONF.getString("AGENT","KEY","");
        	AGENT_CIPHER_KEY	= CONF.getString("AGENT","CIPHER_KEY","");
        	
        	if(DATA_PATH.equals("") || DATA_RETRY_PATH.equals("") || INDEX_PATH.equals("")) {
        		TRACE.TraceLog("Crawl.Init_Configure() : Invalid OfficeWSE.ini data(COMMON).", Trace.LOG_DEBUG);
        		return false;
        	}
        	
         	if(CRAWLER_USE && (SLIP_TABLE.equals("") || SDOC_CRAWL_QUERY.equals("") || CRAWL_UPDATE_QUERY.equals("") || ADD_QUERY.equals("") )) {
         		TRACE.TraceLog("Crawl.Init_Configure() : Invalid crawler.xml data.", Trace.LOG_DEBUG);
        		return false;
        	}
         	
        	if(AGENT_IP.equals("") || AGENT_PORT==0 || AGENT_KEY.equals("") || AGENT_CIPHER_KEY.equals("")) {
        		TRACE.TraceLog("Crawl.Init_Configure() : Invalid OfficeWSE.ini data(AGENT).", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	//TRACE.TraceBox("Crawl.Init_Configure() OK");
        	
        	return true;
        	
    	} catch(Exception e) {
    		TRACE.TraceLog("Crawl.Init_Configure() Fail : " + e.toString(), Trace.LOG_DEBUG);
    		return false;
    	}
    }

	public boolean Run_Crawl(Map<String, String> mParam, boolean bSchedule) {
		return true;
	}

	
	public HashMap<String, String> Run_Crawl(Map<String, String> mParam) {

		HashMap<String, String> mapRes 	= new HashMap<String, String>();
		mapRes.put("RESULT", "F");
		mapRes.put("MSG", "");
		mapRes.put("SE_FLAG", "09");
		mapRes.put("JOB", "CRAWL");
		mapRes.put("SDOC_CHK", "F");
		mapRes.put("UPDATE", "F");
		
		try {
			if(!Init_Configure()) {
				//TRACE.TraceLog(strOutput + "Crawl.Run_Crawl() : Failed init configure.", Trace.LOG_DEBUG);
				return StringUtil.failWithMsg(TRACE, mapRes, "Crawl.Run_Crawl() : Failed init configure.");
			}
			
			boolean bRet		= true;
			String strQuery		= "";
			String strFilePath	= "";
			String chkQuery 	= "";
			String SeQuery		= "";
			
			List<String> CrawlList 		= CRAWLER.getAttValueList("//CRAWLER/PARAMETER/PARAM", "index");
			List<String> keyList		= CRAWLER.getAttValueList("//CRAWLER/PARAMETER/PARAM[@type='key']", "index");
			
			if(CrawlList.size()<=0 || keyList.size()<=0 ) {
				return StringUtil.failWithMsg(TRACE, mapRes, "Crawl.Run_Crawl() : Invalid crawler.xml data.");
			}
			else
			{
				String strKey 		= mParam.get("SDOC_NO");
				//FILE_FLAG 			= mParam.get("FLAG");
				
				if(StringUtil.isBlank(strKey)) /* || StringUtil.isBlank(FILE_FLAG) */
				{
					return StringUtil.failWithMsg(TRACE, mapRes, "Crawl.Run_Crawl() : Failed to receive SDOC_NO.");
				}
				else
				{
					// SDOCNO IMG_SLIPDOC_T 유무 확인
					chkQuery = SDOC_CHK_QUERY;
					
					for(String key:keyList) {
						if(mParam.get(key)!=null && !mParam.get(key).equals("")) {
							chkQuery = chkQuery.replaceFirst("\\?", key + " = '" + mParam.get(key) +"'");	
						}
					}
					
					wdmData SLIP_CHK_DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
					
					if(!SLIP_CHK_DATA.ExecutQuery(chkQuery)) {
						return StringUtil.failWithMsg(TRACE, mapRes, SLIP_CHK_DATA.ERR_MSG);
					} else {
						TRACE.TraceBox("Crawl.Run_Crawl() Select query : " + chkQuery);
					}
					
					if(SLIP_CHK_DATA.next()) {
						mapRes.put("SDOC_CHK", "T");
						//TRACE.TraceBox("Count : " + SLIP_CHK_DATA.getCount() + ", SDocNo : " + strKey);
						TRACE.TraceBox("There is a SDocNo(" + strKey + ") in the IMG_SLIPDOC_T Table.");
					} else {
						mapRes.put("SDOC_CHK", "F");
						//return StringUtil.failWithMsg(TRACE, mapRes, "Count : " + SLIP_CHK_DATA.getCount() + ", SDocNo : " + strKey);
						return StringUtil.failWithMsg(TRACE, mapRes, "There is no SDocNo(" + strKey + ") in the IMG_SLIPDOC_T Table.");
					}
					
					FILE_FLAG = SLIP_CHK_DATA.getString("FOLDER");
					
					SeQuery = SDOC_SE_QUERY;
					
					for(String key:keyList) {
						if(mParam.get(key)!=null && !mParam.get(key).equals("")) {
							SeQuery = SeQuery.replaceFirst("\\?", key + " = '" + mParam.get(key) +"'");	
						}
					}
					
					wdmData SDOCNO_SE_DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
					
					if(!SDOCNO_SE_DATA.ExecutQuery(SeQuery)) {
						return StringUtil.failWithMsg(TRACE, mapRes, SDOCNO_SE_DATA.ERR_MSG);
					} else {
						TRACE.TraceBox("Crawl.Run_Crawl() Select query : " + SeQuery);
					}
					
					if(SDOCNO_SE_DATA.next()) {
						TRACE.TraceBox("There is a SDocNo(" + strKey + ") in the IMG_SEARCH_T table.");
						
						mapRes.put("SDOC_NO", strKey);
						mapRes.put("FILE_FLAG", FILE_FLAG);
						mapRes.put("SE_FLAG", "00");
						mapRes.put("UPDATE", "T");
						
						strQuery = SDOC_UPDATE_QUERY;
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "getDate()");
						//strQuery = strQuery.replaceFirst("\\?", "getDate()");
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
						
						if(!Set_Result(strQuery)) { 
							//return StringUtil.failWithMsg(TRACE, mapRes, "Failed inserting status query." + strQuery);
							return StringUtil.failWithMsg(TRACE, mapRes, "Update query failed : " + strQuery);
						} else {
							TRACE.TraceBox("Crawl.Run_Crawl() Update query : " + strQuery);
						}
					} else {
						TRACE.TraceBox("There is no SDocNo(" + strKey + ") in the IMG_SEARCH_T Table.");
						
						mapRes.put("SDOC_NO", strKey);
						mapRes.put("FILE_FLAG", FILE_FLAG);
						mapRes.put("SE_FLAG", "00");
						mapRes.put("UPDATE", "F");
						
						strQuery = CRAWL_START_QUERY;
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("FILE_FLAG", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "getDate()"); 
//						strQuery = strQuery.replaceFirst("\\?", "getDate()");
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
						
						if(!Set_Result(strQuery)) { 
							//return StringUtil.failWithMsg(TRACE, mapRes, "Failed inserting status query.");
							return StringUtil.failWithMsg(TRACE, mapRes, "Insert query failed : " + strQuery);
						} else {
							TRACE.TraceBox("Crawl.Run_Crawl() Insert query : " + strQuery);
						}
					}
					
				}
			}
		
			strQuery = SDOC_CRAWL_QUERY;
			
			for(String key:keyList) {
				if(mParam.get(key)!=null && !mParam.get(key).equals("")) {
					strQuery = strQuery.replaceFirst("\\?", key + " = '" + mParam.get(key) +"'");	
				}
			}
			
			wdmData DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
			
			if(!DATA.ExecutQuery(strQuery)) {
				return StringUtil.failWithMsg(TRACE, mapRes, DATA.ERR_MSG + " : " + strQuery);
			} else {
				if(DATA.getCount() == 0) {
					ERR_MSG = "Crawl.Run_Crawl() Select query result 0";
					mapRes.put("MSG", ERR_MSG);
					TRACE.TraceBox("Crawl.Run_Crawl() Select query result 0");
				} else {
					TRACE.TraceBox("Crawl.Run_Crawl() Select query : " + strQuery);
				}
			}
			
			while(DATA.next()) {
				TRACE.TraceBox("Crawl.Run_Crawl() Select query Result : " + DATA.getCount());
				Map<String, String> map = new HashMap<>();
				mapRes.put("SE_FLAG", "01");
				
				//Insert status query before begin.
				strQuery = CRAWL_UPDATE_QUERY;
				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
				strQuery = strQuery.replaceFirst("\\?", "getDate()");
				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
				
				if(!Set_Result(strQuery)) { 
					//return StringUtil.failWithMsg(TRACE, mapRes, "Failed inserting status query.");
					return StringUtil.failWithMsg(TRACE, mapRes, "Update query failed : " + strQuery);
				} else {
					TRACE.TraceBox("Crawl.Run_Crawl() Update query : " + strQuery);
				}
				
				String idx			= DATA.getString("IDX");
				String file_name 	= DATA.getString("IDX")+"."+DATA.getString("FILE_TYPE");
				String json_name 	= DATA.getString("IDX")+".json";
				String updateStatus = mapRes.get("UPDATE");

				if(bRet) {
					
					for(String col:CrawlList) {
						String val = null;
						{
							val = DATA.getString(col);	
						}
						
						//map.put(col, val != null ? val.toLowerCase() : val);
						map.put(col, val);
					}	
				} 
				
				if(bRet) {
					
					//map.put("FILE_FLAG", mapRes.getOrDefault("FILE_FLAG", ""));
					
					if(Chk_Validate_File(file_name)) {
						map.put("INDEX_DOC_NAME", file_name);	
					} else {
						map.put("INDEX_DOC_NAME", "");
					}
					
					strFilePath = Get_File(idx, file_name, updateStatus);	
					
					if(Chk_Validate_File(file_name) || !strFilePath.equals("")) {
						
						String json_path = Get_Json(json_name, map, updateStatus);	
						
						if(!json_path.equals("")) {
							bRet = true;	
						} else {
							File file = new File(strFilePath);
							if(file.exists()) {
								if(!FileUtil.deleteFile(strFilePath)) {
									return StringUtil.failWithMsg(TRACE, mapRes, "Can`t delete file : "+strFilePath);
		    					}
							}
							else
							{
								return StringUtil.failWithMsg(TRACE, mapRes,  "Can`t Create json file : IDX = "+idx);
							}
						}
					} else {
						return StringUtil.failWithMsg(TRACE, mapRes, "Can`t download file : IDX = "+idx+" name = "+file_name);
					}
					
					mapRes.put("SE_FLAG", "02");
					mapRes.put("RESULT", "T");
					mapRes.put("MSG", "");

//					if("T".equalsIgnoreCase(updateStatus)) {
//						Index index = new Index();
//						index.Index_Folder_Update("C:/OfficeSE_DATA", "Retry");
//					}
					
			//		Index index = new Index();
			//		index.Index_File(file_path, mParam)
				
					return mapRes;
				}
			}
			return mapRes;
			
		} catch (Exception e) {
			return StringUtil.failWithMsg(TRACE, mapRes, e.toString());
		}
	}
	
	public HashMap<String, String> Run_Add(Map<String, String> mParam) {
		
		HashMap<String, String> mapRes 	= new HashMap<String, String>();
		mapRes.put("RESULT", "F");
		mapRes.put("MSG", "");
		mapRes.put("SE_FLAG", "09");
		mapRes.put("JOB", "ADD");
		mapRes.put("SDOC_CHK", "F");
		mapRes.put("UPDATE", "F");
		
		try {
			
			if(!Init_Configure()) {
				return StringUtil.failWithMsg(TRACE, mapRes, "Crawl.Run_Add() : Failed init configure.");
			}
			
			boolean bRet 		= true;
			String strQuery		= "";
			String SeQuery		= "";
			String strRet		= "";
			String chkQuery		= "";

			List<String> CrawlList 		= CRAWLER.getAttValueList("//CRAWLER/PARAMETER/PARAM", "index");
			List<String> keyList		= CRAWLER.getAttValueList("//CRAWLER/PARAMETER/PARAM[@type='key']", "index");
			
			if(CrawlList.size()<=0 || keyList.size()<=0 ) {
				return StringUtil.failWithMsg(TRACE, mapRes, "Crawl.Run_Add() : Invalid crawler.xml data.");
			}
			else
			{
				String strKey 		= mParam.get("SDOC_NO");

				if(StringUtil.isBlank(strKey))
				{
					return StringUtil.failWithMsg(TRACE, mapRes, "Crawl.Run_Add() : Failed to receive SDOC_NO.");
				}
				else
				{
					chkQuery = SDOC_CHK_QUERY;
					
					for(String key:keyList) {
						if(mParam.get(key)!=null && !mParam.get(key).equals("")) {
							chkQuery = chkQuery.replaceFirst("\\?", key + " = '" + mParam.get(key) +"'");	
						}
					}
					
					wdmData SLIP_CHK_DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
					
					if(!SLIP_CHK_DATA.ExecutQuery(chkQuery)) {
						return StringUtil.failWithMsg(TRACE, mapRes, SLIP_CHK_DATA.ERR_MSG);
					} else {
						TRACE.TraceBox("Crawl.Run_Add() Select query : " + chkQuery);
					}
					
					if(SLIP_CHK_DATA.next()) {
						mapRes.put("SDOC_CHK", "T");
						TRACE.TraceBox("There is a SDocNo(" + strKey + ") in the IMG_SLIPDOC_T table.");
					} else {
						mapRes.put("SDOC_CHK", "F");
						return StringUtil.failWithMsg(TRACE, mapRes, "There is no SDocNo(" + strKey + ") in the IMG_SLIPDOC_T Table.");
					}
					
					FILE_FLAG = SLIP_CHK_DATA.getString("FOLDER");
					
					SeQuery = SDOC_SE_QUERY;
					
					for(String key:keyList) {
						if(mParam.get(key)!=null && !mParam.get(key).equals("")) {
							SeQuery = SeQuery.replaceFirst("\\?", key + " = '" + mParam.get(key) +"'");	
						}
					}
					
					wdmData SDOCNO_SE_DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
					
					if(!SDOCNO_SE_DATA.ExecutQuery(SeQuery)) {
						return StringUtil.failWithMsg(TRACE, mapRes, SDOCNO_SE_DATA.ERR_MSG);
					} else {
						TRACE.TraceBox("Crawl.Run_Add() Select query : " + SeQuery);
					}
					
					if(SDOCNO_SE_DATA.next()) {
						TRACE.TraceBox("There is a SDocNo(" + strKey + ") in the IMG_SEARCH_T table.");
						
						mapRes.put("SDOC_NO", strKey);
						mapRes.put("FILE_FLAG", FILE_FLAG);
						mapRes.put("SE_FLAG", "00");
						mapRes.put("UPDATE", "T");
						
						strQuery = SDOC_UPDATE_QUERY;
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "getDate()");
						//strQuery = strQuery.replaceFirst("\\?", "getDate()");
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
						
						if(!Set_Result(strQuery)) { 
							//return StringUtil.failWithMsg(TRACE, mapRes, "Failed inserting status query." + strQuery);
							return StringUtil.failWithMsg(TRACE, mapRes, "Update query failed : " + strQuery);
						} else {
							TRACE.TraceBox("Crawl.Run_Add() Update query : " + strQuery);
						}
						
					} else {
						TRACE.TraceBox("There is no SDocNo(" + strKey + ") in the IMG_SEARCH_T Table.");
						
						mapRes.put("SDOC_NO", strKey);
						mapRes.put("FILE_FLAG", FILE_FLAG);
						mapRes.put("SE_FLAG", "00");
						mapRes.put("UPDATE", "F");
						
						strQuery = CRAWL_START_QUERY;
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("FILE_FLAG", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "getDate()"); 
//						strQuery = strQuery.replaceFirst("\\?", "getDate()");
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
						
						if(!Set_Result(strQuery)) { 
							//return StringUtil.failWithMsg(TRACE, mapRes, "Failed inserting status query.");
							return StringUtil.failWithMsg(TRACE, mapRes, "Insert query failed : " + strQuery);
						} else {
							TRACE.TraceBox("Crawl.Run_Add() Insert query : " + strQuery);
						}
					}
				}
			}
				
			strQuery = ADD_QUERY;
			
			for(String key:keyList) {
				if(mParam.get(key)!=null && !mParam.get(key).equals("")) {
					strQuery = strQuery.replaceFirst("\\?", key + " = '" + mParam.get(key) +"'");	
				}
			}
			
			wdmData DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
			
			if(!DATA.ExecutQuery(strQuery)) {
				return StringUtil.failWithMsg(TRACE, mapRes, DATA.ERR_MSG + " : " + strQuery);
			} else {
				if(DATA.getCount() == 0) {
					ERR_MSG = "Crawl.Run_Add() Select query result 0";
					mapRes.put("MSG", ERR_MSG);
					TRACE.TraceBox("Crawl.Run_Add() Select query result 0");
				} else {
					TRACE.TraceBox("Crawl.Run_Add() Select query : " + strQuery);
				}
			}
			
			while(DATA.next()) {
				TRACE.TraceBox("Crawl.Run_Add() Select query Result : " + DATA.getCount());
				Map<String, String> map = new HashMap<>();
				mapRes.put("SE_FLAG", "01");
				
				//Insert status query before begin.
				strQuery = CRAWL_UPDATE_QUERY;
				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
				strQuery = strQuery.replaceFirst("\\?", "getDate()");
				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
				strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
				
				if(!Set_Result(strQuery)) { 
					//return StringUtil.failWithMsg(TRACE, mapRes, "Failed inserting status query.");
					return StringUtil.failWithMsg(TRACE, mapRes, "Update query failed : " + strQuery);
				} else {
					TRACE.TraceBox("Crawl.Run_Add() Update query : " + strQuery);
				}
				
				strRet = DATA.getString("SDOC_NAME");
				
				String file_name 	= strRet + ".txt";
				String json_name 	= strRet + ".json";
				String updateStatus = mapRes.get("UPDATE");

				if(bRet) {
					
					for(String col:CrawlList) {
						String val = null;
						{
							val = DATA.getString(col);	
							if(val == null) {
								val = "";
							}
						}
						
						map.put(col, val);
					}
				} 

				if(bRet) {
					
					//map.put("FILE_FLAG", mapRes.getOrDefault("FILE_FLAG", ""));
					if(Chk_Validate_File(file_name)) {
						map.put("INDEX_DOC_NAME", file_name);	
					} else {
						map.put("INDEX_DOC_NAME", "");
					}
					
					String strFilePath = txtCopy(strRet, updateStatus);
					
					if(Chk_Validate_File(file_name) || !strFilePath.equals("")) {
						
						String json_path =  Get_Json(json_name, map, updateStatus);	
						
						if(!json_path.equals("")) {
							bRet = true;	
						} else {
							File file = new File(strFilePath);
							if(file.exists()) {
								if(!FileUtil.deleteFile(strFilePath)) {
									return StringUtil.failWithMsg(TRACE, mapRes, "Can`t delete file : "+strFilePath);
		    					}
							}
							else
							{
								return StringUtil.failWithMsg(TRACE, mapRes,  "Can`t Create json file : json_path => " + json_path);
							}
						}
					} else {
						return StringUtil.failWithMsg(TRACE, mapRes, "Can`t download file. name => "+file_name);
					}
					
					
					mapRes.put("SE_FLAG", "02");
					mapRes.put("RESULT", "T");
					mapRes.put("MSG", "");

					return mapRes;
				}
			}			
			return mapRes;
			
		} catch (Exception e) {
			return StringUtil.failWithMsg(TRACE, mapRes, e.toString());
		}

	}
	
	/**
	 * Copy file when job is add
	 * 
	 * @param 
	 * @return 
	 */
	public String txtCopy(String txtNm, String updateStatus) {
		
		String strCopyPath = "";
		String strPastePath = "";
		
		FileInputStream input = null;
		FileOutputStream output = null;
		
		try {
			strCopyPath = BASE_ROOT_PATH + "\\" + BASE_FOLDER_PATH + "\\Sample.txt";
			
			if("T".equalsIgnoreCase(updateStatus)) {
				strPastePath = ROOT_PATH + "\\" + DATA_RETRY_PATH  + "\\" + txtNm + ".txt";
			} else if ("F".equalsIgnoreCase(updateStatus)) {
				strPastePath = ROOT_PATH + "\\" + DATA_PATH  + "\\" + txtNm + ".txt";
			}
			
			File txtCopyFile = new File(strCopyPath);
			File txtPasteFile = new File(strPastePath);
			
			if(txtCopyFile.isFile()) {
				copyFile(txtCopyFile, txtPasteFile);
				TRACE.TraceBox("Crawl.txtCopy() File copy complete => FilePath : " + txtPasteFile);
			} else {
				TRACE.TraceBox("Crawl.txtCopy() isFile() => " + txtCopyFile + " is not a file.");
			}
			
		} catch (Exception e) {
			TRACE.TraceLog(e.toString(), Trace.LOG_DEBUG);
		}
		return strPastePath;
	}
	
	//파일복사
	private void copyFile(File source, File dest) {
		long startTime = System.currentTimeMillis();
		long totalSize = 0;
		
		int count = 0;
		byte[] b = new byte[128];
	  
		FileInputStream in = null;
		FileOutputStream out = null; 
		
		//성능 향상을 위한 버퍼 스트림 사용
		BufferedInputStream bin = null;
		BufferedOutputStream bout = null;
		
		try { 
			in = new FileInputStream(source);
			bin = new BufferedInputStream(in);
	   
			out = new FileOutputStream(dest);
			bout = new BufferedOutputStream(out);
			
			while((count = bin.read(b))!= -1){
				bout.write(b,0,count);
				totalSize += count;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			TRACE.TraceLog(e.toString(), Trace.LOG_DEBUG);
		} finally {	// 스트림 close 필수
			try {
				if(bout!=null) {
					bout.close();
				}    
				if (out != null) {
					out.close();
				}
				if(bin!=null) {
					bin.close();
				}
				if (in != null) {
					in.close();
				}
	   
			} catch (IOException r) {
				//TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
				TRACE.TraceLog("Crawl.copyFile() : Error during close.", Trace.LOG_DEBUG);
			}
		}
	  	//복사 시간 체크 
//	  	StringBuffer time = new StringBuffer("소요시간 : ");
//	  	time.append(System.currentTimeMillis() - startTime);
//	  	time.append(", FileSize : " + totalSize);
//	  	System.out.println(time);
	  }

	private String Get_File(String idx, String file_name, String updateStatus) {
		String strRet = "";
		try {
			
			if(!Chk_Validate_File(file_name))
				return "";
			
			if("T".equalsIgnoreCase(updateStatus)) {
				strRet = ROOT_PATH+"\\"+DATA_RETRY_PATH+"\\"+file_name;
				TRACE.TraceBox("Crawl.Get_File() datafile import ok, filePath : " + strRet);
			} else if("F".equalsIgnoreCase(updateStatus)) {
				strRet = ROOT_PATH+"\\"+DATA_PATH+"\\"+file_name;
				TRACE.TraceBox("Crawl.Get_File() datafile import ok, filePath : " + strRet);
			}
			
			wdmFile wf = new wdmFile(AGENT_IP ,AGENT_PORT, AGENT_KEY);
			wf.addMetaData("IDX", idx);
			
			byte[] b = null;
			
			b =  wf.Download_Byte(SLIP_TABLE);

			if(b==null)
				return "";
			ARIAChiper m_aria = new ARIAChiper(AGENT_CIPHER_KEY);
			
			if("T".equalsIgnoreCase(updateStatus)) {
				if(m_aria.DecryptFile(b, ROOT_PATH+"\\"+DATA_RETRY_PATH+"\\"+file_name))
				{
					try{
			 	        File lOutFile = new File(strRet);
			 	        if(!lOutFile.exists()) {
			 	        	FileOutputStream lFileOutputStream = new FileOutputStream(lOutFile);
				 	        lFileOutputStream.write(b);
				 	        lFileOutputStream.close();	
			 	        }
			 	    }catch(Throwable e){
			 	    	System.out.println(e.toString());
			 	        e.printStackTrace(System.out);
			 	    }
				}
				else
				{
					throw new Exception();
				}
			} else if("F".equalsIgnoreCase(updateStatus)) {
				if(m_aria.DecryptFile(b, ROOT_PATH+"\\"+DATA_PATH+"\\"+file_name))
				{
					try{
			 	        File lOutFile = new File(strRet);
			 	        if(!lOutFile.exists()) {
			 	        	FileOutputStream lFileOutputStream = new FileOutputStream(lOutFile);
				 	        lFileOutputStream.write(b);
				 	        lFileOutputStream.close();	
			 	        }
			 	    }catch(Throwable e){
			 	    	System.out.println(e.toString());
			 	        e.printStackTrace(System.out);
			 	    }
				}
				else
				{
					throw new Exception();
				}
			}
			
			
		} catch(Exception e) {
			TRACE.TraceLog(e.toString(), Trace.LOG_DEBUG);
			return "";
		}
		return strRet;
	}
	
	@SuppressWarnings("unchecked")
	private String Get_Json(String json_name, Map<String, String> map, String updateStatus) {
		String strRet = "";
		JSONObject SubObj = new JSONObject();
		
		try	{
			if("T".equalsIgnoreCase(updateStatus)) {
				strRet = ROOT_PATH+"\\"+DATA_RETRY_PATH+"\\"+json_name;
				TRACE.TraceBox("Crawl.Get_Json() jsonfile import ok, filePath : " + strRet);
			} else if("F".equalsIgnoreCase(updateStatus)) {
				strRet = ROOT_PATH+"\\"+DATA_PATH+"\\"+json_name;
				TRACE.TraceBox("Crawl.Get_Json() jsonfile import ok, filePath : " + strRet);
			}
			
			Iterator<String> keys = map.keySet().iterator();
			
			while (keys.hasNext()) {
				String key = (String)keys.next();
				String val = map.get(key);
     			SubObj.put(key, val);
			}
			
			FileWriter file = new FileWriter(strRet);
	        file.write(SubObj.toJSONString());
	        file.flush();
	        file.close();
		} catch (Exception e) {
			return "";
		}
		return strRet;
	}

	
//	private String Get_File(String idx, String file_name) {
//		String strRet = "";
//		try {
//			
//			if(!Chk_Validate_File(file_name))
//				return "";
//			
//			strRet = ROOT_PATH+"\\"+DATA_PATH+"\\"+file_name;
//			wdmFile wf = new wdmFile(AGENT_IP ,AGENT_PORT, AGENT_KEY);
//			wf.addMetaData("IDX", idx);
//			
//			byte[] b = null;
//			
//			if(FILE_FLAG.equalsIgnoreCase("ADDFILE")) {
//				b =  wf.Download_Byte(SLIP_TABLE);
//			} else if(FILE_FLAG.equalsIgnoreCase("SLIP")) {
//				b =  wf.Download_Byte(SLIP_TABLE);
//			}
//
//			if(b==null)
//				return "";
//			ARIAChiper m_aria = new ARIAChiper(AGENT_CIPHER_KEY);
//			if(m_aria.DecryptFile(b, ROOT_PATH+"\\"+DATA_PATH+"\\"+file_name))
//			{
//				try{
//		 	        File lOutFile = new File(strRet);
//		 	        if(!lOutFile.exists()) {
//		 	        	FileOutputStream lFileOutputStream = new FileOutputStream(lOutFile);
//			 	        lFileOutputStream.write(b);
//			 	        lFileOutputStream.close();	
//		 	        }
//		 	    }catch(Throwable e){
//		 	    	System.out.println(e.toString());
//		 	        e.printStackTrace(System.out);
//		 	    }
//			}
//			else
//			{
//				throw new Exception();
//			}
//			
//		} catch(Exception e) {
//			return "";
//		}
//		return strRet;
//	}
//	
//	@SuppressWarnings("unchecked")
//	private String Get_Json(String json_name, Map<String, String> map) {
//		String strRet = "";
//		JSONObject SubObj = new JSONObject();
//		
//		try	{
//			strRet = ROOT_PATH+"\\"+DATA_PATH+"\\"+json_name;
//			Iterator<String> keys = map.keySet().iterator();
//			
//			while (keys.hasNext()) {
//				String key = (String)keys.next();
//				String val = map.get(key);
//     			SubObj.put(key, val);
//			}
//			
//			FileWriter file = new FileWriter(strRet);
//	        file.write(SubObj.toJSONString());
//	        file.flush();
//	        file.close();
//		} catch (Exception e) {
//			return "";
//		}
//		return strRet;
//	}
	
	public boolean Set_Result(String query) {
		
		try	{
			
			wdmData DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
			
			if(query==null || query.equals("")) {
				return false;
			}
			
			if(DATA.ExecutUpdate(query)!=1)
				return false;
			
			return true;
		
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean Chk_Validate_File(String file_name) {
		if(	file_name.toUpperCase().endsWith(".TXT") || 
			file_name.toUpperCase().endsWith(".HTM") || 
			file_name.toUpperCase().endsWith(".HTML")|| 
			file_name.toUpperCase().endsWith(".XML") ||
			file_name.toUpperCase().endsWith(".DOC") || 
			file_name.toUpperCase().endsWith(".PPT") || 
			file_name.toUpperCase().endsWith(".XLS") ||
			file_name.toUpperCase().endsWith(".DOCX")|| 
			file_name.toUpperCase().endsWith(".PPTX")|| 
			file_name.toUpperCase().endsWith(".XLSX")|| 
			file_name.toUpperCase().endsWith(".PDF") || 
			file_name.toUpperCase().endsWith(".INI") || 
			file_name.toUpperCase().endsWith(".HWP"))
			return true;
		else
			return true;
	}

	private boolean Set_Folder_Idx(String folder_code, String folder_owner, Map<String, String> map) {
		try {
			String query = "";
			if(folder_code==null || folder_code.equals("") || folder_owner==null || folder_owner.equals("")) {
				ERR_MSG = "Set_Folder_Idx() error : param invalid.";
				return false;
			}
			
		//	query = FOLDER_QUERY.replaceFirst("\\?", "'"+folder_owner+"'");
			query = query.replaceFirst("\\?", "'"+folder_owner+"'");
			query = query.replaceFirst("\\?", "'"+folder_code+"'");
			
			wdmData DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
			if(!DATA.ExecutQuery(query)) {
				ERR_MSG = DATA.ERR_MSG;
				return false;
			}
			
			if(DATA.getCount() != 1) {
				ERR_MSG = "Set_Folder_Idx() error : wrong count.";
				return false;
			}
			
			if(DATA.next()) {
				map.put("CODE_TREE", DATA.getString("CODE_TREE"));
				map.put("NAME_TREE", DATA.getString("NAME_TREE"));
			}
		} catch(Exception e) {
			ERR_MSG = "Set_Folder_Idx() error : "+e.toString();
			return false;
		}
		return true;
	}
	
	private String Get_Folder_Idx(String folder_code, String folder_owner) {
		String strRet = "";
		try {
			String query = "";
			if(folder_code==null || folder_code.equals("") || folder_owner==null || folder_owner.equals("")) {
				ERR_MSG = "Get_Folder_idx() error : param invalid.";
				return "";
			}
			
			//query = FOLDER_QUERY.replaceFirst("\\?", "'"+folder_owner+"'");
			query = query.replaceFirst("\\?", "'"+folder_owner+"'");
			query = query.replaceFirst("\\?", "'"+folder_code+"'");
			
			wdmData DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
			if(!DATA.ExecutQuery(query)) {
				ERR_MSG = DATA.ERR_MSG;
				return "";
			}
			
			if(DATA.getCount() != 1) {
				ERR_MSG = "Get_Folder_idx() error : wrong count.";
				return "";
			}
			
			if(DATA.next()) {
				strRet = DATA.getString("CODE_TREE");
			}
		} catch(Exception e) {
			ERR_MSG = "Get_Folder_idx() error : "+e.toString();
			return "";
		}
		return strRet;
	}
	
	private String Get_Folder_Name(String folder_code, String folder_owner) {
		String strRet = "";
		try {
			String query = "";
			if(folder_code==null || folder_code.equals("") || folder_owner==null || folder_owner.equals("")) {
				ERR_MSG = "Get_Folder_Name() error : param invalid.";
				return "";
			}
			
		//	query = FOLDER_QUERY.replaceFirst("\\?", "'"+folder_owner+"'");
			query = query.replaceFirst("\\?", "'"+folder_owner+"'");
			query = query.replaceFirst("\\?", "'"+folder_code+"'");
			
			wdmData DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
			if(!DATA.ExecutQuery(query)) {
				ERR_MSG = DATA.ERR_MSG;
				return "";
			}
			
			if(DATA.getCount() != 1) {
				ERR_MSG = "Get_Folder_Name() error : wrong count.";
				return "";
			}
			
			if(DATA.next()) {
				strRet = DATA.getString("NAME_TREE");
			}
		} catch(Exception e) {
			ERR_MSG = "Get_Folder_Name() error : "+e.toString();
			return "";
		}
		return strRet;
	}


//	public boolean Run_Crawl_(Map<String, String> mParam) {
//
//		try {
//			boolean bRet		= true;
//			String strQuery		= "";
//			String strFilePath	= "";
//			
//			if(!Init_Configure()) {
//				return false;
//			}
//			
//			List<String> CrawlList 		= CRAWLER.getAttValueList("//CRAWLER/ADD_PARAMETER/PARAM", "index");
//			List<String> keyList		= CRAWLER.getAttValueList("//CRAWLER/ADD_PARAMETER/PARAM[@type='key']", "index");
//			
//			if(CrawlList.size()<=0 || keyList.size()<=0 ) {
//				TRACE.TraceLog("Invalid crawler.xml data.", Trace.LOG_DEBUG);
//				return false;
//			}
//			
//			{
//				SORT_FLAG = (String)mParam.get("FLAG");
//				
//				if(SORT_FLAG.equalsIgnoreCase("ADDFILE")) {
//					strQuery = CRAWL_QUERY;
//				} else {
//					strQuery = CRAWL_QUERY;
//				}
//				
//				
//				for(String key:keyList) {
//					if(mParam.get(key)!=null && !mParam.get(key).equals("")) {
//						strQuery = strQuery.replaceFirst("\\?", key + " = '" + mParam.get(key) +"'");	
//					}
//				}
//        	}
//			
//			wdmData DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
//			
//			if(!DATA.ExecutQuery(strQuery)) {
//				TRACE.TraceLog(DATA.ERR_MSG, Trace.LOG_DEBUG);
//				return false;
//			}
//			
//			while(DATA.next()) {
//			
//				Map<String, String> map = new HashMap<>();
//				
//				map.put("SE_FLAG", "01");
//				
//				String idx			= DATA.getString("IDX");
//				String file_name 	= DATA.getString("IDX")+"."+DATA.getString("FILE_TYPE");
//				String json_name 	= DATA.getString("IDX")+".json";
//
//				
//				if(bRet) {
//					
//					for(String col:CrawlList) {
//						String val = null;
//						{
//							val = DATA.getString(col);	
//						}
//						
//						map.put(col, val);
//					}	
//				} 
//				
//				if(bRet) {
//					
//					String index_key = "";
//					
//					for(String key:keyList) {
//						index_key += DATA.getString(key.toUpperCase());
//					}
//					
//					map.put("SDOC_NO", index_key);
//					
//					if(Chk_Validate_File(file_name)) {
//						map.put("INDEX_DOC_NAME", file_name);	
//					} else {
//						map.put("INDEX_DOC_NAME", "");
//					}
//
//					
//					strFilePath = Get_File(idx, file_name);	
//					
//					SORT_FLAG = (String)mParam.get("FLAG");
//					
//					if(!Chk_Validate_File(file_name) || !strFilePath.equals("")) {
//						
//						if(SORT_FLAG.equalsIgnoreCase("ADDFILE")) {
//							map.put("SORT_FLAG", "A");
//						} else if(SORT_FLAG.equalsIgnoreCase("SLIP")){
//							map.put("SORT_FLAG", "S");
//						}
//						
//						String json_path = Get_Json(json_name, map);	
//
//						if(!json_path.equals("")) {
//							bRet = true;	
//						} else {
//							File file = new File(strFilePath);
//							if(file.exists()) {
//								if(!FileUtil.deleteFile(strFilePath)) {
//									TRACE.TraceLog("Warning : Can`t delete file : "+strFilePath, Trace.LOG_NORMAL);
//		    					}
//							}
//							TRACE.TraceLog("Can`t Create json file : IDX = "+idx, Trace.LOG_DEBUG);
//							bRet = false;
//						}
//					} else {
//						TRACE.TraceLog("Can`t download file : IDX = "+idx+" name = "+file_name, Trace.LOG_DEBUG);
//						bRet = false;
//					}
//					
//					String strRetQuery = "";
//
//					 if(StringUtil.getContainString(CRAWL_RESULT_QUERY,"\\?") != keyList.size()+5){ 
//						 TRACE.TraceLog("Invalid index.xml or crawler.xml data.", Trace.LOG_DEBUG);
//						 return false; 
//					 }
//
//					 strRetQuery = CRAWL_RESULT_QUERY;
//
//					 //strRetQuery = strRetQuery.replaceFirst("\\?", (bRet?"02":"09"));
//					 strRetQuery = strRetQuery.replaceFirst("\\?", "'"+map.getOrDefault("SDOC_NO", "")+"'");
//					 strRetQuery = strRetQuery.replaceFirst("\\?", "'"+map.getOrDefault("SE_FLAG", "")+"'");
//					 strRetQuery = strRetQuery.replaceFirst("\\?", "'"+map.getOrDefault("SORT_FLAG", "")+"'");
//					 strRetQuery = strRetQuery.replaceFirst("\\?", "'"+map.getOrDefault("REG_TIME", "")+"'"); 
//					 strRetQuery = strRetQuery.replaceFirst("\\?", "'"+map.getOrDefault("UPDATE_TIME", "")+"'");
//					 strRetQuery = strRetQuery.replaceFirst("\\?", "'"+map.getOrDefault("MSG", "")+"'");
//
//					 if(!Set_Result(strRetQuery)) { 
//						 TRACE.TraceLog("Set Crawler Result Fail", Trace.LOG_DEBUG); 
//						 return false; 
//					 }
//					 
//					 if(map != null) {
//						 RESULT_CNT++;
//					 }
//					 
//					 if(RESULT_CNT>0) {
//						 
//						 strRetQuery = INDEX_RESULT_QUERY;
//						 strRetQuery = strRetQuery.replaceFirst("\\?", (bRet?"02":"09"));
//						 strRetQuery = strRetQuery.replaceFirst("\\?", (String) (bRet?"":"Crawling Error"));
//						 
//						 for(String key:keyList) { 
//							 strRetQuery = strRetQuery.replaceFirst("\\?", key + " = '" + DATA.getString(key.toUpperCase()) +"'"); 
//						 }
//						 
//						 if(!Set_Result(strRetQuery)) { 
//							 TRACE.TraceLog("Set Crawler Result Fail", Trace.LOG_DEBUG); 
//							 return false; 
//						 }
//						 
//						 TRACE.TraceLog("Crawling complete...", Trace.LOG_RELEASE);	
//					 }
//				}
//				
//				// Indexing Start
//				Index index = new Index();
//				boolean bIdxRet = true;
//				String strRetQuery = "";
//				
//				/*
//				 * Map<String, String> mapRes = new HashMap<>();
//				 * 
//				 * mapRes.put("result", "T"); mapRes.put("SDOC_NO", ""); mapRes.put("SE_FLAG",
//				 * ""); mapRes.put("UPDATE_TIME", ""); mapRes.put("MSG", "");
//				 */
//				
//				if(bIdxRet) {
//					strRetQuery = INDEX_RESULT_QUERY;
//					strRetQuery = strRetQuery.replaceFirst("\\?", (bIdxRet?"10":"19"));
//					strRetQuery = strRetQuery.replaceFirst("\\?", (String) (bIdxRet?"":"Set Index Initialize Fail"));
//					
//					for(String key:keyList) { 
//						strRetQuery = strRetQuery.replaceFirst("\\?", key + " = '" + DATA.getString(key.toUpperCase()) +"'"); 
//					}
//					
//					if(!Set_Result(strRetQuery)) { 
//						/*
//						 * mapRes.put("result", "F"); mapRes.put("UPDATE_TIME", "GETDATE()");
//						 * mapRes.put("msg", "Set Index Initialize Fail");
//						 */
//						//TRACE.TraceLog("Set Index Result Fail", Trace.LOG_DEBUG); 
//						return false; 
//					}
//				}
//				
//				if(index.Init_Configure()) {
//					strRetQuery = INDEX_RESULT_QUERY;
//					strRetQuery = strRetQuery.replaceFirst("\\?", (bIdxRet?"11":"19"));
//					strRetQuery = strRetQuery.replaceFirst("\\?", (String) (bIdxRet?"":"Index Configure Error"));
//					 
//					for(String key:keyList) { 
//						strRetQuery = strRetQuery.replaceFirst("\\?", key + " = '" + DATA.getString(key.toUpperCase()) +"'"); 
//					}
//					 
//					if(!Set_Result(strRetQuery)) { 
//						TRACE.TraceLog("Set Index Result Fail", Trace.LOG_DEBUG); 
//						return false; 
//					}
//				}
//				
//				if(index.Index_Folder()) {
//					strRetQuery = INDEX_RESULT_QUERY;
//					strRetQuery = strRetQuery.replaceFirst("\\?", (bIdxRet?"12":"19"));
//					strRetQuery = strRetQuery.replaceFirst("\\?", (String) (bIdxRet?"":"Index Folder Error"));
//					 
//					for(String key:keyList) { 
//						strRetQuery = strRetQuery.replaceFirst("\\?", key + " = '" + DATA.getString(key.toUpperCase()) +"'"); 
//					}
//					 
//					if(!Set_Result(strRetQuery)) { 
//						TRACE.TraceLog("Set Index Result Fail", Trace.LOG_DEBUG); 
//						return false; 
//					}
//				}
//			}
//			
//			return true;
//			
//		} catch (Exception e) {
//			TRACE.TraceLog(e.toString(), Trace.LOG_DEBUG);
//			return false;
//		}
//	}
	
	
	
/* 기존 크롤링 부분 */
//	public boolean Run_Crawl(Map<String, String> mParam, boolean bSchedule) {
//    	
//		try{
//			
//			boolean bRet 		= true;
//			String strQuery 	= "";
//			String strFilePtah 	= "";
//			
//			Map<String, String> ParentMap 	= new HashMap<String, String>();
//			
//	        if(!bSchedule && (mParam == null || mParam.equals(""))) {
//				TRACE.TraceLog("Can`t find index key from request parameter.", Trace.LOG_DEBUG);
//				return false;
//			}
//			
//			if(!Init_Configure()) {
//				return false;
//			}
//			
//			List<String> index_list = INDEX.getAttValueList("//INDEX/PARAMETER/PARAM","index");
//			List<String> key_list 	= INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@type='key']","index");
//			String index_flag		= INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@type='flag']","index").get(0);
//			
//			if(index_list.size()<=0 || key_list.size()<=0 ) {
//				TRACE.TraceLog("Invalid index.xml data.", Trace.LOG_DEBUG);
//				return false;
//			}
//			
//			if(bSchedule) {
//				strQuery = CRAWL_QUERY;
//			} else {
//		
//				strQuery = INDEX_QUERY;
//				
//				
//				for(String key:key_list) {
//					if(mParam.get(key)!=null && !mParam.get(key).equals("")) {
//						strQuery = strQuery.replaceFirst("\\?", key + " = '" + mParam.get(key) +"'");	
//					}
//				}
//        	}
//			
//			wdmData DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
//			
//			if(!DATA.ExecutQuery(strQuery)) {
//				TRACE.TraceLog(DATA.ERR_MSG, Trace.LOG_DEBUG);
//				return false;
//			}
//			
//			while(DATA.next()) {
//			
//				Map<String, String> map = new HashMap<>();
//			//	Map<String, String> folder_info_map = new HashMap<>();
//				
//				String idx			= DATA.getString("IDX");
//				String file_name 	= DATA.getString("IDX")+"."+DATA.getString("FILE_TYPE");
//				String json_name 	= DATA.getString("IDX")+".json";
//				
////				boolean bFolder_Info = false;
////				for(String col:index_list) {
////					if(col.equalsIgnoreCase("FOLDER_IDX") || col.equalsIgnoreCase("FOLDER_NAME")) 
////						bFolder_Info = true;
////				}
//				
////				if(bFolder_Info) {
////					if(!Set_Folder_Idx(DATA.getString("FOLDER_CODE"), DATA.getString("FOLDER_OWNER"), folder_info_map)) {
////						TRACE.TraceLog(ERR_MSG, Trace.LOG_NORMAL);
////						bRet = false;
////					}
////				}
//				
//				if(bRet) {
//					
//					for(String col:index_list) {
//						String val = null;
////						if(col.equalsIgnoreCase("FOLDER_IDX")) {
////							val =  folder_info_map.get("CODE_TREE");
////							if(val == null || val.equals("")) {
////								TRACE.TraceLog(ERR_MSG, Trace.LOG_NORMAL);
////								bRet = false;
////							}
////						} else if(col.equalsIgnoreCase("FOLDER_NAME")) {
////							val =  folder_info_map.get("NAME_TREE");
////							if(val == null || val.equals("")) {
////								TRACE.TraceLog(ERR_MSG, Trace.LOG_NORMAL);
////								bRet = false;
////							}
////						} else 
//						{
//							val = DATA.getString(col);	
//						}
//						
//						map.put(col, val);
//					}	
//				} 
//				
//				if(bRet) {
//					
//					String index_key = "";
//					
//					for(String key:key_list) {
//						index_key += DATA.getString(key.toUpperCase());
//					}
//					
//					map.put("INDEX_KEY", index_key);
//					
//					if(Chk_Validate_File(file_name)) {
//						map.put("INDEX_DOC_NAME", file_name);	
//					} else {
//						map.put("INDEX_DOC_NAME", "");
//					}
//					
//					if(map.get(index_flag).equalsIgnoreCase("D")) {
//						strFilePtah = "D";
//						map.put("INDEX_DOC_NAME", "");
//					} else {
//						strFilePtah = Get_File(idx, file_name);	
//					}
//										
//					if(!Chk_Validate_File(file_name) || !strFilePtah.equals("")) {
//						
//						String json_path = Get_Json(json_name, map);	
//						
//						if(!json_path.equals("")) {
//							bRet = true;	 
//						} else {
//							File file = new File(strFilePtah);
//							if(file.exists()) {
//								if(!FileUtil.deleteFile(strFilePtah)) {
//									TRACE.TraceLog("Warning : Can`t delete file : "+strFilePtah, Trace.LOG_NORMAL);
//		    					}
//							}
//							TRACE.TraceLog("Can`t Create json file : IDX = "+idx, Trace.LOG_DEBUG);
//							bRet = false;
//						}
//					} else {
//						TRACE.TraceLog("Can`t download file : IDX = "+idx+" name = "+file_name, Trace.LOG_DEBUG);
//						bRet = false;
//					}
//				} 
//				
//				String strRetQuery = "";
//				
//				if(StringUtil.getContainString(RESULT_QUERY,"\\?") != key_list.size()+1) {
//					TRACE.TraceLog("Invalid index.xml or crawler.xml data.", Trace.LOG_DEBUG);
//					return false;
//				}
//				
//				strRetQuery = RESULT_QUERY;
//				
//				strRetQuery = strRetQuery.replaceFirst("\\?", map.get(index_flag)+(bRet?"T":"F"));
//				
//				for(String key:key_list) {
//					strRetQuery = strRetQuery.replaceFirst("\\?", key + " = '" + DATA.getString(key.toUpperCase()) +"'");	
//				}
//				
//				if(!Set_Result(strRetQuery)) {
//					TRACE.TraceLog("Set result fail", Trace.LOG_DEBUG);
//				}
//				
////				if(!ParentMap.containsKey(DATA.getString(key_list.get(0).toUpperCase())) || ParentMap.get(DATA.getString(key_list.get(0).toUpperCase())).equalsIgnoreCase("T")) {
////					ParentMap.put(DATA.getString(key_list.get(0).toUpperCase()), bRet?"T":"F");
////					
////					strRetQuery = RESULT_QUERY_PARENT;
////					strRetQuery = strRetQuery.replaceFirst("\\?", map.get(index_flag)+(bRet?"T":"F"));
////					strRetQuery = strRetQuery.replaceFirst("\\?", key_list.get(0) + " = '" + DATA.getString(key_list.get(0).toUpperCase()) +"'");	
////					
////					if(!Set_Result(strRetQuery)) {
////						TRACE.TraceLog("Set result fail", Trace.LOG_DEBUG);
////					}
////				}
//			}
//			
//			return true;
//		} catch(Exception e) {
//			TRACE.TraceLog(e.toString(), Trace.LOG_DEBUG);
//			return false;
//		}
//	}
	
	
}
