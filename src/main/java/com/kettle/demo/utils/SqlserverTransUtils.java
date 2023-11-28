package com.kettle.demo.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.kettle.demo.constant.Constant;
import com.kettle.demo.response.kettleResponse;
import com.kettle.demo.response.transformResponse;
import org.apache.commons.lang3.StringUtils;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelFactory;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;


/**
 * 中间库为sqlserver数据库
 */

public class SqlserverTransUtils {

    public static String transformData(String databaseType, String baseUrl, String dbname, String schema, String ip, String port,
                                       String username, String password,
                                       String secret, String clientId, String num, String tableNameTest) throws Exception {  //dbname: postgres  schema:test

        LogChannelFactory logChannelFactory = new org.pentaho.di.core.logging.LogChannelFactory();
        LogChannel kettleLog = logChannelFactory.create("上报数据");

        Connection connection = null;
        String s = null;

        try {
            connection = JDBCUtils.getConnection(databaseType, ip, port, dbname, schema, username, password);
        } catch (SQLException e) {
            s = "2";
            kettleLog.logError("database connection error", "");
            return s;
        }

        if (connection != null) {
            connection.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
            kettleLog.logBasic(databaseType + "数据库连接成功");
            try {
                List<String> tableList = new ArrayList<>();
                String tableSql = null;
                String countSql = null;
                String timeSql = null;
                if (databaseType.equals("sqlserver")) {
//                tableSql = Constant.tableSqlPostgreSql.replace("?", schema);
                    tableSql = Constant.tableSqlsqlserver.replace("schemaname", schema);
                    countSql = Constant.countsqlserverSql.replace("@", schema);
                }


                Statement statementTable = null;
                ResultSet resultSetTable = null;
                try {
                    System.out.println("--------" + String.valueOf(connection.getHoldability() == ResultSet.HOLD_CURSORS_OVER_COMMIT));
                    statementTable = connection.createStatement();
                    resultSetTable = statementTable.executeQuery(tableSql);
                    if (resultSetTable != null) {
                        tableList = ResultSetUtils.allResultSet(resultSetTable);    //获取所有表名
                    }
                } finally {
                    close(statementTable, resultSetTable);
                }


                if (tableNameTest != null) {
                    tableList = Arrays.asList(tableNameTest.split(","));
                }

//                String accessToken = getToken(baseUrl, secret, clientId);
//
                //   ----------------每隔30分钟获取一次token，避免多次调用---------------------------
                String accessToken = null;
                Statement tokenTime = null;
                ResultSet resultSetToken = null;
                String tokenSql = "SELECT token  FROM  " + schema + ".token_time  ";
                List<String> tokenList = null;
                tokenTime = connection.createStatement();
                resultSetToken = tokenTime.executeQuery(tokenSql);
                try {
                    if (resultSetToken != null) {
                        tokenList = ResultSetUtils1.allResultSet(resultSetToken);
                    }
                    if (tokenList == null || (tokenList.size() == 0) || (tokenList.size() > 0 && tokenList.get(0) == null)
                            || (tokenList.size() > 0 && tokenList.get(0).equals("")) || (tokenList.size() > 0 && tokenList.get(0).equals("null"))) {
                        accessToken = getToken(baseUrl, secret, clientId);
                        if (accessToken != null) {
                            Date date = new Date();
                            long a = date.getTime() + 30 * 60 * 1000;  //30分钟
                            String sql = "UPDATE " + schema + ".token_time " + " SET token= " + "'" + accessToken + "'" + "  ,  token_time= " + a;
                            tokenTime = connection.createStatement();
                            tokenTime.execute(sql);
                        }
                    } else if (tokenList.size() > 0 && tokenList.get(0) != null && !tokenList.get(0).equals("null")) { //有token
                        tokenSql = "SELECT token_time  FROM  " + schema + ".token_time  ";
                        List<String> timeList = new ArrayList<>();
                        tokenTime = connection.createStatement();
                        resultSetToken = tokenTime.executeQuery(tokenSql);
                        if (resultSetToken != null) {
                            timeList = ResultSetUtils1.allResultSet(resultSetToken);
                        }
                        if (timeList != null && timeList.size() > 0 && timeList.get(0) != null) { //判断时间是否有效
                            Date date = new Date();
                            long a = date.getTime();
                            if (Long.valueOf(timeList.get(0)) > a) {
                                accessToken = tokenList.get(0);
                            } else {
                                accessToken = getToken(baseUrl, secret, clientId);
                                Date date1 = new Date();
                                long a1 = date1.getTime() + 30 * 60 * 1000;
                                String sql1 = "UPDATE " + schema + ".token_time  " + " SET token = " + "'" + accessToken + "'" + "  ,  token_time= " + a1;

                                tokenTime = connection.createStatement();
                                tokenTime.execute(sql1);
                            }
                        }
                    }
                } finally {
                    close(tokenTime, resultSetToken);
                }

//----------------------------------------------


                int successNumbers = 0;
                int errorNumbers = 0;
                int allIds = 0;


                if (tableList != null && tableList.size() > 0) {
                    for (String s1 : tableList) {

                        StringBuilder errorSqlAll = new StringBuilder("insert into  " + schema + ".error_log (tablename, id, success, errorlog, errortime) values"); //针对妇幼医院postgresql，把错误信息写进数据库

                        Statement statementCommon;
                        List<Map<String, Object>> infoMaps = new ArrayList<>();

                        Map<String, Object> transformMap = new HashMap<>();
                        String dataSql = null;
                        String tableName = null;
                        String updateSql = null;
                        String updateSql1 = null;
                        String updateTableEndSql = null;


                        //sqlserver
                        if (databaseType.equals("sqlserver")) {
                            tableName = s1.split("@")[1];
                            String schemaName = s1.split("@")[0];
                            dataSql = (Constant.sqlserverSql).replace("tableName", schemaName + '.' + tableName).replace("number123", num);
                            updateSql = "update  " + schemaName + '.' + tableName;
                            updateSql1 = updateSql;
                            updateTableEndSql = "update  " + schemaName + ".tablestatus   set  status = ";
                        }

                        if (dataSql != null) {
                            statementCommon = connection.createStatement();
                            ResultSet resultSet = statementCommon.executeQuery(dataSql);
                            kettleLog.logBasic("当前传输表名为:  " + s1);


                            infoMaps = ResultSetUtils.allResultSetToJson(resultSet);

                            //新加已查完数据的处理 默认为1，已完成的标识为2，当所有的表传输完成后全部重置为1
                            if (infoMaps == null || infoMaps.size() == 0) {
                                updateTableEndSql = updateTableEndSql + " 2  where tablename = '" + tableName + "'";   //sqlserver数据库注意tablename这个字段如果是text格式会报错，要改成varchar格式
                                statementCommon = connection.createStatement();
                                statementCommon.execute(updateTableEndSql);
                            }

                            transformMap.put("collection", tableName);
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
                                }
                            }
                            kettleLog.logBasic("---查询数据库数据成功--- ");
                            allIds += idList.size();

                            if (accessToken != null) {

                                Statement statementTime = null;
                                ResultSet resultSetTime = null;
                                List<String> startTimeList = new ArrayList<>();

                                Date date = new Date();
                                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                String startTime = formatter.format(date);
                                try {
                                    statementTime = connection.createStatement();
                                    resultSetTime = statementTime.executeQuery(countSql);

                                    startTimeList = ResultSetUtils.allResultSet(resultSetTime);

                                    if ((startTimeList == null || (startTimeList.size() == 0) || (startTimeList.size() > 0 && startTimeList.get(0) == null) || (startTimeList.size() > 0 && startTimeList.get(0).equals("")) || (startTimeList.size() > 0 && startTimeList.get(0).equals("null")))
                                            && (idList.size() > 0)) { //start_time为空且有数据传入，表示开始
                                        //调用数据开始时间接口
                                        Map<String, Object> typeMap = new HashMap<>();
                                        typeMap.put("type", 1);  //1表示开始，2表示结束
                                        transformDataTime(baseUrl, typeMap, accessToken);   //调用开始或结束标志接口
                                        timeSql = "update " + schema + "." + "etl_count  set start_time ='" + startTime + "'";
//                                    timeSql = "insert into " + schema + "." + "etl_count (start_time, end_time) values(' " + startTime + "'" + ", null)";
                                        statementTime = connection.createStatement();
                                        statementTime.execute(timeSql);
                                        kettleLog.logBasic(" -----【数据上报开始时间为：】----" + startTime);
                                    }

                                } finally {
                                    close(statementTime, resultSetTime);
                                }

                                transformResponse transformResponse = transform(baseUrl, accessToken, transformMap, secret, clientId);
                                if (transformResponse != null) {
                                    JSONArray returnJsonObject = transformResponse.getJsonArray();
//                                if(transformResponse.getCode()!=null){
//                                    s=transformResponse.getCode().toString();
//                                }

                                    if (returnJsonObject != null) {
                                        kettleLog.logBasic("---调用上传数据接口成功2--- ");
                                        List<String> returnIds = new ArrayList<>();
                                        if (idList.size() > 0 && returnJsonObject.size() > 0) {
                                            for (Object o : returnJsonObject) {
                                                JSONObject jsonObject = (JSONObject) o;
                                                if (jsonObject.containsKey("id")) {
                                                    returnIds.add((String) jsonObject.get("id"));


                                                    //--------------加一段把错误数据日志写进数据库中，供医院修改------------------
                                                    String errorSql = "  ( " + "'" + tableName + "'"; //tablename
                                                    errorSql = errorSql + "," + "  '" + String.valueOf(jsonObject.get("id")) + "'"; //id
                                                    errorSql = errorSql + "," + "  '" + String.valueOf(jsonObject.get("success")) + "'"; //success
                                                    if (jsonObject.containsKey("errorLog")) {
                                                        JSONObject errorLog = (JSONObject) jsonObject.get("errorLog");
                                                        if (errorLog.containsKey("errorMsg")) {
                                                            String errorMsg = String.valueOf(errorLog.get("errorMsg"));
                                                            if (errorMsg != null && !errorMsg.equals("")) {
                                                                errorSql = errorSql + "," + "  '" + errorMsg + "'"; //errorLog
                                                                Date errordate = new Date();
                                                                SimpleDateFormat formatterError = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                                                String dateError = formatterError.format(errordate);
                                                                errorSql = errorSql + "," + "  '" + dateError + "'" + ")"; //errorLog
                                                                errorSqlAll.append(errorSql);
                                                                if (!returnJsonObject.get(returnJsonObject.size() - 1).equals(o)) {
                                                                    errorSqlAll.append(",");//除去最后一个，其余都加,
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                        }
                                        idList = idList.stream().filter(item -> !returnIds.contains(item)).collect(toList());   //去除未通过校验数据的id

                                        if (idList.size() > 0) {

                                            successNumbers += idList.size();

                                            String newIdList = "'" + StringUtils.join(idList, "','") + "'";  //加上单引号
                                            updateSql = updateSql + " set sjtbzt = 1 " + "where dataid in (" + newIdList + " )";

                                            try {
                                                statementCommon = connection.createStatement();
                                                statementCommon.execute(updateSql);
                                                kettleLog.logBasic("---更新数据同步成功状态-- ");
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                kettleLog.logError(e + "");
                                            }
                                        }
                                        if (returnIds.size() > 0) {

                                            errorNumbers += returnIds.size();
                                            String errorIdList = "'" + StringUtils.join(returnIds, "','") + "'";
                                            String errorDataSql = updateSql1 + " set sjtbzt = 2 " + "where dataid in (" + errorIdList + " )";
                                            try {
                                                statementCommon = connection.createStatement();
                                                statementCommon.execute(errorDataSql);
                                                kettleLog.logBasic("---更新数据同步失败状态-- ");
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                kettleLog.logError(e + "");
                                            }
                                        }

                                    }
                                }


                            }


                            close(statementCommon, resultSet);
                        }
                        errorSqlAll.append(";");
//                        if (ip.equals("10.80.43.251")) {  //针对妇幼保健医院数据库把错误日志信息插入表中，提前建好表
                        try {
                            if (errorSqlAll.toString().contains(tableName)) { //
                                kettleLog.logBasic("errorSqlAll:----  " + errorSqlAll);
                                Statement statementError = connection.createStatement();
                                statementError.execute(errorSqlAll.toString());
                                close(statementError, null);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            kettleLog.logError(e + "");
                        }
//                        }
                        kettleLog.logBasic(s1 + "----------------------success");

                    }
                }
                Date date = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String startTime = formatter.format(date);
                kettleLog.logBasic(" 本次上报数据时间为：" + startTime + "--- 上报成功数据总数：" + successNumbers + "    ----上报失败数据总数：" + errorNumbers); //本地测试需要注释掉


                Statement statementTime1 = null;
                Statement statementTime2 = null;
                ResultSet resultSetTime1 = null;
                List<String> startTimeList1 = new ArrayList<>();
                try {
                    statementTime1 = connection.createStatement();
                    resultSetTime1 = statementTime1.executeQuery(countSql);

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
                            timeSql = "update " + schema + "." + "etl_count  set start_time =null";
                            String updateTableEndSql = "update  " + schema + ".tablestatus   set  status = 1";  //状态全部置为1
                            statementTime1 = connection.createStatement();
                            statementTime1.execute(timeSql);
                            statementTime2 = connection.createStatement();
                            statementTime2.execute(updateTableEndSql);
                            s = "2";   //如果结束传输，s置为2，利用数据校验插件终止kettle循环任务
                        }

                    }
                    if (allIds == 0 && (startTimeList1 == null || startTimeList1.get(0) == null)) { //无起始时间，且无数据直接结束 防止一直死循环
                        String updateTableEndSql = "update  " + schema + ".tablestatus   set  status = 1";  //状态全部置为1
                        statementTime2 = connection.createStatement();
                        statementTime2.execute(updateTableEndSql);
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

        LogChannelFactory logChannelFactory = new org.pentaho.di.core.logging.LogChannelFactory();
        LogChannel kettleLog = logChannelFactory.create("获取token");
        String TokenUrl = baseUrl + "/auth/auth/token?secret=" + secret + "&clientId=" + clientId;
//        kettleLog.logBasic("TokenUrl  " + TokenUrl);
        kettleResponse kettleResponse = null;  //获取token接口
        try {
            kettleResponse = HttpClientUtils.doPost(TokenUrl, null, null);
        } catch (IOException e) {
            e.printStackTrace();
            kettleLog.logError("获取token出现异常: " + e);
            return null;
        }
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
        LogChannelFactory logChannelFactory = new org.pentaho.di.core.logging.LogChannelFactory();
        LogChannel kettleLog = logChannelFactory.create("调用请求");
        String DataUrl = baseUrl + "/etl/etl/import_data";
        //http://10.80.131.129/api-gate/zuul/etl/etl/import_data
        transformResponse response = new transformResponse();
        kettleResponse kettleResponse = null;  //上报数据接口
        try {
            kettleResponse = HttpClientUtils.doPost(DataUrl, accessToken, JSON.toJSONString(transformMap, SerializerFeature.WriteMapNullValue));
        } catch (IOException e) {
            e.printStackTrace();
            kettleLog.logError("调用请求出现异常: " + e);
            return null;
        }
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

//
//    private static Statement executeSql(String sql, Connection connection) throws Exception {
//        Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
//                ResultSet.CLOSE_CURSORS_AT_COMMIT);
//        statement.setQueryTimeout(6000);
//        statement.setFetchSize(100000);
//        statement.setEscapeProcessing(false);
//        return statement;
//    }

    private static void close(Statement statement, ResultSet resultSet) throws SQLException {
        if (resultSet != null) {
            resultSet.close();
        }

        if (statement != null) {
            statement.close();
        }
    }


}


