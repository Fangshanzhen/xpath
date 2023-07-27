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
 * 统计非电子病历数据质量
 */

public class WriteToCsvUtils {

    public static void writeToCsv(String tableName) throws Exception {
        LogChannelFactory logChannelFactory = new org.pentaho.di.core.logging.LogChannelFactory();
        LogChannel kettleLog = logChannelFactory.create("统计数据质量（非电子病历数据）");

        //装业务数据集合
        List<String[]> data = new ArrayList<>();
        //表格标题头
        String[] head = {"数据库名", "表名", "列名", "样本数", "非空数", "缺失数", "缺失率"};
        data.add(head);
        Connection connection = JDBCUtils.getConnection("postgresql", "10.80.131.111", "5432", "mydatabase", "public", "gpadmin", "gpadmin");
        List<String> tables = new ArrayList<>();
        if (tableName.equals("all")) {
            tables = Arrays.asList(
                    "cbjcjb_", "cbjdyz_", "chtbca_", "chtnhv_", "chtcpe_", "chtnbs_", "chtcsq_", "chtndo_", "cbjyyx_", "cbjdbz_", "cbjyqt_", "cbjglb_",
                    "cbjfpe_", "chtcur_", "bdoprc_", "bdoscg_", "bdotso_", "bdofpc_", "bdompc_", "bdopsa_", "bdobdm_", "bdommr_", "bdopgc_", "bsigrx_",
                    "bsijtx_", "jkdgrj_", "jkdjkd_", "jkdggm_", "jkdgjw_", "jkdgss_", "jkdgws_", "jkdgsx_", "jkdgjz_", "tjxwex_", "tjxsys_", "tjxfzj_",
                    "tjxtgj_", "tjxpgj_", "bdmpwh_", "bdmsmi_", "bdmtdp_", "bdmccm_", "bdscap_", "bdsodr_", "bdsimr_", "bdsppr_", "bdsbrf_",
                    "bdsmco_", "bdscrb_", "bdstrt_", "bdssyx_", "bdsyfj_"
//                    "emrpif_", "emrhif_", "emrwss_", "emrylf_", "emroem_", "emreom_", "emrtcm_", "emrwmp_", "emraud_", "emrisr_", "emrtmr_", "emrnsr_",
//                    "emrpai_", "emrard_", "emraai_", "emrbtr_", "emrwfd_", "emrvdr_", "emrcso_", "emrgnr_", "emrccr_", "emrsnr_", "emrvsm_", "emriao_",
//                    "emrhvc_", "emraer_", "emrnpr_", "emrdea_", "emrict_", "emrica_", "emrbtt_", "emrsea_", "emrcin_", "emroic_", "emrhpi_", "emrhpo_",
//                    "emrcir_", "emraad_", "emrdir_", "emrfcr_", "emrdcr_", "emrwrr_", "emrdod_", "emrsfr_", "emrtfr_", "emrssm_", "emrsrd_",
//                    "emrcrd_", "emrps0_", "emrpd0_", "emrtfp_", "emrdsn_", "emrdrs_", "emrdcd_", "emrito_", "emrdsb_", "emrtrd_"
            );
        } else {
            tables = Arrays.asList(tableName.split(","));  //以逗号隔开
        }


        //bdopce, bdopcr,bdmoph, bdsohm, bdompc_这几个表字段数量太多200+，会出现 Canceling query because of high VMEM usage. 需要另外单独分析
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
                    String sql = " select * from count_table('public','@@'); ";  //执行存储过程
                    sql = sql.replace("@@", s);
                    statement = executeSql(sql, connection);
                    resultSet = statement.executeQuery();
                    if (resultSet != null) {
                        List<String[]> shuzuResult = ResultSetUtils.ShuZuResultSet(resultSet);
                        if (shuzuResult.size() == 0) {  //无数据的表直接赋值
                            String[] sample = {"public", s, "无数据", "0", "0", "0", "100%"};
                            shuzuResult.add(sample);
                            kettleLog.logBasic("表【" + s + "】无数据!");
                        }
                        data.addAll(shuzuResult);
                        kettleLog.logBasic("表【" + s + "】已完成数据质量统计!");
                    }
                }
            } finally {
                close(statement, resultSet);

            }

        }

        List<String> LongColumnTables = Arrays.asList("bdopce_", "bdopcr_", "bdmoph_", "bdsohm_");

        List<String> newLongColumnTables = new ArrayList<>();

        if (tableList != null && tableList.size() > 0) {
            for (String s : LongColumnTables) {  // bdopcr
                for (String s1 : tableList) { //bdopcr_11
                    if (s1.contains(s)) {
                        newLongColumnTables.add(s1);
                    }
                }
            }
        }
        for (String s : newLongColumnTables) {
            String countSql = "select count(*) from information_schema.columns where table_schema='public' and table_name='@@';".replace("@@", s);
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = executeSql(countSql, connection);
                resultSet = statement.executeQuery();
                List<String> countList = ResultSetUtils.allResultSet(resultSet);
                if (countList != null && countList.size() > 0) {
                    int count = Integer.parseInt(countList.get(0));
                    if (count > 50) {
                        for (int i = 0; i <= count / 50; i++) {
                            String longSql = " select * from count_table_split('public','@@',50,%%*50) ;".replace("@@", s).replace("%%", String.valueOf(i));
                            statement = executeSql(longSql, connection);
                            resultSet = statement.executeQuery();
                            if (resultSet != null) {
                                List<String[]> LongResult = ResultSetUtils.ShuZuResultSet(resultSet);
                                if (LongResult.size() == 0) {  //无数据的表直接赋值
                                    String[] sample = {"public", s, "无数据", "0", "0", "0", "100%"};
                                    LongResult.add(sample);   //分批了会增加多次
                                }
                                data.addAll(LongResult);
                                kettleLog.logBasic("表【" + s + "】已完成 ( " + i * 50 + "," + (i + 1) * 50 + " ] 数据质量统计!");
                            }
                        }
                    }
                }
            } finally {
                close(statement, resultSet);

            }

        }


        //设置路径及文件名称
        String fileName = "C:\\Users\\Administrator\\Desktop\\data\\mydatabase.csv";
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
