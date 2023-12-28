package com.kettle.demo.javaTest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.kettle.demo.response.kettleResponse;
import com.kettle.demo.utils.HttpClientUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 渭南内网测试调用数据
 */



public class diaoyong {
    public static void main(String[] args) throws IOException {
// http://10.80.131.129/api-gate/zuul
        String secret="b9978f7c0ee5480bb432a5b7ba1b2827";
        String clientId="q_client15";
//        String authUrl="http://10.80.116.73/api-gate";
        String authUrl="http://10.0.108.51:18010";  //本地测试

        String infoUrl="http://10.80.116.73/api-gate/ehr/ehr/access";

        String token = getToken(authUrl, secret, clientId);
        System.out.println("token:  "+token);
        Map<String, Object> transformMap = new HashMap<String, Object>();
        transformMap.put("idType", "03");
        transformMap.put("idCard", "622301195208140815");
        transformMap.put("database", "dzbl");
        List<String> list = Arrays.asList("emrpif", "emrhif");
        transformMap.put("tables", list);
        if (token != null) {
            kettleResponse kettleResponse = doPost(infoUrl, token, JSON.toJSONString(transformMap, SerializerFeature.WriteMapNullValue));
            System.out.println(kettleResponse);
        }

    }


    public static String getToken(String baseUrl, String secret, String clientId) throws IOException {

        try {
            String TokenUrl = baseUrl + "/auth/auth/token?secret=" + secret + "&clientId=" + clientId;
            kettleResponse kettleResponse = doPost(TokenUrl, null, null);  //获取token接口
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




    public static kettleResponse doPost(String url, String token, String jsonStr) throws IOException {

        kettleResponse kettleResponse = new kettleResponse();

        HttpClient httpClient = new HttpClient();

        httpClient.getHttpConnectionManager().getParams()
                .setConnectionTimeout(120 * 1000);
        httpClient.getHttpConnectionManager().getParams().setSoTimeout(120 * 1000);
        PostMethod postMethod = new PostMethod(url);
        postMethod.addRequestHeader("accept", "*/*");
        postMethod.addRequestHeader("connection", "Keep-Alive");
        postMethod.addRequestHeader("Content-Type", "application/json");
        if (token != null) {
            postMethod.addRequestHeader("AIIT-ZHYL-AUTH", token);
        }

        if (jsonStr != null) {
            RequestEntity requestEntity = new StringRequestEntity(jsonStr, "application/json", "UTF-8");
            postMethod.setRequestEntity(requestEntity);
        }

        int statusCode = httpClient.executeMethod(postMethod);
        kettleResponse.setCode(statusCode);

        InputStream inputStream = postMethod.getResponseBodyAsStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8),5*1024*1024);
        StringBuilder stringBuilder = new StringBuilder();
        String str;
        while ((str = br.readLine()) != null) {
            stringBuilder.append(str);
        }
        br.close();
        String log = stringBuilder.toString();


        kettleResponse.setData(log);
        return kettleResponse;
    }






}
