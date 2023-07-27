package com.kettle.demo.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberUtils {
    public static Double getNumber(String s) {
        if (s == null || s.equals("") || s.length() == 0) {
            return null;
        }
        String REGEX_CHINESE = "[\\u4e00-\\u9fa5]";// 中文正则
//        Pattern pat = Pattern.compile(REGEX_CHINESE);
//        Matcher mat = pat.matcher(s);
//        String a=mat.replaceAll("");
//        if(a==null||a.equals("")){
//            return null;
//        }
//        try {
//            return Double.valueOf(a);
//        } catch (NumberFormatException e) {
//            return null;
//        }
        String string = s.replaceAll(REGEX_CHINESE, "");
        try {
            return Double.valueOf(string);
        } catch (NumberFormatException e) {
            return null;
        }

    }

    public static Double format(Double s,String format){
        if(s==null){
            return null;
        }
        String str = String.format(format,s);  //"%.1f"
        double result = Double.parseDouble(str);
        return result;
    }


}
