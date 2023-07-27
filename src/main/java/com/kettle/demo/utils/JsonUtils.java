package com.kettle.demo.utils;

//import net.sf.json.JSONObject;

import com.alibaba.fastjson.JSONObject;
//import java.util.Map;

/**
 * 字符串转json
 */

public class JsonUtils {
//    public static JSONObject toJson(Map map){
//        JSONObject jsonObject = JSONObject.fromObject(map);
//        return jsonObject;
//    }


    public static JSONObject String2JsonObject(String s){

        JSONObject jsonObject = JSONObject.parseObject(s);
        return jsonObject;
    }
}
