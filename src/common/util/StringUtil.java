package common.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.MaskFormatter;

import common.com.Trace;

public class StringUtil {
	
	final static int CURRENCY 	= 1001;
	final static int DATE 		= 1002;
	final static int NUMBER 	= 1003;
	
	/**
	 * NullChange
	 * 
	 * @return Convert String null to blank and return. 
	 */
	public static String nvl(String val) {
		return val==null?"":val; 
	}

	/**
	 * NullChange
	 * 
	 * @return Convert String null to blank and return. 
	 */
	public static int parseInt(String val, int nDefault) {
		int nRet = 0;
		try {
			nRet = Integer.parseInt(val);
		} catch(Exception e) {
			nRet = nDefault;
		}
		return nRet;
	}
	
	/**
	 * String Null Check
	 * 
	 * @param val
	 * @return is null true:false
	 */
	public static boolean isNull(String val) {
		return val==null?true:false;
	}

	/**
	 * String Blank Check
	 * 
	 * @param val
	 * @return is "" true:false
	 */
	public static boolean isBlank(String str) {
		if(str == null || str.replaceAll("\\p{Z}", "").length()<=0)
		{
			return true;
		}
		return false;
	}
	
	public static HashMap<String, String> failWithMsg(Trace TRACE, HashMap<String, String> mapRes, String strMsg) {
		
		TRACE.TraceLog(strMsg, Trace.LOG_DEBUG);
		
		String strJob 		= mapRes.get("JOB");	
		
		if("DELETE".equalsIgnoreCase(strJob)) {
			mapRes.put("SE_FLAG", "99");
			mapRes.put("MSG", strMsg);
		} else if("CRAWL".equalsIgnoreCase(strJob)) {
			mapRes.put("SE_FLAG", "09");
			mapRes.put("MSG", strMsg);
		} else if("INDEX".equalsIgnoreCase(strJob)) {
			mapRes.put("SE_FLAG", "19");
			mapRes.put("MSG", strMsg);
		} else if("BATCH".equalsIgnoreCase(strJob)) {
			mapRes.put("MSG", strMsg);
		}
		
//		mapRes.put("SE_FLAG", "09");
//		mapRes.put("MSG", strMsg);
		
		return mapRes;
	}
	
	public static HashMap<String, String> failWithMsg(Trace TRACE, int TraceLevel,  HashMap<String, String> mapRes, String strMsg) {
		
		TRACE.TraceLog(strMsg, Trace.LOG_DEBUG);
		
		String strJob 		= mapRes.get("JOB");	
		
		if("DELETE".equalsIgnoreCase(strJob)) {
			mapRes.put("SE_FLAG", "99");
			mapRes.put("MSG", strMsg);
		} else if("CRAWL".equalsIgnoreCase(strJob)) {
			mapRes.put("SE_FLAG", "09");
			mapRes.put("MSG", strMsg);
		} else if("INDEX".equalsIgnoreCase(strJob)) {
			mapRes.put("SE_FLAG", "19");
			mapRes.put("MSG", strMsg);
		} else if("BATCH".equalsIgnoreCase(strJob)) {
			mapRes.put("MSG", strMsg);
		}
		
//		mapRes.put("SE_FLAG", "09");
//		mapRes.put("MSG", strMsg);
		
		return mapRes;
	}
	
	/**
	 * Cut the size by the right side of the string.
	 * 
	 * @param str
	 *            input string
	 * @param size
	 *            return size
	 * @return String
	 */
	public static String Right(String str, int size) {
		int tmpStringLength = str.length();

		if (size >= tmpStringLength) {
			return str;
		}

		return str.substring(tmpStringLength - size, str.length());
	}

	/**
	 * Cut the size by the right side of the string.
	 * 
	 * @param str
	 *            input string
	 * @param size
	 *            return size
	 * @return String
	 */
	public static String Left(String str, int size) {
		int tmpStringLength = str.length();

		if (size >= tmpStringLength) {
			return str;
		}

		return str.substring(0, size);
	}
	
	/**
	 * Cut the size by the offset the string.
	 * 
	 * @param str
	 *            input string
	 * @param offSet
	 *            start index
	 * @param size
	 *           return size
	 * @return String
	 */
	public static String Mid(String str, int offSet, int size) {
		int tmpStringLength = str.length();
		if(offSet>0)
			offSet-=1;
		if (offSet+size >= tmpStringLength) {
			return str.substring(offSet, tmpStringLength);
		}

		return str.substring(offSet, offSet+size);
	}

	/**
	 * Check whether Hangul included.
	 * 
	 * @param str
	 *            input String
	 * @return number of Hangul character
	 */
	public static int checkHangul(String str) {
		int cnt = 0;

		if (isBlank(str)) {
			return 0;
		}

		int index = 0;

		while (index < str.length()) {
			if (str.charAt(index++) >= 256) {
				cnt++;
			}
		}

		return cnt;
	}

	/**
	 * SubString by byte unit
	 * 
	 * @param src
	 *            input String
	 * @param beginIndex
	 *            start byte index
	 * @param endIndex
	 *            end byte index
	 * @return String
	 */
	public static String byteSubString(String src, int beginIndex, int endIndex) {
		if (StringUtil.isBlank(src)) {
			return "";
		}

		byte[] value = src.getBytes();

		if (beginIndex < 0) {
			throw new StringIndexOutOfBoundsException(beginIndex);
		}
		if (endIndex > value.length) {
			throw new StringIndexOutOfBoundsException(endIndex);
		}
		if (beginIndex > endIndex) {
			throw new StringIndexOutOfBoundsException(endIndex - beginIndex);
		}
		byte[] tmpByte = new byte[endIndex - beginIndex];
		System.arraycopy(value, beginIndex, tmpByte, 0, tmpByte.length);

		return new String(tmpByte);
	}

	/**
	 * Check whether Patterns are included.
	 * 
	 * @param val
	 *            input string
	 * @param sPattern
	 *            pattern (ex, numper = "0123456789")
	 * @return true/false
	 */
	public static boolean isPattern(String val, String sPattern) {
		for (int i = 0; i < val.length(); i++) {
			if (sPattern.indexOf(val.charAt(i)) < 0) {
				return false;
			}
		}
		return true;
	}

	public static byte[] hexFromString(String hex) {
		int len = hex.length();
		int bufLen = (len + 1) / 2;
		byte[] buf = new byte[bufLen];

		int i = 0, j = 0;
		i = len % 2;
		if (i == 1) {
			buf[j++] = (byte) fromDigit(hex.charAt(i++));
		}
		while (i < len) {
			buf[j++] = (byte) ((fromDigit(hex.charAt(i++)) << 4) | fromDigit(hex
					.charAt(i++)));
		}
		return buf;
	}

	public static int fromDigit(char ch) {
		if (ch >= '0' && ch <= '9') {
			return ch - '0';
		}
		if (ch >= 'A' && ch <= 'F') {
			return ch - 'A' + 10;
		}
		if (ch >= 'a' && ch <= 'f') {
			return ch - 'a' + 10;
		}

		throw new IllegalArgumentException("invalid hex digit '" + ch + "'");
	}

	/**
	 * Read the content of the given URL and return it as a string.
	 * 
	 * @param url
	 * @return
	 */
	public static String urlToString(URL url) throws IOException {
		StringBuffer sb = new StringBuffer("");
		InputStream is = url.openStream();
		int n = 0;
		do {
			n = is.read();
			if (n >= 0) {
				sb.append((char) n);
			}
		} while (n >= 0);
		is.close();
		return sb.toString();
	}

	public static String getEncoding() {
		String strTest = "���ڵ�";
		try {
			if((new String(strTest.getBytes(),"EUC-KR")).equals("���ڵ�")) {
				return "EUC-KR";
			}
			if((new String(strTest.getBytes(),"UTF-8")).equals("���ڵ�")) {
				return "UTF-8";
			}
			if((new String(strTest.getBytes(),"ISO-8859-1")).equals("���ڵ�")) {
				return "ISO-8859-1";
			}
			return "EUC-KR";
		} catch(Exception e) {
			return "EUC-KR";
		}
	}
	
	public static String makeRandomDigit() {
		String rtn = null;
		try {
			char[] retChar = new char[5];
			for (int i = 0; i < retChar.length; i++) {
				double randomDigit = Math.random() * (double) 25.0 % 25.0;
				retChar[i] = (char) (randomDigit + 65);
				if ((char) retChar[i] < 'A' || (char) retChar[i] > 'Z') {
					throw new Exception("Unexpected text : " + retChar[i]);
				}
			}
			rtn = new String(retChar);
		} catch (Exception e) {
			e.printStackTrace(System.out);
			return "";
		}
		return rtn;
	}
	
	/**
	 * Apply the Currency format to strString.
	 * 
	 * @param strString
	 *            String to which format applies
	 * @return Converted string
	 */
	public static String getStringCurrency(String strString) {
		try {
			long num = Long.parseLong(strString);
			NumberFormat formatter2 = NumberFormat.getNumberInstance();
			return formatter2.format(num);	
		} catch(Exception e) {
			return strString;
		}
	}
	
	/**
	 * Apply the format to strString.
	 * 
	 * @param strString
	 *            String to which format applies
	 * @param format
	 *            FORMAT (ex, String:"AA-AA", Number:"##-##")
	 * @return Converted string
	 */
	public static String getStringFormat(String strString, String format ) {
		try {
			MaskFormatter fmt = new MaskFormatter(format);
			fmt.setValueContainsLiteralCharacters(false);
			return fmt.valueToString(strString);	
		} catch(Exception e) {
			return "";
		}
	}
	
	/**
	 * Fill it with the specified characters on the left or right side.
	 * 
	 * @param strString
	 *            String to fill
	 * @param cDefault
	 *            Specified characters
	 * @param nLength
	 *            String length
	 * @param bLeftAlign
	 *            true:Left alignment, false:Right alignment
	 * @return Converted string
	 */
	public static String getStringDefault(String strString, char cDefault, int nLength, boolean bLeftAlign) {
		char[] arrData = new char[nLength];
		for( int i=0; i<nLength; i++ ) {
			arrData[i] = cDefault;
		}
		
		int nVal = strString.length();
		
		if( nVal>0 ) {
			if( bLeftAlign ) {
				for( int i=0; i<nVal&&i<nLength; i++ ) 
					arrData[i] = strString.charAt(i);
			} else {
				for( int i=nLength, j=nVal; j>0&&i>0 ;i--, j--)
						arrData[i-1] = strString.charAt(j-1);
			}
		}
		return new String(arrData);
	}
	
	/**
	 * Remove duplicate patterns
	 * 
	 * @param strString
	 *            String to remove
	 * @param syntax
	 *            duplicate patterns
	 * @return Converted string
	 */
    public static String remDuplicatePattern(String strString, String syntax) {
    	String duplicatePattern 	= "(["+syntax+"]){1,}";
        Pattern p					= Pattern.compile(duplicatePattern);
        Matcher m 					= p.matcher(strString);
        while (m.find()) {
        	strString = strString.replaceFirst(m.group().toString(), syntax);
        }
        return strString;
    }
    
    /**
	 * find contain String number
	 * 
	 * @param strString
	 *            String
	 * @param syntax
	 *            pattern String
	 * @return contain number
	 */
    public static int getContainString(String strString, String syntax) {
    	int nRet = 0;
    	Pattern pattern = Pattern.compile(syntax);            
        Matcher matcher = pattern.matcher(strString);
        while (matcher.find()) {
        	nRet++;
        }
        return nRet;
    }
    
    public static String encode( String s) {

    	StringBuffer uni_s = new StringBuffer();
    	String temp_s = null;
    	
    	for( int i=0 ; i < s.length() ; i++) {
    	    temp_s = Integer.toHexString( s.charAt(i) );
    	    uni_s.append("\\u"+(temp_s.length()==4 ? temp_s : "00" + temp_s ) );
    	}
    	return uni_s.toString();
    }
    
    public static String decode( String uni){
    	StringBuffer str = new StringBuffer();
    	for( int i= uni.indexOf("\\u") ; i > -1 ; i = uni.indexOf("\\u") ){
    	    str.append( uni.substring( 0, i ) );
    	    str.append( String.valueOf( (char)Integer.parseInt( uni.substring( i + 2, i + 6 ) ,16) ) );
    	    uni = uni.substring( i +6);
    	}
    	str.append( uni );
    	return str.toString();
    }
}
	

	 

