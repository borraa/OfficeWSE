package common.wdclient;

import java.awt.image.BufferedImage;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.SimpleTimeZone;

import javax.imageio.ImageIO;

import common.com.Trace;
import common.util.StringUtil;
import crawler.Crawl;
import delete.Delete;
import index.Index;

public class wdmCom {
	
	public static int getLength(String strInput) {
        int len=0;
 		for(int i=0;i<strInput.length();i++) {
			String temp = strInput.charAt(i)+"";
			if(temp.getBytes().length!=1)   len = len+2;
			else                    		len++;
		}
		return len;
	}
	
	public static String getFilesSize(File[] Files) {		
		int Filesize 		= 0;
		int FilesizeTotal	= 0;
		for(File file:Files) {	
			if (file.exists()) {
		      Filesize = (int) (file.length()/1024);
		      FilesizeTotal = FilesizeTotal + Filesize;
		    }
		}
	    return Integer.toString(FilesizeTotal);
	}
	
	public static String getFileSize(File file) {		
		int Filesize 				= 0;
		if (file.exists()) {
			Filesize = (int) (file.length()/1024);
		}
		return Integer.toString(Filesize);
	}
	
	/**
	 * Update crawling, indexing results to DB.
	 * @param objTigger
	 * @param mapRes
	 * @return
	 */
	public static HashMap<String, String> updateProcResult(Object objTigger, HashMap<String, String> mapVal) {
		
		HashMap<String, String> mapRes	 = new HashMap<String, String>();
		mapRes.put("RESULT", "F");
		mapRes.put("SE_RESULT", mapVal.get("RESULT"));
		mapRes.put("UPDATE", mapVal.get("UPDATE"));
		mapRes.put("MSG", mapVal.get("MSG"));
		
		String strMsg 			= mapVal.get("MSG");
		String strSDocNo 		= mapVal.get("SDOC_NO");
		String strType			= mapVal.get("FILE_FLAG");
		String strSEFlag		= mapVal.get("SE_FLAG");
		String strSDocChkFlag	= mapVal.get("SDOC_CHK");
		String strQuery 		= null;
		
		if("F".equalsIgnoreCase(strSDocChkFlag)) {
			return mapRes;
		}
		
		if(StringUtil.isBlank(strSDocNo)) {
			return mapRes;
		}
		
    	//If trigger is Crawl
    	if(objTigger instanceof Crawl) {

    		try {
    			Crawl crawl 		= (Crawl) objTigger;
    			strQuery = crawl.CRAWL_UPDATE_QUERY;
        		strQuery = strQuery.replaceFirst("\\?", "'"+strSEFlag+"'");
				strQuery = strQuery.replaceFirst("\\?", "getDate()");
				strQuery = strQuery.replaceFirst("\\?", "'"+strMsg+"'");
				strQuery = strQuery.replaceFirst("\\?", "'"+strSDocNo+"'");
				
				if(crawl.Set_Result(strQuery))
				{
					mapRes.put("QUERY", strQuery);
					mapRes.put("RESULT", "T");
				} 
				
    		} catch(Exception e) {
    			return mapRes;
    		}
    	} 
    	
    	return mapRes;
	}
	
	public static String getDateTime(String format) {
		Date dateNow 				= Calendar.getInstance(new SimpleTimeZone(0x1ee6280, "KST")).getTime();
		SimpleDateFormat formatter 	= new SimpleDateFormat(format, Locale.KOREA);
		return formatter.format(dateNow);		
	}
	
	public String getDocirn(String strpartcd,  String struserid) {		
		
		String doc_irn = "";
		
		try	{
		
			long mill 	= System.currentTimeMillis();
			long mic 	= System.nanoTime();
			
			doc_irn 	= Long.toHexString(mill)+(Long.toHexString(mic)).substring((Long.toHexString(mic).length())-4, (Long.toHexString(mic).length()));
			strpartcd 	= StringUtil.getStringDefault(strpartcd, '0', 3, true);
			struserid 	= StringUtil.getStringDefault(struserid, '0', 2, true);
			
		} catch (Exception e) { 
			e.printStackTrace();       
        } 
		
		return (doc_irn+strpartcd+struserid).toUpperCase();
	}
	
	public static String getSdocno(String strpartcd,  String struserid) {
		
		SimpleDateFormat formatter = new SimpleDateFormat ( "yyMMddHHmmss", Locale.KOREA);
		Date currentTime = new Date ( );
		long time = System.currentTimeMillis(); 
		String strMill	=	Long.toString(time);
		String strToday = formatter.format (currentTime).substring(0,6);
		String strTime  = formatter.format (currentTime).substring(6,12);
		
		strpartcd = StringUtil.getStringDefault(strpartcd, '0', 4, true);
		struserid = StringUtil.getStringDefault(struserid, '0', 5, true);
		
		strMill		=	strMill.substring(strMill.length()-2,strMill.length());
		int 	nHh   	= Integer.parseInt(strTime.substring(0, 2));
		int 	nMm   	= Integer.parseInt(strTime.substring(2, 4));
		int 	nSs   	= Integer.parseInt(strTime.substring(4, 6));
		int 	nTime 	= nHh*3600 + nMm*60 + nSs;
		String 	strHxtime = int_2_36str( nTime );                                          
		String sdocno = strToday + strpartcd + struserid + strHxtime + strMill;
		
		return sdocno;
	}
	
	private static String int_2_36str( int value ) {		
		String	str36Digit = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String	strResult  = "";
		
		while(value > 0)
		{
			int	div = value / 36;
			int	mod = value - (div * 36);
			strResult = str36Digit.charAt(mod) + strResult;
			value = div;
		}

		strResult = "0000" + strResult;                      
		strResult = strResult.substring(strResult.length()-4, strResult.length());
		
		return strResult;
	}
	
	public static String getImageSize(File file, int nBit) {
		String ret = "";
		try	{
			BufferedImage bi = ImageIO.read( file );
	
			String width =  Integer.toString(bi.getWidth()*nBit);
			String height =  Integer.toString(bi.getHeight()*nBit);
		
			ret = width + ";" + height;
		} catch(Exception e) {
			System.out.println(e.toString());
			return e.toString();
		}
	
		return ret;
	}
}
