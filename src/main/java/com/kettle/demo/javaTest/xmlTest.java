package com.kettle.demo.javaTest;


import com.kettle.demo.utils.SqlserverTransUtils;
import com.kettle.demo.utils.xmlUtils;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.database.DatabaseMeta;
//import org.pentaho.di.core.KettleEnvironment;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;


public class xmlTest {
    public static void main(String[] args) throws Exception {
        SqlserverTransUtils.transformData("sqlserver","http://10.80.116.73/api-gate/zuul","mydatabase","mydatabase.dbo",
               "127.0.0.1","1433","sa","123456","d65e5127f9774c52ae48653fd00de074","client1","1000",
                "dbo@emrpif"
                );


    }
}




