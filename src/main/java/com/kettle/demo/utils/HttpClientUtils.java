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
    //       kettleLog.logBasic("--------jsonStr-----------"+jsonStr);  //打印json数据
        if (jsonStr != null) {
            RequestEntity requestEntity = new StringRequestEntity(jsonStr, "application/json", "UTF-8");

            postMethod.setRequestEntity(requestEntity);
        }

        int statusCode = httpClient.executeMethod(postMethod);
        kettleResponse.setCode(statusCode);
        kettleLog.logBasic("---statusCode--- "+ statusCode);

        InputStream inputStream = postMethod.getResponseBodyAsStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8),5*1024*1024);
        StringBuilder stringBuilder = new StringBuilder();
        String str;
        kettleLog.logBasic("---调用数据接口成功1--- ");

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
//          kettleLog.logBasic(log);
        }
        kettleLog.logBasic("--------log-----------"+log);


        kettleResponse.setData(log);
        return kettleResponse;
    }

}