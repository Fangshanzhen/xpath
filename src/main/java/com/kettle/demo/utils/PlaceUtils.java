package com.kettle.demo.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceUtils {
    /**
     * 解析地址
     */
    public static Map<String, String> address(String address) {
        Map<String, String> row = new LinkedHashMap<String, String>();
        String regex = "(?<province>[^省]+自治区|.*?省|.*?行政区)(?<city>[^市]+自治州|.*?地区|.*?行政单位|.+盟|市辖区|.*?市)?(?<county>[^区]+县|.*?区)?(?<town>[^镇]+镇|.*?乡|.*?街道)?(?<village>[^村]+村|.*?弄|.*?路|.*?社区)?(?<other>.*)";
        try {
            Matcher m = Pattern.compile(regex).matcher(address);
            String province = null, city = null, county = null, town = null, village = null, other = null;
            while (m.find()) {

                province = m.group("province");
                row.put("province", province.trim());
                city = m.group("city");
                if (city != null) {
                    row.put("city", city.trim());
                }
                county = m.group("county");
                if (county != null) {
                    row.put("county", county.trim());
                }

                town = m.group("town");
                if (town != null) {
                    row.put("town", town.trim());
                }

                village = m.group("village");
                if (village != null) {
                    row.put("village", village.trim());
                }
                other = m.group("other");
                if (other != null) {
                    row.put("other", other.trim());
                }

            }
        } catch (Exception e) {

        }

        return row;
    }
}
