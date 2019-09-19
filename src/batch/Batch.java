package batch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import common.chiper.ARIAChiper;
import common.com.IniParser;
import common.com.Parser;
import common.com.Trace;
import common.com.XmlParser;
import common.util.FileUtil;
import common.util.StringUtil;
import common.wdclient.wdmData;
import common.wdclient.wdmFile;
import index.Index;

@DisallowConcurrentExecution
public class Batch implements Job {

	private Parser 		CONF					= null;
	private Parser 		INDEX					= null;
	private Parser 		BATCH					= null;
	
	private boolean 	BATCH_USE				= false;
	
	private String 		ROOT_PATH				= null;
	private String 		DATA_PATH				= null;
    private String 		DATA_RETRY_PATH			= null;
	private String 		INDEX_PATH				= null;
	
	private String 		AGENT_IP				= null;
	private int 		AGENT_PORT				= 0;
	private String 		AGENT_KEY				= null;
	private String 		AGENT_CIPHER_KEY		= null;

	private String		SLIP_TABLE 				= null;
	
//	private String 		ADD_BATCH_QUERY			= null;
	private String 		SDOC_BATCH_QUERY		= null;
//	private String 		ADD_SELECT_QUERY		= null;
	private String 		SDOC_SELECT_QUERY		= null;
	private String 		BATCH_START_QUERY		= null;
	private String 		BATCH_UPDATE_QUERY		= null;
//	private String 		ADD_CHECK_QUERY			= null;
	private String 		SDOC_CHECK_QUERY		= null;
	private String 		SDOC_SE_QUERY			= null;
	private String 		SDOC_UPDATE_QUERY		= null;

	private String		FILE_FLAG				= null;
	
	private int 		RESULT_CNT				= 0;
	
	private Trace		TRACE					= null;
	public String 		ERR_MSG					= null;
	
	
	//###################################//
	//## PUBLIC FUNCTION 
	//###################################//
	    
	public void execute(JobExecutionContext context) throws JobExecutionException {
	    	
		try {
	    	
			JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
			TRACE = (Trace) jobDataMap.get("trace");
			
			SimpleDateFormat format1 = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
			Date time = new Date();
			
			String time1 = format1.format(time);
			System.out.println("Batch : "+time1);
			
			if(!Init_Configure()) {
				return;
			}
			
			HashMap<String, String> mapRes = new HashMap<String, String>();
			Run_Batch(mapRes);
			
			Index index = new Index();
			index.Init_Configure();
			index.Index_Folder();
			index.Index_Folder_Update();

			
	    } catch(Exception e) {
	    	TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
	    } 
	}
	
	public boolean Init_Configure() {
    	try {
    		
    		CONF 		= new IniParser();
    		INDEX		= new XmlParser();
    		BATCH 		= new XmlParser();
    		
        	if(!CONF.setParser("ini", "OfficeWSE")) {
        		TRACE.TraceLog("OfficeWSE.ini is Wrong.", Trace.LOG_DEBUG);
        		return false;
        	}
        
        	if(!INDEX.setParser("xml", "index")) {
        		TRACE.TraceLog("index.xml is Wrong.", Trace.LOG_DEBUG);
        		return false;
        	}

        	if(!BATCH.setParser("xml", "batch")) {
        		TRACE.TraceLog("batch.xml is Wrong.", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	if(TRACE == null) {
        		String LOG_PATH = CONF.getString("COMMON","LOG_PATH","log");
            	int LOG_LV 		= CONF.getInt("COMMON", "LOG_LV", 3);
            	TRACE = new Trace(common.com.Path.getProjectPath()+LOG_PATH, LOG_LV);
        	}
        	
        	BATCH_USE 			= CONF.getString("BATCH","USE").equalsIgnoreCase("YES")?true:false;
        	
        	ROOT_PATH 			= CONF.getString("COMMON","ROOT_PATH","");
        	DATA_PATH 			= CONF.getString("COMMON","DATA_PATH","");
        	DATA_RETRY_PATH 	= CONF.getString("COMMON","DATA_RETRY_PATH","");
        	INDEX_PATH 			= CONF.getString("COMMON","INDEX_PATH","");
        	
        	SLIP_TABLE			= BATCH.getValListByNode("//BATCH/BATCH_TABLE/SLIP_TABLE").get(0).trim();
        	
        	SDOC_BATCH_QUERY	= BATCH.getValListByNode("//BATCH/BATCH_QUERY/SDOC_BATCH_QUERY").get(0).trim();
        	SDOC_SELECT_QUERY	= BATCH.getValListByNode("//BATCH/BATCH_SELECT_QUERY/SDOC_SELECT_QUERY").get(0).trim();
        	BATCH_START_QUERY	= BATCH.getValListByNode("//BATCH/BATCH_START_QUERY").get(0).trim();
        	BATCH_UPDATE_QUERY	= BATCH.getValListByNode("//BATCH/BATCH_UPDATE_QUERY").get(0).trim();
        	
        	SDOC_CHECK_QUERY	= BATCH.getValListByNode("//BATCH/SDOC_CHECK_QUERY").get(0).trim();
        	SDOC_SE_QUERY		= BATCH.getValListByNode("//BATCH/SDOC_SE_QUERY").get(0).trim();
        	SDOC_UPDATE_QUERY	= BATCH.getValListByNode("//BATCH/SDOC_UPDATE_QUERY").get(0).trim();

        	AGENT_IP			= CONF.getString("AGENT","IP","");
        	AGENT_PORT			= Integer.parseInt(CONF.getString("AGENT","PORT","0"));
        	AGENT_KEY			= CONF.getString("AGENT","KEY","");
        	AGENT_CIPHER_KEY	= CONF.getString("AGENT","CIPHER_KEY","");
        	
        	if(DATA_PATH.equals("") || DATA_RETRY_PATH.equals("") || INDEX_PATH.equals("")) {
        		TRACE.TraceLog("Invalid OfficeWSE.ini data(COMMON).", Trace.LOG_DEBUG);
        		return false;
        	}
        	
         	if(BATCH_USE && (SLIP_TABLE.equals("") || SDOC_BATCH_QUERY.equals("") || SDOC_SELECT_QUERY.equals(""))) {
         		TRACE.TraceLog("Invalid batch.xml data.", Trace.LOG_DEBUG);
        		return false;
        	}
         	
        	if(AGENT_IP.equals("") || AGENT_PORT==0 || AGENT_KEY.equals("") || AGENT_CIPHER_KEY.equals("")) {
        		TRACE.TraceLog("Invalid OfficeWSE.ini data(AGENT).", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	return true;
        	
    	} catch(Exception e) {
    		TRACE.TraceLog(e.toString(), Trace.LOG_DEBUG);
    		return false;
    	}
    }
	
	public HashMap<String, String> Run_Batch(Map<String, String> mParam) {
		
		HashMap<String, String> mapRes = new HashMap<String, String>();
		mapRes.put("RESULT", "F");
		mapRes.put("MSG", "");
		mapRes.put("SE_FLAG", "09");
		mapRes.put("JOB", "BATCH");
		mapRes.put("SDOC_CHK", "F");
		mapRes.put("UPDATE", "F");
		
		try {
			boolean bRet			= true;
			String strSelectQuery	= "";
			String strQuery			= "";
			String strFilePath		= "";
			String chkQuery			= "";
			String SeQuery			= "";
			
			if(!Init_Configure()) {
				return StringUtil.failWithMsg(TRACE, mapRes, "Failed init Configure.");
			}
			
			List<String> CrawlList 		= BATCH.getAttValueList("//BATCH/PARAMETER/PARAM", "index");
			List<String> keyList		= BATCH.getAttValueList("//BATCH/PARAMETER/PARAM[@type='key']", "index");
			
			// IMG_SLIPDOC_T 에 SDOC_NO 유무 확인
			strSelectQuery = SDOC_SELECT_QUERY;
			
			wdmData SLIP_DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
			
			if(!SLIP_DATA.ExecutQuery(strSelectQuery)) {
				mapRes.put("MSG", SLIP_DATA.ERR_MSG);
				return StringUtil.failWithMsg(TRACE, mapRes, SLIP_DATA.ERR_MSG);
			}
			
			while(SLIP_DATA.next()) {
				if(CrawlList.size()<=0 || keyList.size()<=0 ) {
					return StringUtil.failWithMsg(TRACE, mapRes, "Invalid batch.xml data.");
				}
				else
				{
					String strKey 	= SLIP_DATA.getString("SDOC_NO");
					FILE_FLAG 		= SLIP_DATA.getString("FOLDER");
					
					if (StringUtil.isBlank(strKey))  /* || StringUtil.isBlank(FILE_FLAG) */
					{
						return StringUtil.failWithMsg(TRACE, mapRes, "Failed to receive SDOC_NO.");
					}
					else
					{			
						// 증빙/첨부 테이블에서 SDocNo 유무 확인
						chkQuery = SDOC_CHECK_QUERY;
						
						for(String key:keyList) {
							if(SLIP_DATA.getString(key)!=null && !SLIP_DATA.getString(key).equals("")) {
								chkQuery = chkQuery.replaceFirst("\\?", key + " = '" + SLIP_DATA.getString(key) +"'");	
							}
						}
						
						wdmData SLIP_CHK_DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
						
						if(!SLIP_CHK_DATA.ExecutQuery(chkQuery)) {
							return StringUtil.failWithMsg(TRACE, mapRes, SLIP_CHK_DATA.ERR_MSG);
						}
						
						if(SLIP_CHK_DATA.next()) {
							mapRes.put("SDOC_CHK", "T");
						} else {
							mapRes.put("SDOC_CHK", "F");
							return StringUtil.failWithMsg(TRACE, mapRes, "There is no SDocNo("+strKey+") in the table.");
						}
						
//						if(!SLIP_CHK_DATA.next()) {
//							return StringUtil.failWithMsg(TRACE, mapRes, "There is no SDocNo("+strKey+") in the table.");
//						}
						
						// IMG_SEARCH_T 에서 SDocNo 유무 확인
						SeQuery = SDOC_SE_QUERY;
						
						for(String key:keyList) {
							if(SLIP_DATA.getString(key)!=null && !SLIP_DATA.getString(key).equals("")) {
								SeQuery = SeQuery.replaceFirst("\\?", key + " = '" + SLIP_DATA.getString(key) +"'");	
							}
						}
						
						wdmData SDOCNO_SE_DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
						
						if(!SDOCNO_SE_DATA.ExecutQuery(SeQuery)) {
							return StringUtil.failWithMsg(TRACE, mapRes, SDOCNO_SE_DATA.ERR_MSG);
						} else {
							TRACE.TraceBox("Batch.Run_Batch() Select query : " + SeQuery);
						}
						
						
						if(SDOCNO_SE_DATA.next()) {
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
								mapRes.put("MSG", ERR_MSG);
								//return StringUtil.failWithMsg(TRACE, mapRes, "Failed inserting status query.");
							} else {
								TRACE.TraceBox("Batch.Run_Batch() Update query : " + strQuery);
							}
						} else {
							mapRes.put("SDOC_NO", strKey);
							mapRes.put("FILE_FLAG", FILE_FLAG);
							mapRes.put("SE_FLAG", "00");
							mapRes.put("UPDATE", "F");
							
							strSelectQuery = BATCH_START_QUERY;
							strSelectQuery = strSelectQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
							strSelectQuery = strSelectQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
							strSelectQuery = strSelectQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("FILE_FLAG", "")+"'");
							strSelectQuery = strSelectQuery.replaceFirst("\\?", "getDate()"); 
							//strSelectQuery = strSelectQuery.replaceFirst("\\?", "getDate()");
							strSelectQuery = strSelectQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
							
							if(!Set_Result(strSelectQuery)) { 
								mapRes.put("MSG", ERR_MSG);
								//return StringUtil.failWithMsg(TRACE, mapRes, "Batch 1: Failed inserting status query.");
							} else {
								TRACE.TraceBox("Batch.Run_Batch() Insert query : " + strQuery);
							}
						}
					}
				}

				strQuery = SDOC_BATCH_QUERY;

				for(String key:keyList) {
					if(SLIP_DATA.getString(key)!=null && !SLIP_DATA.getString(key).equals("")) {
						strQuery = strQuery.replaceFirst("\\?", key + " = '" + SLIP_DATA.getString(key) +"'");	
					}
				}
				
				wdmData SLIP_UPDATE_DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
				
				if(!SLIP_UPDATE_DATA.ExecutQuery(strQuery)) {
					mapRes.put("MSG", "The batch query fails to execute.");
					//return StringUtil.failWithMsg(TRACE, mapRes, SLIP_UPDATE_DATA.ERR_MSG);
				}
				
//				if(!SLIP_UPDATE_DATA.next()) {
//					mapRes.put("MSG", "The batch query result is 0.");
//				}
				
				while(SLIP_UPDATE_DATA.next()) {
						
					Map<String, String> map = new HashMap<>();
					mapRes.put("SE_FLAG", "01");
					
					//Insert status query before begin.
					strQuery = BATCH_UPDATE_QUERY;
					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
					strQuery = strQuery.replaceFirst("\\?", "getDate()");
					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
					
					if(!Set_Result(strQuery)) { 
						mapRes.put("MSG", ERR_MSG);
						//return StringUtil.failWithMsg(TRACE, mapRes, "Batch 2: Failed inserting status query.");
					} else {
						TRACE.TraceBox("Batch.Run_Batch() Update query : " + strQuery);
					}
					
					String idx			= SLIP_UPDATE_DATA.getString("IDX");
					String file_name 	= SLIP_UPDATE_DATA.getString("IDX")+"." + SLIP_UPDATE_DATA.getString("FILE_TYPE");
					String json_name 	= SLIP_UPDATE_DATA.getString("IDX")+".json";
					String updateStatus = mapRes.get("UPDATE");
					
					if(bRet) {
						
						for(String col:CrawlList) {
							String val = null;
							{
								val = SLIP_UPDATE_DATA.getString(col);	
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
						
						strFilePath = Get_File(idx, file_name, updateStatus);	
						
						if(Chk_Validate_File(file_name) && !strFilePath.equals("")) {
							
							String json_path = Get_Json(json_name, map, updateStatus);	

							if(!json_path.equals("")) {
								bRet = true;	
								
								mapRes.put("SE_FLAG", "02");
								mapRes.put("RESULT", "T");
								mapRes.put("MSG", "");
								
								strQuery = BATCH_UPDATE_QUERY;
								strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
								strQuery = strQuery.replaceFirst("\\?", "getDate()");
								strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
								strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
								
								if(!Set_Result(strQuery)) { 
									mapRes.put("MSG", ERR_MSG);
									//return StringUtil.failWithMsg(TRACE, mapRes, "Batch 3 : Failed inserting status query.");
								}
							} else {
								File file = new File(strFilePath);
								if(file.exists()) {
									if(!FileUtil.deleteFile(strFilePath)) {
										mapRes.put("MSG", "Can`t delete file : "+strFilePath);
									//	return StringUtil.failWithMsg(TRACE, mapRes, "Can`t delete file : "+strFilePath);
			    					}
								}
								else
								{
									mapRes.put("MSG", "Can`t delete file : "+strFilePath);
									//return StringUtil.failWithMsg(TRACE, mapRes,  "Can`t Create json file : IDX = "+idx);
								}
							}
						} else {
							mapRes.put("MSG", "Can`t download file : IDX = "+idx+" name = "+file_name);
							//return StringUtil.failWithMsg(TRACE, mapRes, "Can`t download file : IDX = "+idx+" name = "+file_name);
						}

					}

				}
				
			}
			
			mapRes.put("RESULT", "T");
			return mapRes;
			
		} catch (Exception e) {
			ERR_MSG = e.toString();
		}
		
		return mapRes;
	}

	private String Get_File(String idx, String file_name, String updateStatus) {
		String strRet = "";
		try {
			
			if(!Chk_Validate_File(file_name))
				return "";
			
			if("T".equalsIgnoreCase(updateStatus)) {
				strRet = ROOT_PATH+"\\"+DATA_RETRY_PATH+"\\"+file_name;
				TRACE.TraceBox("Batch.Get_File() datafile import ok, filePath : " + strRet);
			} else if("F".equalsIgnoreCase(updateStatus)) {
				strRet = ROOT_PATH+"\\"+DATA_PATH+"\\"+file_name;
				TRACE.TraceBox("Batch.Get_File() datafile import ok, filePath : " + strRet);
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
				TRACE.TraceBox("Batch.Get_Json() jsonfile import ok, filePath : " + strRet);
			} else if("F".equalsIgnoreCase(updateStatus)) {
				strRet = ROOT_PATH+"\\"+DATA_PATH+"\\"+json_name;
				TRACE.TraceBox("Batch.Get_Json() jsonfile import ok, filePath : " + strRet);
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
	
	public boolean Set_Result(String query) {
		
		try	{
			
			wdmData DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
			
			if(query==null || query.equals("")) {
				return false;
			}
			
			if(DATA.ExecutUpdate(query)!=1) {
				ERR_MSG = DATA.ERR_MSG;
				return false;
			}
			
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
			return false;
	}
	
}
