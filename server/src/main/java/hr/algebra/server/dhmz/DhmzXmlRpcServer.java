package hr.algebra.server.dhmz;

import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.webserver.WebServer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class DhmzXmlRpcServer {

    public String[] getTemperatures(String cityFragment) throws Exception {
        URL url = new URL("https://vrijeme.hr/hrvatska_n.xml");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url.openStream());

        NodeList gradNodes = doc.getElementsByTagName("Grad");
        List<String> results = new ArrayList<>();
        for (int i = 0; i < gradNodes.getLength(); i++) {
            Element gradElem = (Element) gradNodes.item(i);
            String gradIme = gradElem.getElementsByTagName("GradIme").item(0).getTextContent().trim();

            if (gradIme.toLowerCase().contains(cityFragment.toLowerCase())) {
                String temp = gradElem.getElementsByTagName("Temp").item(0).getTextContent().trim();
                String vlaga = gradElem.getElementsByTagName("Vlaga").item(0).getTextContent().trim();
                String tlak = gradElem.getElementsByTagName("Tlak").item(0).getTextContent().trim();

                results.add(String.format("%s - Temp: %s °C, Vlaga: %s%%, Tlak: %s hPa", gradIme, temp, vlaga, tlak));
            }
        }

        return results.toArray(new String[0]);
    }

    public static void main(String[] args) throws Exception {
        int port = 8082;
        WebServer webServer = new WebServer(port);
        XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();

        PropertyHandlerMapping phm = new PropertyHandlerMapping();
        phm.addHandler("dhmz", DhmzXmlRpcServer.class);

        xmlRpcServer.setHandlerMapping(phm);
        webServer.start();
        System.out.println("XML-RPC server pokrenut na portu " + port);
    }
}