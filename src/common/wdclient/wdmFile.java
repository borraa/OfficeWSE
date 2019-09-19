package common.wdclient;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class wdmFile {
	
	public 	String 	ERR_MSG = null;
	
	private String	IP 		= null;
	private String	KEY 	= null;
	private int	PORT	= 0;
	
	private Map<String, String> META = null;
	
	public wdmFile(String ip, int port, String key) {
		
		KEY 	= key;
		IP 		= ip;
		PORT 	= port;
		META 	= new HashMap<String, String>();
		
	}
	
	private byte[] Export(String idx, String index_table, boolean bClear) {
		
		byte[] buf = null;
		
		if(idx== null || index_table== null || idx.equals("") || index_table.equals("") ) {
			ERR_MSG = "Export fail : invalid parameter";
			return null;
		}
		
		try {
	     
	    	wdmStream wdmstream = new wdmStream();
			wdmSocket wdmsocket = new wdmSocket(IP, PORT);
			
			if(!wdmstream.setKey(KEY)) {
				ERR_MSG = "Export fail : invalid Key";
				return null;
			}
				
			if(!wdmstream.setOp(wdmStream.WDMS)) {
				ERR_MSG = "Export fail : invalid Op";
				return null;
			}
			
			if(!wdmstream.download(idx, index_table)) {
				ERR_MSG = "Export fail : invalid Stream";
				return null;
			}
			
			buf = wdmsocket.requestDownload(wdmstream);
			if(buf==null) {
				ERR_MSG = "Export fail. "+wdmsocket.ERR_MSG;
				return null;
			}
			
			return buf;
			
	    } catch( Exception e ) {
	    	ERR_MSG = "Export fail. "+e.toString();
	    	return null;
	    } finally {
	    	if(bClear)	clearMetaData();
	    }
	}
	
	private boolean Import(String file_path, String index_table, boolean bClear) {
		
		boolean bRet = false;
		
	    try {
	    	
	    	wdmStream wdmstream = new wdmStream();
			wdmSocket wdmsocket = new wdmSocket(IP, PORT);
			
			if(index_table== null || index_table.equals("")) {
				ERR_MSG = "Import fail : invalid parameter";
				return false;
			}
			
			if(META.size()<1) {
				ERR_MSG = "Import fail : invalid meta data";
				return false;
			}
			
			if(!wdmstream.setKey(KEY)) {
				ERR_MSG = "Import fail : invalid Key";
				return false;
			}
				
			if(!wdmstream.setOp(wdmStream.WDMS)) {
				ERR_MSG = "Import fail : invalid Op";
				return false;
			}
			
			if(!wdmstream.upload(META, file_path, index_table)) {
				ERR_MSG = "Import fail : invalid Stream";
				return false;
			}
			
			bRet = wdmsocket.requestUpload(wdmstream);
	    		   
	    } catch( Exception e ) {
	    	ERR_MSG = "Import fail. "+e.toString();
	    	bRet = false;
	    } finally {
	    	if(bClear)	clearMetaData();
	    }
	    return bRet;
	}
	
	private boolean Elimination(String idx, String index_table, boolean bClear) {
		boolean bRet = false;
		
		if(idx== null || index_table== null || idx.equals("") || index_table.equals("") ) {
			ERR_MSG = "Elimination fail : invalid parameter";
			return false;
		}
	    
	    try {
	    	
	    	wdmStream wdmstream = new wdmStream();
			wdmSocket wdmsocket = new wdmSocket(IP, PORT);
			
			if(!wdmstream.setKey(KEY)) {
				ERR_MSG = "invalid Key";
				return false;
			}
				
			if(!wdmstream.setOp(wdmStream.WDMS)) {
				ERR_MSG = "invalid Op";
				return false;
			}
			
			if(!wdmstream.delete(idx, index_table)) {
				ERR_MSG = "invalid Stream";
				return false;
			}
			
			bRet = wdmsocket.requestDelete(wdmstream);
	    		   
	    } catch( Exception e ) {
	    	bRet = false;
	    } finally {
	    	if(bClear)	clearMetaData();
	    }
	    return bRet;
	}
	
	public byte[] Download_Byte(String index_table) {
		byte[] bRet = null;
		try {
			if(index_table== null || index_table.equals("")) {
				ERR_MSG = "Download_Byte fail : invalid parameter";
				return null;
			}
			
			if(META.size()<1) {
				ERR_MSG = "Download_Byte fail : invalid meta data";
				return null;
			}
			
			StringBuffer query = new StringBuffer();
			query.append(" SELECT IDX FROM ");
			query.append("wd_"+index_table);
			query.append(" WHERE 1=1 ");
			for(String col:META.keySet()) {
				query.append(" AND "+col+"=");
				query.append("'"+META.get(col)+"'");
			}
			
			wdmData wdmdata = new wdmData(IP, PORT, KEY);
			
			if(!wdmdata.ExecutQuery(query.toString())) {
				ERR_MSG = "Download_Byte fail : wrong meta data";
				return null;
			}
			
			if(wdmdata.getCount()>1) {
				ERR_MSG = "Download_Byte fail : target file is not one";
				return null;
			}
			
			if(!wdmdata.next()) {
				ERR_MSG = "Download_Byte fail : target file is not exist";
				return null;
			}
			
			bRet = Export(wdmdata.getString("IDX"), index_table, true);
			
		} catch(Exception e) {
			ERR_MSG = "Download_Byte fail : "+e.toString();
		}
		return bRet;
	}
	
	public Map<String, byte[]> Downloads_Byte(String index_table) {
		Map<String, byte[]> map = null;
		try {
			
			map = new HashMap<String, byte[]>();
			
			if(index_table== null || index_table.equals("")) {
				ERR_MSG = "Downloads_Byte fail : invalid parameter";
				return null;
			}
			
			if(META.size()<1) {
				ERR_MSG = "Downloads_Byte fail : invalid meta data";
				return null;
			}
			
			StringBuffer query = new StringBuffer();
			query.append(" SELECT IDX, FILENAME FROM ");
			query.append("wd_"+index_table);
			query.append(" WHERE 1=1 ");
			for(String col:META.keySet()) {
				query.append(" AND "+col+"=");
				query.append("'"+META.get(col)+"'");
			}
			
			wdmData wdmdata = new wdmData(IP, PORT, KEY);
			
			if(!wdmdata.ExecutQuery(query.toString())) {
				ERR_MSG = "Downloads_Byte fail : wrong meta data";
				return null;
			}
			
			if(wdmdata.getCount()==0) {
				ERR_MSG = "Downloads_Byte fail : target file is not exist";
				return null;
			}
			int seq = 0;
			while(wdmdata.next()) {
				String file_name = wdmdata.getString("FILENAME");
				map.put(file_name.equals("")?Integer.toString(++seq):file_name, Export(wdmdata.getString("IDX"), index_table, true));
			}
			
		} catch(Exception e) {
			ERR_MSG = "Downloads_Byte fail : "+e.toString();
		}
		return map;
	}
	
	public String Download_File(String out_path, String index_table) {
		String strRet = "";
		try {
			byte[] b = Download_Byte(index_table);
			if(b==null) {
				return "";
			}
			
			FileOutputStream stream = new FileOutputStream(out_path);
		    stream.write(b);
		    stream.close();
		    strRet = out_path;	
		} catch(Exception e) {
			ERR_MSG = "Download_File fail. "+e.toString();
		}
		return strRet;
	}
	
	public String Download_Files(String out_path, String index_table) {
		String strRet = "";
		try {
			
			File file = new File(out_path);
			
			if(!file.isDirectory()) {
				ERR_MSG = "Download_Files fail : path is not directory";
				return "";
			}
			
			Map<String,byte[]> map = Downloads_Byte(index_table);
			if(map.size()==0) {
				return "";
			}
			
			for(String file_name:map.keySet()) {
				FileOutputStream stream = new FileOutputStream(out_path+file_name);
			    stream.write(map.get(file_name));
			    stream.close();
			}
			strRet = out_path;
		} catch(Exception e) {
			ERR_MSG = "Download_Files fail. "+e.toString();
		}
		return strRet;
	}
	
	public String Download_base64(String index_table) {
		String strRet = "";
		try {
			byte[] b = Download_Byte(index_table);
			if(b==null) {
				return "";
			}
			strRet = javax.xml.bind.DatatypeConverter.printBase64Binary(b);
		    
		} catch(Exception e) {
			ERR_MSG = "Download_base64 fail. "+e.toString();
			return "";
		} 
		return strRet;
	}
	
	public ArrayList<String> Downloads_base64(String index_table) {
		
		ArrayList<String> lRet = null;
		
		try {
			lRet = new ArrayList<String>();
			Map<String,byte[]> map = Downloads_Byte(index_table);
			
			if(map.size()==0) {
				return null;
			}
			
			for(String file_name:map.keySet()) {
				lRet.add(javax.xml.bind.DatatypeConverter.printBase64Binary(map.get(file_name)));
			}
		} catch(Exception e) {
			ERR_MSG = "Downloads_base64 fail. "+e.toString();
			return null;
		} 
		return lRet;
	}
	
	public boolean Upload(String file_path, String index_table) {
		return Import(file_path, index_table, true);
	}
	
	public boolean Delete(String idx, String index_table) {
		return Elimination(idx, index_table, true);
	}	
	
	public boolean Replace(String file_path, String index_table) {
		
		try {
			if(index_table== null || index_table.equals("")) {
				ERR_MSG = "Replace fail : invalid parameter";
				return false;
			}
			
			if(META.size()<1) {
				ERR_MSG = "Replace fail : invalid meta data";
				return false;
			}
			
			StringBuffer query = new StringBuffer();
			query.append(" SELECT IDX FROM ");
			query.append("wd_"+index_table);
			query.append(" WHERE 1=1 ");
			for(String col:META.keySet()) {
				query.append(" AND "+col+"=");
				query.append("'"+META.get(col)+"'");
			}
			
			wdmData wdmdata = new wdmData(IP, PORT, KEY);
			
			if(!wdmdata.ExecutQuery(query.toString())) {
				ERR_MSG = "Replace fail : wrong meta data";
				return false;
			}
			
			if(wdmdata.getCount()!=1) {
				ERR_MSG = "Replace fail : target file is not one";
				return false;
			}
			
			if(!wdmdata.next()) {
				ERR_MSG = "Replace fail : target file is not exist";
				return false;
			}
			
			String idx = wdmdata.getString("IDX");
			
			if(!Elimination(idx, index_table, false)) {
				ERR_MSG = "Replace fail : target delete fail";
				return false;
			}
			
			if(!Import(file_path, index_table, true)) {
				ERR_MSG = "Replace fail : target Upload fail";
				return false;
			}
			
			return true;
			
		} catch(Exception e) {
			ERR_MSG = "Replace fail : "+e.toString();
			return false;
		}
	}
	
	public boolean addMetaData(String col, String val) {
		try {
			META.put(col, val);	
		} catch(Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean clearMetaData() {
		try {
			META.clear();	
		} catch(Exception e) {
			return false;
		}
		return true;
	}

}



