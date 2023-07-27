package com.kettle.demo.response;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

@Data
public class transformResponse {
    JSONArray jsonArray;
    Integer  code;
    String result;
}
