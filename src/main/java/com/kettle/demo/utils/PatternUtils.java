package com.kettle.demo.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternUtils {
    public static String matcher(String line, String pattern) {
        // 创建 Pattern 对象
        Pattern r = Pattern.compile(pattern,Pattern.DOTALL);

        // 现在创建 matcher 对象
        Matcher m = r.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
