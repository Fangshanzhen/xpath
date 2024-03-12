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

        String secret = "j9q8t5r3b6n7a4l2k0y1v9x8m5c3p7s2";
        String clientId = "q_client41";
        String authUrl = "https://weinan.wit-health.net/api-gate/";
        String infoUrl = "https://weinan.wit-health.net/api-gate/ehr/ehr/access";
        String token = getToken(authUrl, secret, clientId);



        Map<String, Object> transformMap = new HashMap<>();
        transformMap.put("idType", "01");
        transformMap.put("idCard", "610502198808300656");
        transformMap.put("database", "dzbl");
        List<String> list = Arrays.asList("emrpif", "emrhif");
        transformMap.put("tables", list);
        if (token != null) {
            kettleResponse kettleResponse = doPost(infoUrl, token, JSON.toJSONString(transformMap, SerializerFeature.WriteMapNullValue));
            System.out.println(kettleResponse);
        }

    }


}
