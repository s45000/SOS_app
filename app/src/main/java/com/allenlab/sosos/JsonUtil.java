package com.allenlab.sosos;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtil {
    public static String toJSon(Note note) {
        try {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("sensitive", note.getSensitive());
            jsonObj.put("password", note.getPassword());
            jsonObj.put("phone1", note.getFirstPhone());
            jsonObj.put("phone2", note.getSecondPhone());
            jsonObj.put("phone3", note.getThirdPhone());
            jsonObj.put("tut",note.getTutorial());
            return jsonObj.toString();
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
