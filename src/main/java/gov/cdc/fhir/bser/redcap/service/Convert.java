package gov.cdc.fhir.bser.redcap.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Convert {
    public static JSONObject toJson(Map<String, Object> map) throws  JSONException {
        JSONObject jsonObject = new JSONObject();

        for (String key : map.keySet()) {
            
                Object obj = map.get(key);
                if (obj instanceof Map) {
                    jsonObject.put(key, toJson((Map) obj));
                }
                else if (obj instanceof List) {
                    jsonObject.put(key, toJson((List) obj));
                }
                else {
                    jsonObject.put(key, map.get(key));
                }
            }
      

        return jsonObject;
    }

    public static JSONArray toJson(List<Object> list) throws  JSONException {
        JSONArray jsonArray = new JSONArray();

        for (Object obj : list) {
            if (obj instanceof Map) {
                jsonArray.put(toJson((Map) obj));
            }
            else if (obj instanceof List) {
                jsonArray.put(toJson((List) obj));
            }
            else {
                jsonArray.put(obj);
            }
        }

        return jsonArray;
    }

    public static Map<String, Object> fromJson(JSONObject jsonObject) throws  JSONException{
        Map<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keyIterator = jsonObject.keys();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
         
                Object obj = jsonObject.get(key);

                if (obj instanceof JSONObject) {
                    map.put(key, fromJson((JSONObject) obj));
                }
                else if (obj instanceof JSONArray) {
                    map.put(key, fromJson((JSONArray) obj));
                }
                else {
                    map.put(key, obj);
                }
            }
           
       

        return map;
    }

    public static List<Object> fromJson(JSONArray jsonArray) throws  JSONException{
        List<Object> list = new ArrayList<Object>();

        for (int i = 0; i < jsonArray.length(); i++) {
          
                Object obj = jsonArray.get(i);

                if (obj instanceof JSONObject) {
                    list.add(fromJson((JSONObject) obj));
                }
                else if (obj instanceof JSONArray) {
                    list.add(fromJson((JSONArray) obj));
                }
                else {
                    list.add(obj);
                }
           
        }

        return list;
    }
}