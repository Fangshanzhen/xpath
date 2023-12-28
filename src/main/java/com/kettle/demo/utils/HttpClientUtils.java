package com.kettle.demo.utils;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kettle.demo.response.kettleResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HttpClientUtils {


    public static kettleResponse doPost(String url, String token, String jsonStr) throws IOException {

        kettleResponse kettleResponse = new kettleResponse();

        LogChannelFactory logChannelFactory = new org.pentaho.di.core.logging.LogChannelFactory();
        LogChannel kettleLog = logChannelFactory.create("接口返回结果");

        HttpClient httpClient = new HttpClient();
//        httpClient.setConnectionTimeout(60 * 1000);
//        httpClient.setTimeout(60*1000);
        httpClient.getHttpConnectionManager().getParams()
                .setConnectionTimeout(120 * 1000);
        httpClient.getHttpConnectionManager().getParams().setSoTimeout(120 * 1000);
        PostMethod postMethod = new PostMethod(url);
        postMethod.addRequestHeader("accept", "*/*");
        postMethod.addRequestHeader("connection", "Keep-Alive");
        postMethod.addRequestHeader("Content-Type", "application/json");
        postMethod.addRequestHeader("AIIT-ZHYL-PLATFORM", "13");

        if (token != null) {
            postMethod.addRequestHeader("AIIT-ZHYL-AUTH", token);
        }
        //   kettleLog.logBasic("--------jsonStr-----------"+jsonStr);
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
        kettleLog.logBasic("---调用上传数据接口成功1--- ");
        while ((str = br.readLine()) != null) {
            stringBuilder.append(str);
        }
        br.close();
        String log = stringBuilder.toString();

        if (log.contains("import error collection")) {
            kettleLog.logBasic("该表已完成传输或空表！");
        } else if (log.contains("accessToken")) {
            kettleLog.logBasic("accessToken: " + log);
        } else {
//            kettleLog.logBasic(log);
        }
//        kettleLog.logBasic("--------log-----------"+log);

//        if(log.contains("valid rule failed") && !log.contains("accessToken")) {
//            JSONObject jsonObject = JSON.parseObject(log);
//            List<String[]> data = new ArrayList<>();
//            if (jsonObject.get("code").equals(0)) {
//                JSONArray jsonArray = jsonObject.getJSONArray("data");
//                if (jsonArray.size() > 0) {
//                    for (int i = 0; i < jsonArray.size(); i++) {
//                        List<String> list = new ArrayList<>();
//                        JSONObject a = (JSONObject) jsonArray.get(i);
//                        list.add((String) a.get("id"));
//                        JSONObject errorLog = (JSONObject) a.get("errorLog");
//
//                        String errorMsg = (String) errorLog.get("errorMsg");
//                        String errorType = (String) errorLog.get("errorType");
//
//                        if (errorMsg != null && errorMsg.contains("field")) {
//                            Pattern pattern = Pattern.compile("valid rule failed, field (.*) value");
//                            Matcher matcher = pattern.matcher(errorMsg);
//                            if (matcher.find()) {
//                                list.add(matcher.group(1)); //group(0)表示匹配到的子字符串,group(1)表示匹配到的子字符串的第一组
//                            }
//                        }
//                        if (errorMsg != null && errorMsg.contains("value")) {
//                            Pattern pattern = Pattern.compile(" value:(.*)");
//                            Matcher matcher = pattern.matcher(errorMsg);
//                            if (matcher.find()) {
//                                list.add(matcher.group(1).replace("(", "").replace(")", ""));
//                            }
//                        }
//                        list.add(errorType.substring(37));
//                        String[] xx = new String[4];
//                        data.add(list.toArray(xx));
//
//                        System.out.println(list);
//
//                    }
//                }
//
//            }
//            CsvUtils.appendCSV("C:\\Users\\Administrator\\Desktop\\test.csv", data);
//        }

        kettleResponse.setData(log);
        return kettleResponse;
    }

}