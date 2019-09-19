package listener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import admin.Admin;
import batch.Batch;
import common.com.HttpParser;
import common.com.Trace;
import common.util.StringUtil;
import common.wdclient.wdmCom;
import crawler.Crawl;
import delete.Delete;
import index.Index;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import io.netty.channel.ChannelInboundHandlerAdapter;
import search.Search;

public class ServerHandler extends ChannelInboundHandlerAdapter {

	private Trace TRACE 		= null;
	private String ENCODING 	= null;
	
	public ServerHandler(Trace trace, String encoding) {
		ENCODING 	= encoding;
		TRACE 		= trace;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		
		String strRet 				= "";
		HttpParser hp 				= null;
		//String strURL 				= "http://officeslip.woonamsoft.com:14485/?";
		//String strURL 				= "http://localhost:8089/?";
		String strURL 				= "http://10.10.142.22:9785/?";
		
		try {
			
			ByteBuf buffer = ((ByteBuf) msg);
			byte[] bytes = new byte[buffer.readableBytes()];
			buffer.readBytes(bytes);
			
			hp = new HttpParser();
			hp.setRequest(bytes, ENCODING);
			/*
			 * switch(hp.getContent("JOB").toUpperCase()) { case "SEARCH" :
			 * 
			 * 
			 * 
			 * break; case "INDEX" :
			 * 
			 * 
			 * break; }
			 */
			
			// Search
		    if(hp.getContent("JOB").equalsIgnoreCase("SEARCH")) {
		    	TRACE.TraceBox("JOB SEARCH START >>");
		    	TRACE.TraceBox(strURL + hp.getQueryString());
		 
		    	Search search = new Search(TRACE);
		    	if(hp.getContent("RET").equalsIgnoreCase("XML")) {
		    		strRet = search.Run_Search(hp.getQueryMap(), search.XML);	
		    	} else if(hp.getContent("RET").equalsIgnoreCase("JSON")) {
		    		strRet = search.Run_Search(hp.getQueryMap(), search.JSON);
		    	} else if(hp.getContent("RET").equalsIgnoreCase("TEXT")) {
		    		strRet = search.Run_Search(hp.getQueryMap(), search.TEXT);
		    	} else if(hp.getContent("RETURN").equalsIgnoreCase("JSON")){
		    		strRet = search.Run_Search(hp.getQueryMap(), search.JSON);
		    	} else if(hp.getContent("RETURN").equalsIgnoreCase("DB")){
		    		strRet = search.Run_Search(hp.getQueryMap(), search.DB);
		    		if(strRet=="" || strRet==null || ("F").equalsIgnoreCase(strRet.substring(0, 1))) {
		    			strRet = strRet.substring(1);
		    			strRet = "RETURN=F, MSG=" + strRet;
		    		} 
		    	} else {
		    		strRet = search.Run_Search(hp.getQueryMap(), search.JSON);
		    	}
		    	
		    // Crawl & Index
		    } else if(hp.getContent("JOB").equalsIgnoreCase("INDEX")) {
		    	TRACE.TraceBox("JOB INDEX START >>");
		    	TRACE.TraceBox(strURL + hp.getQueryString());
		    	
		    	Crawl crawl = new Crawl();
		    	HashMap<String, String> mapRes = wdmCom.updateProcResult(crawl, crawl.Run_Crawl(hp.getQueryMap()));
		    	
		    	String strRes 		= mapRes.get("RESULT");		// Query result
		    	String strSEFlag 	= mapRes.get("SE_RESULT"); 	// Crawling result
		    	String strUpdate	= mapRes.get("UPDATE");	
		    	String strErrMsg	= mapRes.get("MSG");
		    	String strQuery		= mapRes.get("QUERY");
		    	
		    	
		    	if("T".equalsIgnoreCase(strSEFlag))
		    	{
		    		strRet = "RETURN=" + strRes;
		    		TRACE.TraceBox("CRAWL OK > " + strRet + ", \r\r" + strQuery);
		    		
		    		Index index = new Index();
					index.Init_Configure();

					if("F".equalsIgnoreCase(strUpdate)) {
						if(!index.Index_Folder()) {
							strRet = "RETURN=F";
				    		TRACE.TraceBox("INDEX FAIL > " + strRet);
						} else {
							strRet = "RETURN=T";
				    		TRACE.TraceBox("INDEX OK > " + strRet);
						};

					} else {
						if(!index.Index_Folder_Update()) {
							strRet = "RETURN=F";
				    		TRACE.TraceBox("INDEX FAIL > " + strRet);
						} else {
							strRet = "RETURN=T";
				    		TRACE.TraceBox("INDEX OK > " + strRet);
						};
					}
		    	}
		    	else
		    	{
		    		strRet = "RETURN=" + strSEFlag + ", MSG=" + strErrMsg;
		    		TRACE.TraceBox("CRAWL FAIL > " + strRet);
		    	}

		    // Delete
			} else if(hp.getContent("JOB").equalsIgnoreCase("DELETE")) {
				TRACE.TraceBox("JOB DELETE START >>");
		    	TRACE.TraceBox(strURL + hp.getQueryString());
				
				Delete delete = new Delete();
				HashMap<String, String> mapRes = delete.Run_Delete(hp.getQueryMap());
				
				String strRes 		= mapRes.get("RESULT");	
				String strErrMsg	= mapRes.get("MSG");
				
				if("T".equalsIgnoreCase(strRes))
		    	{
					strRet = "RETURN=" + strRes;
					TRACE.TraceBox("DELETE OK > " + strRet);
		    	}
		    	else
		    	{
		    		strRet = "RETURN=" + strRes + ", MSG=" + strErrMsg;
		    		TRACE.TraceBox("DELETE FAIL > " + strRet);
		    	}
			
			// Add
			} else if(hp.getContent("JOB").equalsIgnoreCase("ADD")) {
				TRACE.TraceBox("JOB ADD START >>");
				TRACE.TraceBox(strURL + hp.getQueryString());
				
				Crawl crawl = new Crawl();
				HashMap<String, String> mapRes = wdmCom.updateProcResult(crawl, crawl.Run_Add(hp.getQueryMap()));
				
				String strRes 		= mapRes.get("RESULT");	
				String strErrMsg	= mapRes.get("MSG");
				
				if("T".equalsIgnoreCase(strRes))
				{
					strRet = "RETURN=" + strRes;
					TRACE.TraceBox("ADD OK > " + strRet);
				}
				else
				{
					strRet = "RETURN=" + strRes + ", MSG=" + strErrMsg;
					TRACE.TraceBox("ADD FAIL > " + strRet);
				}
				
				Index index = new Index();
				index.Init_Configure();
				index.Index_Folder();
				index.Index_Folder_Update();

			// Batch
			} else if(hp.getContent("JOB").equalsIgnoreCase("BATCH")) {
				TRACE.TraceBox("JOB BATCH START >>");
		    	TRACE.TraceBox(strURL + hp.getQueryString());
				
				Batch batch = new Batch();
				HashMap<String, String> mapRes = batch.Run_Batch(hp.getQueryMap());
				
				String strRes 		= mapRes.get("RESULT");	
				String strUpdate	= mapRes.get("UPDATE");	
				String strErrMsg	= mapRes.get("MSG");
				
				if("T".equalsIgnoreCase(strRes))
		    	{
					strRet = "RETURN=" + strRes;
					TRACE.TraceBox("BATCH OK > " + strRet);
		    	}
		    	else
		    	{
		    		strRet = "RETURN=" + strRes + ", MSG=" + strErrMsg;
		    		TRACE.TraceBox("BATCH FAIL > " + strRet);
		    	}
				
				Index index = new Index();
				index.Init_Configure();
				index.Index_Folder();
				index.Index_Folder_Update();

			}
			
			else {
				strRet = "WRONG PARAM.";
			}
		} catch(Exception e) {
			strRet = "SERVER ERROR : "+e.toString();
		}
		
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("HTTP/1.1 200 OK \r\n");
			sb.append("Access-Control-Allow-Origin : *\r\n");
			sb.append("Access-Control-Allow-Headers : Origin, X-Requested-With, Content-Type, Accept\r\n");
			sb.append("Content-Type: text/html; charset=UTF-8\r\n");
			sb.append("Content-Length: " + strRet.getBytes(hp.getCharSet()).length + "\r\n");
			sb.append("\r\n");
			sb.append(strRet);
			sb.append("\r\n");

			ByteBuf buf = Unpooled.buffer(sb.length());
			buf.writeBytes(String.valueOf(sb).getBytes(hp.getCharSet()));
			ctx.write(buf);

		} catch(Exception e) {
			System.out.println(e.toString());
		}
	}
	
  
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx){
		ctx.flush();
		ctx.close();
	}
  
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
	
	private String getLogTime(String format) {
		SimpleDateFormat sdfDate = new SimpleDateFormat(format);
		return sdfDate.format(new Date());
	}
}


