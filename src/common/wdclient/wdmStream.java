package common.wdclient;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Random;

import common.util.StringUtil;

public class wdmStream {
	
	private byte[] b 			= null;
	private String KEY 			= null;
	private String OP			= null;
	private String FILEPATH 	= null;
	private byte[] FILESOURCE 	= null;
	
	public final static String CREATE_KEY 		= "C";
	public final static String SELECT_QUERY 	= "S";
	public final static String UPDATE_QUERY 	= "Q";
	public final static String WDMS 			= "W";
	public final static String STATUS 		= "S";
	
	private boolean chkInfo() {
		if(KEY==null || OP==null) 
			return false;
		else
			return true;
	}
	
	private String getTempName() {
		Random r = new Random();
		long l = r.nextLong();
		return Long.toString(l<0?l*-1:l, 16);
	}
	
	public boolean status() {
		try {
			if(b!=null)
				b = null;
			
			String query = "test";
			StringBuffer buff = new StringBuffer();
				buff.append( OP );
				buff.append( KEY );
				buff.append((new DecimalFormat("000000000000")).format(wdmCom.getLength(query)));
				buff.append(query);
				
			b = buff.toString().getBytes(); 
		} catch(Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean docirn(String query) {
		try {
			if(!chkInfo())
				return false;
			if(b!=null)
				b = null;
			
			int Querylength = query.getBytes("UTF-8").length;
			
			StringBuffer buff = new StringBuffer();
				buff.append( OP );
				buff.append( KEY );
				//buff.append((new DecimalFormat("000000000000")).format(wdmCom.getLength(query)));
				buff.append((new DecimalFormat("000000000000")).format(Querylength));
				buff.append(query);	
			b = buff.toString().getBytes(); 
		} catch(Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean query(String query) {
		try {
			if(!chkInfo())
				return false;
			if(b!=null)
				b = null;
			//int oldQueryLength = wdmCom.getLength(query);
			int Querylength = query.getBytes("UTF-8").length;
			StringBuffer buff = new StringBuffer();
				buff.append( OP );
				buff.append( KEY );
				buff.append((new DecimalFormat("000000000000")).format(Querylength));
				buff.append(query);	
			b = buff.toString().getBytes(); 
		} catch(Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean download(String idx, String Table) {
		//WDPS5000                         000000000044op=download;table=GENDOC_T;doc_id=7234567896
		try {
			if(!chkInfo())
				return false;
			if(b!=null)
				b = null;
			StringBuffer _msg = new StringBuffer();
				_msg.append("op=download");
				_msg.append(";table=");
				_msg.append(Table);
				_msg.append(";doc_id=");
				_msg.append(idx);
			
			StringBuffer buff = new StringBuffer();
				buff.append( OP );
				buff.append( KEY );
			    buff.append((new DecimalFormat("000000000000")).format(_msg.length()));
			    buff.append(_msg);	
			b = buff.toString().getBytes();
		} catch(Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean upload(Map<String, String> meta, String file_path, String index_table) {
		//W<--FileName------ ------------->000000000080op=create;table=GENDOC_T;doc_no=0;doc_irn=5234567895;filename=xxx;filesize=26;&&ABCDEFGHIJKLMNOPQRSTUVqqqq
		try {
			if(!chkInfo())
				return false;
			if(b!=null)
				b = null;
			
			FILEPATH = file_path;
			
			StringBuffer _msg = new StringBuffer();
				_msg.append("op=create");
				_msg.append(";table=");
				_msg.append(index_table);
				_msg.append(";filename=");
				_msg.append(getFileName(file_path));
				_msg.append(";filesize=");
				_msg.append((int)new File(file_path).length());
				
			for(String col:meta.keySet()) {
				_msg.append(";"+col+"=");
				_msg.append(meta.get(col));
			}
					
			StringBuffer buff = new StringBuffer();
				buff.append( "W" );
				buff.append( KEY );
				buff.append((new DecimalFormat("000000000000")).format(wdmCom.getLength(_msg.toString())));
				buff.append(_msg);
			
			b = buff.toString().getBytes();
		} catch(Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean upload(String DocIrn, String DocNo, String strFilePath, String Table) {
		//W<--FileName------ ------------->000000000080op=create;table=GENDOC_T;doc_no=0;doc_irn=5234567895;filename=xxx;filesize=26;&&ABCDEFGHIJKLMNOPQRSTUVqqqq
		try {
			if(!chkInfo())
				return false;
			if(b!=null)
				b = null;
			FILEPATH = strFilePath;
			String fileName = strFilePath.lastIndexOf("\\")<0?strFilePath.substring(strFilePath.lastIndexOf("/") +1 ):strFilePath.substring(strFilePath.lastIndexOf("\\") +1 );
			StringBuffer _msg = new StringBuffer()
					.append("op=create")
					.append(";table=")
					.append(Table)
					.append(";doc_no=")
					.append(DocNo)
					.append(";doc_irn=")
					.append(DocIrn)
					.append(";filename=")
					.append(fileName)
					.append(";filesize=")
					.append((int)new File(FILEPATH).length())
					.append(";&&");
					
					StringBuffer buff = new StringBuffer()
				    .append( "W" )
				    .append( KEY )
				    .append((new DecimalFormat("000000000000")).format(wdmCom.getLength(_msg.toString())))
				    .append(_msg);
					b = buff.toString().getBytes();
		} catch(Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean upload(String FileName, String DocIrn, String DocNo, byte[] b, String Table) {
		//W<--FileName------ ------------->000000000080op=create;table=GENDOC_T;doc_no=0;doc_irn=5234567895;filename=xxx;filesize=26;&&ABCDEFGHIJKLMNOPQRSTUVqqqq
		try {
			if(!chkInfo())
				return false;
			if(b!=null)
				b = null;
			FILESOURCE = b;
			StringBuffer _msg = new StringBuffer()
					.append("op=create")
					.append(";table=")
					.append(Table)
					.append(";doc_no=")
					.append(DocNo)
					.append(";doc_irn=")
					.append(DocIrn)
					.append(";filename=")
					.append((FileName==null||FileName.equals(""))?getTempName():FileName)
					.append(";filesize=")
					.append((int)new File(FILEPATH).length())
					.append(";&&");
					
					StringBuffer buff = new StringBuffer()
				    .append( "W" )
				    .append( KEY )
				    .append((new DecimalFormat("000000000000")).format(wdmCom.getLength(_msg.toString())))
				    .append(_msg);
					b = buff.toString().getBytes();
		} catch(Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean delete(String Idx, String Table) {
		try {
			if(!chkInfo())
				return false;
			if(b!=null)
				b = null;
			StringBuffer _msg = new StringBuffer()
					.append("op=delete")
					.append(";table=")
					.append(Table)
					.append(";doc_id=")
					.append(Idx);
			
			StringBuffer buff = new StringBuffer()
					.append( "W" )
				    .append( KEY )
				    .append((new DecimalFormat("000000000000")).format(_msg.length()))
				    .append(_msg);
					b = buff.toString().getBytes(); 
		} catch(Exception e) {
			System.out.println(e.toString());
			return false;
		}
		return true;
	}

	public boolean setKey(String key) {
		if(key.length() > 32) {
			return false;
		} else {
			KEY = StringUtil.getStringDefault(key, ' ', 32, true);
		}
		return true;
	}
	
	public boolean setOp(String op) {
		OP = op;
		return true;
	}
	
	public byte[] getStream() {
		return b;
	}
	
	public String getFilePath() {
		return FILEPATH;
	}
	
	public byte[] getFileSource() {
		return FILESOURCE;
	}
	
	private String getFileName(String file_path) {
		String strRet = "";
		try {
			if(file_path.lastIndexOf("\\")<0) {
				strRet = file_path.substring(file_path.lastIndexOf("/")+1);
			} else {
				strRet = file_path.substring(file_path.lastIndexOf("\\")+1);
			}	
		} catch(Exception e) {
			strRet = "";
		}
		return strRet;
	}
	
	public boolean info(String op, String job) {
		try {
			if(b!=null)
				b = null;
			
			String query = "Op="+op+";Job="+job;
			StringBuffer buff = new StringBuffer();
				buff.append( OP );
				buff.append( KEY );
				buff.append((new DecimalFormat("000000000000")).format(wdmCom.getLength(query)));
				buff.append(query);
				
			b = buff.toString().getBytes(); 
		} catch(Exception e) {
			return false;
		}
		return true;
	}
}
