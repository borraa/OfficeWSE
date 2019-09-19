package common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class JsonUtil {
	/**
	 * @param map Map<String, Object>.
	 * @return String.
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject getJsonStringFromMap( Map<String, String> map ) {

		JSONObject json = new JSONObject();
		for( Map.Entry<String, String> entry : map.entrySet() ) {
			String key = entry.getKey();
			Object value = entry.getValue();
			json.put(key, value);
		}
		
		return json;
	}
	
	/**
	 * @param list List<Map<String, Object>>.
	 * @return JSONArray.
	 */
	@SuppressWarnings("unchecked")
	public static JSONArray getJsonArrayFromList( List<Map<String, String>> list ) {

		JSONArray jsonArray = new JSONArray();
		for( Map<String, String> map : list ) {
			jsonArray.add( getJsonStringFromMap( map ) );
		}
		
		return jsonArray;
	}
	
	/**
	 * @param list ArrayList<Map<String, Object>>.
	 * @return JSONArray.
	 */
	@SuppressWarnings("unchecked")
	public static JSONArray getJsonArrayFromList( ArrayList<Map<String, String>> list ) {

		JSONArray jsonArray = new JSONArray();
		for( Map<String, String> map : list ) {
			jsonArray.add( getJsonStringFromMap( map ) );
		}
		
		return jsonArray;
	}
	
	/**
	 * @param list List<Map<String, Object>>.
	 * @return String.
	 */
	@SuppressWarnings("unchecked")
	public static String getJsonStringFromList( List<Map<String, String>> list ) {

		JSONArray jsonArray = new JSONArray();
		for( Map<String, String> map : list ) {
			jsonArray.add( getJsonStringFromMap( map ) );
		}
		
		return jsonArray.toJSONString();
	}

	/**
	 * @param jsonObj JSONObject.
	 * @return String.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getMapFromJsonObject( JSONObject jsonObj ) {

		Map<String, String> map = new HashMap<String, String>();
		String 				key	= null;
		try {
			Iterator<String> itr = jsonObj.keySet().iterator();
			while( itr.hasNext()) {
				key = itr.next();
				map.put(key, (String) jsonObj.get(key));
			}
			
		} catch(Exception e) {
            e.printStackTrace();
        }
        return map;
	}

	/**
	 * @param jsonArray JSONArray.
	 * @return List<Map<String, Object>>.
	 */
	public static List<Map<String, String>> getListMapFromJsonArray( JSONArray jsonArray ) {

		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		
		if( jsonArray != null )
		{
			int jsonSize = jsonArray.size();
			for( int i = 0; i < jsonSize; i++ )
			{
				Map<String, String> map = JsonUtil.getMapFromJsonObject( ( JSONObject ) jsonArray.get(i) );
				list.add( map );
			}
		}
		
		return list;
	}
}
