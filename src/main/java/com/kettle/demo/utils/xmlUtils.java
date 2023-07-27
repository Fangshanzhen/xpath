package com.kettle.demo.utils;

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

public class xmlUtils {

    public static void change(String filePath,String name ,String server) {

        Document document = load(filePath);
        Node root = document.getDocumentElement();

        if (root.hasChildNodes()) {

            NodeList ftpNodes = root.getChildNodes();


            for (int i = 0; i < ftpNodes.getLength(); i++) {
                NodeList ftpList = ftpNodes.item(i).getChildNodes();

                for (int k = 0; k < ftpList.getLength(); k++) {
                    Node subNode = ftpList.item(k);

                    if (subNode.getNodeType() == Node.ELEMENT_NODE
                            && subNode.getNodeName().equals("name") && subNode.getFirstChild().getNodeValue().equals(name)  ) {

                        for (int k1 = 0; k1 < ftpList.getLength(); k1++) {
                            Node subNode1 = ftpList.item(k1);

                            if (subNode1.getNodeType() == Node.ELEMENT_NODE
                                    && subNode1.getNodeName().equals("server")) {
                                subNode1.getFirstChild().setNodeValue(server);
                            }
                        }
                    }
                }


            }
        }

        doc2XmlFile(document, filePath);
    }

    public static Document load(String filename) {
        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new File(filename));
            document.normalize();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return document;
    }

    public static boolean doc2XmlFile(Document document, String filename) {
        boolean flag = true;
        try {

            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();

            // transformer.setOutputProperty(OutputKeys.ENCODING, "GB2312");
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new File(filename));
            transformer.transform(source, result);
        } catch (Exception ex) {
            flag = false;
            ex.printStackTrace();
        }
        return flag;
    }

}
