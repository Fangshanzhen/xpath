package com.kettle.demo.javaTest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
//

public class gaode {
    // 高德地图核心配置（替换为你的高德Key，需申请并勾选POI搜索、路径规划权限）
    private static final String AMAP_KEY = "0a267a4e43651d21de282355e763aad2";
    private static final String TARGET_CITY = "渭南市"; // 目标城市
    private static final int TIME_LIMIT_SECONDS = 900; // 15分钟=900秒
    private static final int SEARCH_RADIUS = 30000; // 医院搜索半径（30公里，覆盖全市）

    public static void main(String[] args) {
        try {
            // 步骤1：获取渭南市区中心坐标（基于高德地理编码API）
            Coordinate weinanCenter = getCityCenterCoordinate(TARGET_CITY);
            if (weinanCenter == null) {
                System.out.println("❌ 无法获取渭南市区坐标，程序退出");
                return;
            }
            System.out.println("✅ 渭南市区中心坐标：" + weinanCenter);

            // 步骤2：搜索渭南市所有医院（基于高德POI搜索API）
            List<Hospital> allHospitals = searchHospitalsInCity(weinanCenter);
            if (allHospitals.isEmpty()) {
                System.out.println("❌ 未搜索到渭南市医院，程序退出");
                return;
            }
            System.out.println("✅ 搜索到渭南市医院总数：" + allHospitals.size() + " 家");

            // 步骤3：筛选15分钟（驾车）可达的医院（基于高德路径规划API）
            List<Hospital> reachableHospitals = filterReachableHospitals(weinanCenter, allHospitals);
            System.out.println("\n🚗 渭南市15分钟可达医院（共 " + reachableHospitals.size() + " 家）：");
            for (int i = 0; i < reachableHospitals.size(); i++) {
                Hospital h = reachableHospitals.get(i);
                System.out.printf(
                        "%d. 医院名称：%s%n   医院地址：%s%n   医院坐标：%s%n   驾车耗时：%d分%d秒%n%n",
                        i + 1, h.name, h.fullAddress, h.coordinate, h.durationSeconds / 60, h.durationSeconds % 60
                );
            }

        } catch (Exception e) {
            System.out.println("程序执行异常：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 步骤1：获取城市中心坐标（高德地理编码API）
     * 文档：https://lbs.amap.com/api/webservice/guide/api/geocode
     */
    private static Coordinate getCityCenterCoordinate(String cityName) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // 高德地理编码API：将城市名称转换为坐标
        String url = "https://restapi.amap.com/v3/geocode/geo?" +
                "address=" + cityName + "&" + // 地址：渭南市
                "city=" + cityName + "&" +   // 限定城市（避免同名）
                "output=json&" +             // 返回格式JSON
                "key=" + AMAP_KEY;           // 你的高德Key

        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();

        System.out.println("地理编码API URL：" + url);
        if (!response.isSuccessful()) {
            System.out.println("地理编码API失败，响应码：" + response.code());
            return null;
        }

        // 解析响应（高德地理编码返回格式）
        String responseBody = response.body().string();
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        if (!"1".equals(json.get("status").getAsString())) {
            System.out.println("地理编码API返回失败：" + json.get("info").getAsString());
            return null;
        }

        JsonArray geocodes = json.getAsJsonArray("geocodes");
        if (geocodes.size() == 0) {
            System.out.println("未查询到[" + cityName + "]的坐标");
            return null;
        }

        // 提取中心点坐标（高德返回格式：lon,lat）
        String location = geocodes.get(0).getAsJsonObject().get("location").getAsString();
        String[] lonLat = location.split(",");
        double lon = Double.parseDouble(lonLat[0]);
        double lat = Double.parseDouble(lonLat[1]);
        return new Coordinate(lon, lat);
    }

    /**
     * 步骤2：搜索城市内所有医院（高德POI搜索API）
     * 文档：https://lbs.amap.com/api/webservice/guide/api/search
     */
    private static List<Hospital> searchHospitalsInCity(Coordinate cityCenter) throws IOException, InterruptedException {
        List<Hospital> result = new ArrayList<>();
        String[] highLevelKeywords = {
                "第一医院", "第二医院", "第三医院",
                "中心医院", "人民医院", "中医医院",
                "附属医院", "妇幼保健院", "专科医院"
        };

        int offset = 50;
        int page = 1;
        int totalPages = 1;
        int totalCount = 0; // 存储初始总结果数（从第1页获取）
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        do {
            String url = "https://restapi.amap.com/v3/place/text?" +
                    "keywords=医院&" +
                    "city=" + TARGET_CITY + "&" +
                    "location=" + cityCenter.lon + "," + cityCenter.lat + "&" +
                    "radius=" + SEARCH_RADIUS + "&" +
                    "offset=" + offset + "&" +
                    "page=" + page + "&" +
                    "key=" + AMAP_KEY;

            System.out.println("正在请求第" + page + "页，URL：" + url);
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                System.out.println("第" + page + "页请求失败，响应码：" + response.code());
                page++;
                continue; // 跳过失败页，继续下一页
            }

            String responseBody = response.body().string();
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (!"1".equals(json.get("status").getAsString())) {
                System.out.println("第" + page + "页返回失败：" + json.get("info").getAsString());
                page++;
                continue;
            }

            // 关键优化：仅第1页计算总结果数和总页数，后续页不再重新计算
            if (page == 1) {
                totalCount = json.get("count").getAsInt();
                totalPages = (totalCount + offset - 1) / offset; // 基于初始总结果数计算9页
                System.out.println("初始总结果数：" + totalCount + "，总页数：" + totalPages);
            } else {
                // 后续页忽略返回的count，沿用初始totalPages
                System.out.println("第" + page + "页请求成功（沿用初始总页数）");
            }

            // 解析当前页医院数据（逻辑不变）
            JsonArray pois = json.getAsJsonArray("pois");
            List<Hospital> currentPageHospitals = new ArrayList<>();
            for (int i = 0; i < pois.size(); i++) {
                JsonObject poi = pois.get(i).getAsJsonObject();
                try {
                    // 1. 名称容错
                    String name = poi.has("name") && !poi.get("name").isJsonNull()
                            ? poi.get("name").getAsString().trim()
                            : "未知医院";

                    // 2. 地址容错（不变）
                    String address;
                    if (poi.has("address") && !poi.get("address").isJsonNull()) {
                        if (poi.get("address").isJsonPrimitive()) {
                            address = poi.get("address").getAsString().trim();
                            address = address.isEmpty() ? TARGET_CITY + "（地址未公开）" : address;
                        } else if (poi.get("address").isJsonArray()) {
                            JsonArray addrArr = poi.get("address").getAsJsonArray();
                            address = addrArr.size() > 0 && addrArr.get(0).isJsonPrimitive()
                                    ? addrArr.get(0).getAsString().trim()
                                    : TARGET_CITY + "（地址格式异常）";
                        } else {
                            address = TARGET_CITY + "（地址未明确）";
                        }
                    } else {
                        String adname = poi.has("adname") && !poi.get("adname").isJsonNull()
                                ? poi.get("adname").getAsString()
                                : "";
                        address = TARGET_CITY + adname + "（地址未公开）";
                    }

                    // 3. 坐标容错（不变）
                    String locationStr = "";
                    if (poi.has("location") && !poi.get("location").isJsonNull()) {
                        if (poi.get("location").isJsonPrimitive()) {
                            locationStr = poi.get("location").getAsString().trim();
                        } else if (poi.get("location").isJsonArray()) {
                            JsonArray locArr = poi.get("location").getAsJsonArray();
                            locationStr = locArr.size() > 0 ? locArr.get(0).getAsString().trim() : "";
                        }
                    }
                    if (locationStr.isEmpty() || !locationStr.contains(",")) {
                        System.out.println("第" + page + "页第" + (i+1) + "条坐标异常，跳过");
                        continue;
                    }
                    String[] lonLat = locationStr.split(",");
                    if (lonLat.length != 2) {
                        System.out.println("第" + page + "页第" + (i+1) + "条坐标拆分失败，跳过");
                        continue;
                    }
                    double lon = Double.parseDouble(lonLat[0]);
                    double lat = Double.parseDouble(lonLat[1]);

                    currentPageHospitals.add(new Hospital(name, address, new Coordinate(lon, lat)));
                } catch (Exception e) {
                    System.out.println("第" + page + "页第" + (i+1) + "条解析失败：" + e.getMessage());
                    continue;
                }
            }

            // 筛选当前页符合条件的医院
            for (Hospital hospital : currentPageHospitals) {
                for (String keyword : highLevelKeywords) {
                    if (hospital.name.contains(keyword)) {
                        result.add(hospital);
                        break;
                    }
                }
            }

            System.out.println("第" + page + "页解析完成，新增符合条件医院：" + currentPageHospitals.size() + " 家");

            // 延迟1秒，降低QPS压力（免费版建议延长）
            Thread.sleep(1000);

            page++;

        } while (page <= totalPages); // 强制循环到初始计算的9页

        System.out.println("✅ 分页请求完成，共筛选出二级及以上医院：" + result.size() + " 家");
        return result;
    }

    /**
     * 步骤3：筛选15分钟可达医院（高德路径规划API）
     * 文档：https://lbs.amap.com/api/webservice/guide/api/direction
     */
    /**
     * 步骤3：筛选15分钟可达医院（修复路径规划解析异常）
     */
    private static List<Hospital> filterReachableHospitals(Coordinate origin, List<Hospital> hospitals) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        List<Hospital> reachableHospitals = new ArrayList<>();
        String originCoord = origin.lon + "," + origin.lat; // 起点坐标

        for (Hospital hospital : hospitals) {
            String destCoord = hospital.coordinate.lon + "," + hospital.coordinate.lat; // 终点坐标

            // 高德驾车路径规划API
            String url = "https://restapi.amap.com/v3/direction/driving?" +
                    "origin=" + originCoord + "&" +
                    "destination=" + destCoord + "&" +
                    "output=json&" +
                    "key=" + AMAP_KEY;

            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                System.out.println("路径规划失败：" + hospital.name + "，响应码：" + response.code());
                continue;
            }

            try {
                String responseBody = response.body().string();
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                // 1. 先判断API返回是否成功
                if (!"1".equals(json.get("status").getAsString())) {
                    System.out.println("路径规划失败：" + hospital.name + "，原因：" + json.get("info").getAsString());
                    continue;
                }

                // 2. 解析route对象（关键修复：route是JsonObject，不是JsonArray）
                if (!json.has("route") || json.get("route").isJsonNull()) {
                    System.out.println("路径规划无结果：" + hospital.name);
                    continue;
                }
                JsonObject route = json.getAsJsonObject("route"); // 正确：获取route对象

                // 3. 解析paths数组（路径列表）
                if (!route.has("paths") || route.get("paths").isJsonNull()) {
                    System.out.println("路径规划无可用路线：" + hospital.name);
                    continue;
                }
                JsonArray paths = route.getAsJsonArray("paths"); // 从route对象中获取paths数组

                // 4. 提取第一条路径的耗时
                if (paths.size() == 0) {
                    System.out.println("路径规划无有效路径：" + hospital.name);
                    continue;
                }
                JsonObject firstPath = paths.get(0).getAsJsonObject();
                int durationSeconds = firstPath.has("duration")
                        ? firstPath.get("duration").getAsInt()
                        : Integer.MAX_VALUE; // 无耗时则视为不可达

                if (durationSeconds <= TIME_LIMIT_SECONDS) {
                    hospital.durationSeconds = durationSeconds;
                    reachableHospitals.add(hospital);
                }

            } catch (Exception e) {
                // 捕获单条路径解析异常，不影响整体
                System.out.println("⚠️  解析[" + hospital.name + "]路径失败：" + e.getMessage());
                continue;
            }
        }
        return reachableHospitals;
    }

    /**
     * 坐标模型（经纬度）
     */
    static class Coordinate {
        double lon; // 经度
        double lat; // 纬度

        Coordinate(double lon, double lat) {
            this.lon = lon;
            this.lat = lat;
        }

        @Override
        public String toString() {
            return String.format("(%.6f, %.6f)", lon, lat);
        }
    }

    /**
     * 医院模型（名称、地址、坐标、驾车耗时）
     */
    static class Hospital {
        String name; // 医院名称
        String fullAddress; // 完整地址
        Coordinate coordinate; // 坐标
        int durationSeconds; // 驾车耗时（秒）

        Hospital(String name, String fullAddress, Coordinate coordinate) {
            this.name = name;
            this.fullAddress = fullAddress;
            this.coordinate = coordinate;
        }
    }
}