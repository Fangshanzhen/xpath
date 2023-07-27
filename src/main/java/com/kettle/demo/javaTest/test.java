package com.kettle.demo.javaTest;

import com.kettle.demo.utils.OracleTransUtils;
import org.pentaho.di.core.database.DatabaseMeta;
//import org.pentaho.di.core.KettleEnvironment;
public class test {
    public static void main(String[] args) throws Exception {

        OracleTransUtils.transformData("oracle","http://10.80.116.73/api-gate/zuul","orcl",
                "HUAYIN","172.16.202.120","1521","huayin","huayin","878a3a2084be4102ba81fd6b9136780d",
                "client8","200","HUAYIN@EMRDCD");

    }
}




