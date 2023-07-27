package com.kettle.demo.utils;

import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.kettle.demo.utils.CsvUtils.writeCSV;

/**
 * 统计数据集人数（不包含电子病历）
 */

public class WriteToCsvUtilsPerson {
    public static void writeToCsv(String tableName) throws Exception {
        LogChannelFactory logChannelFactory = new org.pentaho.di.core.logging.LogChannelFactory();
        LogChannel kettleLog = logChannelFactory.create("统计数据集人数");

        //装业务数据集合
        List<String[]> data = new ArrayList<>();
        //表格标题头
        String[] head = {"表名", "总数"};
        data.add(head);
        Connection connection = JDBCUtils.getConnection("postgresql", "10.80.131.111", "5432", "mydatabase", "public", "gpadmin", "gpadmin");
        List<String> tables = new ArrayList<>();
        if (tableName.equals("all")) {
            tables = Arrays.asList(

                    "etbj_", "fnbj_", "jbxx_", "bdmp_", "bdms_", "bdmo_", "bdmt_", "bdmc_", "jbkz_", "sbgw_"

            );
        } else {
            tables = Arrays.asList(tableName.split(","));  //以逗号隔开
        }


        List<String> tableList = new ArrayList<>();
        PreparedStatement statementTable = null;
        ResultSet resultSetTable = null;
        try {
            if (connection != null) {
                statementTable = executeSql("select tablename  from pg_tables where schemaname='public'", connection);
                resultSetTable = statementTable.executeQuery();
                if (resultSetTable != null) {
                    tableList = ResultSetUtils.allResultSet(resultSetTable);    //获取所有表名
                }
            }
        } finally {
            close(statementTable, resultSetTable);
        }

        List<String> newTableList = new ArrayList<>();   //存放数据库中的表名，如cbjcjb_6, bdopcr_111
        if (tableList != null && tableList.size() > 0) {
            for (String s : tables) {  // tjxpgj_
                for (String s1 : tableList) { //tjxpgj_2
                    if (s1.contains(s)) {
                        newTableList.add(s1);
                    }
                }
            }
        }

        if (newTableList.size() > 0) {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                for (String s : newTableList) {
                    String sql = null;

                    sql = " select '@@', count1  from (select   count(*)  as count1 from @@ ) ss ;  ";
//  select count(*) from (select * , row_number()over(partition by emrpif0002_9 ) as num1  from emrpif_9 where id  like '13\_%' ) cc  where num1=1
//  select count(*) from (select * , row_number()over(partition by emrpif0002_9 ) as num1  from emrpif_9 where  id  ~ '^[1-9]\_|1[0-2]\_' ) cc  where num1=1
                    sql = sql.replace("@@", s);
                    statement = executeSql(sql, connection);
                    resultSet = statement.executeQuery();
                    if (resultSet != null) {
                        List<String[]> shuzuResult = ResultSetUtils.PersonResultSet(resultSet);
                        if (shuzuResult.size() == 0) {  //无数据的表直接赋值
                            String[] sample = { s, "无数据"};
                            shuzuResult.add(sample);
                            kettleLog.logBasic("根表【" + s + "】无数据!");
                        }
                        data.addAll(shuzuResult);
                        kettleLog.logBasic("根表【" + s + "】已完成数据质量统计!");
                    }
                }


            } finally {
                close(statement, resultSet);

            }

        }


        //设置路径及文件名称
        String fileName = "C:\\Users\\Administrator\\Desktop\\data\\peopleCount.csv";
        writeCSV(fileName, data);
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

