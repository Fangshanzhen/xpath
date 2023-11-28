package com.kettle.demo.utils;

import org.json.JSONException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;


public class ResultSetUtils1 {

    public static List<Map<String, Object>> allResultSetToJson(ResultSet rs,String table) throws SQLException, JSONException {
        // json数组
//        JSONArray array = new JSONArray();
        List<Map<String, Object>> list = new ArrayList<>();
        // 获取列数
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // 遍历ResultSet中的每条数据
        while (rs.next()) {
//            JSONObject jsonObj = new JSONObject();
            Map<String, Object> map = new HashMap<>();
            // 遍历每一列
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = null;
                try {
                    value = rs.getObject(columnName);
                } catch (SQLException e) {
                }
                if(table.equalsIgnoreCase("ylfy")||table.equalsIgnoreCase("gzxl")
                        ||table.equalsIgnoreCase("zlzl")||table.equalsIgnoreCase("hlyy")){
                    map.put(columnName.toLowerCase(), String.valueOf(value));  //oracle 字段名大写改成小写
                }
                else {
                    map.put(columnName.toLowerCase(), value);  //oracle 字段名大写改成小写
                }
            }
            list.add(map);
        }

        list = list.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(p -> (String) p.get("dataid")))),
                ArrayList::new));   //根据dataid去重数据，同一批数据必须去重，否则批量插入会报错


        if (list.size() > 0) {
            return list;
        }
        return null;
    }

    public static List<String> allResultSet(ResultSet rs) throws SQLException, JSONException {
        // json数组
//        JSONArray array = new JSONArray();
        List<String> list = new ArrayList<>();
        // 获取列数
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // 遍历ResultSet中的每条数据
        while (rs.next()) {
            // 遍历每一列
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                if (rs.getObject(columnName) != null) {
                    String value = String.valueOf(rs.getObject(columnName));
                    list.add(value);
                }
            }
        }

        if (list.size() > 0) {
            return list;
        }
        return null;
    }


    public static List<String> originalResult(ResultSet rs) throws SQLException, JSONException {
        // json数组
//        JSONArray array = new JSONArray();
        List<String> list = new ArrayList<>();
        // 获取列数
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // 遍历ResultSet中的每条数据
        while (rs.next()) {
            // 遍历每一列
            for (int i = 1; i <= columnCount; i++) {
                String columnName = "dataid";
                String value = (String) rs.getObject(columnName);
                list.add(value);
            }
        }

        if (list.size() > 0) {
            return list;
        }
        return null;
    }

    public static List<String[]> ShuZuResultSet(ResultSet rs) throws SQLException {

        List<String[]> list = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            // 遍历每一列
            List<String> list1 = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                if (rs.getObject(columnName) != null) {
                    String value = String.valueOf(rs.getObject(columnName));
                    list1.add(value);
                }
            }
            String[] a = new String[7];
            list.add(list1.toArray(a));
        }

        return list;
    }


//    public static Map<String, Object> oneResultSetToJson(ResultSet rs) throws SQLException, JSONException {
////
////        ResultSetMetaData metaData = rs.getMetaData();
////        int columnCount = metaData.getColumnCount();
//////        JSONObject jsonObj = new JSONObject();
////        Map<String, Object> map=new HashMap<>();
////
////        while (rs.next()) {
////
////            for (int i = 1; i <= columnCount; i++) {
////                String columnName = metaData.getColumnName(i);
////                Object value = rs.getObject(columnName);
////                map.put(columnName, value);
////            }
////            break;
////        }
////        if (map.size() > 0) {
////            return map;
////        }
////        return null;
////    }
////
////    public static List< Map<String, Object> > manyResultSetToJson(ResultSet rs, int num) throws SQLException, JSONException {
////
//////        JSONArray array = new JSONArray();
////        List< Map<String, Object> >list=new ArrayList<>();
////
////        ResultSetMetaData metaData = rs.getMetaData();
////        int columnCount = metaData.getColumnCount();
////
////        while (rs.next()) {
//////            JSONObject jsonObj = new JSONObject();
////            Map<String, Object> map=new HashMap<>();
////            for (int i = 1; i <= columnCount; i++) {
////                String columnName = metaData.getColumnName(i);
////                Object value = rs.getObject(columnName);
////                map.put(columnName, value);
////            }
////            list.add(map);
////            if (list.size() >=num) {
////                break;
////            }
////        }
////        if (list.size() > 0) {
////            return list;
////        }
////        return null;
////    }
}
