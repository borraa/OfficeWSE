
package search;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ko.KoreanAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;

import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import common.com.IniParser;
import common.com.Parser;
import common.com.Trace;
import common.com.XmlParser;
import common.util.StringUtil;
import common.wdclient.wdmData;

public class Search { 
	
//###################################//
//## MEMBER VARIABLE 
//###################################//
	
	private Parser 		CONF		= null;
	private Parser 		SEARCH		= null;
	private Parser 		INDEX		= null;
	
	private Trace		TRACE		= null;
	private search.Query QUERY		= null;
	
	private final int 	LATEST 		= 1000;
	private final int 	ACCURACY	= 1001;
	
	public final int 	JSON		= 1010;
	public final int 	XML			= 1011;
	public final int 	TEXT		= 1012;
	public final int 	DB			= 1013;
	
	private String 		ROOT_PATH	= null;
	private String 		INDEX_PATH	= null;
	private String 		DATA_PATH	= null;
	
	private String 		AGENT_IP			= null;
	private int 		AGENT_PORT			= 0;
	private String 		AGENT_KEY			= null;
	private String 		AGENT_CIPHER_KEY	= null;
	
	private String 		DB_INSERT_QUERY	= null;
	
	//private int 		SEARCH_HIGHLIGHT_NUM 	= 0;
	//private int 		SEARCH_HIGHLIGHT_LEN 	= 0;
	
	public String ERR_MSG			= null;
//###################################//
//## PUBLIC FUNCTION 
//###################################//
	public Search(Trace trace) {
		TRACE = trace;
	}
	
	public boolean Init_Configure() {
    	try {
    		
    		CONF 		= new IniParser();
    		INDEX		= new XmlParser();
    		SEARCH 		= new XmlParser();
    		
        	if(!CONF.setParser("ini", "OfficeWSE")) {
        		if(TRACE == null) {
            		String LOG_PATH = CONF.getString("COMMON","LOG_PATH","log");
                	int LOG_LV 		= CONF.getInt("COMMON", "LOG_LV", 3);
            		TRACE = new Trace(common.com.Path.getProjectPath()+LOG_PATH, LOG_LV);
            	}
        		TRACE.TraceLog("Search.Init_Configure() : Wrong OfficeWSE.ini file", Trace.LOG_DEBUG);
        		return false;
        	}
        
        	if(!INDEX.setParser("xml", "index")) {
        		if(TRACE != null)
        			TRACE.TraceLog("Search.Init_Configure() : Wrong index.xml file", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	if(!SEARCH.setParser("xml", "search")) {
        		if(TRACE != null)
        			TRACE.TraceLog("Search.Init_Configure() : Wrong search.xml file", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	if(TRACE == null) {
        		String LOG_PATH = CONF.getString("COMMON","LOG_PATH","log");
            	int LOG_LV 		= CONF.getInt("COMMON", "LOG_LV", 1);
        		TRACE = new Trace(common.com.Path.getProjectPath()+LOG_PATH, LOG_LV);
        	}
        	
        	ROOT_PATH 	= CONF.getString("COMMON","ROOT_PATH","");
        	DATA_PATH 	= CONF.getString("COMMON","DATA_PATH","");
        	INDEX_PATH 	= CONF.getString("COMMON","INDEX_PATH","");
        	
        	DB_INSERT_QUERY = SEARCH.getValListByNode("//SEARCH/DB_INSERT_QUERY").get(0).trim();
        	
        	//SEARCH_HIGHLIGHT_NUM 	= CONF.getInt("SEARCH","HIGHLIGHT_NUM",3);
			//SEARCH_HIGHLIGHT_LEN 	= CONF.getInt("SEARCH","HIGHLIGHT_LEN",30);
			
        	AGENT_IP			= CONF.getString("AGENT","IP","");
        	AGENT_PORT			= Integer.parseInt(CONF.getString("AGENT","PORT","0"));
        	AGENT_KEY			= CONF.getString("AGENT","KEY","");
        	AGENT_CIPHER_KEY	= CONF.getString("AGENT","CIPHER_KEY","");
        	
        	if(DATA_PATH.equals("") || INDEX_PATH.equals("")) {
        		TRACE.TraceLog("Search.Init_Configure() : Invalid configure data(COMMON)", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	if(AGENT_IP.equals("") || AGENT_PORT==0 || AGENT_KEY.equals("") || AGENT_CIPHER_KEY.equals("")) {
        		TRACE.TraceLog("Search.Init_Configure() : Invalid OfficeWSE.ini data(AGENT).", Trace.LOG_DEBUG);
        		return false;
        	}
        	
        	//TRACE.TraceBox("Search.Init_Configure() OK");
        	
        	return true;
        	
    	} catch(Exception e) {
    		TRACE.TraceLog("Search.Init_Configure() Fail : " + e.toString(), Trace.LOG_DEBUG);
    		return false;
    	}
    }
	
	public String Run_Search(Map<String,String> mParam) {
		ERR_MSG = null;
		return Run_Search(mParam, JSON);
	}
	
	public String Run_Search(Map<String,String> mParam, int nRetType) {
		ERR_MSG = null;


    	//Map<String, String> ReturnRes = new HashMap<String, String>();
		
		if(!Init_Configure()) {
			TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
			return ERR_MSG;
		}
		
		if(nRetType==DB) {
			ArrayList<Map<String,String>> ReturnRes = new ArrayList<Map<String,String>>(); 
			
			if(!Searching(mParam, nRetType, ReturnRes)) {
				TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
				return "F" + ERR_MSG;
			}
			
			TRACE.TraceBox("Search OK : " + getJonsObject(ReturnRes).toJSONString());
			return getJonsObject(ReturnRes).toJSONString();
			
		} else if(nRetType==JSON) {
			ArrayList<Map<String,String>> listRet = new ArrayList<Map<String,String>>(); 
			ArrayList<Map<String,String>> ReturnRes = new ArrayList<Map<String,String>>(); 
			
//			if(!Searching(mParam, listRet, nRetType, ReturnRes)) {
//				TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
//				return "F" + ERR_MSG;
//			}
			
			if(!Searching(mParam, listRet, nRetType)) {
				TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
				return "F" + ERR_MSG;
			}
			
			TRACE.TraceBox("Search OK : " + getJonsObject(listRet).toJSONString());
			return getJonsObject(listRet).toJSONString();
			
		} else {
			TRACE.TraceBox("Check return type");
			return "Check return type";
		}
		
		
//		if(nRetType==JSON) {
//			TRACE.TraceBox("Search OK : " + getJonsObject(listRet).toJSONString());
//			return getJonsObject(listRet).toJSONString();
//		} else if(nRetType==XML) {
//			TRACE.TraceBox("Search OK : " + getXml(listRet));
//			return getXml(listRet);
//		} else if(nRetType==TEXT) {
//			TRACE.TraceBox("Search OK : " + listRet.toString());
//			return listRet.toString();
//		} else if(nRetType==DB) {
//			TRACE.TraceBox("Search OK : " + getJonsObject(ReturnRes).toJSONString());
//			return getJonsObject(ReturnRes).toJSONString();
//		} else {
//			TRACE.TraceBox("Check return type");
//			return "Check return type";
//		}
			
	}
	
	// JSON
    private boolean Searching(Map<String,String> mParam, ArrayList<Map<String,String>> listRet, int nRetType) { 
    	
    	boolean	 bAnalyzer		= false;
    	boolean	 bPaging		= false;
    	int nSortFalg			= 0;		
    	int nHighlightLen 		= 0;
    	
		IndexReader index_reader 		= null;
		IndexSearcher index_searcher	= null;
		
		TopDocs hits			= null;
		Query query				= null;
		Highlighter highlighter	= null;
		
		int nRow				= 0;
		int nPage				= 0;
		int nIndexStart			= 0;
		int nIndexEnd			= 0;
		String strReturnIDX		= "";
		
		try {
			TRACE.TraceBox("Start Search JSON > ");
			
			long start = System.currentTimeMillis(); 
			
			String strQuery 	= mParam.get(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='query']", "col").get(0)); 
			String strSort 		= mParam.get(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='sort']", "col").get(0));
			String strAnayzer 	= mParam.get(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='analyzer']", "col").get(0));
			String strRow 		= mParam.get(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='row']", "col").get(0));
			String strPage 		= mParam.get(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='page']", "col").get(0));
			
			String strSortField = INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@index='REG_TIME']", "index").get(0);
			
			if(strQuery == null ||  strQuery.equalsIgnoreCase("NULL")) {
				ERR_MSG = " Searching fail : query is null ";
				TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
				return false;
			} else if(strQuery.equals("")) {
				strQuery = "";
			} else {
				strQuery = ReplaceSpecialChar(strQuery);
			}

			if(strAnayzer == null || strAnayzer.equals("") || strAnayzer.equalsIgnoreCase("NULL") || strAnayzer.equalsIgnoreCase("FALSE")) {
				if(StringUtil.checkHangul(strQuery) > 0) {
					bAnalyzer = true;
					TRACE.TraceBox("Search.Searching() number of Hangul character : " + StringUtil.checkHangul(strQuery));
				} else {
					bAnalyzer = false;	
					TRACE.TraceBox("Search.Searching() number of Hangul character : " + StringUtil.checkHangul(strQuery));
				}
			} else {
				bAnalyzer = true;
			}
						
			QUERY = new search.Query();
			query = QUERY.Get_Query(strQuery, mParam, bAnalyzer);
			TRACE.TraceBox("Start keyword search by column : '" + strQuery + "'");
			
			if(query == null || query.equals(""))	{
    			ERR_MSG = " Searching fail : "+ QUERY.ERR_MSG ;
    			TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
    			return false;
    		}
			
			nHighlightLen = StringUtil.parseInt(SEARCH.getAttValueList("//SEARCH/OPTION/HIGHLIGHT[@type='len']", "val").get(0), 30);
			highlighter = getHighlighter(strQuery, nHighlightLen, bAnalyzer);
	        
			if(highlighter == null) {
	        	ERR_MSG = " Searching fail : " + ERR_MSG ;
	        	TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
	        	return false;
	        }
			
			if(strRow == null || strRow.equals("") || strPage == null || strPage.equals("")) {
				bPaging = false;
			} else {
				try {
					nRow			= Integer.parseInt(strRow);
					nPage			= Integer.parseInt(strPage);
					nIndexStart		= nRow*(nPage-1);
					nIndexEnd		= (nRow*nPage)-1;
					bPaging 			= true;
				} catch(Exception e) {
					bPaging 			= false;	
				}
			}
	        
			if(strSort == null || strSort.equalsIgnoreCase("TRUE")) {
				nSortFalg = ACCURACY;
				TRACE.TraceBox("Search by 'ACCURACY'");
			} else {
				nSortFalg = LATEST;
				TRACE.TraceBox("Search by 'LATEST'");
			}
	        
			index_reader 	= DirectoryReader.open(FSDirectory.open(Paths.get(ROOT_PATH+"\\"+INDEX_PATH))); 
			index_searcher	= new IndexSearcher(index_reader); 

			if(nSortFalg == LATEST) {
				SortField sf = new SortField(strSortField, SortField.Type.LONG, true);
				Sort sort = new Sort(sf);
				//TopFieldCollector collector = TopFieldCollector.create(sort, StringUtil.parseInt(SEARCH.getAttValueList("//SEARCH/OPTION/SEARCH[@type='cnt']", "val").get(0),1000), false, true, false);
				TopFieldCollector collector = TopFieldCollector.create(sort, StringUtil.parseInt(SEARCH.getAttValueList("//SEARCH/OPTION/SEARCH[@type='cnt']", "val").get(0),1000), Integer.MAX_VALUE);

				index_searcher.search(query,collector); 
				hits = collector.topDocs();
				TRACE.TraceBox("The result of the query : " + hits.totalHits);
				
			} else if(nSortFalg==ACCURACY) {
				hits = index_searcher.search(query, StringUtil.parseInt(SEARCH.getAttValueList("//SEARCH/OPTION/SEARCH[@type='cnt']", "val").get(0),1000)); 
				TRACE.TraceBox("The result of the query : " + hits.totalHits);
			}
			
			List<String> listIndex 	= INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@type!='content']","index");
			Map<String, String> mRet = new HashMap<String, String>();
			
			mRet.put("PAGE", Integer.toString(nPage));
			mRet.put("RECORDS", Integer.toString(nRow));
			mRet.put("TOTAL", Integer.toString(hits.scoreDocs.length));
			mRet.put("ROWS", Integer.toString(hits.scoreDocs.length));
	        listRet.add(mRet);
    	    	
			long end = System.currentTimeMillis(); 
	        //System.err.println("Found "+ hits.totalHits + " document(s)  ( in " + (end - start) + " milliseconds) that mached query '" + query+ "':" ); 
			
			if(hits.scoreDocs.length != 0) {
				//Map<String, String> mDBRet = new HashMap<String, String>();
				for(int i=0;i<hits.scoreDocs.length;i++) {
	            	if(bPaging && (i < nIndexStart || i > nIndexEnd)) {
	            		continue;
	            	}
	            	
	            	Map<String, String> mSubRet = new HashMap<String, String>();
	        		int id 			= hits.scoreDocs[i].doc;
	            	Document doc 	= index_searcher.doc(hits.scoreDocs[i].doc); 
	            	
	            	TokenStream tokenStreamCon 	= TokenSources.getAnyTokenStream(index_searcher.getIndexReader(), id, "CONTENTS", doc, getAnalyzer(bAnalyzer));
	            	TokenStream tokenStreamTit 	= TokenSources.getAnyTokenStream(index_searcher.getIndexReader(), id, INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@type='title']", "index").get(0), doc, getAnalyzer(bAnalyzer));
	           
	            	// 	TokenStream tokenStreamSdoc 	= TokenSources.getAnyTokenStream(index_searcher.getIndexReader(), id, INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@index='SDOC_NO']", "index").get(0), doc, getAnalyzer(bAnalyzer));
	            	// 	String strHighlightContents	= highlighter.getBestFragments(tokenStreamCon, doc.get("CONTENTS"), Integer.parseInt(SEARCH.getAttValueList("//SEARCH/OPTION/HIGHLIGHT[@type='num']", "val").get(0)), "...");

	            	/*strHighlightContents = duplicatePattern(strHighlightContents,"\r");
	            	strHighlightContents = duplicatePattern(strHighlightContents,"\n");
	            	strHighlightContents = duplicatePattern(strHighlightContents,"\r\n");
	            	
	            	strHighlightTitle = duplicatePattern(strHighlightTitle,"\r");
	            	strHighlightTitle = duplicatePattern(strHighlightTitle,"\n");
	            	strHighlightTitle = duplicatePattern(strHighlightTitle,"\r\n");*/
	            	
	            	for (int j=0;j<listIndex.size();j++) {
	            		mSubRet.put(listIndex.get(j), doc.get(listIndex.get(j)));
	//        			if(listIndex.get(j).equalsIgnoreCase(strTitleField))	{
	//	            		if(strHighlightTitle.length() > 0) {
	//	            			mSubRet.put(strTitleField,strHighlightTitle);
	//	            		} else {
	//	            			mSubRet.put(strTitleField,doc.get(strTitleField));
	//	            		}
	//	            	} else {
	//	            		mSubRet.put(listIndex.get(j), doc.get(listIndex.get(j)));
	//	            	}
		            }
	            	
//	            	if(strHighlightContents.length() > 0) {
//	            		mSubRet.put("CONTENTS",strHighlightContents.trim());
//	            	} else
	            	
	            	if(doc.get("CONTENTS").length() > 60) {
            			mSubRet.put("CONTENTS", doc.get("CONTENTS").substring(0, 55));
            		} else {
            			mSubRet.put("CONTENTS", doc.get("CONTENTS"));
            		}
	            	
	            	listRet.add(mSubRet);
	            } 
        	
//            	ReturnRes.add(mDBRet);

			} else {

			}

            index_reader.close(); 
    	} catch(Exception e) {
    		ERR_MSG = "Search.Searching() fail : " + e.toString();
			TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
    		return false;
    	}
		return true;
    }
    
    // DB
    private boolean Searching(Map<String,String> mParam, int nRetType, ArrayList<Map<String,String>> ReturnRes) { 
    	
    	boolean	 bAnalyzer		= false;
    	boolean	 bPaging		= false;
    	int nSortFalg			= 0;		
    	int nHighlightLen 		= 0;
    	
		IndexReader index_reader 		= null;
		IndexSearcher index_searcher	= null;
		
		TopDocs hits			= null;
		Query query				= null;
		Highlighter highlighter	= null;
		
		int nRow				= 0;
		int nPage				= 0;
		int nIndexStart			= 0;
		int nIndexEnd			= 0;
		String strReturnIDX		= "";
		
		try {
			
			TRACE.TraceBox("Start Search DB > ");
			strReturnIDX = getIRN("R");
			
			long start = System.currentTimeMillis(); 
			
			String strQuery 	= mParam.get(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='query']", "col").get(0)); 
			String strSort 		= mParam.get(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='sort']", "col").get(0));
			String strAnayzer 	= mParam.get(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='analyzer']", "col").get(0));
			String strRow 		= mParam.get(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='row']", "col").get(0));
			String strPage 		= mParam.get(SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='page']", "col").get(0));
			
			String strSortField = INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@index='REG_TIME']", "index").get(0);
			
			if(strQuery == null ||  strQuery.equalsIgnoreCase("NULL")) {
				ERR_MSG = " Searching fail : query is null ";
				TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
				return false;
			} else if(strQuery.equals("")) {
				strQuery = "";
			} else {
				strQuery = ReplaceSpecialChar(strQuery);
			}

			if(strAnayzer == null || strAnayzer.equals("") || strAnayzer.equalsIgnoreCase("NULL") || strAnayzer.equalsIgnoreCase("FALSE")) {
				if(StringUtil.checkHangul(strQuery) > 0) {
					bAnalyzer = true;
					TRACE.TraceBox("Search.Searching() number of Hangul character : " + StringUtil.checkHangul(strQuery));
				} else {
					bAnalyzer = false;	
					TRACE.TraceBox("Search.Searching() number of Hangul character : " + StringUtil.checkHangul(strQuery));
				}
			} else {
				bAnalyzer = true;
			}
						
			QUERY = new search.Query();
			query = QUERY.Get_Query(strQuery, mParam, bAnalyzer);
			TRACE.TraceBox("Start keyword search by column : '" + strQuery + "'");
			
			if(query == null || query.equals(""))	{
    			ERR_MSG = " Searching fail : "+ QUERY.ERR_MSG ;
    			TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
    			return false;
    		}
			
			nHighlightLen = StringUtil.parseInt(SEARCH.getAttValueList("//SEARCH/OPTION/HIGHLIGHT[@type='len']", "val").get(0), 30);
			highlighter = getHighlighter(strQuery, nHighlightLen, bAnalyzer);
	        
			if(highlighter == null) {
	        	ERR_MSG = " Searching fail : " + ERR_MSG ;
	        	TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
	        	return false;
	        }
			
			if(strRow == null || strRow.equals("") || strPage == null || strPage.equals("")) {
				bPaging = false;
			} else {
				try {
					nRow			= Integer.parseInt(strRow);
					nPage			= Integer.parseInt(strPage);
					nIndexStart		= nRow*(nPage-1);
					nIndexEnd		= (nRow*nPage)-1;
					bPaging 			= true;
				} catch(Exception e) {
					bPaging 			= false;	
				}
			}
	        
			if(strSort == null || strSort.equalsIgnoreCase("TRUE")) {
				nSortFalg = ACCURACY;
				TRACE.TraceBox("Search by 'ACCURACY'");
			} else {
				nSortFalg = LATEST;
				TRACE.TraceBox("Search by 'LATEST'");
			}
	        
			index_reader 	= DirectoryReader.open(FSDirectory.open(Paths.get(ROOT_PATH+"\\"+INDEX_PATH))); 
			index_searcher	= new IndexSearcher(index_reader); 

			if(nSortFalg == LATEST) {
				SortField sf = new SortField(strSortField, SortField.Type.LONG, true);
				Sort sort = new Sort(sf);
				//TopFieldCollector collector = TopFieldCollector.create(sort, StringUtil.parseInt(SEARCH.getAttValueList("//SEARCH/OPTION/SEARCH[@type='cnt']", "val").get(0),1000), false, true, false);
				TopFieldCollector collector = TopFieldCollector.create(sort, StringUtil.parseInt(SEARCH.getAttValueList("//SEARCH/OPTION/SEARCH[@type='cnt']", "val").get(0),1000), Integer.MAX_VALUE);

				index_searcher.search(query,collector); 
				hits = collector.topDocs();
				TRACE.TraceBox("The result of the query : " + hits.totalHits);
				
			} else if(nSortFalg==ACCURACY) {
				hits = index_searcher.search(query, StringUtil.parseInt(SEARCH.getAttValueList("//SEARCH/OPTION/SEARCH[@type='cnt']", "val").get(0),1000)); 
				TRACE.TraceBox("The result of the query : " + hits.totalHits);
			}
			
			List<String> listIndex 	= INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@type!='content']","index");
    	    	
			long end = System.currentTimeMillis(); 
	        //System.err.println("Found "+ hits.totalHits + " document(s)  ( in " + (end - start) + " milliseconds) that mached query '" + query+ "':" ); 
			
			if(hits.scoreDocs.length != 0) {
				Map<String, String> mDBRet = new HashMap<String, String>();
            	String strDBQuery		= "";
            	
				for(int i=0;i<hits.scoreDocs.length;i++) {
	            	if(bPaging && (i < nIndexStart || i > nIndexEnd)) {
	            		continue;
	            	}
	            	
	            	Map<String, String> mSubRet = new HashMap<String, String>();
	        		int id 			= hits.scoreDocs[i].doc;
	            	Document doc 	= index_searcher.doc(hits.scoreDocs[i].doc); 
	            	
	            	if(nRetType==DB) { 
	            		
	            		String strKey 			= doc.get("SDOC_NO");
	                	String strContents 		= "";
	                	
	                	if(doc.get("CONTENTS").length() > 60) {
	                		strContents = doc.get("CONTENTS").trim().substring(0, 55);
	            		} else {
	            			strContents = doc.get("CONTENTS");
	            		}

	                	String lastContents = StringReplace(strContents);
	                	
	                	// IMG_SEARCH_R data insert
	                	strDBQuery = DB_INSERT_QUERY;
	                	strDBQuery = strDBQuery.replaceFirst("\\?", "'"+strReturnIDX+"'");
	                	strDBQuery = strDBQuery.replaceFirst("\\?", "'"+ strKey +"'");
	                	strDBQuery = strDBQuery.replaceFirst("\\?", "'"+ lastContents +"'");
	                	strDBQuery = strDBQuery.replaceFirst("\\?", "getDate()");

	                	TRACE.TraceBox("Search.Searching() Insert Query : " + strDBQuery);
	                	
	    				if(!Set_Result(strDBQuery)) { 
	    					TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
	    					return false;
	    				}
	            	}
	            	
	            	TokenStream tokenStreamCon 	= TokenSources.getAnyTokenStream(index_searcher.getIndexReader(), id, "CONTENTS", doc, getAnalyzer(bAnalyzer));
	            //	TokenStream tokenStreamTit 	= TokenSources.getAnyTokenStream(index_searcher.getIndexReader(), id, INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@type='title']", "index").get(0), doc, getAnalyzer(bAnalyzer));
	            //	TokenStream tokenStreamSdoc 	= TokenSources.getAnyTokenStream(index_searcher.getIndexReader(), id, INDEX.getAttValueList("//INDEX/PARAMETER/PARAM[@index='SDOC_NO']", "index").get(0), doc, getAnalyzer(bAnalyzer));
	            	//String strHighlightContents	= highlighter.getBestFragments(tokenStreamCon, doc.get("CONTENTS"), Integer.parseInt(SEARCH.getAttValueList("//SEARCH/OPTION/HIGHLIGHT[@type='num']", "val").get(0)), "...");
	            	//String strHighlightTitle	= highlighter.getBestFragments(tokenStreamTit, doc.get("TITLE"), Integer.parseInt(SEARCH.getAttValueList("//SEARCH/OPTION/HIGHLIGHT[@type='num']", "val").get(0)), "...");
	            	//String strHighlightSdoc	= highlighter.getBestFragments(tokenStreamSdoc, doc.get(strSdocField), Integer.parseInt(SEARCH.getAttValueList("//SEARCH/OPTION/HIGHLIGHT[@type='num']", "val").get(0)), "...");
	            	
	            	/*strHighlightContents = duplicatePattern(strHighlightContents,"\r");
	            	strHighlightContents = duplicatePattern(strHighlightContents,"\n");
	            	strHighlightContents = duplicatePattern(strHighlightContents,"\r\n");
	            	
	            	strHighlightTitle = duplicatePattern(strHighlightTitle,"\r");
	            	strHighlightTitle = duplicatePattern(strHighlightTitle,"\n");
	            	strHighlightTitle = duplicatePattern(strHighlightTitle,"\r\n");*/
	            	
	            	for (int j=0;j<listIndex.size();j++) {
	            		mSubRet.put(listIndex.get(j), doc.get(listIndex.get(j)));
		            }
	            	
//	            	if(strHighlightContents.length() > 0) {
//	            		mSubRet.put("CONTENTS",strHighlightContents.trim());
//	            	} else {
	            		if(doc.get("CONTENTS").length()>60) {
	            			mSubRet.put("CONTENTS",doc.get("CONTENTS").substring(0, 55));
	            		} else {
	            			mSubRet.put("CONTENTS",doc.get("CONTENTS"));
	            		}
//	            	}
	            } 
				
				mDBRet.put("TOTAL", Integer.toString(hits.scoreDocs.length));
            	mDBRet.put("RIDX", strReturnIDX);
            	
            	ReturnRes.add(mDBRet);

			} else {

				if(nRetType==DB) { 
            		Map<String, String> mDBRet = new HashMap<String, String>();
            		mDBRet.put("TOTAL", Integer.toString(hits.scoreDocs.length));
                	mDBRet.put("RIDX", strReturnIDX);
                	
                	ReturnRes.add(mDBRet);
				}
			}

            index_reader.close(); 
    	} catch(Exception e) {
    		ERR_MSG = "Search.Searching() fail : " + e.toString();
			TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
    		return false;
    	}
		return true;
    }
        
    private Highlighter getHighlighter(String q, int nHighLength, boolean bAnal ) {
    	Highlighter highlighter = null;
    	try	{
    		highlighter 				= new Highlighter(new QueryScorer(getQuery(q, bAnal)));
        	Fragmenter fragmenter 		= new SimpleFragmenter(nHighLength);
        	highlighter.setTextFragmenter(fragmenter);
    	}	catch(Exception e)	{
    		ERR_MSG = e.toString();
    	}
		 return highlighter;
    }
    
    private Query getQuery(String q, boolean bAnal) {
    	Query query = null;
    	try	{
    		QueryParser parser 	= new QueryParser("CONTENTS", getAnalyzer(bAnal));
    		query 					= parser.parse(q);
    	} catch(Exception e) {
    		query = null;
    	}
		 return query;
    }
    
    private Analyzer getAnalyzer(boolean bAnal) {
    	Analyzer retAnalyzer = null;
    	if(!bAnal) {	 	
    		retAnalyzer = new KoreanAnalyzer();
    	} else {
    		retAnalyzer = new StandardAnalyzer();
    	}
    	return retAnalyzer;
    }
    
    private String ReplaceSpecialChar (String phrase) {
    	if(phrase == null || phrase.equals(""))
    		return "";
      //	String 	strPattern 	= "([^\"'\\{\\}\\[\\]/,;:|\\)\\(*~`^\\-_+<>@#$%^\\\\=]){0,}";
      	String 	strPattern 	= "([^\"'\\{\\}\\[\\]/,;:\\)\\(*~`^\\-_+<>@#$%^\\\\=]){0,}";
        Pattern 	p 			= Pattern.compile(strPattern);
        Matcher m 				= p.matcher(phrase);
        StringBuffer sb 		= new StringBuffer();
        while (m.find()) {
        	sb.append(m.group().toString());
        }
        return ReplaceFirstEndSpecialChar(sb.toString());
    }
    
    private String duplicatePattern(String phrase) {
    	String duplicatePattern 	= "( ){1,}";
        Pattern p					= Pattern.compile(duplicatePattern);
        Matcher m 					= p.matcher(phrase);
        while (m.find()) {
        	phrase = phrase.replaceFirst(m.group().toString(), " ");
        }
        
    	duplicatePattern = "([!]){1,}";
        p	= Pattern.compile(duplicatePattern);
        m	= p.matcher(phrase);
        while (m.find()) {
        	phrase = phrase.replaceAll(m.group().toString(), " NOT ");
        }
        
        duplicatePattern = "([&]){1,}";
        p 		= Pattern.compile(duplicatePattern);
        m 		= p.matcher(phrase);
        while (m.find()) {
        	phrase = phrase.replaceAll(m.group().toString(), " AND ");
        }
        
        return phrase;
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
    
    private String ReplaceFirstEndSpecialChar(String phrase) {
    	while(phrase.charAt(0)=='!'||phrase.charAt(0)=='&')	{
    		phrase = phrase.substring(1,phrase.length());
    	}
    	
    	while(phrase.charAt(phrase.length()-1)=='!'||phrase.charAt(phrase.length()-1)=='&')	{
    		phrase = phrase.substring(0,phrase.length()-1);
    	}
    	return duplicatePattern(phrase);
    }
    
    public JSONObject getJonsObject(ArrayList<Map<String, String>> list) {
		JSONArray arr 			= new JSONArray();
		JSONObject SubObj 		= new JSONObject();
		try	{
			for (int i = 0; i<list.size(); i++) {
				Iterator<String> keys = ((Map)list.get(i)).keySet().iterator();
				JSONObject obj = new JSONObject();
				while (keys.hasNext()) {
					String key = (String)keys.next();
         			if(i==0) {
						SubObj.put(key, (list.get(i).get(key)));
					} else			
						obj.put(key, (list.get(i).get(key)));
				}
				if (!obj.isEmpty()) {
					arr.add(obj);
				}
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		if(!arr.isEmpty())	SubObj.put("ROWS", arr);
		
		return SubObj;
	}
    
    public String getXml(List<Map<String, String>> list) {
		String strRet = "";
		try {
				org.jdom.Document 	doc		= new org.jdom.Document();
				Element 	root	= new Element("ListData");
				Element 	row		= null;
				Element 	Node 	= null;
				String 		Key		= "";
				
				for (int i = 0; i<list.size(); i++) {
					Iterator<String> keys = ((Map)list.get(i)).keySet().iterator();
					row	= new Element("Row");
					while (keys.hasNext()) {
						Key = (String)keys.next();
						if(i==0) {
							root.setAttribute(Key,list.get(i).get(Key));
						}	else	{
							Node 	= new Element(Key);
				       		Node.setText(list.get(i).get(Key));
				       		row.addContent(Node);
				       }
			        }
					if(Node!=null&&list.get(i).size()!=0)
						root.addContent(row);	
				}
				doc.setRootElement(root);
				XMLOutputter xout = new XMLOutputter();
		        Format fo = xout.getFormat();
		        //fo.setEncoding("euc-kr");
		        fo.setIndent("  ");
		        fo.setLineSeparator("\r\n");
		        fo.setTextMode(Format.TextMode.TRIM);							
	        	xout.setFormat(fo);
	            strRet = xout.outputString(doc);
	            //strRet = strRet.substring(strRet.indexOf("\n"), strRet.length());
		} catch (Exception e) {
			ERR_MSG = "Search.getXml() : an error occurred while getXml : " + e.toString();
			TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
		}

		return strRet;
	}
    
    public String getIRN(String strIdxFlag) {
		
		String strUUID = UUID.randomUUID().toString();
		
		StringBuffer sbRes = new StringBuffer();
		sbRes.append(strIdxFlag);
		sbRes.append(getToday("yyyyMMdd"));
		sbRes.append(strUUID.substring(strUUID.length() - 5, strUUID.length()));
		sbRes.append(getToday("HHmmssSSS"));
		
		return sbRes.toString();
	}
    
    //포맷대로 현재 날짜부르기
  	public String getToday(String strFormat)
  	{
  		Date dateNow = Calendar.getInstance(new SimpleTimeZone(0x1ee6280, "KST")).getTime();
  		SimpleDateFormat formatter = new SimpleDateFormat(strFormat, Locale.KOREA);
  		String today = formatter.format(dateNow);	
  			
  		return today;
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
			ERR_MSG = "Search.Set_Result() Error :  " + e.toString();
			TRACE.TraceLog(ERR_MSG, Trace.LOG_DEBUG);
			return false;
		}
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
  	

    
}



