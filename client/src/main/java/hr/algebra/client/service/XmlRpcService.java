package hr.algebra.client.service;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

public class XmlRpcService {
    private static final String SERVER_URL = "http://localhost:8082/RPC2";

    public String getTemperatures(String cityFragment) {
        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(SERVER_URL));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);

            Vector<String> params = new Vector<>();
            params.add(cityFragment);

            Object[] result = (Object[]) client.execute("dhmz.getTemperatures", params);

            if (result.length == 0) return "Nema rezultata za traženi grad.";
            StringBuilder sb = new StringBuilder();
            for (Object o : result) {
                sb.append(o.toString()).append("\n");
            }
            return sb.toString();
        } catch (MalformedURLException | XmlRpcException e) {
            return "Greška: " + e.getMessage();
        }
    }
}