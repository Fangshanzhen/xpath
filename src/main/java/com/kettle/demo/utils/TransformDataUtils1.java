package com.kettle.demo.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.kettle.demo.constant.Constant;
import com.kettle.demo.response.kettleResponse;
import com.kettle.demo.response.numResponse;
import com.kettle.demo.response.transformResponse;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static java.util.stream.Collectors.toList;


/**
 * 默认中间库为postgresql数据库，若为别的数据库则需要修改代码
 */

public class TransformDataUtils1 {

    public static String transformData(String databaseType, String baseUrl, String dbname, String schema, String ip, String port,
                                       String username, String password,
                                       String secret, String clientId, String num, String tableNameTest) throws Exception {  //dbname: postgres  schema:test

        LogChannelFactory logChannelFactory = new org.pentaho.di.core.logging.LogChannelFactory();
        LogChannel kettleLog = logChannelFactory.create("上报数据");

        Connection connection = JDBCUtils.getConnection(databaseType, ip, port, dbname, schema, username, password); //默认postgresql

        kettleLog.logBasic(databaseType + "数据库连接成功");
        String s = null;
        try {
            List<String> tableList = new ArrayList<>();
            String tableSql = null;
            String countSql = null;
            String timeSql = null;
            if (databaseType.equals("postgresql")) {
                tableSql = Constant.tableSqlPostgreSql.replace("?", schema);
                countSql = Constant.countSql.replace("@", schema);
            }
            if (databaseType.equals("mysql")) {
                tableSql = Constant.tablesqlMysql.replace("?", dbname);
            }
            if (databaseType.equals("oracle")) {
                tableSql = Constant.tablesqlOracle.replace("?", dbname);
            }
            PreparedStatement statementTable = null;
            ResultSet resultSetTable = null;
            try {
                assert connection != null;
                statementTable = executeSql(tableSql, connection);
                resultSetTable = statementTable.executeQuery();
                if (resultSetTable != null) {
                    tableList = ResultSetUtils.allResultSet(resultSetTable);    //获取所有表名
                }
            } finally {
                close(statementTable, resultSetTable);
            }

            if (tableNameTest != null) {
                tableList = Arrays.asList(tableNameTest.split(","));
            }

            String accessToken = getToken(baseUrl, secret, clientId);

            int successNumbers = 0;
            int errorNumbers = 0;

            //声明线程池  定长线程池,超出的线程会在队列中等待
            ExecutorService pool = Executors.newFixedThreadPool(3);
            //声明数据回调处理类
            List<FutureTask<numResponse>> futureTasks = new ArrayList<>();

            if (tableList != null && tableList.size() > 0) {


                try {
                    for (String table : tableList) {  //参数列表
                        CallableService callable = new CallableService(table, databaseType, num, connection, dbname, schema, accessToken,
                                countSql,  baseUrl, secret, clientId);
                        //创建一个异步任务
                        FutureTask<numResponse> futureTask = new FutureTask<>(callable);
                        futureTasks.add(futureTask);
                        //提交异步任务到线程
                        pool.submit(futureTask);
                    }

//                    所有线程执行完毕之后通过执行线程的get()方法获取每个线程执行的结果
                    for (FutureTask<numResponse> future : futureTasks) {
                        numResponse object = future.get();
                        if (object != null) {
                            successNumbers += object.getSuccess();
                            errorNumbers += object.getError();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    pool.shutdown();
                }
            }


            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String startTime = formatter.format(date);
            kettleLog.logBasic(" 本次上报数据时间为：" + startTime + "--- 上报成功数据总数：" + successNumbers + "    ----上报失败数据总数：" + errorNumbers); //本地测试需要注释掉


            PreparedStatement statementTime1 = null;
            ResultSet resultSetTime1 = null;
            List<String> startTimeList1 = new ArrayList<>();
            try {
                statementTime1 = executeSql(countSql, connection);
                resultSetTime1 = statementTime1.executeQuery();

                startTimeList1 = ResultSetUtils.allResultSet(resultSetTime1);
                if (startTimeList1 != null && startTimeList1.get(0) != null) { //有start_time 且无新数据时,表示结束
                    if (successNumbers == 0 && errorNumbers == 0) {
                        Date date1 = new Date();
                        SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String endTime = formatter1.format(date1);
                        //调用数据结束时间接口
                        Map<String, Object> typeMap = new HashMap<>();
                        typeMap.put("type", 2);
                        transformDataTime(baseUrl, typeMap, accessToken);
                        kettleLog.logBasic(" -----【数据上报结束时间为:】----" + endTime);
                        timeSql = "update " + schema + "." + "etl_count  set start_time =null";
                        statementTime1 = executeSql(timeSql, connection);
                        statementTime1.execute();
                        s = "2";   //如果结束传输，s置为2，利用数据校验插件终止kettle循环任务
                    }

                }

            } finally {
                close(statementTime1, resultSetTime1);
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        return s;

    }

    public static class CallableService implements Callable<numResponse> {

        String table;
        String databaseType;
        String num;
        Connection connection;
        String dbname;
        String accessToken;
        String schema;
        String baseUrl;
        String countSql;
        String secret;
        String clientId;

        public CallableService(String table, String databaseType, String num, Connection connection, String dbname, String schema,
                               String accessToken, String countSql, String baseUrl,
                               String secret, String clientId) {
            this.table = table;
            this.num = num;
            this.databaseType = databaseType;
            this.connection = connection;
            this.dbname = dbname;
            this.schema = schema;
            this.accessToken = accessToken;
            this.baseUrl = baseUrl;
            this.countSql = countSql;
            this.secret = secret;
            this.clientId = clientId;
        }

        @Override
        public numResponse call() throws Exception {
            numResponse numResponse = new numResponse();
            LogChannelFactory logChannelFactory = new org.pentaho.di.core.logging.LogChannelFactory();
            LogChannel kettleLog = logChannelFactory.create("线程池:");
            PreparedStatement statementCommon = null;
            ResultSet resultSet = null;
            List<Map<String, Object>> infoMaps = new ArrayList<>();

            Map<String, Object> transformMap = new HashMap<>();
            String dataSql = null;
            String tableName = null;
            String updateSql = null;
            String updateSql1 = null;
            int success = 0;
            int error = 0;

            //POSTGRESQL
            if (databaseType.equals("postgresql")) {
                if (!table.contains("old") && !table.contains("etl") && !table.contains("company") && !table.contains("immu") && !table.contains("postalcode")) {
                    kettleLog.logBasic("当前传输表名为: " + table);
                    tableName = table.split("@")[1];
                    String schemaName = table.split("@")[0];
                    dataSql = (Constant.dataSql + num).replace("tableName", schemaName + '.' + tableName);
                    if (table.contains("bdmpwh") || table.contains("bdmsmi") || table.contains("bdmoph") || table.contains("bdmtdp")) {
                        dataSql = (Constant.dataSql + (Integer.valueOf(num)-1000)).replace("tableName", schemaName + '.' + tableName);
                    }
                    kettleLog.logBasic("dataSql: " + dataSql);
                    updateSql = "update  " + schemaName + '.' + tableName;
                    updateSql1 = updateSql;
                }
            }
            //MYSQL
            if (databaseType.equals("mysql")) {
                tableName = table;
                dataSql = (Constant.dataSql + num).replace("tableName", tableName);
                updateSql = "update  " + dbname + '.' + tableName;
            }
            //ORACLE
            if (databaseType.equals("oracle")) {
                tableName = table.split("@")[1];
                String owner = table.split("@")[0];
                dataSql = (Constant.oracleSql + num).replace("tableName", owner + '.' + tableName);
                updateSql = "update  " + owner + '.' + tableName;

            }

            try {
                if (dataSql != null) {
                    statementCommon = executeSql(dataSql, connection);
                    resultSet = statementCommon.executeQuery();

                    infoMaps = ResultSetUtils.allResultSetToJson(resultSet);

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
                    kettleLog.logBasic(tableName + "---数据获取成功--- ");


                    if (accessToken != null) {

                        PreparedStatement statementTime = null;
                        ResultSet resultSetTime = null;
                        List<String> startTimeList = new ArrayList<>();

                        Date date = new Date();
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String startTime = formatter.format(date);
                        try {
                            statementTime = executeSql(countSql, connection);
                            resultSetTime = statementTime.executeQuery();

                            startTimeList = ResultSetUtils.allResultSet(resultSetTime);
                            if ((startTimeList == null || (startTimeList.size() == 0)
                                    || (startTimeList != null && startTimeList.get(0) == null)
                                    || (startTimeList != null && startTimeList.get(0).equals("")))
                                    && (idList.size() > 0)) { //start_time为空且有数据传入，表示开始
                                //调用数据开始时间接口
                                Map<String, Object> typeMap = new HashMap<>();
                                typeMap.put("type", 1);  //1表示开始，2表示结束
                                transformDataTime(baseUrl, typeMap, accessToken);   //调用开始或结束标志接口
                                String timeSql = "update " + schema + "." + "etl_count  set start_time ='" + startTime + "'";
                                //                                    timeSql = "insert into " + schema + "." + "etl_count (start_time, end_time) values(' " + startTime + "'" + ", null)";
                                statementTime = executeSql(timeSql, connection);
                                statementTime.execute();
                                kettleLog.logBasic(" -----【数据上报开始时间为：】----" + startTime);
                            }

                        } finally {
                            close(statementTime, resultSetTime);
                        }

                        transformResponse transformResponse = transform(baseUrl, accessToken, transformMap, secret, clientId);
                        if (transformResponse != null) {
                            JSONArray returnJsonObject = transformResponse.getJsonArray();
                            if (returnJsonObject != null) {
                                kettleLog.logBasic(tableName + "---上报接口结果返回成功--- " + transformResponse.getResult());
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

                                    success += idList.size();

                                    String newIdList = "'" + StringUtils.join(idList, "','") + "'";  //加上单引号
                                    updateSql = updateSql + " set sjtbzt = 1 " + "where dataid in (" + newIdList + " )";
                                    try {
                                        statementCommon = executeSql(updateSql, connection);
                                        statementCommon.execute();
                                        kettleLog.logBasic("---更新数据同步成功状态-- " + updateSql);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (returnIds.size() > 0) {

                                    error += returnIds.size();
                                    String errorIdList = "'" + StringUtils.join(returnIds, "','") + "'";
                                    String errorDataSql = updateSql1 + " set sjtbzt = 2 " + "where dataid in (" + errorIdList + " )";
                                    try {
                                        statementCommon = executeSql(errorDataSql, connection);
                                        statementCommon.execute();
                                        kettleLog.logBasic("---更新数据同步失败状态-- " + errorDataSql);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                            }
                        }

                    }
                }
            } finally {
                close(statementCommon, resultSet);
            }
            numResponse.setError(error);
            numResponse.setSuccess(success);
            return numResponse;


        }
    }


    private static String transformDataTime(String baseUrl, Map<String, Object> transformMap, String token) throws IOException {

        LogChannelFactory logChannelFactory = new org.pentaho.di.core.logging.LogChannelFactory();
        LogChannel kettleLog = logChannelFactory.create("上传数据同步起始或结束时间");

        String DataUrl = baseUrl + "/etl/etl/import_data_type";

        kettleResponse kettleResponse = HttpClientUtils.doPost(DataUrl, token, JSON.toJSONString(transformMap, SerializerFeature.WriteMapNullValue));  //上报数据接口
        if (kettleResponse.getCode() == 200) {
            JSONObject returnJsonObject = JSON.parseObject(kettleResponse.getData());
            if ((int) returnJsonObject.get("code") == 0) {
                kettleLog.logBasic(" 上传数据同步起始或结束时间成功! ");
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
        LogChannel kettleLog = logChannelFactory.create("上报数据");


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
        //http://10.80.131.129/api-gate/etl/etl/import_data
        transformResponse response = new transformResponse();
        kettleResponse kettleResponse = HttpClientUtils.doPost(DataUrl, accessToken, JSON.toJSONString(transformMap, SerializerFeature.WriteMapNullValue));  //上报数据接口
        if (kettleResponse.getCode() == 200) {
            JSONObject returnJsonObject = JSON.parseObject(kettleResponse.getData());
            if ((int) returnJsonObject.get("code") == 0) {
                response.setJsonArray((JSONArray) returnJsonObject.get("data"));
                response.setCode((int) returnJsonObject.get("code"));
                response.setResult(kettleResponse.getData());
                return response;
            }
            return null;
        } else if (kettleResponse.getCode() == 401) {  //token失效，则重新获取token
            String TokenUrl = baseUrl + "/auth/auth/token?secret=" + secret + "&clientId=" + clientId;
            String token = getToken(TokenUrl, secret, clientId);
            if (token != null) {
                return transform(DataUrl, token, transformMap, null, null);
            }
        } else {
            throw new Exception(" 上报接口返回错误！" + kettleResponse.getCode());
        }

        return null;

    }


    private static PreparedStatement executeSql(String sql, Connection connection) throws Exception {
        PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        statement.setQueryTimeout(6000);
        statement.setFetchSize(100000);
        return statement;
    }

    private static void close(PreparedStatement statement, ResultSet resultSet) throws SQLException {
        if (resultSet != null) {
            resultSet.close();
        }

        if (statement != null) {
            statement.close();
        }
    }


}


