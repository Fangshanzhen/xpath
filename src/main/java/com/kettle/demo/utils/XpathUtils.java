package com.kettle.demo.utils;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.w3c.dom.Document;


public class XpathUtils {
    public static String getValueByXpath(String xPath, String html) {
        TagNode tagNode = new HtmlCleaner().clean(html);
        String value = null;

        try {
            Document doc = new DomSerializer(new CleanerProperties()).createDOM(tagNode);
            XPath xpath = XPathFactory.newInstance().newXPath();
            value = (String) xpath.evaluate(xPath, doc, XPathConstants.STRING);
//            arr=tagNode.evaluateXPath(xPath);
        } catch (Exception e) {
            System.out.println("Extract value error. " + e.getMessage());
            e.printStackTrace();
        }
        return value;
    }
}
