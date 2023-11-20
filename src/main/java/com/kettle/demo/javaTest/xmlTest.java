package com.kettle.demo.javaTest;


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
//        String filePath = "C:\\Users\\fsz\\Desktop\\.kettle\\shared.xml";
//        xmlUtils.change(filePath,"127.0.0.1","127.0.0.1");
         String a="V_EMRTMR";
        System.out.println(a.substring(2));


    }
}




