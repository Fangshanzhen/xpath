package com.kettle.demo.javaTest;

import com.kettle.demo.utils.DeleteUtils;
import org.pentaho.di.core.KettleEnvironment;

public class test0506 {
    public static void main(String[] args) throws Exception {
        KettleEnvironment.init();
        DeleteUtils.deleteData("oracle","http://10.80.116.73/api-gate/zuul","orcl","HUAYIN","172.16.202.120","1521","huayin",
                "huayin","a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p8","client30","1000","HUAYIN@EMRPIF");
    }
}
