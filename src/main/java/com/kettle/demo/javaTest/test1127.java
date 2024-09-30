package com.kettle.demo.javaTest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.kettle.demo.response.kettleResponse;
import com.kettle.demo.utils.HttpClientUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kettle.demo.javaTest.diaoyong.doPost;
import static com.kettle.demo.javaTest.diaoyong.getToken;

/**
 * 渭南外网测试调用数据
 * authUrl = https://weinan.wit-health.net/api-gate/auth/auth/token?secret= secret &clientId= clientId
 * infoUrl = https://weinan.wit-health.net/api-gate/ehr/ehr/access
 */

public class test1127 {
    public static void main(String[] args) throws IOException {

        String secret = "8fcf8e0c97244b23aa029342dcea92a5";
        String clientId = "q_client25";
        String authUrl = "https://weinan.wit-health.net/api-gate/";
        String infoUrl = "https://weinan.wit-health.net/api-gate/ehr/ehr/access";
        String token = getToken(authUrl, secret, clientId);


        Map<String, Object> transformMap = new HashMap<>();
        transformMap.put("idType", "01");
        transformMap.put("idCard", "610202195005243213");
        transformMap.put("database", "dzbl");
        List<String> list = Arrays.asList("emrpif", "emrhif");
        transformMap.put("tables", list);
        if (token != null) {
            kettleResponse kettleResponse = doPost(infoUrl, token, JSON.toJSONString(transformMap, SerializerFeature.WriteMapNullValue));
            System.out.println(kettleResponse);
        }

    }


}
