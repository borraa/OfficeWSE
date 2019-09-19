/*2018/04/19
Moon je sung*/

package search;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.ko.KoreanAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.GroupQueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.WildcardQueryNode;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.QueryBuilder;

import common.com.Parser;
import common.com.XmlParser;

public class Query {
	
//###################################//
//## MEMBER VARIABLE 
//###################################//
	
	private Parser 	SEARCH;
	
	public String 	ERR_MSG = "";
	
//###################################//
//## PUBLIC FUNCTION 
//###################################//
	
	public Query() {
		SEARCH = new XmlParser();
		SEARCH.setParser("xml", "search");
	}
	
	public Query(Parser search) {
		SEARCH = search;
	}
	
	public BooleanQuery Get_Query(String strQuery, Map<String,String> mParam, boolean bAnalyzer) {

		BooleanQuery.Builder retQuery = new BooleanQuery.Builder();
		retQuery.setMinimumNumberShouldMatch(1);
		
		if(strQuery.equalsIgnoreCase("")) {
			strQuery = "";
		}
		
		strQuery = strQuery.trim();
		
		if(!Set_Content_Query(strQuery, retQuery, bAnalyzer)) 		
			return null;
		
		if(!Set_Term_Query(mParam, retQuery)) 						
			return null;
	
		if(!Set_Range_Query(mParam, retQuery))						
			return null;

//		if(!Set_Wildcard_Query(mParam, retQuery)) 					
//			return null;	
////	
//		if(!Set_DefaultTerm_Query(retQuery))					
//			return null;	
		return retQuery.build();
	}
	
//###################################//
//## PRIVATE FUNCTION 
//###################################//
	
	@SuppressWarnings("deprecation")
	private boolean Set_Content_Query(String strQuery, Builder retQuery, boolean bAnalyzer) {
	//	QueryParser parser = null;
		try {
			if(strQuery == null) {
				ERR_MSG = " Set_Content_Query Fail : query is null"; 
				return false;
			}
			
			if(strQuery.equals("")) {
				strQuery = "";
			}
			
			//or
			if(strQuery.indexOf(" ") > -1 && strQuery.indexOf("|") > -1 || strQuery.indexOf("|") > -1) {

				String[] array = strQuery.split("[|]");
				String array2 = "";
				
				for(int i=0;i<array.length;i++) {
					array2 = array[i].trim();

					WildcardQuery query = new WildcardQuery(new Term("SDOC_NAME", "*" + array2 + "*"));
					WildcardQuery query1 = new WildcardQuery(new Term("CONTENTS", "*" + array2 + "*"));
					TermQuery query2 = new TermQuery(new Term("SDOC_NO", array2));	
					
					retQuery.add(query, BooleanClause.Occur.SHOULD);
					retQuery.add(query1, BooleanClause.Occur.SHOULD);
					retQuery.add(query2, BooleanClause.Occur.SHOULD);
					
					//WildcardQuery query = new WildcardQuery(new Term("FILE_NAME", "*" + array2 + "*"));
					//TermQuery query4 = new TermQuery(new Term("FILE_TYPE", array2));
					//retQuery.add(query, BooleanClause.Occur.SHOULD);
					//retQuery.add(query4, BooleanClause.Occur.SHOULD);
				}
			// and
			} else if(strQuery.indexOf(" ") > -1 && strQuery.indexOf("|") == -1) {

				String[] array = strQuery.split(" ");
				
				Builder innerQuery = new BooleanQuery.Builder();
				Builder innerQuery1 = new BooleanQuery.Builder();

				for(int i=0;i<array.length;i++) {
					if(i==0) {
						innerQuery.add(new WildcardQuery(new Term("SDOC_NAME","*"+array[0]+"*")), BooleanClause.Occur.SHOULD);
						innerQuery.add(new WildcardQuery(new Term("CONTENTS","*"+array[0]+"*")), BooleanClause.Occur.SHOULD);
						retQuery.add(innerQuery.build(), BooleanClause.Occur.SHOULD);
					} else if(i > 0) {
						innerQuery1.add(new WildcardQuery(new Term("SDOC_NAME","*"+array[i]+"*")), BooleanClause.Occur.SHOULD);
						innerQuery1.add(new WildcardQuery(new Term("CONTENTS","*"+array[i]+"*")), BooleanClause.Occur.SHOULD);
						retQuery.add(innerQuery1.build(), BooleanClause.Occur.MUST);
					}
					
				}			
				
				//retQuery.add(innerQuery.build(), BooleanClause.Occur.SHOULD);
				
				//innerQuery1.add(new WildcardQuery(new Term("SDOC_NAME","*"+array[1]+"*")), BooleanClause.Occur.SHOULD);
				//innerQuery1.add(new WildcardQuery(new Term("FILE_NAME","*"+array[1]+"*")), BooleanClause.Occur.SHOULD);
				//innerQuery1.add(new WildcardQuery(new Term("CONTENTS","*"+array[1]+"*")), BooleanClause.Occur.SHOULD);
				
				//retQuery.add(innerQuery1.build(), BooleanClause.Occur.MUST);

				//retQuery.add(innerQuery.build(), BooleanClause.Occur.SHOULD);
				//retQuery.add(innerQuery1.build(), BooleanClause.Occur.SHOULD);
				//retQuery.add(innerQuery2.build(), BooleanClause.Occur.SHOULD);
					
			} else {
				WildcardQuery query = new WildcardQuery(new Term("SDOC_NAME", "*"+strQuery+"*"));
				WildcardQuery query1 = new WildcardQuery(new Term("CONTENTS", "*"+strQuery+"*"));
				TermQuery query2 = new TermQuery(new Term("SDOC_NO", strQuery));	
				
				retQuery.add(query, BooleanClause.Occur.SHOULD);
				retQuery.add(query1, BooleanClause.Occur.SHOULD);
				retQuery.add(query2, BooleanClause.Occur.SHOULD);
				
				//WildcardQuery query = new WildcardQuery(new Term("FILE_NAME", "*"+strQuery+"*"));
				//TermQuery query4 = new TermQuery(new Term("FILE_TYPE", strQuery));
				//TermQuery query1 = new TermQuery(new Term("ADD_TITLE", "*"+strQuery+"*"));
				//	TermQuery query2 = new TermQuery(new Term("CONTENTS", "*"+strQuery+"*"));
				//	TermQuery query3 = new TermQuery(new Term("SDOC_NO", "*"+strQuery+"*"));
				//retQuery.add(query, BooleanClause.Occur.SHOULD);
				//retQuery.add(query4, BooleanClause.Occur.SHOULD);
			}

		} catch(Exception e) {
			ERR_MSG = " Set_Content_Query Fail : "+e.toString();
			return false;
		}
		return true;
	}
	
	@SuppressWarnings("deprecation")
	private boolean Set_Term_Query(Map<String,String> mParam, Builder retQuery) {
		
		try {
			List<String> listTermCol = SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='term']", "col");
			for(String strTermCol:listTermCol) {
				
				String strTermVal 			= mParam.get(strTermCol.toUpperCase());
				if("All".equalsIgnoreCase(strTermVal) || ("").equalsIgnoreCase(strTermVal) || null == strTermVal) {
					strTermVal = "";
				}
				String strTermIndex 		= SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='term' and @col='"+strTermCol+"']", "index").get(0);
				String strTermCondition 	= SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='term' and @col='"+strTermCol+"']", "condition").get(0);
				
				if(strTermVal == null || strTermVal.equals("") || strTermVal.equalsIgnoreCase("NULL")) {
					continue;
				}
				
				TermQuery subQuery = new TermQuery(new Term(strTermIndex, strTermVal));
				 
				if(strTermCondition.equalsIgnoreCase("EQUALS"))	{
					retQuery.add(subQuery , BooleanClause.Occur.MUST);
				} else if(strTermCondition.equalsIgnoreCase("NOT_EQUALS"))		{
					retQuery.add(subQuery , BooleanClause.Occur.MUST_NOT);
				} else {
					retQuery.add(subQuery , BooleanClause.Occur.SHOULD);
				}
			}
		}	catch(Exception e)	{
			ERR_MSG = " Set_Term_Query Fail : "+e.toString();
			return false;
		}
		return true;
	}
	
	@SuppressWarnings("deprecation")
	private boolean Set_Range_Query(Map<String,String> mParam, Builder retQuery) {
		
		try {
			
			String strTermIndex = SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='range']", "index").get(0);
			String strTermCol1 = SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='range' and @condition='START']", "col").get(0);
			String strTermCol2 = SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='range' and @condition='END']", "col").get(0);
			
			String strTermVal1 = mParam.get(strTermCol1.toUpperCase());
			String strTermVal2 = mParam.get(strTermCol2.toUpperCase());
			
			if(strTermVal1.indexOf('-') > -1 && strTermVal2.indexOf('-') > -1) {
				strTermVal1 = mParam.get(strTermCol1.toUpperCase()) + " 00:00:00";
				strTermVal2 = mParam.get(strTermCol2.toUpperCase()) + " 23:59:59";
				
				if(strTermVal1 == null || strTermVal1.equals("") || strTermVal1.equalsIgnoreCase("NULL") ||
						strTermVal2 == null || strTermVal2.equals("") || strTermVal2.equalsIgnoreCase("NULL")) {
					return true;
				}
				
				TermQuery query1 = new TermQuery(new Term("REG_TIME", strTermVal1));
				TermQuery query2 = new TermQuery(new Term("REG_TIME", strTermVal2));
				
				retQuery.add(TermRangeQuery.newStringRange(strTermIndex, strTermVal1, strTermVal2, true, true),BooleanClause.Occur.MUST);
				
			} else {
//				String strTermVal1 = mParam.get(strTermCol1.toUpperCase());
//				String strTermVal2 = mParam.get(strTermCol2.toUpperCase());
				
				String strSplitVal1 = strTermVal1.substring(0, 4) + "-" + strTermVal1.substring(4, 6) + "-" + strTermVal1.substring(6, 8) + " 00:00:00";
				String strSplitVal2 = strTermVal2.substring(0, 4) + "-" + strTermVal2.substring(4, 6) + "-" + strTermVal2.substring(6, 8) + " 23:59:59";
				
				if(strTermVal1 == null || strTermVal1.equals("") || strTermVal1.equalsIgnoreCase("NULL") ||
						strTermVal2 == null || strTermVal2.equals("") || strTermVal2.equalsIgnoreCase("NULL")) {
					return true;
				}
				
				TermQuery query1 = new TermQuery(new Term("REG_TIME", strSplitVal1));
				TermQuery query2 = new TermQuery(new Term("REG_TIME", strSplitVal2));
				
				retQuery.add(TermRangeQuery.newStringRange(strTermIndex, strSplitVal1, strSplitVal2, true, true),BooleanClause.Occur.MUST);
			}

		
			
//			if(strTermVal1 == null || strTermVal1.equals("") || strTermVal1.equalsIgnoreCase("NULL") ||
//					strTermVal2 == null || strTermVal2.equals("") || strTermVal2.equalsIgnoreCase("NULL")) {
//				return true;
//			}
//			
//			TermQuery query1 = new TermQuery(new Term("REG_TIME", strSplitVal1));
//			TermQuery query2 = new TermQuery(new Term("REG_TIME", strSplitVal2));

		//	retQuery.add(query1, BooleanClause.Occur.MUST);
		//	retQuery.add(query2, BooleanClause.Occur.MUST);
//			retQuery.add(TermRangeQuery.newStringRange(strTermIndex, strSplitVal1, strSplitVal2, true, true),BooleanClause.Occur.MUST);
			
		} catch(Exception e) {
			ERR_MSG = " Set_Range_Query Fail : "+e.toString();
			return false;
		}
		return true;
	}
	
	@SuppressWarnings("deprecation")
	private boolean Set_Wildcard_Query(Map<String,String> mParam, Builder retQuery) {
		WildcardQuery subject_query	= null;
		try {
			List<String> listTermCol = SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='wild']", "col");
			for(String strTermCol:listTermCol) {
			
				String strTermVal 			= mParam.get(strTermCol.toUpperCase());
				String strTermIndex 		= SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='wild' and @col='"+strTermCol+"']", "index").get(0);
				String strTermCondition 	= SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='wild' and @col='"+strTermCol+"']", "condition").get(0);
				
				if(strTermVal == null || strTermVal.equals("") || strTermVal.equalsIgnoreCase("NULL")) {
					continue;
				}
					
				if(strTermCondition.equalsIgnoreCase("LEFT"))	{
					subject_query = new WildcardQuery(new Term(strTermIndex, strTermVal+"*"));
				} else if(strTermCondition.equalsIgnoreCase("RIGHT"))		{
					subject_query = new WildcardQuery(new Term(strTermIndex, "*"+strTermVal));
				} else if(strTermCondition.equalsIgnoreCase("MID")){
					subject_query = new WildcardQuery(new Term(strTermIndex, "*"+strTermVal+"*"));
				}
				retQuery.add(subject_query , BooleanClause.Occur.MUST);
			}
		}	catch(Exception e)	{
			ERR_MSG = " Set_Wildcard_Query Fail : "+e.toString();
			return false;
		}
		return true;
	}
	
	@SuppressWarnings("deprecation")
	private boolean Set_DefaultTerm_Query(Builder retQuery) {
		
		try {
			List<String> listTermCol = SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='default']", "col");
			for(String strTermCol:listTermCol) {
				
				String strTermVal 			= SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='default' and @col='"+strTermCol+"']", "value").get(0);
				String strTermIndex 		= SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='default' and @col='"+strTermCol+"']", "index").get(0);
				String strTermCondition 	= SEARCH.getAttValueList("//SEARCH/PARAMETER/PARAM[@type='default' and @col='"+strTermCol+"']", "condition").get(0);
				
				TermQuery subject_query = new TermQuery(new Term(strTermIndex, strTermVal));
				if(strTermCondition.equalsIgnoreCase("EQUALS"))
					retQuery.add(subject_query , BooleanClause.Occur.MUST);
				else if(strTermCondition.equalsIgnoreCase("NOT_EQUALS"))		
					retQuery.add(subject_query , BooleanClause.Occur.MUST_NOT);
			}
		}	catch(Exception e)	{
			ERR_MSG = " Set_DefaultTerm_Query Fail : "+e.toString();
			return false;
		}
		return true;
	}
 }
