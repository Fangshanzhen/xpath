package com.kettle.demo.javaTest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *  Mapbox 国内数据覆盖不足，可能导致医院搜索为空,可以给定各医院坐标及中心坐标
 */

public class mapbox {
    // 核心配置（替换为你的有效Token）
    private static final String ACCESS_TOKEN = "pk.eyJ1IjoidmRmaGVzIiwiYSI6ImNtaHB5dTByZzBwa2EybHFvOHoya3R3dngifQ.uvD2bPrshv1psdNpcUmcVg";
    private static final String TARGET_AREA = "渭南市"; // 搜索文本
    private static final int TIME_LIMIT_SECONDS = 900; // 15分钟
    private static final String HOSPITAL_CATEGORY = "hospital"; // 医院分类ID
    private static final String LINYI_BBOX = "109.30,34.40,109.85,34.70"; // 临渭区边界框

    public static void main(String[] args) {
        try {
            // 步骤1：用Geocoding v6获取中心点
            Coordinate linyiCenter = getAreaCenterV6(TARGET_AREA);
            if (linyiCenter == null) {
                System.out.println("❌ 无法获取坐标，程序退出");
                return;
            }
            System.out.println("✅ 中心点坐标：" + linyiCenter);

            // 步骤2：Search Box API分类搜索医院
            List<Hospital> allHospitals = searchHospitalsV1(linyiCenter);
            if (allHospitals.isEmpty()) {
                System.out.println("❌ 未搜索到医院，程序退出");
                return;
            }
            System.out.println("✅ 搜索到医院总数：" + allHospitals.size() + " 家");

            // 步骤3：筛选15分钟可达医院
            List<Hospital> reachableHospitals = filterReachableHospitals(linyiCenter, allHospitals);
            System.out.println("\n🚗 15分钟可达医院（共 " + reachableHospitals.size() + " 家）：");
            for (int i = 0; i < reachableHospitals.size(); i++) {
                Hospital h = reachableHospitals.get(i);
                System.out.printf(
                        "%d. 名称：%s%n   地址：%s%n   坐标：%s%n   驾车耗时：%d分%d秒%n%n",
                        i + 1, h.name, h.fullAddress, h.coordinate, h.durationSeconds / 60, h.durationSeconds % 60
                );
            }

        } catch (Exception e) {
            System.out.println("程序执行异常：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 步骤1：基于Geocoding API获取区域中心点
     * 文档：https://docs.mapbox.com/api/search/geocoding/#forward-geocoding
     * https://api.mapbox.com/search/geocode/v6/forward?q={search_text}
     */
    private static Coordinate getAreaCenterV6(String areaName) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        // 最新Geocoding v6端点：https://api.mapbox.com/search/geocode/v6/forward
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("api.mapbox.com")
                .addPathSegment("search")
                .addPathSegment("geocode")
                .addPathSegment("v6")
                .addPathSegment("forward")
                .addQueryParameter("q", areaName) // 搜索文本（必传）
                .addQueryParameter("access_token", ACCESS_TOKEN)
                .addQueryParameter("country", "cn") // 限定中国（ISO 3166代码）
                .addQueryParameter("types", "place") // 关键：指定“城市”类型（地级市级别）
                .addQueryParameter("limit", "1") // 只返回1个最匹配结果
                .build();
        System.out.println("1  "+httpUrl);

        Request request = new Request.Builder().url(httpUrl).build();
        Response response = client.newCall(request).execute();

        // 调试信息
        System.out.println("Geocoding v6 URL：" + httpUrl);
        System.out.println("Geocoding v6 响应码：" + response.code());
        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "无错误信息";
            System.out.println("Geocoding v6 错误详情：" + errorBody);
            return null;
        }

        // 解析v6响应（GeoJSON格式，坐标在features[0].geometry.coordinates）
        String responseBody = response.body().string();
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray features = json.getAsJsonArray("features");
        if (features.size() == 0) {
            System.out.println("未查询到[" + areaName + "]，响应内容：" + responseBody);
            return null;
        }

        JsonObject feature = features.get(0).getAsJsonObject();
        JsonArray coordinates = feature.getAsJsonObject("geometry").getAsJsonArray("coordinates");
        double lon = coordinates.get(0).getAsDouble(); // 经度（v6保持[lon, lat]顺序）
        double lat = coordinates.get(1).getAsDouble(); // 纬度
        return new Coordinate(lon, lat);
    }

    /**
     * 步骤2：查找医院
     * https://docs.mapbox.com/api/search/search-box/
     * https://api.mapbox.com/search/searchbox/v1/forward?q={search_text}
     * 放弃分类搜索，改用关键词 + 区域组合搜索
     * https://api.mapbox.com/search/searchbox/v1/category/{canonical_category_id}
     */
    private static List<Hospital> searchHospitalsV1(Coordinate center) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        // 最新分类搜索端点：https://api.mapbox.com/search/geocode/v6/category
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("api.mapbox.com")
                .addPathSegment("search")
                .addPathSegment("searchbox")
                .addPathSegment("v1")
            //    .addPathSegment("category")
             //   .addPathSegment(HOSPITAL_CATEGORY) // 医院分类ID作为路径参数（关键修复）
              //  .addPathSegment("healthcare")  // 尝试兼容 healthcare 大类（包含医院、诊所等）替代原有的 "hospital"


                .addPathSegment("forward")
                .addQueryParameter("q", "渭南市 医院") // 增加城市名限定，提高相关性
                .addQueryParameter("access_token", ACCESS_TOKEN)
           //     .addQueryParameter("proximity", center.lon + "," + center.lat) // 中心点附近优先
                .addQueryParameter("country", "cn") // 限定中国
                .addQueryParameter("limit", "10") // Limit must be in range [1,10]
                .build();
        System.out.println("分类搜索URL "+httpUrl);
        Request request = new Request.Builder().url(httpUrl).build();
        Response response = client.newCall(request).execute();

        // 调试信息
        System.out.println("医院搜索v6 URL：" + httpUrl);
        System.out.println("医院搜索v6 响应码：" + response.code());
        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "无错误信息";
            System.out.println("医院搜索v6 错误详情：" + errorBody);
            return new ArrayList<>();
        }

        // 解析医院列表
        String responseBody = response.body().string();
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray features = json.getAsJsonArray("features");
        List<Hospital> hospitals = new ArrayList<>();

        // 解析医院列表时，对地址字段做容错处理
        for (int i = 0; i < features.size(); i++) {
            JsonObject feature = features.get(i).getAsJsonObject();
            JsonObject properties = feature.getAsJsonObject("properties");

            // 1. 医院名称（必须字段，若不存在用默认值）
            String name = properties.has("name")
                    ? properties.get("name").getAsString()
                    : "未知医院";

            // 2. 完整地址（非必须字段，分多级容错）
            String fullAddress;
            if (properties.has("full_address")) {
                // 优先使用 full_address
                fullAddress = properties.get("full_address").getAsString();
            } else if (properties.has("address")) {
                // 若没有 full_address，尝试 address 字段
                fullAddress = properties.get("address").getAsString();
            } else if (properties.has("place_formatted")) {
                // 部分结果可能用 place_formatted 显示地址
                fullAddress = properties.get("place_formatted").getAsString();
            } else {
                // 所有字段都不存在时，用中心点所在区域兜底
                fullAddress = "渭南市（地址未明确）";
            }

            // 3. 坐标（必须字段，Mapbox 地理编码必返回）
            JsonArray coordinates = feature.getAsJsonObject("geometry").getAsJsonArray("coordinates");
            double lon = coordinates.get(0).getAsDouble();
            double lat = coordinates.get(1).getAsDouble();

            hospitals.add(new Hospital(name, fullAddress, new Coordinate(lon, lat)));
        }
        return hospitals;
    }

    /**
     * https://docs.mapbox.com/api/navigation/directions/
     * 步骤3：路径规划（沿用Directions API，与v6兼容）
     */
    private static List<Hospital> filterReachableHospitals(Coordinate origin, List<Hospital> hospitals) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        List<Hospital> reachableHospitals = new ArrayList<>();
        String originCoord = origin.lon + "," + origin.lat;

        for (Hospital hospital : hospitals) {
            String destCoord = hospital.coordinate.lon + "," + hospital.coordinate.lat;
            String coordinates = originCoord + ";" + destCoord;

            HttpUrl httpUrl = new HttpUrl.Builder()
                    .scheme("https")
                    .host("api.mapbox.com")
                    .addPathSegment("directions")
                    .addPathSegment("v5")
                    .addPathSegment("mapbox")
                    .addPathSegment("driving")
                    .addPathSegment(coordinates + ".json")
                    .addQueryParameter("access_token", ACCESS_TOKEN)
                    .addQueryParameter("alternatives", "false")  //alternatives=false：不返回备选路线
                    .addQueryParameter("overview", "simplified")
                    .build();
            System.out.println("3  "+httpUrl);
            Request request = new Request.Builder().url(httpUrl).build();
            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                System.out.println("路径规划失败：" + hospital.name + "，响应码=" + response.code());
                continue;
            }

            String responseBody = response.body().string();
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray routes = json.getAsJsonArray("routes");
            if (routes.size() == 0) continue;

            int durationSeconds = routes.get(0).getAsJsonObject().get("duration").getAsInt();
            if (durationSeconds <= TIME_LIMIT_SECONDS) {
                hospital.durationSeconds = durationSeconds;
                reachableHospitals.add(hospital);
            }
        }
        return reachableHospitals;
    }

    // 坐标模型
    static class Coordinate {
        double lon;
        double lat;

        Coordinate(double lon, double lat) {
            this.lon = lon;
            this.lat = lat;
        }

        @Override
        public String toString() {
            return String.format("(%.6f, %.6f)", lon, lat);
        }
    }

    // 医院模型
    static class Hospital {
        String name;
        String fullAddress;
        Coordinate coordinate;
        int durationSeconds;

        Hospital(String name, String fullAddress, Coordinate coordinate) {
            this.name = name;
            this.fullAddress = fullAddress;
            this.coordinate = coordinate;
        }
    }
}