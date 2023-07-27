package com.kettle.demo.utils;



public class BirthUtils {

    public static String getBirth(String idCard) {
        if (idCard.length() != 18 && idCard.length() != 15) {
            return "请输入正确的身份证号码";
        }
        if (idCard.length() == 18) {
            String year = idCard.substring(6).substring(0, 4);// 得到年份
            String month = idCard.substring(10).substring(0, 2);// 得到月份
            String day = idCard.substring(12).substring(0, 2);// 得到日
            return (year + "-" + month + "-" + day);
        } else if (idCard.length() == 15) {
            String year = "19" + idCard.substring(6, 8);// 年份
            String month = idCard.substring(8, 10);// 月份
            String day = idCard.substring(10, 12);// 得到日
            return (year + "-" + month + "-" + day);
        }
        return null;
    }

}
