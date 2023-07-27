package com.kettle.demo.utils;

public class EyeUtils {

    public static Double getEye(String eye, String s) {

        if (eye != null && !eye.contains("/")) {
            if (s.equals("左")) {
                if (eye.contains("左") & eye.contains("右")) {
                    int a = eye.indexOf("左");
                    int b = eye.indexOf("右");
                    String result = null;
                    if (b > a) {
                        result = eye.substring(a + 1, b);
                    } else {
                        result = eye.substring(a + 1);
                    }
                    if (result.contains("：")) {
                        result = result.replace("：", "");
                    }
                    if (result.contains(":")) {
                        result = result.replace(":", "");
                    }
                    return NumberUtils.getNumber(result);
                }
                try {
                    return NumberUtils.getNumber(eye);
                } catch (Exception e) {
                    return null;
                }
            }

            if (s.equals("右")) {
                if (eye.contains("左") & eye.contains("右")) {
                    int a = eye.indexOf("左");
                    int b = eye.indexOf("右");
                    String result = null;
                    if (b > a) {
                        result = eye.substring(b + 1);
                    } else {
                        result = eye.substring(b + 1, a);
                    }
                    if (result.contains("：")) {
                        result = result.replace("：", "");
                    }
                    if (result.contains(":")) {
                        result = result.replace(":", "");
                    }
                    return NumberUtils.getNumber(result);
                }
            }

        }
        if (eye != null && eye.contains("/")) {
            if (s.equals("左")) {
                int a = eye.indexOf("/");
                String result = eye.substring(0, a);
                return NumberUtils.getNumber(result);
            }
            if (s.equals("右")) {
                int a = eye.indexOf("/");
                String result = eye.substring(a + 1);
                return NumberUtils.getNumber(result);
            }
        }


        return null;

    }

}
