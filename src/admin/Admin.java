package admin;

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

@DisallowConcurrentExecution
public class Admin implements Job {

	private Parser 		CONF				= null;
	private Parser 		INDEX				= null;
	private Parser 		BATCH				= null;
	
	private boolean 	BATCH_USE			= false;
	
	private String 		ROOT_PATH			= null;
	private String 		DATA_PATH			= null;
	private String 		INDEX_PATH			= null;
	
	private String 		AGENT_IP			= null;
	private int 		AGENT_PORT			= 0;
	private String 		AGENT_KEY			= null;
	private String 		AGENT_CIPHER_KEY	= null;
	
	private String		ADD_TABLE	 		= null;
	private String		SLIP_TABLE 			= null;

	private String 		ADD_BATCH_QUERY		= null;
	private String 		SLIP_BATCH_QUERY	= null;
	private String 		ADD_SELECT_QUERY	= null;
	private String 		SLIP_SELECT_QUERY	= null;
	private String 		BATCH_START_QUERY	= null;
	private String 		BATCH_UPDATE_QUERY	= null;

	private String		FILE_FLAG			= null;
	
	private int 		RESULT_CNT			= 0;
	
	private Trace		TRACE				= null;
	public String 		ERR_MSG				= null;
	
	
	//###################################//
	//## PUBLIC FUNCTION 
	//###################################//
	    
	public void execute(JobExecutionContext context) throws JobExecutionException {
	    	
		try {
	    	
			JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
			TRACE = (Trace) jobDataMap.get("trace");
			
			//System.out.println("Running Batch");

//			SimpleDateFormat format1 = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
//			
//			Date time = new Date();	
//			
//			
//			String time1 = format1.format(time);
//			System.out.println("Running Batch : "+time1);
			
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
        	INDEX_PATH 			= CONF.getString("COMMON","INDEX_PATH","");
        	
        	ADD_TABLE			= BATCH.getValListByNode("//BATCH/BATCH_TABLE/ADD_TABLE").get(0).trim();
        	SLIP_TABLE			= BATCH.getValListByNode("//BATCH/BATCH_TABLE/SLIP_TABLE").get(0).trim();
        	
        	//BATCH_QUERY			= BATCH.getValListByNode("//BATCH/BATCH_QUERY").get(0).trim();
        	ADD_BATCH_QUERY		= BATCH.getValListByNode("//BATCH/BATCH_QUERY/ADD_BATCH_QUERY").get(0).trim();
        	SLIP_BATCH_QUERY	= BATCH.getValListByNode("//BATCH/BATCH_QUERY/SLIP_BATCH_QUERY").get(0).trim();
        	ADD_SELECT_QUERY	= BATCH.getValListByNode("//BATCH/BATCH_SELECT_QUERY/ADD_SELECT_QUERY").get(0).trim();
        	SLIP_SELECT_QUERY	= BATCH.getValListByNode("//BATCH/BATCH_SELECT_QUERY/SLIP_SELECT_QUERY").get(0).trim();
        	BATCH_START_QUERY	= BATCH.getValListByNode("//BATCH/BATCH_START_QUERY").get(0).trim();
        	BATCH_UPDATE_QUERY	= BATCH.getValListByNode("//BATCH/BATCH_UPDATE_QUERY").get(0).trim();
        	
        	AGENT_IP			= CONF.getString("AGENT","IP","");
        	AGENT_PORT			= Integer.parseInt(CONF.getString("AGENT","PORT","0"));
        	AGENT_KEY			= CONF.getString("AGENT","KEY","");
        	AGENT_CIPHER_KEY	= CONF.getString("AGENT","CIPHER_KEY","");
        	
        	if(DATA_PATH.equals("") || INDEX_PATH.equals("")) {
        		TRACE.TraceLog("Invalid OfficeWSE.ini data(COMMON).", Trace.LOG_DEBUG);
        		return false;
        	}
        	
         	if(BATCH_USE && (ADD_TABLE.equals("") || SLIP_TABLE.equals("") || ADD_BATCH_QUERY.equals("") || SLIP_BATCH_QUERY.equals("") || ADD_SELECT_QUERY.equals("") || SLIP_SELECT_QUERY.equals(""))) {
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
	
	public HashMap<String, String> Run_Admin(Map<String, String> mParam) {

		HashMap<String, String> mapRes = new HashMap<String, String>();
		mapRes.put("RESULT", "F");
		mapRes.put("MSG", "");
		mapRes.put("JOB", "ADMIN");
		
		try {
			String AddField = (String)mParam.get("FIELD");
			mapRes.put("FIELD", AddField);
			return mapRes;
		} catch (Exception e) {
			ERR_MSG = e.toString();
		}
		
		return mapRes;
	}
	

	private String Get_File(String idx, String file_name) {
		String strRet = "";
		try {
			
			if(!Chk_Validate_File(file_name))
				return "";
			
			strRet = ROOT_PATH+"/"+DATA_PATH+"/"+file_name;
			wdmFile wf = new wdmFile(AGENT_IP ,AGENT_PORT, AGENT_KEY);
			wf.addMetaData("IDX", idx);
			
			byte[] b = null;
			
			if(FILE_FLAG.equalsIgnoreCase("ADDFILE")) {
				b =  wf.Download_Byte(ADD_TABLE);
			} else if(FILE_FLAG.equalsIgnoreCase("SLIP")) {
				b =  wf.Download_Byte(SLIP_TABLE);
			}

			if(b==null)
				return "";
			ARIAChiper m_aria = new ARIAChiper(AGENT_CIPHER_KEY);
			if(m_aria.DecryptFile(b, ROOT_PATH+"/"+DATA_PATH+"/"+file_name))
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
			
		} catch(Exception e) {
			return "";
		}
		return strRet;
	}
	
	@SuppressWarnings("unchecked")
	private String Get_Json(String json_name, Map<String, String> map) {
		String strRet = "";
		JSONObject SubObj = new JSONObject();
		
		try	{
			strRet = ROOT_PATH+"/"+DATA_PATH+"/"+json_name;
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
			file_name.toUpperCase().endsWith(".HWP"))
			return true;
		else
			return false;
	}
	
}
