import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;

public class test {

	public static void main(String [] args) {
		
		Map<String, String> map = new HashMap<String, String>();

		map.put("FILE_NAME","nbox/ㄱㅣㅁㅈㅣㅇㅏㄴ.pdf");
		map.put("INDEX_DOC_NAME","20180413180249A119.PDF");
		map.put("FOLDER_IDX","167Xyagu2A6BF0L3IZ1/GSJ16Y320000AL1YL1");
		map.put("REG_DATE","20180413");
		map.put("FILE_CODE","1304201800014837");
		map.put("DOC_IRN","HSJ1IQ420000GXTTF1");
		map.put("REG_TIME","180250");
		map.put("OFFICE_SE_FLAG","IS");
		map.put("INDEX_KEY","1304201800014837HSJ1IQ420000GXTTF1");
		map.put("FILE_TYPE","pdf");
		map.put("FILE_OWNER","yaguboo20050");
		map.put("REG_USER","yaguboo20050");
		map.put("FILE_DETAIL","123");
		map.put("FILE_VERSION","1");
		map.put("FILE_STATUS","1");
		map.put("STATUS","1");
		
		test t = new test();
		t.Get_Json("20180413180249A119.json", map);
	}
	
	@SuppressWarnings("unchecked")
	private String Get_Json(String json_name, Map<String, String> map) {
		String strRet = "";
		JSONObject SubObj = new JSONObject();
		try	{
			strRet = "D:/"+json_name;
			Iterator<String> keys = map.keySet().iterator();
			
			while (keys.hasNext()) {
				String key = (String)keys.next();
				String val = map.get(key);
     			SubObj.put(key, val);
			}
			
			FileWriter file = new FileWriter(strRet);
	        file.write(SubObj.toJSONString());
	        file.flush();
	        file.close();
		} catch (Exception e) {
			return "";
		}
		return strRet;
	}
}
