package index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.ko.KoreanAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.woonam.DocParser;

import common.com.Parser;
import common.com.Trace;
import common.com.IniParser;
import common.com.XmlParser;
import common.util.FileUtil;
import common.util.JsonUtil;
import common.util.StringUtil;
import common.wdclient.wdmCom;
import common.wdclient.wdmData;

@DisallowConcurrentExecution
public class Index implements Job {
	
	private Parser 			CONF				= null;
	private Parser 			INDEX 				= null;
	
	private IndexWriter 	INDEX_WRITER 		= null;
	private Path 			INDEX_DIR 			= null;
    private Trace			TRACE				= null;
    
    private String 			ROOT_PATH			= null;
    private String 			DATA_PATH			= null;
    private String 			DATA_RETRY_PATH		= null;
    private String 			INDEX_PATH			= null;
    
	private String 			AGENT_IP			= null;
	private int 			AGENT_PORT			= 0;
	private String 			AGENT_KEY			= null;
	private String 			AGENT_CIPHER_KEY	= null;
    
	//private String			INDEX_QUERY			= null;
	public String 			INDEX_RESULT_QUERY  = null;
	public String 			INDEX_UPDATE_QUERY  = null;
	
	//private String			FILE_FLAG			= null;
	
	private int 			INDEX_CNT			= 0;
	public int 				RESULT_CNT			= 0;
	
    private final int 		TXT_FILE 			= 0;
    private final int 		DOC_FILE 			= 1;
    private final int 		ADD_FILE 			= 2;
    
    public String 			ERR_MSG				= null;
	
	//###################################//
	//## PUBLIC FUNCTION 
	//###################################//
   
    public void execute(JobExecutionContext context) throws JobExecutionException {
    	//HashMap<String, String> mapRes = null;
		try {
			
			JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
			TRACE = (Trace) jobDataMap.get("trace");
			
			SimpleDateFormat format1 = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
			Date time = new Date();
			
			String time1 = format1.format(time);
			System.out.println("Index : "+time1);
			
			if(!Init_Configure()) {
				return;
			}
			
			if(!Index_Folder()) {
				return;
			}
			
		//	HashMap mapRes = Index_Folder();
			
			if(RESULT_CNT>0) {
				TRACE.TraceLog("Indexing complete...", Trace.LOG_RELEASE);	
			}
			
	    } catch(Exception e) {
	    	TRACE.TraceLog(e.toString(), Trace.LOG_DEBUG);
	    } 
	}
    
    public boolean Init_Configure() {
    	
    	try {
    		CONF 	= new IniParser();
    		INDEX	= new XmlParser();
        	
        	if(!CONF.setParser("ini", "OfficeWSE")) {
        		if(TRACE == null) {
            		String LOG_PATH = CONF.getString("COMMON","LOG_PATH","log");
                	int LOG_LV 		= CONF.getInt("COMMON", "LOG_LV", 3);
                	TRACE = new Trace(common.com.Path.getProjectPath() + LOG_PATH, LOG_LV);
            	}
        		TRACE.TraceLog("Index.Init_Configure() : Wrong OfficeWSE.ini file.", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	if(TRACE == null) {
        		String LOG_PATH = CONF.getString("COMMON","LOG_PATH","log");
            	int LOG_LV 		= CONF.getInt("COMMON", "LOG_LV", 3);
            	TRACE = new Trace(common.com.Path.getProjectPath() + LOG_PATH, LOG_LV);
        	}
        
        	if(!INDEX.setParser("xml", "index")) {
        		if(TRACE != null)
        			TRACE.TraceLog("Index.Init_Configure() : Wrong index.xml file", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	ROOT_PATH 			= CONF.getString("COMMON","ROOT_PATH","");
        	DATA_PATH 			= CONF.getString("COMMON","DATA_PATH","");
        	DATA_RETRY_PATH	 	= CONF.getString("COMMON","DATA_RETRY_PATH","");
        	INDEX_PATH 			= CONF.getString("COMMON","INDEX_PATH","");
        	
        	AGENT_IP			= CONF.getString("AGENT","IP","");
        	AGENT_PORT			= Integer.parseInt(CONF.getString("AGENT","PORT","0"));
        	AGENT_KEY			= CONF.getString("AGENT","KEY","");
        	AGENT_CIPHER_KEY	= CONF.getString("AGENT","CIPHER_KEY","");
        	
        	INDEX_CNT 			= Integer.parseInt(INDEX.getAttValueList("//INDEX/OPTION/INDEX[@type='cnt']", "val").get(0));
        	INDEX_UPDATE_QUERY	= INDEX.getValListByNode("//INDEX/INDEX_UPDATE_QUERY").get(0).trim();
        	INDEX_RESULT_QUERY 	= INDEX.getValListByNode("//INDEX/INDEX_RESULT_QUERY").get(0).trim();
        	
        	if(DATA_PATH.equals("") || DATA_RETRY_PATH.equals("") || INDEX_PATH.equals("")) {
        		TRACE.TraceLog("Index.Init_Configure() : Invalid configure data(COMMON)", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	//TRACE.TraceBox("Index.Init_Configure() OK");
        	
        	return true;
        	
    	} catch(Exception e) {
    		TRACE.TraceLog("Index.Init_Configure() Fail : " + e.toString(), Trace.LOG_DEBUG);
    		return false;
    	}
    }
    
    public boolean Index_Folder() {
    	HashMap<String, String> mapRes = new HashMap<String, String>();
    	try {
    				
    		mapRes.put("RESULT", "F");
    		mapRes.put("MSG", "");
    		mapRes.put("SE_FLAG", "19");
    		mapRes.put("JOB", "INDEX");
    		
    		String strQuery = "";

    		String file_path = "";
    				
    		File file = new File(ROOT_PATH + "\\" + DATA_PATH);
    		
    		if(!file.isDirectory()) {
    			TRACE.TraceLog("Index.Index_Folder() Invalid folder : " + ROOT_PATH + "\\" + DATA_PATH, Trace.LOG_DEBUG);
    			return false;
    		}
    		
    		if(!file.exists()) {
    			TRACE.TraceLog("Index.Index_Folder() folder does not exist : " + ROOT_PATH + "\\" + DATA_PATH, Trace.LOG_DEBUG);
    			return false;
    		}
    		
    		File[] fList = file.listFiles(new Limit_FileFilter(INDEX_CNT));
    		Arrays.sort(fList, new Modified_Date("ASC"));
    		wdmData DATA = new wdmData(AGENT_IP, AGENT_PORT, AGENT_KEY);
    		
    		for(File f:fList) {
    			mapRes.put("MSG", "");
    			if(!f.isFile() || !(f.getName().toUpperCase().endsWith(".JSON"))) 
    				continue;
    				
    			Map<String, String> map = Get_JsonFileToMap(f);
    			
    			if(map != null) {
    					
    				String strKey = map.get("SDOC_NO");
    				if(!StringUtil.isBlank(strKey)) 
    				{
    					mapRes.put("SE_FLAG", "11");
    					mapRes.put("SDOC_NO", strKey);
    					
    					strQuery = INDEX_UPDATE_QUERY;
    					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
    					strQuery = strQuery.replaceFirst("\\?", "getDate()");
    					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
    					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
    					strQuery = strQuery.replaceFirst("\\?", "'02'");
    					
    					TRACE.TraceBox("Index.Index_Folder() Update query : " + strQuery);
    					
    					int nQueryRes = DATA.ExecutUpdate(strQuery);
    					if(nQueryRes <= 0) {
    						
    						mapRes.put("SE_FLAG", "19");
    						mapRes.put("RESULT", "F");
    						mapRes.put("MSG", "Query error : Failed to update SE_FLAG status");

    						strQuery = INDEX_UPDATE_QUERY;
        					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
        					strQuery = strQuery.replaceFirst("\\?", "getDate()");
        					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
        					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
        					strQuery = strQuery.replaceFirst("\\?", "'02'");
    						
        					TRACE.TraceBox("Index.Index_Folder() Update query failed : " + strQuery);
        					
        					DATA.ExecutUpdate(strQuery);
        					continue;
    					//	return StringUtil.failWithMsg(TRACE, mapRes, DATA.ERR_MSG);
    					}
    				}
    				else
    				{
    					mapRes.put("MSG", "Index.Index_Folder() : Failed to receive SDOC_NO.");
    					StringUtil.failWithMsg(TRACE, mapRes, "Index.Index_Folder() : Failed to receive SDOC_NO.");
    				}
    				file_path = map.get("INDEX_DOC_NAME").equals("")?"":ROOT_PATH+"\\"+DATA_PATH+"\\"+map.get("INDEX_DOC_NAME");
        				
        			if(!Index_File(file_path, map)) {
        				mapRes.put("MSG", "Warning : Indexing fail : " + ERR_MSG + ", file_path : " + file_path + ", map : " + map);
        				StringUtil.failWithMsg(TRACE, Trace.LOG_NORMAL, mapRes, "Warning : Indexing fail : " + ERR_MSG + ", file_path : " + file_path + ", map : " + map);
        			}
        			RESULT_CNT++;
    			} else {
    				mapRes.put("MSG","Warning : wrong json data : "+ERR_MSG+" : " + f.getPath());
    				StringUtil.failWithMsg(TRACE, Trace.LOG_NORMAL, mapRes, "Warning : wrong json data : "+ERR_MSG+" : "+f.getPath());
    			}
    			
    			// datafile Delete
				if(!file_path.equals("")) {
					if(!FileUtil.deleteFile(file_path)) {
						mapRes.put("MSG","Warning : data file delete fail, filePath : " + file_path);
						StringUtil.failWithMsg(TRACE, Trace.LOG_NORMAL, mapRes, "Warning : data file delete fail, filePath : " + file_path);
    				} else {
    					TRACE.TraceBox("data file delete ok, filePath : " + file_path);
    				}
				}
					
				// jsonfile Delete
				if(!FileUtil.deleteFile(f)) {
					mapRes.put("MSG", "Warning : json file delete fail, jsonPath : " + f.getPath());
					StringUtil.failWithMsg(TRACE, Trace.LOG_NORMAL, mapRes, "Warning : json file delete fail, jsonPath : " + f.getPath());
				} else {
					TRACE.TraceBox("json file delete ok, jsonPath : "+ f.getPath());
				}
				
				if("11".equalsIgnoreCase(mapRes.get("SE_FLAG"))) {
					mapRes.put("SE_FLAG", "12");
					mapRes.put("RESULT","T");
					mapRes.put("MSG","");
					
					strQuery = INDEX_RESULT_QUERY;
					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
					strQuery = strQuery.replaceFirst("\\?", "getDate()");
					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
					strQuery = strQuery.replaceFirst("\\?", "'11'");
					
					TRACE.TraceBox("Index.Index_Folder() Update query : " + strQuery);
					
					int nQueryRes = DATA.ExecutUpdate(strQuery);
					if(nQueryRes <= 0) {
						
						mapRes.put("SE_FLAG", "19");
						mapRes.put("RESULT","F");
						mapRes.put("MSG","Index.Index_Folder() Update query failed : Failed to update SE_FLAG status");
						
						strQuery = INDEX_UPDATE_QUERY;
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "getDate()");
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "'11'");
						
						TRACE.TraceBox("Index.Index_Folder() Index query failed : " + strQuery);
						
						DATA.ExecutUpdate(strQuery);
						continue;
					//	return StringUtil.failWithMsg(TRACE, mapRes, DATA.ERR_MSG);
					}
				} else if("19".equalsIgnoreCase(mapRes.get("SE_FLAG"))) {
					mapRes.put("RESULT","F");
					mapRes.put("MSG",mapRes.get("MSG"));
					
					strQuery = INDEX_UPDATE_QUERY;
					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
					strQuery = strQuery.replaceFirst("\\?", "getDate()");
					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
					strQuery = strQuery.replaceFirst("\\?", "'11'");
					
					TRACE.TraceBox("Index.Index_Folder() Index query failed : " + strQuery);
					
					DATA.ExecutUpdate(strQuery);
					return false;
	    		}
    		}
    		
    		return true;
    		
    	} catch(Exception e) {
    		TRACE.TraceLog(e.toString(), Trace.LOG_DEBUG);
    		return false;
    	}
    }
    
    public boolean Index_Folder_Update() {
    	HashMap<String, String> mapRes = new HashMap<String, String>();
    	try {
    				
    		mapRes.put("RESULT", "F");
    		mapRes.put("MSG", "");
    		mapRes.put("SE_FLAG", "19");
    		mapRes.put("JOB", "INDEX");
    		
    		String strQuery = "";

    		String file_path = "";
    				
    		File file = new File(ROOT_PATH+"\\"+DATA_RETRY_PATH);
    		
    		if(!file.isDirectory()) {
    			TRACE.TraceLog("Index.Index_Folder_Update() Invalid folder : " + ROOT_PATH + "\\" + DATA_RETRY_PATH, Trace.LOG_DEBUG);
    			return false;
    		}
    		
    		if(!file.exists()) {
    			TRACE.TraceLog("Index.Index_Folder_Update() folder does not exist : " + ROOT_PATH + "\\" + DATA_RETRY_PATH, Trace.LOG_DEBUG);
    			return false;
    		}
    		
    		File[] fList = file.listFiles(new Limit_FileFilter(INDEX_CNT));
    		Arrays.sort(fList, new Modified_Date("ASC"));
    		wdmData DATA = new wdmData(AGENT_IP ,AGENT_PORT, AGENT_KEY);
    		
    		for(File f:fList) {
    			
    			if(!f.isFile() || !(f.getName().toUpperCase().endsWith(".JSON"))) 
    				continue;
    				
    			Map<String, String> map = Get_JsonFileToMap(f);
    				
    			if(map != null) {
    					
    				String strKey = map.get("SDOC_NO");
    				if(!StringUtil.isBlank(strKey)) 
    				{
    					mapRes.put("SE_FLAG", "11");
    					mapRes.put("SDOC_NO", strKey);
    					
    					strQuery = INDEX_UPDATE_QUERY;
    					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
    					strQuery = strQuery.replaceFirst("\\?", "getDate()");
    					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
    					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
    					strQuery = strQuery.replaceFirst("\\?", "'02'");
    					
    					TRACE.TraceBox("Index.Index_Folder_Update() Update query : " + strQuery);
    					
    					int nQueryRes = DATA.ExecutUpdate(strQuery);
    					if(nQueryRes <= 0) {
    						
    						mapRes.put("SE_FLAG", "19");
    						mapRes.put("RESULT","F");
    						mapRes.put("MSG","Query error. : Failed to update SE_FLAG status");
    						
    						strQuery = INDEX_UPDATE_QUERY;
        					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
        					strQuery = strQuery.replaceFirst("\\?", "getDate()");
        					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
        					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
        					strQuery = strQuery.replaceFirst("\\?", "'02'");
    						
        					TRACE.TraceBox("Index.Index_Folder_Update() Update query failed : " + strQuery);
        					
        					DATA.ExecutUpdate(strQuery);
        					continue;
    					//	return StringUtil.failWithMsg(TRACE, mapRes, DATA.ERR_MSG);
    					}
    				}
    				else
    				{
    					mapRes.put("MSG", "Index.Index_Folder_Update() : Failed to receive SDOC_NO.");
    					StringUtil.failWithMsg(TRACE, mapRes, "Index.Index_Folder_Update() : Failed to receive SDOC_NO.");
    				}
    				file_path = map.get("INDEX_DOC_NAME").equals("")?"":ROOT_PATH+"\\"+DATA_RETRY_PATH+"\\"+map.get("INDEX_DOC_NAME");
        				
        			if(!Index_File(file_path, map)) {
        				mapRes.put("MSG", "Warning : Indexing fail : " + ERR_MSG + ", file_path : " + file_path + ", map : " + map);
        				StringUtil.failWithMsg(TRACE, Trace.LOG_NORMAL, mapRes, "Warning : Indexing fail : " + ERR_MSG + ", file_path : " + file_path + ", map : " + map);
        			}
        			RESULT_CNT++;
    			} else {
    				mapRes.put("MSG","Warning : wrong json data : "+ERR_MSG+" : " + f.getPath());
    				StringUtil.failWithMsg(TRACE, Trace.LOG_NORMAL, mapRes, "Warning : wrong json data : "+ERR_MSG+" : "+f.getPath());
    			}
    			
    			// dataFile Delete
				if(!file_path.equals("")) {
					if(!FileUtil.deleteFile(file_path)) {
						mapRes.put("MSG","Warning : data file delete fail, filePath : " + file_path);
						StringUtil.failWithMsg(TRACE, Trace.LOG_NORMAL, mapRes, "Warning : data file delete fail, filePath : " + file_path);
    				} else {
    					TRACE.TraceBox("data file delete ok, filePath : " + file_path);
    				}
				}
				
				//mapRes.put("MSG","Warning : Data file delete fail : "+f.getPath());
				//TRACE.TraceLog("Warning : Data file delete fail : "+f.getPath(), Trace.LOG_NORMAL);
					
				// jsonFile Delete
				if(!FileUtil.deleteFile(f)) {
					mapRes.put("MSG", "Warning : json file delete fail, jsonPath : " + f.getPath());
					StringUtil.failWithMsg(TRACE, Trace.LOG_NORMAL, mapRes, "Warning : json file delete fail, jsonPath : " + f.getPath());
				} else {
					TRACE.TraceBox("json file delete ok, jsonPath : "+ f.getPath());
				}

				if("11".equalsIgnoreCase(mapRes.get("SE_FLAG"))) {
					mapRes.put("SE_FLAG", "12");
					mapRes.put("RESULT","T");
					mapRes.put("MSG","");
					
					strQuery = INDEX_RESULT_QUERY;
					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
					strQuery = strQuery.replaceFirst("\\?", "getDate()");
					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
					strQuery = strQuery.replaceFirst("\\?", "'11'");
					
					TRACE.TraceBox("Index.Index_Folder_Update() Update query : " + strQuery);
					
					int nQueryRes = DATA.ExecutUpdate(strQuery);
					if(nQueryRes <= 0) {
						
						mapRes.put("SE_FLAG", "19");
						mapRes.put("RESULT","F");
						mapRes.put("MSG","Index.Index_Folder() Index query failed : Failed to update SE_FLAG status");
						
						strQuery = INDEX_UPDATE_QUERY;
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "getDate()");
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
						strQuery = strQuery.replaceFirst("\\?", "'11'");
						
						TRACE.TraceBox("Index.Index_Folder_Update() Index query failed : " + strQuery);
						
						DATA.ExecutUpdate(strQuery);
						continue;
					//	return StringUtil.failWithMsg(TRACE, mapRes, DATA.ERR_MSG);
					}
	    		} else if("19".equalsIgnoreCase(mapRes.get("SE_FLAG"))) {
					mapRes.put("RESULT","F");
					mapRes.put("MSG", mapRes.get("MSG"));
					
					strQuery = INDEX_UPDATE_QUERY;
					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SE_FLAG", "")+"'");
					strQuery = strQuery.replaceFirst("\\?", "getDate()");
					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("MSG", "")+"'");
					strQuery = strQuery.replaceFirst("\\?", "'"+mapRes.getOrDefault("SDOC_NO", "")+"'");
					strQuery = strQuery.replaceFirst("\\?", "'11'");
					
					TRACE.TraceBox("Index.Index_Folder_Update() Index query failed : " + strQuery);
					
					DATA.ExecutUpdate(strQuery);
					//continue;
					return false;
	    		}
    		}
    		
    		return true;
    		
    	} catch(Exception e) {
    		ERR_MSG = e.toString();
    		TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
    		return false;
    	}
    }
    
    public boolean Index_File(String file_path, Map<String,String> mParam) {
    	
    	try {
    		
    		int nFileType = chk_File_Validity(file_path);
    		
    		if(nFileType<0) {
    			return false;
            } 
            
    		if(!Indexing(file_path, mParam, nFileType)) {
            	return false;
    		}
    		
    	    return true;
    		
    	} catch (Exception e) {
    		ERR_MSG = e.toString();
    		TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
    		return false;
        } finally {
        	Close_Index_Writer();
        }
    }
    
	//###################################//
	//## PRIVATE FUNCTION 
	//###################################//
    
    private boolean Create_Index_Writer(boolean anal) {
    	boolean bRet = true;
        try	 {
        	INDEX_DIR			= Paths.get(ROOT_PATH+"\\"+INDEX_PATH);
            FSDirectory dir 	= FSDirectory.open(INDEX_DIR);
            IndexWriterConfig config = null;
            if(anal) {
            	KoreanAnalyzer analyzer	= new KoreanAnalyzer();
            	config = new IndexWriterConfig(analyzer);
            } else {
            	StandardAnalyzer analyzer	= new StandardAnalyzer();
            	config = new IndexWriterConfig(analyzer);
            	
            }
            config.setOpenMode(OpenMode.CREATE_OR_APPEND);
            INDEX_WRITER 			= new IndexWriter(dir, config);
        } catch(Exception e) {
            ERR_MSG	= e.toString();
            TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
            bRet 	= false;
        }
        return bRet;
    }
    
    private boolean Close_Index_Writer() {
        boolean bRet = true;
    	try {
            if(INDEX_WRITER!=null)	INDEX_WRITER.close();
        } catch(Exception e) {
        	bRet	= false;
        	ERR_MSG = e.toString();
        	TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
        }
    	return bRet;
    }
    
    @SuppressWarnings("deprecation")
	public boolean Indexing(String filePath, Map<String,String> mParam, int nType) {
     	boolean bRet = true;
    	try {
    		
    		if(!chk_Meta_Data(mParam)) {
    			return false;
    		}
    		
    		DocParser DP = new DocParser();
    		String fileContent = "";
			File file = new File(filePath);
			
    		if(nType == DOC_FILE) {
    			if(file.canRead()) {
    				fileContent = DP.Extractor(filePath, "UTF-8");
    				fileContent = StringReplace(fileContent);
    				fileContent = fileContent.replaceAll("(\r\n|\r|\n|\n\r)", " ");
    				fileContent = fileContent.trim().replaceAll(" +", " ");
    			}
    			
    		} else if(nType == TXT_FILE) {
    			if(file.canRead()) {
    				String encodingChkType = txtEncodingCheck(filePath);
        			
        			if(encodingChkType.equalsIgnoreCase("EUC-KR")) {
        				// 한글 파일 읽기
        				fileContent = fileRead(filePath, "EUC-KR");
            			// UTF-8 파일 쓰기
        				fileWrite(filePath, fileContent, "UTF-8");
        			} 
        			
    				fileContent = DP.Extractor(filePath, "UTF-8");
    				fileContent = StringReplace(fileContent);
    				//fileContent = fileContent.replaceAll("(\r\n|\r|\n|\n\r)", " ");
    				fileContent = fileContent.trim().replaceAll(" +", " ");
    			}
    		}
    			
        	if(fileContent==null) {
        		ERR_MSG = "Index.Indexing() : file Contents is null.";
        		TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	if(StringUtil.checkHangul(fileContent)>0) {
        		bRet = Create_Index_Writer(true);
        	} else {
        		bRet = Create_Index_Writer(false);
        	}
        	
        	if(!bRet) {
        		ERR_MSG = "Index.Indexing() : Create_Index_Writer fail.";
        		TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
        		return false;
        	} else {
        		ERR_MSG = "Index.Indexing() : Create_Index_Writer true.";
        		TRACE.TraceBox(ERR_MSG);
        	}
        	
        	String strKey			= "";
    		String strAddContents	= "";
    		String strSDocNm		= "";
    		Document doc 			= new Document();
    		Term updateTerm 		= null;	
    		List<String> typeList 	= INDEX.getAttValueList("//INDEX/PARAMETER/PARAM","type");
    		List<String> valueList 	= INDEX.getAttValueList("//INDEX/PARAMETER/PARAM","index");
    		
    		for(int i=0;i<valueList.size();i++) {
    			if(typeList.get(i).equalsIgnoreCase("KEY")) {
    				strKey += mParam.get(valueList.get(i));
    			} else if(typeList.get(i).equalsIgnoreCase("NUMERIC")||typeList.get(i).equalsIgnoreCase("SORT")) {
    				doc.add(new NumericDocValuesField(valueList.get(i), Long.valueOf(mParam.get(valueList.get(i)))));
    				doc.add(new StringField(valueList.get(i), Remove_Tag(mParam.get(valueList.get(i))), Field.Store.YES));
    			} else if(typeList.get(i).equalsIgnoreCase("STRING")) {
    				doc.add(new StringField(valueList.get(i), Remove_Tag(mParam.get(valueList.get(i))), Field.Store.YES));
    			} else if(typeList.get(i).equalsIgnoreCase("CONTENT")) {
    				strAddContents += " " + Remove_Tag(mParam.get(valueList.get(i)));
    				//doc.add(new StringField(valueList.get(i), Remove_Tag(mParam.get(valueList.get(i))), Field.Store.YES));
    			} else if(typeList.get(i).equalsIgnoreCase("TITLE")) {
    				if(valueList.get(i).equalsIgnoreCase("SDOC_NAME")) {
    					strSDocNm = mParam.get(valueList.get(i));
    				} else {
        				doc.add(new StringField(valueList.get(i), Remove_Tag(mParam.get(valueList.get(i))), Field.Store.YES));
    				}
    			} else {
    				doc.add(new StringField(valueList.get(i), Remove_Tag(mParam.get(valueList.get(i))), Field.Store.YES));
    			}
    			
//        		else if(typeList.get(i).equalsIgnoreCase("FILEFLAG")) {
//    				doc.add(new StringField(valueList.get(i), Remove_Tag(mParam.get(valueList.get(i))), Field.Store.YES));
//    			} 
    		}

    		updateTerm 			= new Term("SDOC_NO", strKey);
    		
    		FieldType idf = new FieldType(); 
    		idf.setStored(true); 
    		idf.setOmitNorms(true); 
    		idf.setIndexOptions(IndexOptions.DOCS); 
    		idf.setTokenized(false); 
    		idf.freeze(); 
    		
    		// 소문자 인덱싱 필드
    		FieldType fieldType = new FieldType(TextField.TYPE_STORED); 
    		fieldType.setIndexOptions(IndexOptions.DOCS);
    		fieldType.setTokenized(true);
    		fieldType.setStored(true);
    		
    		//fileContent = Remove_line_Char(fileContent);
    		//doc.add(new Field("CONTENTS", Remove_Special_Char(fileContent).trim(), getFieldType(nType)));
    				
    		doc.add(new Field("SDOC_NO", strKey, idf)); 
    		doc.add(new Field("SDOC_NAME", strSDocNm, fieldType)); 
    		doc.add(new Field("CONTENTS", fileContent.trim(), getFieldType(nType)));
    	    
    		if (doc != null) {
    			{	
    				INDEX_WRITER.updateDocument(updateTerm, doc);	
    			}
    			INDEX_WRITER.commit();
            }	
    	} catch(Exception e)	{
    		ERR_MSG = e.toString();
    		TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
    		bRet = false;
    	}
    	return bRet;
    }

    	

	public boolean deleteRealFile(String strKey) throws IOException {
    	
    	boolean strRes = true;
    	
    	if(!Init_Configure()) {
    		TRACE.TraceLog("Index.Init_Configure() Fail.", Trace.LOG_DEBUG);
			return false;
		}
    		
    	if(INDEX_WRITER == null) {
    		Create_Index_Writer(true);
    	}
    	
    	Query deleteQuery = new TermQuery(new Term("SDOC_NO", strKey));
    	//Query stepQuery = new TermQuery(new Term("SDOC_STEP", "9"));
    	TRACE.TraceBox("Start index file deletion with key : '" + deleteQuery + "'");
    	
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		builder.add(deleteQuery, BooleanClause.Occur.MUST);
	//	builder.add(stepQuery, BooleanClause.Occur.MUST_NOT);
	
		INDEX_WRITER.deleteDocuments(builder.build());

		INDEX_WRITER.commit();
		INDEX_WRITER.close();
		
    	return strRes;
    }
    
    private boolean chk_Meta_Data(Map<String,String> mParam) {
    	
    	Iterator<String> keys = INDEX.getAttValueList("//INDEX/PARAMETER/PARAM","index").iterator();

    	if(!keys.hasNext()) {
    		ERR_MSG = "Index.chk_Meta_Data() : wrong infomation -> index.xml";
    		TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);;
    		return false;
    	}

    	while(keys.hasNext()) {
    		if(!mParam.containsKey(keys.next())) {
    			ERR_MSG = "Index.chk_Meta_Data() : check parameters -> index.xml";
    			TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
    			return false; 
    		}
    	}
    	return true;
    }
    
    private int chk_File_Validity(String file_path) {
        int nRet = 0;
    	try	{
    		if(file_path.toUpperCase().endsWith(".TXT") || file_path.toUpperCase().endsWith(".HTM") || file_path.toUpperCase().endsWith(".INI") || 
    				file_path.toUpperCase().endsWith(".HTML") || file_path.toUpperCase().endsWith(".XML")) {					
				nRet = TXT_FILE;
			} else if(file_path.toUpperCase().endsWith(".DOC") || file_path.toUpperCase().endsWith(".PPT") || file_path.toUpperCase().endsWith(".XLS") ||
					file_path.toUpperCase().endsWith(".DOCX") || file_path.toUpperCase().endsWith(".PPTX") || file_path.toUpperCase().endsWith(".XLSX") || 
					file_path.toUpperCase().endsWith(".PDF") || file_path.toUpperCase().endsWith(".HWP")) {
				nRet = DOC_FILE;
			} else	
				nRet = ADD_FILE;
    		
    		if(nRet == ADD_FILE) {
    			return nRet;
    		}
    		
    		File file = new File(file_path);
			if(!file.exists()) {
				ERR_MSG = "Index.chk_File_Validity() file not exists : " + file_path;
				TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
				return -1;
			}
			if(file.isHidden() || !file.canRead() || file.length() <= 0.0 || !file.isFile()) {
				ERR_MSG = "Index.chk_File_Validity() file is wrong : " + file_path;
				TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
				return -1;
			}
    		
			return nRet;
			
    	} catch (Exception e) {
    		ERR_MSG = e.toString();
    		TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
    		return -1;
    	}
    }
   
    private static String Remove_Tag(String s) {
        return s.replaceAll("<[^>]*>", "");
    }

    private String Remove_Special_Char (String phrase) {
    	if(phrase == null || phrase.equals(""))
    		return "";
      	String 	strPattern 	= "([^\"'\\{\\}\\[\\]/,;:|\\)\\(*~`^\\-_+<>@#$%^\\\\=]){0,}";
    	Pattern 	p 			= Pattern.compile(strPattern);
        Matcher m 				= p.matcher(phrase);
        StringBuffer sb 		= new StringBuffer();
        while (m.find()) {
        	sb.append(m.group().toString());
        }
        return sb.toString();
    }
    
    private String Remove_line_Char(String phrase) {
    	String ret = "";
    	ret = phrase.replace( System.getProperty( "line.separator" ), " " );
    	return duplicatePattern(ret, " ");
    }
    
    private String duplicatePattern(String phrase, String syntax) {
    	String duplicatePattern 	= "(["+syntax+"]){1,}";
        Pattern p					= Pattern.compile(duplicatePattern);
        Matcher m 					= p.matcher(phrase);
        while (m.find()) {
        	phrase = phrase.replaceFirst(m.group().toString(), syntax);
        }
        
        return phrase;
    }
    
    private FieldType getFieldType(int nDoc) {
    	FieldType fieldType = new FieldType(TextField.TYPE_STORED); 
    	switch(nDoc)	{
			case ADD_FILE:	{
				fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
				fieldType.setTokenized(true);
				fieldType.setStored(true);	
				break;
			}
			case DOC_FILE:	{
				fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
				fieldType.setTokenized(true);
				fieldType.setStored(true);	
				break;
			}
			case TXT_FILE:	{
				fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
				fieldType.setTokenized(true);
				fieldType.setStored(true);	
				break;
			}
    	}
        return fieldType;
    }
    
    private Map<String, String> Get_JsonFileToMap(File file) {
    	Map<String, String> map = null;
    	try {
    		FileReader fileReader = new FileReader(file);
    		JSONParser parser = new JSONParser();
			map = JsonUtil.getMapFromJsonObject((JSONObject) (parser.parse(fileReader)));
			fileReader.close();
		} catch(Exception e) {
			ERR_MSG = e.toString();
			TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
			return null;
    	} 
    	return map;
    }
        
    private class Modified_Date implements Comparator<File> {
    	private int nSrot = 0;
    	public Modified_Date(String strSort) {
    		if(strSort.equalsIgnoreCase("ASC"))	nSrot=1;
    		else								nSrot=-1;
    	}
    	public int compare(File f1, File f2) {
    		if(f1.lastModified()>f2.lastModified())
    			return nSrot;
    		else if(f1.lastModified()==f2.lastModified())
    			return 0;
    		return nSrot*-1;
    	}
    }
    
    private class Limit_FileFilter implements FilenameFilter {
    	private int counter = 0;
    	private int limit = 300;
    	public Limit_FileFilter(int limit) {
    		this.limit = limit;
   		 }
   		public boolean accept(File dir, String name) {
    		if (counter <= limit) {
    			counter++;
    		 	return true;
    		}
    		return false;
    	}
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
    
    /* step 9 Test */
    public boolean deleteTestFile(String strKey) throws IOException {
    	
    	boolean strRes = true;
    	
    	if(!Init_Configure()) {
			return false;
		}
    	
    	if(INDEX_WRITER == null) {
     		Create_Index_Writer(true);
     	}

    	Query deleteQuery = new TermQuery(new Term("SDOC_NO", strKey));
    	BooleanQuery.Builder builder = new BooleanQuery.Builder();
    	builder.add(deleteQuery, BooleanClause.Occur.MUST);
    	
    	IndexReader reader 		= DirectoryReader.open(FSDirectory.open(Paths.get(ROOT_PATH+"\\"+INDEX_PATH))); 
    	IndexSearcher searcher	= new IndexSearcher(reader); 

    	TopDocs docs = searcher.search(builder.build(), 1);
        if (docs.scoreDocs.length > 0) {
            	Document doc = searcher.doc(docs.scoreDocs[0].doc);
            	
            	//Document doc2 = new Document();
            	//Field field = new TextField("ADD_STEP", "9", Field.Store.YES);
            	FieldType idf = new FieldType(); 
        		idf.setStored(true); 
        		idf.setOmitNorms(true); 
        		idf.setIndexOptions(IndexOptions.DOCS); 
        		idf.setTokenized(true); 
        		idf.freeze(); 
    		
        		doc.removeField("ADD_STEP");
            	doc.add(new Field("ADD_STEP", "9", idf)); 
            	//doc.add(field);

            	INDEX_WRITER.updateDocument(new Term("SDOC_NO", strKey), doc);
            	
            	INDEX_WRITER.commit();
        		INDEX_WRITER.close();
        }
       

//		Document doc = INDEX_WRITER.getdo
//		doc.add(new Field("SDOC_STEP", "1", Field.Store.YES, Field.Index.NOT_ANALYZED)); updateDoc.add(new Field("title", "document 1 update", Field.Store.YES, Field.Index.NOT_ANALYZED)); 

    	return strRes;
    }
    
    /**
     * Remove special characters from strings
     * 
     * @param word
     * @return
     */
    public static String StringReplace(String str) { 
  		String match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]";
  		str = str.replaceAll(match, " "); 
  		return str; 
  	} 
    
    /**
     * Read file
     * 
     * @param filePath
     * @param encode
     * @return defaultContents
     */
    public String fileRead(String filePath, String encode) {
    	BufferedReader reader = null;
    	String defaultContents = "";
    	//System.out.println("----- 파일 읽기 인코딩 : ["+encode+"]");
    	
    	try {
    		File configFile = new File(filePath);
    		if(configFile.exists() && configFile.isFile() && configFile.length()>0) {
    			reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), encode));
    			char[] contents = new char[(int)configFile.length()];
    			reader.read(contents);
    			defaultContents = new String(contents);
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	} finally {
    		try {
    			reader.close();
    		} catch (Exception e) {
    			e.printStackTrace();
			}
    		
//    		if(reader != null) {
//    			try {
//    				reader.close();
//    			} catch(Exception e) {
//    				e.printStackTrace();
//    			}
//    		}
    	}

		return defaultContents;
	}
    
    /**
     * Write file
     * 
     * @param filePath
     * @param fileContent
     * @param encode
     */
    public void fileWrite(String filePath, String fileContent, String encode) {
    	try {
    		OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(filePath), encode);
    		out.write(fileContent);
    		out.close();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }
    
    /**
     * Check text file encoding
     * 
     * @param filePath
     * @return EncodingType
     * 
     */
    public String txtEncodingCheck(String filePath) {
		String EncodingType = "";
    	/*
    	 * 유니코드 파일은 파일을 저장할 때 파일의 케릭터셋을 알려주기위해 다음 표와 같은 유니코드 파일 헤더(BOM)를 파일의 처음에 기록하여 알려준다.
    	 * BOM(Byte-order mark)
    	 * 
    	 */
    	try {
    		// 1. 파일 열기
    		FileInputStream fis = new FileInputStream(filePath);

    		// 2. 파일 읽기 (4Byte)
    		byte[] BOM = new byte[4];
    		fis.read(BOM, 0, 4);

    		// 3. 파일 인코딩 확인하기
    		if( (BOM[0] & 0xFF) == 0xEF && (BOM[1] & 0xFF) == 0xBB && (BOM[2] & 0xFF) == 0xBF ) {
    			//System.out.println("UTF-8");
    			EncodingType = "UTF-8";
    		} else if( (BOM[0] & 0xFF) == 0xFE && (BOM[1] & 0xFF) == 0xFF ) {
    			//System.out.println("UTF-16BE");
    		} else if( (BOM[0] & 0xFF) == 0xFF && (BOM[1] & 0xFF) == 0xFE ) {
    			//System.out.println("UTF-16LE");
    		} else if( (BOM[0] & 0xFF) == 0x00 && (BOM[1] & 0xFF) == 0x00 && (BOM[0] & 0xFF) == 0xFE && (BOM[1] & 0xFF) == 0xFF ) {
    			//System.out.println("UTF-32BE");
    		} else if( (BOM[0] & 0xFF) == 0xFF && (BOM[1] & 0xFF) == 0xFE && (BOM[0] & 0xFF) == 0x00 && (BOM[1] & 0xFF) == 0x00 ) {
    			//System.out.println("UTF-32LE");
    		} else {
    			//System.out.println("EUC-KR");
    			EncodingType = "EUC-KR";
    		}
    		
    		fis.close();
    	} catch (Exception e) {
    		ERR_MSG = e.toString();
    		TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
		} 
    	
		return EncodingType;
    }

}
