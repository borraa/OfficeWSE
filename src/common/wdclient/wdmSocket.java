package common.wdclient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import common.com.Parser;
import common.com.XmlParser;

public class wdmSocket {
	
	private String IP 		= null;
	private int PORT 		= 0;
	private Parser PARSER 	= null;
	
	public String ERR_MSG 	= null;
	
	public wdmSocket(String ip, int port) {
		PARSER = new XmlParser();
		IP 		= ip;
		PORT 	= port;
	}
	
	public String requestData(wdmStream stream) {
		
		OutputStreamWriter 	OSW 	= null;
		BufferedWriter		BW 		= null;
		BufferedReader		BR		= null;
		PrintWriter			PW		= null;
		InputStreamReader	ISR		= null;
		Socket 				socket	= null;
		
		try {
			
			socket = new Socket(IP, PORT);
			
			OSW = new OutputStreamWriter(socket.getOutputStream(),"UTF-8");
			BW	= new BufferedWriter(OSW);
			PW = new PrintWriter(BW);
			PW.write(new String(stream.getStream()));
			PW.flush();
			
			ISR = new InputStreamReader(socket.getInputStream(),"UTF-8");
			BR = new BufferedReader(ISR);
			
			String ret 	= getString(BR, 2);
			int nLen 	= Integer.parseInt(getString(BR, 12));
			
			if(ret.charAt(0)=='S'&&ret.charAt(1)!='T') {
				ERR_MSG = getString(BR, nLen);
				return null;
	    	}
	    	
	    	return getString(BR, nLen);
	    	
	    } catch( Exception e ) {
	    	ERR_MSG = e.toString();
	    	return null;
	    } finally {
	    	try {
	    		if(OSW!= null) OSW.close();
				if(BW!= null)	BW.close();
				if(PW!= null)	PW.close();
				if(ISR!= null)	ISR.close();
				if(BR!= null)	BR.close();
				if(socket!= null)	socket.close();	
	    	} catch(Exception e) {
	    		
	    	}
	    }
	}
	
	public byte[] requestDownload(wdmStream stream) {
		
		byte[] buf 		= null;
		
		BufferedOutputStream 	BO 		= null;
		BufferedInputStream		BI 		= null;
		Socket 					socket	= null;
		
	    try {
	    	socket = new Socket(IP, PORT);
	    	
	    	BO = new BufferedOutputStream( socket.getOutputStream() );
	    	BO.write(stream.getStream());
	    	BO.flush();
	      
	    	BI = new BufferedInputStream( socket.getInputStream() );
	    	
	    	String ret 	= getString(BI, 2);
			int nLen 	= Integer.parseInt(getString(BI, 12));
	    	
	    	if (ret.charAt(1) != 'T') {
	    		ERR_MSG = getString(BI, nLen);
	    		return null;
	    	}
	      
	    	PARSER.setStringXml(getString(BI, nLen));
	    	buf = new byte[Integer.parseInt(PARSER.getAttValueList("//ListData", "FileSize").get(0))];
	      
	    	byte[] _buf = new byte[1024];
	    	int readlen = 0;
	    	int pos = 0;
	    	while( (readlen =BI.read( _buf )) != -1 ) {
	    		System.arraycopy(_buf, 0,  buf, pos, readlen);
	    		pos += readlen;
	    	}
	    	return buf;
	    } catch( Exception e ) {
	    	ERR_MSG = "Download fail. "+e.toString();
	    	return null;
	    } finally {
	    	try {
	    		if(BO!=null)		BO.close();
				if(BI!=null)		BI.close();
				if(socket!=null)	socket.close();	
	    	} catch(Exception e) {
	    		
	    	}
	    }
	}
	
	public boolean requestUpload(wdmStream stream) {
		
		ByteArrayInputStream BAS 	= null;
		BufferedOutputStream BO		= null;
		BufferedInputStream BI		= null;
		Socket	socket				= null;
	    
	    try {
	    	socket = new Socket(IP, PORT);
	    	
	    	FileInputStream FS 			= null;
	    	
	    	BO = new BufferedOutputStream( socket.getOutputStream() );
	    	//BO.write( new String(stream.getStream(),"UTF-8").getBytes("UTF-8") );
	    	BO.write( stream.getStream() );
	    	BO.flush();
	      
	    	if(stream.getFilePath()!=null) {
	    		FS = new FileInputStream( stream.getFilePath());
	    		BI = new BufferedInputStream(FS);	
	    	} else {
	    		BAS = new ByteArrayInputStream( stream.getFileSource());
	    		BI = new BufferedInputStream(BAS);	
	    	}
	    	
	    	int ch = 0;
	    	while((ch = BI.read()) != -1) {
	    		BO.write(ch);        
	    	}    
	      
	    	BO.flush();
	    	if(FS!=null)	FS.close();
	    	if(BAS!=null) 	BAS.close();
	    } catch( Exception e ) {
	    	ERR_MSG = "Upload fail. "+e.toString();
	    	return false;
	    } finally {
	    	try {
	    		if(BAS!=null)		BAS.close();
	    		if(BI!=null)		BI.close();
				if(BO!=null)		BO.close();
				if(socket!=null)	socket.close();	
	    	} catch(Exception e) {
	    		
	    	}
	    }
	    return true;
	}
	
	public boolean requestDelete(wdmStream stream) {
		
		BufferedOutputStream BO		= null;
		BufferedInputStream BI		= null;
		Socket socket				= null;
		
	    try {
	    	socket = new Socket(IP, PORT);
	    	
	    	BO = new BufferedOutputStream( socket.getOutputStream() );
	    	BO.write( stream.getStream() );
	    	BO.flush();
	      
	    	BI = new BufferedInputStream( socket.getInputStream() );
	    	
	    	String ret 	= getString(BI, 2);
			int nLen 	= Integer.parseInt(getString(BI, 12));
	    	
	    	if (ret.charAt(1) != 'T') {
	    		ERR_MSG = getString(BI, nLen);
	    		return false;
	    	}
	    	PARSER.setStringXml(getString(BI, nLen));
	    	if(PARSER.getValListByNode("//Return/Code").get(0).equalsIgnoreCase("OK")) {
	    		return true;
	    	} else {
	    		return false;	
	    	}
	    } catch( Exception e ) {
	    	ERR_MSG = "Download fail. "+e.toString();
	    	return false;
	    } finally {
	    	try {
	    		if(BI!=null)		BI.close();
				if(BO!=null)		BO.close();
				if(socket!=null)	socket.close();	
	    	} catch(Exception e) {
	    		
	    	}
	    }
	}

	private String getString(BufferedReader BR, int idx) {

		StringBuffer sb = new StringBuffer();
		try {
			int c = 0;
			for(int i=0;i<idx;){
				if((c=BR.read())!=-1) {
					sb.append(String.valueOf((char)c));	
				}
				i+=getHanLen(c);
			}
		} catch (Exception e) {
		}
		return sb.toString();
	}
	
	private String getString(BufferedInputStream BI, int idx) {

		byte[] c = new byte[idx];
		try {
			BI.read(c);
		} catch (Exception e) {
		}
		return new String(c);
	}
	
	public int getHanLen(int c) {
		int len = 1;
		try {
			len = String.valueOf((char)c).getBytes("UTF-8").length;	
		} catch(Exception e) {
		}
		return len;
	}
}

