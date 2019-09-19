package main;

import java.io.File;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CronTriggerImpl;

import common.com.IniParser;
import common.com.Parser;
import common.com.Path;
import common.com.Trace;
import common.com.XmlParser;

public class OfficeSE {
	
	private SchedulerFactory 	schedulFactoty	= null;
	private Scheduler 			scheduler 		= null;
	private Trace				TRACE			= null;
	
	private boolean 	LISTENER_USE 			= false;
	private String 		LISTENER_TIME 			= null;
	private String 		LISTENER_ENCODING 		= null;
	private int 		LISTENER_PORT 			= 0;
	
	private boolean 	INDEX_USE 				= false;
	private String 		INDEX_TIME 				= null;
	
	private boolean 	CRAWLER_USE 			= false;
	private String		CRAWLER_TIME 			= null;
	private String		CRAWLER_CLASS			= null;
	

	private boolean 	BATCH_USE 			= false;
	private String		BATCH_TIME 			= null;
	
	private String 		ERR_MSG 		= "";
	
	public static void main(String[] args) {
		
		OfficeSE OS = new OfficeSE();
		OS.Start_OfficeSE();
	}
	
	public void Start_OfficeSE() {
		
		if(!Init_Configure()) {
			System.out.println(ERR_MSG);
			return;
		}
		
			TRACE.TraceBox("===================================================================");
			TRACE.TraceBox("=                      WOONAMSOFT EDM SYSTEM                      =");
			TRACE.TraceBox("=                      OfficeWSE Ver 0.1.1.1                      =");
			TRACE.TraceBox("=             Copyright(c) 2019 Woonamsoft Corporation            =");
			TRACE.TraceBox("===================================================================");
			TRACE.TraceBox("=      Start OfficeWSE ...                                        =");
			TRACE.TraceBox("===================================================================");
			TRACE.TraceBox("=      InitConfigure() ................................... OK     =");
		
		if(!Init_Scheduler()) {
			TRACE.TraceLog(ERR_MSG, Trace.LOG_RELEASE);
			return;
		}
			TRACE.TraceBox("=      InitSchedule() .................................... OK     =");
		if(LISTENER_USE && !Start_Schedule_Listener()) {
			TRACE.TraceLog(ERR_MSG, Trace.LOG_RELEASE);
			return;
		} else {
			TRACE.TraceBox("=      Listener deamon start ............................. OK     =");
		}
		if(INDEX_USE && !Start_Schedule_Index()) {
			TRACE.TraceLog(ERR_MSG, Trace.LOG_RELEASE);
			return;
		} else {
			TRACE.TraceBox("=      Index deamon start ................................ OK     =");	
		}
		if(CRAWLER_USE && !Start_Schedule_Crawler()) {
			TRACE.TraceLog(ERR_MSG, Trace.LOG_RELEASE);
			return;
		} else {
			TRACE.TraceBox("=      Crawler deamon start .............................. OK     =");	
		}
		if(BATCH_USE && !Start_Schedule_Batch()) {
			TRACE.TraceLog(ERR_MSG, Trace.LOG_RELEASE);
			return;
		} else {
			TRACE.TraceBox("=      Batch deamon start .............................. OK       =");	
		}
			TRACE.TraceBox("===================================================================");
	}
	
	private boolean Start_Schedule_Listener() {
		
		try {
			@SuppressWarnings("rawtypes")
			Class c 		= Class.forName("listener.ListenerServer");
			@SuppressWarnings({ "deprecation", "unchecked" })
			JobDetail job 	= new JobDetailImpl("Listener", "Listener", c);
			@SuppressWarnings("deprecation")
			Trigger	trigger	= new CronTriggerImpl("Listener", "Listener", LISTENER_TIME);
			JobDataMap jobDataMap = job.getJobDataMap();
			jobDataMap.put("trace", TRACE);
			jobDataMap.put("port", LISTENER_PORT);
			jobDataMap.put("encoding", LISTENER_ENCODING);
			
			if(!scheduler.checkExists(job.getKey())) {
				scheduler.scheduleJob(job,trigger);
			} else {
				scheduler.rescheduleJob(trigger.getKey(),trigger);
			}	
			return true;
		} catch(Exception e) {
			ERR_MSG = "Start_Schedule_Listener() fail : " + e.toString();
			return false;
		}
	}

	private boolean Start_Schedule_Index() {
		
		try {
			@SuppressWarnings("rawtypes")
			Class c			= Class.forName("index.Index");
			@SuppressWarnings({ "deprecation", "unchecked" })
			JobDetail job 	= new JobDetailImpl("Index", "Index", c);
			@SuppressWarnings("deprecation")
			Trigger	trigger	= new CronTriggerImpl("Index", "Index", INDEX_TIME);
			JobDataMap jobDataMap = job.getJobDataMap();
			jobDataMap.put("trace", TRACE);
			
			if(!scheduler.checkExists(job.getKey())) {
				scheduler.scheduleJob(job,trigger);
			} else {
				scheduler.rescheduleJob(trigger.getKey(),trigger);
			}	
			return true;
		} catch(Exception e) {
			ERR_MSG = "Start_Schedule_Index() fail : " + e.toString();
			return false;
		}
	}
	
	private boolean Start_Schedule_Crawler() {
		
		try {
			@SuppressWarnings("rawtypes")
			Class c 		= Class.forName("crawler." + CRAWLER_CLASS);
			@SuppressWarnings({ "deprecation", "unchecked" })
			JobDetail job 	= new JobDetailImpl("Crawler", "Crawler", c);
			@SuppressWarnings("deprecation")
			Trigger	trigger	= new CronTriggerImpl("Crawler", "Crawler", CRAWLER_TIME);
			JobDataMap jobDataMap = job.getJobDataMap();
			jobDataMap.put("trace", TRACE);
			
			if(!scheduler.checkExists(job.getKey())) {
				scheduler.scheduleJob(job,trigger);
			} else {
				scheduler.rescheduleJob(trigger.getKey(),trigger);
			}	
			return true;
		} catch(Exception e) {
			ERR_MSG = "Start_Schedule_Crawler() fail : " + e.toString();
			return false;
		}
	}
	
	private boolean Start_Schedule_Batch() {
		
		try {
			@SuppressWarnings("rawtypes")
			Class c 		= Class.forName("batch.Batch");
			@SuppressWarnings({ "deprecation", "unchecked" })
			JobDetail job 	= new JobDetailImpl("Batch", "Batch", c);
			@SuppressWarnings("deprecation")
			Trigger	trigger	= new CronTriggerImpl("Batch", "Batch", BATCH_TIME);
			JobDataMap jobDataMap = job.getJobDataMap();
			jobDataMap.put("trace", TRACE);
			
			if(!scheduler.checkExists(job.getKey())) {
				scheduler.scheduleJob(job,trigger);
			} else {
				scheduler.rescheduleJob(trigger.getKey(),trigger);
			}	
			return true;
		} catch(Exception e) {
			ERR_MSG = "Start_Schedule_Batch() fail : " + e.toString();
			return false;
		}
	}
	
	private boolean Init_Configure() {
		
		try {
			
			Parser CONF 	= new IniParser();
			Parser INDEX 	= new XmlParser();
			Parser SEARCH 	= new XmlParser();
			Parser CRAWLER 	= new XmlParser();
			Parser BATCH 	= new XmlParser();
			
			if(!CONF.setParser("ini", "OfficeSE")) {
        		ERR_MSG = "Init_Configure() Fail : Wrong ini file(OfficeSE.ini)";
        		return false;
        	}
			
        	if(!INDEX.setParser("xml", "index")) {
        		ERR_MSG = "Init_Configure() Fail : Wrong xml file(index.xml)";
        		return false;
        	}
        	
        	if(!SEARCH.setParser("xml", "search")) {
        		ERR_MSG = "Init_Configure() Fail : Wrong xml file(search.xml)";
        		return false;
        	}
			
        	if(!CRAWLER.setParser("xml", "crawler")) {
        		ERR_MSG = "Init_Configure() Fail : Wrong xml file(crawler.xml)";
        		return false;
        	}
        	
        	if(!BATCH.setParser("xml", "batch")) {
        		ERR_MSG = "Init_Configure() Fail : Wrong xml file(batch.xml)";
        		return false;
        	}
			
        	////////////////////////////////////////////////////////////////////
        	// common
			String root_path		= CONF.getString("COMMON","ROOT_PATH","");
			String index_path		= CONF.getString("COMMON","INDEX_PATH","");
			String data_path		= CONF.getString("COMMON","DATA_PATH","");
			String data_retry_path	= CONF.getString("COMMON","DATA_RETRY_PATH","");
			String log_path			= CONF.getString("COMMON","LOG_PATH","log");
			int log_lv				= CONF.getInt("COMMON","LOG_LV",3);

			////////////////////////////////////////////////////////////////////
			// agent
			
			String agent_ip			= CONF.getString("AGENT","IP","");
			String agent_port		= CONF.getString("AGENT","PORT","");
			String agent_key		= CONF.getString("AGENT","KEY","");
			String agent_cipher_key	= CONF.getString("AGENT","CIPHER_KEY","");

			////////////////////////////////////////////////////////////////////
			// index
			
			INDEX_USE 				= CONF.getString("INDEX","USE").equalsIgnoreCase("YES")?true:false;
			INDEX_TIME 				= CONF.getString("INDEX","TIME","*/5 * * * * ?");
			
			////////////////////////////////////////////////////////////////////
			// search
			
			LISTENER_USE 			= CONF.getString("LISTENER","USE").equalsIgnoreCase("YES")?true:false;
			LISTENER_TIME 			= CONF.getString("LISTENER","TIME","*/10 * * * * ?");
			LISTENER_PORT			= CONF.getInt("LISTENER","PORT",0);
			LISTENER_ENCODING 		= CONF.getString("LISTENER","ENCODING","UTF-8");
			
			////////////////////////////////////////////////////////////////////
			// crawler
			
			CRAWLER_USE 			= CONF.getString("CRAWLER","USE").equalsIgnoreCase("YES")?true:false;
			CRAWLER_TIME 			= CONF.getString("CRAWLER","TIME","*/10 * * * * ?");
			CRAWLER_CLASS			= CONF.getString("CRAWLER","CLASS","");
			
			////////////////////////////////////////////////////////////////////
			// batch
			
			BATCH_USE 				= CONF.getString("BATCH","USE").equalsIgnoreCase("YES")?true:false;
			BATCH_TIME 				= CONF.getString("BATCH","TIME","40 * * * * ?");

			
			
			if(root_path.equals("") || index_path.equals("") || data_path.equals("")) {
        		ERR_MSG = "Init_Configure() Fail : Invalid configure data(COMMON)";
        		return false;
        	}
			
			if(agent_ip.equals("") || agent_port.equals("") || agent_key.equals("") || agent_cipher_key.equals("")) {
        		ERR_MSG = "Init_Configure() Fail : Invalid configure data(AGENT)";
        		return false;
        	}
			
			if(LISTENER_PORT == 0) {
        		ERR_MSG = "Init_Configure() Fail : Invalid configure data(LISTENER)";
        		return false;
        	}
			
			TRACE = new Trace(Path.getProjectPath()+log_path, log_lv);
			
			String index_err_msg = "Init_Configure() Fail : Invalid xml node(index.xml)";
			if(INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@type='key']", "index").size()<=0)		{ ERR_MSG = index_err_msg;	return false; }
			/*
			 * if(INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@type='sort']",
			 * "index").size()<=0) { ERR_MSG = index_err_msg; return false; }
			 */
			if(INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@type='title']", "index").size()<=0)		{ ERR_MSG = index_err_msg;	return false; }
			if(INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@type='fileflag']", "index").size()<=0)	{ ERR_MSG = index_err_msg;	return false; }
			if(INDEX.getAttValueList("//INDEX/OPTION/INDEX[@type='cnt']", "val").size()<=0)				{ ERR_MSG = index_err_msg;	return false; }
					
			String search_err_msg = "Init_Configure() Fail : Invalid xml node(search.xml)";
			if(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='row']", "col").size()<=0)		{ ERR_MSG = search_err_msg;	return false; }
			if(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='page']", "col").size()<=0)		{ ERR_MSG = search_err_msg;	return false; }
			if(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='query']", "col").size()<=0)		{ ERR_MSG = search_err_msg;	return false; }
			if(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='sort']", "col").size()<=0)		{ ERR_MSG = search_err_msg;	return false; }
			if(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='analyzer']", "col").size()<=0)	{ ERR_MSG = search_err_msg;	return false; }
			if(SEARCH.getAttValueList("//SEARCH/OPTION/SEARCH[@type='cnt']", "val").size()<=0)			{ ERR_MSG = search_err_msg;	return false; }
			if(SEARCH.getAttValueList("//SEARCH/OPTION/HIGHLIGHT[@type='num']", "val").size()<=0)		{ ERR_MSG = search_err_msg;	return false; }
			if(SEARCH.getAttValueList("//SEARCH/OPTION/HIGHLIGHT[@type='len']", "val").size()<=0)		{ ERR_MSG = search_err_msg;	return false; }
			
			index_err_msg = "Init_Configure() Fail : Invalid xml data(index.xml)";
			if(INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@type='key']", "index").get(0).trim().equals(""))			{ ERR_MSG = index_err_msg;	return false; }
			/*
			 * if(INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@type='sort']",
			 * "index").get(0).trim().equals("")) { ERR_MSG = index_err_msg; return false; }
			 */
			if(INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@type='title']", "index").get(0).trim().equals(""))		{ ERR_MSG = index_err_msg;	return false; }
			if(INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@type='fileflag']", "index").get(0).trim().equals(""))	{ ERR_MSG = index_err_msg;	return false; }
			if(INDEX.getAttValueList("//INDEX/OPTION/INDEX[@type='cnt']", "val").get(0).trim().equals(""))				{ ERR_MSG = index_err_msg;	return false; }
					
			search_err_msg = "Init_Configure() Fail : Invalid xml data(search.xml)";
			if(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='row']", "col").get(0).trim().equals(""))			{ ERR_MSG = search_err_msg;	return false; }
			if(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='page']", "col").get(0).trim().equals(""))		{ ERR_MSG = search_err_msg;	return false; }
			if(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='query']", "col").get(0).trim().equals(""))		{ ERR_MSG = search_err_msg;	return false; }
			if(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='sort']", "col").get(0).trim().equals(""))		{ ERR_MSG = search_err_msg;	return false; }
			if(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='analyzer']", "col").get(0).trim().equals(""))	{ ERR_MSG = search_err_msg;	return false; }
			if(SEARCH.getAttValueList("//SEARCH/OPTION/SEARCH[@type='cnt']", "val").get(0).trim().equals(""))			{ ERR_MSG = search_err_msg;	return false; }
			if(SEARCH.getAttValueList("//SEARCH/OPTION/HIGHLIGHT[@type='num']", "val").get(0).trim().equals(""))		{ ERR_MSG = search_err_msg;	return false; }
			if(SEARCH.getAttValueList("//SEARCH/OPTION/HIGHLIGHT[@type='len']", "val").get(0).trim().equals(""))		{ ERR_MSG = search_err_msg;	return false; }
			
			if(!Make_Dir(root_path+"/"+data_path)) {
        		ERR_MSG = "Init_Configure() Fail : Can`t Create Folder ("+root_path+"/"+data_path+")";
        		return false;
        	}
			
			if(!Make_Dir(root_path+"/"+data_retry_path)) {
        		ERR_MSG = "Init_Configure() Fail : Can`t Create Folder ("+root_path+"/"+data_retry_path+")";
        		return false;
        	}
			
			if(!Make_Dir(root_path+"/"+index_path)) {
        		ERR_MSG = "Init_Configure() Fail : Can`t Create Folder ("+root_path+"/"+index_path+")";
        		return false;
        	}
        	
		} catch (Exception e) {
			ERR_MSG = "Init_Configure() Fail : " + e.toString();
        	return false;
        }
		return true;
	}
	
	private boolean Init_Scheduler() {
		try {
            schedulFactoty 	= new StdSchedulerFactory();
            scheduler 		= schedulFactoty.getScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
        	ERR_MSG = "Init_Scheduler() Fail : "+e.toString();
        	return false;
        }
		return true;
	}
	
	private boolean Make_Dir(String strDirectory) {
		boolean bOk = false;
		try {
			File dirDest = new File(strDirectory);
			if(!dirDest.exists()) {
				bOk = dirDest.mkdirs();
			} else	bOk = true;
		} catch(Exception e) {
			bOk = false;
		}
		return bOk;
	}
}
