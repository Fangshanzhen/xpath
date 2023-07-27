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

public class diaoyong {
    public static void main(String[] args) throws IOException {
// http://10.80.131.129/api-gate/zuul
        String token = getToken(null, null, null);
        Map<String, Object> transformMap = new HashMap<String, Object>();
        transformMap.put("idType", "01");
        transformMap.put("idCard", null);
        transformMap.put("database", null);
        List<String> list = Arrays.asList("", "");
        transformMap.put("tables", list);
        kettleResponse kettleResponse = HttpClientUtils.doPost(null, token, JSON.toJSONString(transformMap, SerializerFeature.WriteMapNullValue));
        System.out.println(kettleResponse);

    }


    private static String getToken(String baseUrl, String secret, String clientId) throws IOException {

        try {
            String TokenUrl = baseUrl + "/auth/auth/token?secret=" + secret + "&clientId=" + clientId;
            kettleResponse kettleResponse = HttpClientUtils.doPost(TokenUrl, null, null);  //获取token接口
            if (kettleResponse.getCode() == 200) {
                JSONObject jsonObject = JSON.parseObject(kettleResponse.getData());
                JSONObject jsonObject1 = (JSONObject) jsonObject.get("data");
                String accessToken = String.valueOf(jsonObject1.get("accessToken"));
                String expiresIn = String.valueOf(jsonObject1.get("expiresIn"));

                return accessToken;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
