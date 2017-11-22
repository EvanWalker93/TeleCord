package main.java.TCBot;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

class TokenReader {

    //Set branch to 0 for main, 1 for dev
    private int branch = 1;
    private String token = "TEMP";

    String getUserName() {
        if (branch == 0) {
            return "TeleCord";
        }
        if (branch == 1) {
            return "TeleCord_Dev";
        }
        return null;
    }

    String getTokens(String bot) {
        try {
            File inputFile = new File("tokens.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("tokens");

            Node nNode = nList.item(branch);
            Element eElement = (Element) nNode;
            token = eElement.getElementsByTagName(bot).item(0).getTextContent();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return token;
    }
}
