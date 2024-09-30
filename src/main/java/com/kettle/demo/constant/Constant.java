package com.kettle.demo.constant;

public class Constant {


    public final static String tableSqlPostgreSql = "select schemaname ||'@' ||tablename  from pg_tables where schemaname='?'";

    public final static String dataSql = "select * from tableName where  dataid is not null and  patientid is not null and patientid !='未采集' and patientid !='-'  and patientid !='888888888888888888'   " +
            " and  sjtbzt =0    limit ";

    //  and sjgxsj >'2023-02-06 00:00:00'
//    public final static String dataSql =
//            "select * from (select * , row_number()over(partition by dataid order by sjgxsj) as num1  from tableName where dataid is not null and patientid is not null and sjtbzt=0 ) cc  where num1=1 limit ";


    public final static String tablesqlMysql="SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='?'";
    //SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='test'
// UPDATED_DATE >SYSDATE -2


  public final static String oracleViewSql = "select * from tableName  where sjgxsj>= to_date('last_time','yyyy-mm-dd hh24:mi:ss') and   rownum <=    " ;
//    public final static String oracleViewSql = "select * from tableName  where PRESCRIBE_DATE >= 'last_time' and   rownum <=  " ;  //test

    public final static String oracleSql = "select * from tableName  where sjtbzt=0 and  dataid is not null and   rownum <=    " ;
//    public final static String oracleSql = "select * from tableName  where    rownum <=  " ;   //test

    public final static String tablesqlOracle="  select OWNER ||'@' ||TABLE_NAME  from all_tables where OWNER='?'    ";
    //SELECT * FROM all_tables WHERE OWNER = 'EHR'  //test

    public final static String viewsqlOracle="  select OWNER ||'@' ||view_name  from dba_views where OWNER='?'    ";
    //select OWNER ||'@' ||view_name  from dba_views where OWNER='EHR'

    public final static String countSql = "select start_time from @.etl_count limit 1 ";

    public final static String countOracleSql = "select start_time from @.etl_count   where rownum =1 ";

    public final static String countsqlserverSql = "select top 1 start_time from @.etl_count   ";

    public final static String tableSqlPostgreSql1 = "select 'schemaname' ||'@'|| tableName  from schemaname.tablestatus where status=1 ";

    public final static String tableSqlOraclel1 = "select 'schemaname' ||'@'|| TABLENAME  from schemaname.tablestatus where status=1 ";

    public final static String sqlserverSql = "select top number123 * from tableName  where sjtbzt=0   " ;


//    public final static String tableSqlsqlserver = "select concat ('schemaname' ,'@', tablename )  from schemaname.tablestatus where status=1 ";
    public final static String tableSqlsqlserver = "select 'schemaname'+ '@'+ tablename   from schemaname.tablestatus where status=1 ";

    public final static String tableSqlmysql = "select concat ('schemaname' ,'@', tablename )  from schemaname.tablestatus where status=1 ";


    //删除数据
    public final static String deleteSql = "select * from tableName where  dataid is not null and  patientid is not null and patientid !='未采集' and patientid !='-'  and patientid !='888888888888888888'   " +
            " and  sjtbzt =-1    limit ";
    public final static String oracleDeleteSql = "select * from tableName  where sjtbzt=-1 and  dataid is not null and   rownum <=    " ;

    public final static String sqlserveDeleterSql = "select top number123 * from tableName  where sjtbzt=-1   " ;

}
