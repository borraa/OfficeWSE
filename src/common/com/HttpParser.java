package common.com;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class HttpParser {

	private String HTTP_METHOD 			= null;
	private String QUERY_STRING 		= null;
	
	private String ACCEPT_TYPE 			= null;
	private String CONTENT_TYPE 		= null;
	private String CONTENT_CHARSET 		= null;
	private int CONTENT_LEN 			= 0;
	
	Map<String, String> QUERY_MAP		= null;
	
	public boolean setRequest(byte[] b) {
		return setRequest(b, "");
	}
	
	public boolean setRequest(byte[] b, String encoding) {
		 
		int nFirstLine 	= 0;
		boolean bData 	= false;
        try {
        	QUERY_MAP = new HashMap<String, String>();
        	InputStream is = new ByteArrayInputStream(b);
        	BufferedReader bfReader = new BufferedReader(new InputStreamReader(is, Charset.forName(encoding)));
            String temp = null;
            while((temp = bfReader.readLine()) != null) {
            	if(temp.trim().equals("")) {
            		bData = true;
            		continue;
            	}
            	
            	if(bData) {
            		QUERY_STRING = temp;
            	}
            	
            	if(nFirstLine++==0) {
            		StringTokenizer tokenizer 	= new StringTokenizer(temp);	
            		HTTP_METHOD	= tokenizer.nextToken().toUpperCase();
            		if(HTTP_METHOD.equals("GET")) {
            			String httpQueryString	= tokenizer.nextToken();
            			if(!httpQueryString.equals("/")) {
            				QUERY_STRING = httpQueryString.replaceFirst("/\\?", "");
      		        	  	//queryString = URLDecoder.decode(queryString,ENCODING);
            			} 
            		}
            	} else {
            		if(temp.toUpperCase().startsWith("CONTENT-TYPE:")) {
                		StringTokenizer tokenizer = new StringTokenizer(temp.substring(13, temp.length()),";");
                		CONTENT_TYPE = tokenizer.nextToken().trim();
                		String strCharSet = tokenizer.nextToken();
                		if(strCharSet.indexOf("=")>=0) {
                			CONTENT_CHARSET = strCharSet.substring(strCharSet.indexOf("=")+1, strCharSet.length()).trim();;
                		}
                	} else if(temp.toUpperCase().startsWith("ACCEPT:")) {
                		StringTokenizer tokenizer = new StringTokenizer(temp.substring(7, temp.length()),";");
                		ACCEPT_TYPE = tokenizer.nextToken().trim();;
                	} else if(temp.toUpperCase().startsWith("CONTENT-LENGTH:")) {
                		CONTENT_LEN = Integer.parseInt(temp.substring(15, temp.length()).trim());
                	}	
             	}
            }  
            if(CONTENT_CHARSET == null) {
            	QUERY_STRING = URLDecoder.decode(QUERY_STRING,encoding);
            	CONTENT_CHARSET = encoding;
            } else {
            	QUERY_STRING = URLDecoder.decode(QUERY_STRING,CONTENT_CHARSET);
            }
            
            if(QUERY_STRING.charAt(0)=='{') {
            	JSONParser parser 		= new JSONParser();
            	JSONObject jsonInObj 	= (JSONObject)parser.parse(QUERY_STRING);
            	String 				key	= null;
        		Iterator<String> itr = jsonInObj.keySet().iterator();
        		while( itr.hasNext()) {
        			key = itr.next();
        			QUERY_MAP.put(key.toUpperCase(), (String) jsonInObj.get(key));
        		} 	
            } else {
            	if(QUERY_STRING.charAt(0)=='?')
            		QUERY_STRING = QUERY_STRING.substring(1, QUERY_STRING.length());
            		
            	String[] arrParam = QUERY_STRING.split("&");
            	
            	for(int i=arrParam.length-1;i>=0;i--) {
            		
    		    	int nIdx = arrParam[i].indexOf("=");
    		    	if(nIdx<0) {
    		    		arrParam[i-1] = arrParam[i-1] + "&" + arrParam[i];
    		    		continue;
    		    	}
    		    	QUERY_MAP.put(arrParam[i].substring(0, nIdx).toUpperCase(), arrParam[i].substring(nIdx+1, arrParam[i].length()));
    			}
           }
            is.close();
            bfReader.close();
        } catch(Exception e) {
        	return false;
        } 
		return true;
	}

	public String getHttpMethod() {
		if(HTTP_METHOD != null)	return HTTP_METHOD;
		else					return "";
	}
	
	public String getQueryString() {
		if(QUERY_STRING != null)	return QUERY_STRING;
		else						return "";
	}
	
	public String getAcceptType() {
		if(ACCEPT_TYPE != null)	return ACCEPT_TYPE;
		else					return "";
	}
	
	public String getContentType() {
		if(CONTENT_TYPE != null)	return CONTENT_TYPE;
		else						return "";
	}
	
	public String getCharSet() {
		if(CONTENT_CHARSET != null)	return CONTENT_CHARSET;
		else						return "";
	}
	
	public int getContentLength() {
		return CONTENT_LEN;
	}
	
	public String getContent(String strCol) {
		if(QUERY_MAP.get(strCol)==null) 
			return "";
		else 
			return QUERY_MAP.get(strCol).toUpperCase();
	}
	
	public Map<String, String> getQueryMap() {
		return QUERY_MAP;
	}
}
