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
 */

public class test1127 {
    public static void main(String[] args) throws IOException {

        String secret = "b9978f7c0ee5480bb432a5b7ba1b2827";
        String clientId = "q_client15";
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
