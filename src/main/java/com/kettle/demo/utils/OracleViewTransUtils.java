package com.kettle.demo.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.kettle.demo.constant.Constant;
import com.kettle.demo.response.kettleResponse;
import com.kettle.demo.response.transformResponse;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelFactory;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import static java.util.stream.Collectors.toList;


/**
 * 当数据是Oracle视图时，databaseType填Oracle等相关信息
 * databaseType1 填postgresql相关信息，etl_count表记录数据上报起始与终止时间，tablestatus表记录是否已经同步完成
 * 需要准备2张表，tablestatus， 字段为tablename，status，last_time
 * etl_count 字段为start_time，end_time
 */

public class OracleViewTransUtils {

    public static String transformData(String databaseType, String baseUrl, String dbname, String schema, String ip, String port,
                                       String username, String password,
                                       String secret, String clientId, String num, String tableNameTest,  //oracle

                                       String databaseType1, String dbname1, String schema1, String ip1, String port1,
                                       String username1, String password1 //postgresql
    ) throws Exception {

        LogChannelFactory logChannelFactory = new org.pentaho.di.core.logging.LogChannelFactory();
        LogChannel kettleLog = logChannelFactory.create("上报数据");


        Connection connection = null; //oracle
        Connection connection1 = null; //postgresql   etl_count,tablestatus
        String s = null;

        try {
            connection = JDBCUtils.getConnection(databaseType, ip, port, dbname, schema, username, password);
            connection1 = JDBCUtils.getConnection(databaseType1, ip1, port1, dbname1, schema1, username1, password1);
        } catch (SQLException e) {
            s = "2";
            kettleLog.logError("database connection error", "");
            return s;
        }

        if (connection != null && connection1 != null) {
            kettleLog.logBasic(databaseType +  "数据库连接成功");
            kettleLog.logBasic(databaseType1 + "数据库连接成功");
            try {
                List<String> tableList = new ArrayList<>();
                String tableSql = null;
                String countSql = null;
                countSql = Constant.countSql.replace("@", schema1);
                String timeSql = null;
                if (databaseType.equals("oracle")) {
                    tableSql = Constant.viewsqlOracle.replace("?", schema);
                }
                PreparedStatement statementTable = null;
                ResultSet resultSetTable = null;
                try {
                    statementTable = executeSql(tableSql, connection);
                    resultSetTable = statementTable.executeQuery();
                    if (resultSetTable != null) {
                        tableList = ResultSetUtils.allResultSet(resultSetTable);    //获取某个schema如EHR下所有视图名
                    }
                } finally {
                    close(statementTable, resultSetTable);
                }

                if (tableNameTest != null) { //测试用的
                    tableList = Arrays.asList(tableNameTest.split(","));
                }

                String accessToken = getToken(baseUrl, secret, clientId);

                int successNumbers = 0;
                int errorNumbers = 0;
                int allIds = 0;


                if (tableList != null && tableList.size() > 0) {
                    for (String s1 : tableList) {

                        Statement statementCommon;
                        List<Map<String, Object>> infoMaps = new ArrayList<>();

                        Map<String, Object> transformMap = new HashMap<>();
                        String dataSql = null;
                        String tableName = null;
                        String updateTableEndSql = null;


                        if (databaseType.equals("oracle")) {
                            tableName = s1.split("@")[1];    //V_XXX
                            String owner = s1.split("@")[0]; //EHR
                            //select last_time from linwei.tablestatus where tablename=''
                            String lastEtlTimeSql = "select  last_time from  " + schema1 + ".tablestatus  where tablename='" + tableName + "'"; //tablestatus中的表名注意与视图名一样，注意区分大小写
                            statementCommon = connection1.createStatement();
                            ResultSet resultSetLastEtlTime = statementCommon.executeQuery(lastEtlTimeSql);

                            List<String> lastEtlTimeList = ResultSetUtils1.allResultSet(resultSetLastEtlTime); //获取该表上次的时间, 默认起始给定为1800-01-01

                            //select * from tableName  where sjgxsj >= to_date('last_time','yyyy-mm-dd hh24:mi:ss') and   rownum <= num  order by sjgxsj asc

                            dataSql = (Constant.oracleViewSql + num + "  order by sjgxsj asc").replace("tableName", owner + '.' + tableName)
//                            dataSql = (Constant.oracleViewSql + num + "   order by PRESCRIBE_DATE asc").replace("tableName", owner + '.' + tableName)
                                    .replace("last_time", lastEtlTimeList.get(0)) //把last_time替换为得到的时间,要注意区分时间是字符型还是日期型的
                            ;
                            updateTableEndSql = "update  " + schema1 + ".tablestatus   set  status = "; //用于更新该表的状态，是否已经同步完成

                        }

                        if (dataSql != null) {
                            statementCommon = connection.createStatement();   //查询视图数据
                            ResultSet resultSet = statementCommon.executeQuery(dataSql); //oracle用jdbc需要用 stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql);
                            kettleLog.logBasic("当前传输视图为:  " + s1);
                            infoMaps = ResultSetUtils1.allResultSetToJson(resultSet,null);


                            String newMaxTimeSql = dataSql.replace("*", "max(sjgxsj)"); //获取本次数据中的最大时间
//                            String newMaxTimeSql = dataSql.replace("*", "max(PRESCRIBE_DATE)"); //获取本次数据中的最大时间
                            statementCommon = connection.createStatement();
                            ResultSet MaxTimeResultSet = statementCommon.executeQuery(newMaxTimeSql);
                            List<String> MaxTimeList = ResultSetUtils1.allResultSet(MaxTimeResultSet);
                            //update linwei.tablestatus set last_time=''   where tablename=''

                            if (infoMaps == null || infoMaps.size() == 0) { //查询数据为空了，更新状态为2
                                updateTableEndSql = updateTableEndSql + " 2  where tableName = '" + tableName + "'";
                                statementCommon = connection1.createStatement();  //tablestatus  postgresql  connection1
                                statementCommon.execute(updateTableEndSql);
                            }

                            transformMap.put("collection", tableName.toLowerCase());
                            transformMap.put("infoMaps", infoMaps);

                            List<String> idList = new ArrayList<>();

                            if (infoMaps != null) {
                                for (Map map : infoMaps) {
                                    if (map.containsKey("dataid")) {
                                        idList.add((String) map.get("dataid"));
                                    }
                                    if (map.containsKey("DATAID")) {
                                        idList.add((String) map.get("DATAID"));
                                    }
//                                    if (map.containsKey("ID")) {
//                                        idList.add((String) map.get("ID"));
//                                    }

                                }
                            }
                            allIds += idList.size();

                            if (accessToken != null) {

                                Statement statementTime = null;
                                ResultSet resultSetTime = null;
                                List<String> startTimeList = new ArrayList<>();

                                Date date = new Date();
                                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                String startTime = formatter.format(date);
                                try {
                                    statementTime = connection1.createStatement();
                                    resultSetTime = statementTime.executeQuery(countSql);  //etl_count  postgresql  connection1

                                    startTimeList = ResultSetUtils.allResultSet(resultSetTime);

                                    if ((startTimeList == null || (startTimeList.size() == 0) || (startTimeList.size() > 0 && startTimeList.get(0) == null) || (startTimeList.size() > 0 && startTimeList.get(0).equals("")) || (startTimeList.size() > 0 && startTimeList.get(0).equals("null")))
                                            && (idList.size() > 0)) { //start_time为空且有数据传入，表示开始
                                        //调用数据开始时间接口
                                        Map<String, Object> typeMap = new HashMap<>();
                                        typeMap.put("type", 1);  //1表示开始，2表示结束
                                        transformDataTime(baseUrl, typeMap, accessToken);   //调用开始或结束标志接口
                                        timeSql = "update " + schema1 + "." + "etl_count  set start_time ='" + startTime + "'";
                                        statementTime = connection1.createStatement();  //etl_count  postgresql  connection1
                                        statementTime.execute(timeSql);
                                        kettleLog.logBasic(" -----【数据上报开始时间为：】----" + startTime);
                                    }

                                } finally {
                                    close(statementTime, resultSetTime);
                                }

                                transformResponse transformResponse = transform(baseUrl, accessToken, transformMap, secret, clientId);
                                if (transformResponse != null) {
                                    JSONArray returnJsonObject = transformResponse.getJsonArray();

                                    if (returnJsonObject != null) {
                                        kettleLog.logBasic("---调用上传数据接口成功--- ");
                                        List<String> returnIds = new ArrayList<>();
                                        if (idList.size() > 0 && returnJsonObject.size() > 0) {
                                            for (Object o : returnJsonObject) {
                                                JSONObject jsonObject = (JSONObject) o;
                                                if (jsonObject.containsKey("id")) {
                                                    returnIds.add((String) jsonObject.get("id"));
                                                }
                                            }
                                        }
                                        idList = idList.stream().filter(item -> !returnIds.contains(item)).collect(toList());   //去除未通过校验数据的id

                                        if (idList.size() > 0) {
                                            successNumbers += idList.size();

                                        }
                                        if (returnIds.size() > 0) {
                                            errorNumbers += returnIds.size();
                                        }
                                        //上报成功后，更新该视图的时间戳
                                        Statement updateTimeStatement = null;
                                        try {
                                            updateTimeStatement = connection1.createStatement();
                                            String updateTimeSql = "update " + schema1 + ".tablestatus  set last_time ='" + MaxTimeList.get(0) + "'  where tablename='" + tableName + "'";  //需要更新时间的sql
                                            updateTimeStatement.execute(updateTimeSql); //调用接口成功后修改last_time
                                            kettleLog.logBasic(" -----更新视图----" + tableName + " 上次时间: "+MaxTimeList.get(0));
                                        } finally {
                                            close(updateTimeStatement, null);
                                        }


                                    }
                                }

                            }

                            close(statementCommon, resultSet);
                        }
                    }


                }
                Date date = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String startTime = formatter.format(date);
                kettleLog.logBasic(" 本次上报数据时间为：" + startTime + "--- 上报成功数据总数：" + successNumbers + "    ----上报失败数据总数：" + errorNumbers); //本地测试需要注释掉


                PreparedStatement statementTime1 = null;
                PreparedStatement statementTime2 = null;
                ResultSet resultSetTime1 = null;
                List<String> startTimeList1 = new ArrayList<>();
                try {
                    statementTime1 = executeSql(countSql, connection1); //etl_count  postgresql  connection1
                    resultSetTime1 = statementTime1.executeQuery();

                    startTimeList1 = ResultSetUtils.allResultSet(resultSetTime1);
                    if (startTimeList1 != null && startTimeList1.get(0) != null) { //有start_time 且无数据时，传入结束时间
                        if (successNumbers == 0 && errorNumbers == 0 && allIds == 0) {
                            Date date1 = new Date();
                            SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String endTime = formatter1.format(date1);
                            //调用数据结束时间接口
                            Map<String, Object> typeMap = new HashMap<>();
                            typeMap.put("type", 2);
                            transformDataTime(baseUrl, typeMap, accessToken);
                            kettleLog.logBasic(" -----【数据上报结束时间为：】----" + endTime);
                            timeSql = "update " + schema1 + "." + "etl_count  set start_time = null";
                            String updateTableEndSql = "update  " + schema1 + ".tablestatus   set  status = 1";  //状态全部置为1
                            statementTime1 = executeSql(timeSql, connection1);   //tablestatus  postgresql  connection1
                            statementTime1.execute();
                            statementTime2 = executeSql(updateTableEndSql, connection1);
                            statementTime2.execute();
                            s = "2";   //如果结束传输，s置为2，利用数据校验插件终止kettle循环任务
                        }

                    }
                    if (allIds == 0 && (startTimeList1 == null || startTimeList1.get(0) == null)) { //无起始时间，且无数据直接结束 防止一直死循环
                        String updateTableEndSql = "update  " + schema1 + ".tablestatus   set  status = 1";  //状态全部置为1
                        statementTime2 = executeSql(updateTableEndSql, connection1);
                        statementTime2.execute();
                        kettleLog.logBasic("无新数据上报，结束上报任务！");
                        return "2";
                    }

                } finally {
                    close(statementTime1, resultSetTime1);
                    close(statementTime2, null);
                }
            } finally {
                if (connection != null) {
                    connection.close();
                }
                if (connection1 != null) {
                    connection1.close();
                }
            }
        }

        return s;

    }


    private static String transformDataTime(String baseUrl, Map<String, Object> transformMap, String token) throws IOException {

        LogChannelFactory logChannelFactory = new org.pentaho.di.core.logging.LogChannelFactory();
        LogChannel kettleLog = logChannelFactory.create("上传数据同步起始或结束时间");

        String DataUrl = baseUrl + "/etl/etl/import_data_type";

        kettleResponse kettleResponse = HttpClientUtils.doPost(DataUrl, token, JSON.toJSONString(transformMap, SerializerFeature.WriteMapNullValue));  //上报数据接口
        if (kettleResponse.getCode() == 200) {
            JSONObject returnJsonObject = JSON.parseObject(kettleResponse.getData());
            if ((int) returnJsonObject.get("code") == 0) {
                kettleLog.logBasic(" startTime or endTime transform success");
                return " startTime or endTime transform success";
            }
            return null;
        }
        return null;
    }


    private static String isForbid(String baseUrl, Map<String, Object> transformMap, String token) throws IOException {

        LogChannelFactory logChannelFactory = new org.pentaho.di.core.logging.LogChannelFactory();
        LogChannel kettleLog = logChannelFactory.create("查看是否被禁用");

        String DataUrl = baseUrl + "/etl/etl/check_forbid";

        kettleResponse kettleResponse = HttpClientUtils.doPost(DataUrl, token, JSON.toJSONString(transformMap, SerializerFeature.WriteMapNullValue));  //上报数据接口
        if (kettleResponse.getCode() == 200) {
            JSONObject returnJsonObject = JSON.parseObject(kettleResponse.getData());
            if ((int) returnJsonObject.get("code") == 0) {
                kettleLog.logBasic(" ");
                return " ";
            }
            return null;
        }

        return null;
    }


    private static String getToken(String baseUrl, String secret, String clientId) throws IOException {


        String TokenUrl = baseUrl + "/auth/auth/token?secret=" + secret + "&clientId=" + clientId;
//        kettleLog.logBasic("TokenUrl  " + TokenUrl);
        kettleResponse kettleResponse = HttpClientUtils.doPost(TokenUrl, null, null);  //获取token接口
        if (kettleResponse.getCode() == 200) {
            JSONObject jsonObject = JSON.parseObject(kettleResponse.getData());
            JSONObject jsonObject1 = (JSONObject) jsonObject.get("data");
            String accessToken = String.valueOf(jsonObject1.get("accessToken"));
            String expiresIn = String.valueOf(jsonObject1.get("expiresIn"));
//            long time = System.currentTimeMillis() / 1000;  //获取token时间秒
//            if (time + Integer.valueOf(expiresIn) > System.currentTimeMillis() / 1000) { //校验过期时间 上次获得token时间 + 有效期内（秒）> 此时时间
//                return accessToken;
//            } else {
//                getToken(TokenUrl, secret, clientId);
//            }
            return accessToken;
        }
        return null;

    }


    private static transformResponse transform(String baseUrl, String accessToken, Map<String, Object> transformMap,
                                               String secret, String clientId) throws Exception {

        String DataUrl = baseUrl + "/etl/etl/import_data";
        //http://10.80.131.129/api-gate/zuul/etl/etl/import_data
        transformResponse response = new transformResponse();
        kettleResponse kettleResponse = HttpClientUtils.doPost(DataUrl, accessToken, JSON.toJSONString(transformMap, SerializerFeature.WriteMapNullValue));  //上报数据接口
        if (kettleResponse.getCode() == 200) {
            JSONObject returnJsonObject = JSON.parseObject(kettleResponse.getData());
            if ((int) returnJsonObject.get("code") == 0) {
                response.setJsonArray((JSONArray) returnJsonObject.get("data"));
                response.setCode((int) returnJsonObject.get("code"));
                return response;
            }
            return null;
        } else if (kettleResponse.getCode() == 401) {  //token失效，则重新获取token
            String token = getToken(baseUrl, secret, clientId);
            if (token != null) {
                transformResponse transformResponse = transform(baseUrl, token, transformMap, secret, clientId);
                if (transformResponse != null && transformResponse.getCode() != 200) {
                    throw new Exception(" 上报接口返回错误！" + transformResponse.getCode());
                }
                if (transformResponse != null && transformResponse.getCode() == 200) {
                    return transformResponse;
                }
            }
        } else throw new Exception(" 上报接口返回错误！" + kettleResponse.getCode());

        return null;

    }


    private static PreparedStatement executeSql(String sql, Connection connection) throws Exception {
        PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        statement.setQueryTimeout(6000);
        statement.setFetchSize(100000);
        return statement;
    }

    private static void close(Statement statement, ResultSet resultSet) throws SQLException {
        if (resultSet != null) {
            resultSet.close();
        }

        if (statement != null) {
            statement.close();
        }
    }


}


