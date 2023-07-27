package com.kettle.demo.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {
    public static long dateDiff(String a, String b) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");// 自定义时间格式

        Calendar calendar_a = Calendar.getInstance();// 获取日历对象
        Calendar calendar_b = Calendar.getInstance();

        Date date_a = null;
        Date date_b = null;

        try {
            date_a = simpleDateFormat.parse(a);//字符串转Date
            date_b = simpleDateFormat.parse(b);
            calendar_a.setTime(date_a);// 设置日历
            calendar_b.setTime(date_b);
        } catch (ParseException e) {
            e.printStackTrace();//格式化异常
        }

        long time_a = calendar_a.getTimeInMillis();
        long time_b = calendar_b.getTimeInMillis();

        long between_days = (time_b - time_a) / (1000 * 3600 * 24);//计算相差天数

        return between_days;
    }


    public static String minuteDiff(String a, String b) {
        if (a.length() == 14 && b.length() == 14) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");// 自定义时间格式

            Calendar calendar_a = Calendar.getInstance();// 获取日历对象
            Calendar calendar_b = Calendar.getInstance();

            Date date_a = null;
            Date date_b = null;

            try {
                date_a = simpleDateFormat.parse(a);//字符串转Date
                date_b = simpleDateFormat.parse(b);
                calendar_a.setTime(date_a);// 设置日历
                calendar_b.setTime(date_b);
            } catch (ParseException e) {
                e.printStackTrace();//格式化异常
            }

            long time_a = calendar_a.getTimeInMillis();
            long time_b = calendar_b.getTimeInMillis();

            long between_minutes = Math.abs(time_b - time_a) / (1000 * 60);//计算相差分钟

            return String.valueOf(between_minutes);
        }
        return null;
    }


    public static String getWeekFirst(int year, int week) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, 0, 1);
        int weeks = 0;
        while ((weeks = calendar.get(Calendar.WEEK_OF_YEAR)) <= week) {
            calendar.add(Calendar.MONTH, 1);
//System.out.println(calendar.get(Calendar.WEEK_OF_YEAR));
        }
        calendar.add(Calendar.MONTH, -1);
//System.out.println(calendar.get(Calendar.WEEK_OF_YEAR));
        while ((weeks = calendar.get(Calendar.WEEK_OF_YEAR)) < week) {
            calendar.add(Calendar.DATE, 1);
        }
        Date date = calendar.getTime();
        calendar.add(calendar.DATE, 7);
        Date date1 = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    public static String dateChange(Date a) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");//设置日期格式DT8
        if (a != null) {
            String time = sdf.format(a);
            return time;
        }
        return null;

    }

    public static String dateChange15(Date a) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");//设置日期格式DT15
        if (a != null) {
            String time = sdf.format(a);
            if (time != null) {
                time = time.substring(0, 8) + "T" + time.substring(8);   //加上T
            }
            return time;
        }
        return null;

    }


}
