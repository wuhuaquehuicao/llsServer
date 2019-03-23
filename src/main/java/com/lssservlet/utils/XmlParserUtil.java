package com.lssservlet.utils;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlParserUtil {

    private static final String content = "<xml> <ToUserName>< ![CDATA[toUser] ]></ToUserName> <FromUserName>< ![CDATA[fromUser] ]></FromUserName> <CreateTime>1348831860</CreateTime> <MsgType>< ![CDATA[image] ]></MsgType> <PicUrl>< ![CDATA[this is a url] ]></PicUrl> <MediaId>< ![CDATA[media_id] ]></MediaId> <MsgId>1234567890123456</MsgId> </xml>";

    public static void main(String[] args) {
        SAXReader reader = new SAXReader();

        try {
            String data = content.replaceAll(" !", "!");
            data = data.replaceAll(" ]", "]");
            System.out.println("Content: " + data);
            Document document = reader.read(new ByteArrayInputStream(data.getBytes("utf-8")));
            Element rootElement = document.getRootElement();
            traverseElement(rootElement);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }

        // DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // try {
        // String data = content.replaceAll(" ", "");
        // DocumentBuilder builder = factory.newDocumentBuilder();
        // org.w3c.dom.Document document = builder.parse(new ByteArrayInputStream(data.getBytes("utf-8")));
        // org.w3c.dom.Element element = document.getDocumentElement();
        // traverseW3CElement(element);
        // } catch (Exception e) {
        // // TODO: handle exception
        // }
    }

    private static void traverseElement(Element element) {
        if (element != null) {
            String name = element.getName();
            String text = element.getText();
            System.out.println("name: " + name + ", text: " + text);

            List<Element> childs = element.elements();
            if (childs != null && !childs.isEmpty()) {
                for (Element child : childs) {
                    traverseElement(child);
                }
            }
        }
    }

    private static void traverseW3CElement(org.w3c.dom.Element element) {
        if (element != null) {
            NodeList childs = element.getChildNodes();
            for (int index = 0; index < childs.getLength(); index++) {
                Node node = childs.item(index);
                System.out.println("Node type: " + node.getNodeType());
                System.out.println("Node name: " + node.getNodeName());
                System.out.println("Node value: " + node.getNodeValue());
                System.out.println("First child value: " + node.getFirstChild().getNodeValue());
            }
        }
    }
}
