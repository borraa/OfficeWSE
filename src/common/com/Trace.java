package common.com;
import java.io.*;
import java.util.*;
import java.text.*;

public class Trace {
	
	private String	HEADER 		= null;
	private String	LOGNAME 	= null;
	private String 	WORKDIR 	= null;
	private int	LOGLEVEL	= 0;
	private long	LOGSIZE		= 3000;
	
	public static final int	LOG_DEBUG		= 3;	// print all message
	public static final int	LOG_NORMAL		= 2;	// print method paramters and return value
	public static final int 	LOG_RELEASE	= 1;	// print error message
	
	public Trace( String strPath, int nLevel ) {
		//LOGNAME		= new Path().ProjectName();
		SimpleDateFormat sdfTime 	= new SimpleDateFormat("yyyyMMdd");
		LOGNAME		= sdfTime.format(new Date());
		WORKDIR 	= strPath;
		LOGLEVEL	= nLevel;
		MakeLogDir(WORKDIR);
	}
	
	public Trace( String strPath, int nLevel, int nSize ) {
		//LOGNAME		= new Path().ProjectName();
		SimpleDateFormat sdfTime 	= new SimpleDateFormat("yyyyMMdd");
		LOGNAME		= sdfTime.format(new Date());
		WORKDIR 	= strPath;
		LOGLEVEL	= nLevel;
		LOGSIZE		= nSize;
		MakeLogDir(WORKDIR);
	}
	
	public void setHeader(String strHeader) {
		HEADER = strHeader;
	}
	
	public void TraceBox(String message) {
		//SimpleDateFormat sdfTime 	= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat sdfTime 	= new SimpleDateFormat("HH:mm:ss:SSS");
		String strOutput 			= "[" + sdfTime.format(new Date()) + "]  ";
		
		PrintStream printStream = null;
		try {
			if(message==null||message.equals(""))
				return;
			FileOutputStream outputStream 	= new FileOutputStream(WORKDIR+"/"+LOGNAME+".log", true);
			printStream	= new PrintStream(outputStream);
			printStream.print(strOutput);
			printStream.print(message+"\r\n");
			outputStream.close();  
		} catch(Exception ioe) { 
        	
        } finally { 
            if(printStream!=null) 
            	printStream.close();
            reNameLogFile();
        }
	}
	
    public void TraceLog(String message, int nLevel) {
    	//SimpleDateFormat sdfTime 	= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	SimpleDateFormat sdfTime 	= new SimpleDateFormat("HH:mm:ss:SSS");
		String strOutput 			= "[" + sdfTime.format(new Date()) + "]  ";
    	
		if( nLevel <= LOGLEVEL ) {
			PrintStream printStream = null;
			try {
	        	if(message==null||message.equals(""))
					return;
	        	StringBuffer sb = new StringBuffer();	
	        	//sb.append(getLogTime("yyyy-MM-dd HH:mm:ss")+"\r\n");
	        	//sb.append("info :");
//	        	sb.append(strOutput + "info : ");
//	        	sb.append(HEADER==null?"":" ["+HEADER+"] ");
//	        	sb.append("["+Thread.currentThread().getStackTrace()[2].getMethodName()+"(), ");
//	        	sb.append(Thread.currentThread().getStackTrace()[2].getClassName()+".java:"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"]\r\n");
//	        	sb.append("----------------------------------------------------------------------------------------\r\n");
//	        	sb.append(message+"\r\n");
//	        	sb.append("----------------------------------------------------------------------------------------\r\n\r\n");
	        	sb.append(strOutput + "Error > ");
	        	sb.append(message + "\r\n");
	        	FileOutputStream outputStream 	= new FileOutputStream(WORKDIR+"/"+LOGNAME+".log", true);
	        	printStream	= new PrintStream(outputStream);
	        	printStream.print(sb.toString());
	            outputStream.close();
	        } catch(IOException ioe) { 
	        	
	        } finally { 
	            if(printStream!=null) 
	            	printStream.close(); 
	            reNameLogFile();
	        }
		}
    }
    
    public void TraceLog(Exception exception) {
    	PrintStream printStream = null;
    	try {
    		StringBuffer sb = new StringBuffer();
        	sb.append(getLogTime("yyyy-MM-dd HH:mm:ss")+"\r\n");
        	sb.append("info :");
        	sb.append(HEADER==null?"":" ["+HEADER+"] ");
        	sb.append("["+Thread.currentThread().getStackTrace()[2].getMethodName()+"(), ");
        	sb.append(Thread.currentThread().getStackTrace()[2].getClassName()+".java:"+Thread.currentThread().getStackTrace()[2].getLineNumber()+"]\r\n");
        	sb.append("----------------------------------------------------------------------------------------\r\n");
        	sb.append(exception.getLocalizedMessage()+"\r\n");
        	sb.append("----------------------------------------------------------------------------------------\r\n\r\n");
        	FileOutputStream outputStream 	= new FileOutputStream(WORKDIR+"/"+LOGNAME+".log", true);
        	printStream	= new PrintStream(outputStream);
        	printStream.print(sb.toString());
            outputStream.close();
        } catch(IOException ioe) { 
        	
        } finally { 
            if(printStream!=null) 
            	printStream.close();
            reNameLogFile();
        }
    }
    
	private boolean MakeLogDir(String strDirectory) {
		boolean bOK = false;
		try {
			File dirDest = new File(strDirectory);
			if(!dirDest.exists()) {
				bOK = dirDest.mkdirs();
			} else	bOK = true;
		} catch(Exception e) {
			bOK = false;
		}
		return bOK;
	}
	
	private boolean reNameLogFile() {
		
		int nFileIdx 	= 1;
		File file1 		= new File(WORKDIR+"/"+LOGNAME+".log");
		File file2		= new File(WORKDIR+"/"+LOGNAME+".log."+getLogTime("yyyy-MM-dd"));
		
		try {
			while(file2.exists()) {
				file2 = new File(WORKDIR+"/"+LOGNAME+".log."+getLogTime("yyyy-MM-dd")+"."+nFileIdx);
				nFileIdx++;
			}
			
			if(!chkLogFile()) {
				return true;
			}
			
			if(!file1.renameTo(file2)) {
				return false;
			} 
		} catch(Exception e) {
			return false;
		}	
		return true;
	}
	
	private boolean chkLogFile() {
		boolean bOK = false;
		try {
			File file1 = new File(WORKDIR+"/"+LOGNAME+".log");
			
			if(file1.exists()) {
				if((file1.length())/1000 > LOGSIZE) {
					bOK = true;
				}
			} 
		} catch(Exception e) {
			bOK = false;
		}
		return bOK;
	}
	
	private String getLogTime(String format) {
		SimpleDateFormat sdfDate = new SimpleDateFormat(format);
		return sdfDate.format(new Date());
	}
	
	public static void main(String[] agg) {
		Trace trace = new Trace("C:/test", 3);
		trace.setHeader("192.168.10.19");
		int i = 0;
		while(i<100) {
			trace.TraceLog(trace.getClass().toString(),3);
			i++;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

