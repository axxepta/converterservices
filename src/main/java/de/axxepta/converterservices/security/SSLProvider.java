package de.axxepta.converterservices.security;

import de.axxepta.converterservices.tools.Saxon;
import de.axxepta.converterservices.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import spark.Spark;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;

public class SSLProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSLProvider.class);

    private static final String SSL_FILE = "secure.xml";
    private static final String KEYSTORE_LOCATION   = "keystoreLocation";
    private static final String KEYSTORE_PASSWORD   = "keystorePassword";
    private static final String TRUSTSTORE_LOCATION = "truststoreLocation";
    private static final String TRUSTSTORE_PASSWORD = "truststorePassword";

    private static String keystoreLocation;
    private static String keystorePassword;
    private static String truststoreLocation;
    private static String truststorePassword;

    /**
     * Check for a valid XML file <i>secure.xml</i> in the same directory as the JAR. If present it should contain the path (as text content
     * of an XML element <b>keystoreLocation</b>) and password (<b>keystorePassword</b>) of a keystore file generated by the
     * <a href="https://docs.oracle.com/cd/E19509-01/820-3503/ggfen/index.html">Java keytool</a>.
     */
    public static void checkSSL() {
        String path = IOUtils.firstExistingPath(
                IOUtils.pathCombine(IOUtils.jarPath(), SSL_FILE),
                IOUtils.pathCombine(IOUtils.executionContextPath(), SSL_FILE)
        );
        if (!path.equals("")) {
            try {
                loadSecureFile(path);
                Spark.secure(keystoreLocation, keystorePassword, truststoreLocation, truststorePassword);
            } catch (Exception ex) {
                LOGGER.warn("Error while loading file secure.xml, no secure connection enabled. ", ex);
            }
        } else {
            LOGGER.info("File secure.xml not found, no secure connection enabled.");
        }
    }


    private static void loadSecureFile(String sslFile) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException
    {
        keystoreLocation = null;
        keystorePassword = null;
        truststoreLocation = null;
        truststorePassword = null;
        Document dom = Saxon.loadDOM(sslFile);
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();

        Node keyLocNode = (Node) xPath.compile("//" + KEYSTORE_LOCATION).evaluate(dom, XPathConstants.NODE);
        if (keyLocNode != null ){
            keystoreLocation = keyLocNode.getTextContent();
        }

        Node keyPwdNode = (Node) xPath.compile("//" + KEYSTORE_PASSWORD).evaluate(dom, XPathConstants.NODE);
        if (keyPwdNode != null ){
            keystorePassword = keyPwdNode.getTextContent();
        }

        Node trustLocNode = (Node) xPath.compile("//" + TRUSTSTORE_LOCATION).evaluate(dom, XPathConstants.NODE);
        if (trustLocNode != null ){
            truststoreLocation = trustLocNode.getTextContent();
        }

        Node trustPwdNode = (Node) xPath.compile("//" + TRUSTSTORE_PASSWORD).evaluate(dom, XPathConstants.NODE);
        if (trustPwdNode != null ){
            truststorePassword = trustPwdNode.getTextContent();
        }
    }
}