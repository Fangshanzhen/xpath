package com.kettle.demo.utils;


import java.sql.Connection;
import java.sql.SQLException;

public class CountUtils {

    public void count(String tableName) throws Exception {

        Connection connection = JDBCUtils.getConnection("postgresql", "10.80.131.111", "5432", "mydatabase", "public", "gpadmin", "zhyl123456");


        String  column="select column_name from information_schema.columns where table_schema='public' and table_name='@';".replace("@",tableName);

    }
}
