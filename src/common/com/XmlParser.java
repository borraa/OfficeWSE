package common.com;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlParser implements Parser {
	
	Document DOC	= null;
	XPath XPATH 	= null;
	
	public XmlParser() {}
	
	public boolean setParser(String type, String name) {
		return setXml(new parserPath().getPath(type,name));
	}
	
	public boolean setStreamXml(InputStreamReader xml) {
		XPATH 	= XPathFactory.newInstance().newXPath();
		return StreamXML(xml);
	}
	
	public boolean setStringXml(String xml) {
		XPATH 	= XPathFactory.newInstance().newXPath();
		return StringXML(xml);
	}
	
	public boolean setByteXml(byte[] xml) {
		XPATH 	= XPathFactory.newInstance().newXPath();
		return ByteXML(xml);
	}
	
	public boolean setXml(String xml) {
		XPATH 	= XPathFactory.newInstance().newXPath();
		return parseXML(xml);
	}
	
    public boolean getNodeList(String node, ArrayList<Map<String, String>> arrInput) {
		
		try {
			NodeList cols = (NodeList)XPATH.evaluate(node, DOC, XPathConstants.NODESET);	
			for( int idx=0; idx<cols.getLength(); idx++ ) {
	        	NodeList childCols = cols.item(idx).getChildNodes();
	        	Map<String, String> map = new HashMap<String, String>();
	        	for(int i=0;i<childCols.getLength();i++) {
	        		String strCol = childCols.item(i).getNodeName();
					String strVal = childCols.item(i).getTextContent();
					if(strVal==null)
						strVal = "";
					map.put(strCol.toUpperCase(), strVal);	
	        	}
	        	arrInput.add(map);
		    }
		} catch(Exception e) {
			return false;
		}
		return true;
	}
    
	public NodeList getNodeList(String exp) {
    	String path	= exp.equals("")?"//*":exp;
    	try {
    		return (NodeList)XPATH.evaluate(path, DOC, XPathConstants.NODESET);
    	} catch(Exception e) {
    		return null;
    	}
    }
    
    /*getValue*/
    
    /**
	 * Get value list by node.
	 * 
	 * @param exp
	 * @return List<String> value
	 */
	public List<String> getValListByNode(String exp) {
    	List<String> list = new ArrayList<String>();
    	try {
    		NodeList cols = getNodeList(exp);
            for( int idx=0; idx<cols.getLength(); idx++ ){
            	String val = cols.item(idx).getTextContent();
            	list.add(val==null?"":val);
            }
    	} catch(Exception e) {
    		return null;
    	}
    	return list;
    }
    
    /*getAttribute value*/
    
    /**
	 * Get attribute value list 
	 * 
	 * @param exp, attId
	 * @return List<String> attribute value
	 */
	public List<String> getAttValueList(String exp, String attId) {
    	List<String> list = new ArrayList<String>();
    	try {
    		NodeList cols = getNodeList(exp);
    		for( int idx=0; idx<cols.getLength(); idx++ ) {
    			for(int i=0;i<cols.item(idx).getAttributes().getLength();i++) {
        			if(cols.item(idx).getAttributes().item(i).getNodeName().equalsIgnoreCase(attId)) {
        				String val = cols.item(idx).getAttributes().item(i).getNodeValue();
    	            	list.add(val==null?"":val);
        			}
        		}
            }
    	} catch(Exception e) {
    		return null;
    	}
    	return list;
    }
    
    /*getAttribute*/
    
    /**
	 * Get attribute value list 
	 * 
	 * @param exp, attId
	 * @return List<String> attribute value
	 */
	public ArrayList<Map<String,String>> getAttList(String exp) {
    	ArrayList<Map<String,String>> list = new ArrayList<Map<String,String>>();
    	try {
    		NodeList cols = getNodeList(exp);
    		for( int idx=0; idx<cols.getLength(); idx++ ) {
    			Map<String, String> map = new HashMap<String, String>();
    			for(int i=0;i<cols.item(idx).getAttributes().getLength();i++) {
    				String col = cols.item(idx).getAttributes().item(i).getNodeName();
    				String val = cols.item(idx).getAttributes().item(i).getNodeValue();
    				map.put(col==null?"":col, val==null?"":val);
        		}
    			list.add(map);
            }
    	} catch(Exception e) {
    		return null;
    	}
    	return list;
    }
	
	public String getString(String strSection, String strKey) 						{return "";}
	public String getString(String strSection, String strKey, String strDefault) 	{return "";}
	public int getInt(String strSection, String strKey) 							{return 0;}
	public int getInt(String strSection, String strKey, int nDefault) 				{return 0;}
	
    private boolean parseXML(String path) {
	    try {
        	File file = new File(path);
        	DocumentBuilderFactory objDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder objDocumentBuilder = objDocumentBuilderFactory.newDocumentBuilder();
        	DOC = objDocumentBuilder.parse(file);
        	
         } catch(Exception ex) {
        	return false;
        }       
        return true;
    }
    
    private boolean StringXML(String xml) {
	   try {
		   	DocumentBuilderFactory objDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder objDocumentBuilder = objDocumentBuilderFactory.newDocumentBuilder();
            DOC = objDocumentBuilder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
            
        } catch(Exception ex) {
        	return false;
        }       
        return true;
    }
    
    private boolean StreamXML(InputStreamReader xml) {
	    try {
        	DocumentBuilderFactory objDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder objDocumentBuilder = objDocumentBuilderFactory.newDocumentBuilder();
            DOC = objDocumentBuilder.parse(new InputSource(xml));
            
        } catch(Exception ex) {
        	return false;
        }       
        return true;
    }
    
    private boolean ByteXML(byte[] xml) {
	    try {
        	DocumentBuilderFactory objDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder objDocumentBuilder = objDocumentBuilderFactory.newDocumentBuilder();
            DOC = objDocumentBuilder.parse(new ByteArrayInputStream(xml));
            
        } catch(Exception ex) {
        	return false;
        }       
        return true;
    }
}