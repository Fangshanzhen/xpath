package com.kettle.demo.javaTest;

import com.kettle.demo.utils.pgDeleteUtils;
import org.pentaho.di.core.KettleEnvironment;

public class test0506 {
    public static void main(String[] args) throws Exception {
        KettleEnvironment.init();
        pgDeleteUtils.deleteData("oracle","http://10.80.116.73/api-gate/zuul","orcl","HUAYIN","172.16.202.120","1521","huayin",
                "huayin","d65e5127f9774c52ae48653fd00de074","client1","1000","HUAYIN@EMRPIF");
    }
}
