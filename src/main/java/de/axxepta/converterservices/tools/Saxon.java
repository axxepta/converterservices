package de.axxepta.converterservices.tools;

import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.s9api.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.util.*;

public class Saxon {

    private ErrorListener _err;
    private Processor processor = new Processor(false);

    public static final String XQUERY_NO_CONTEXT = "NO:CONTEXT";
    public static final String XQUERY_OUTPUT = ":OUTPUT:";
    public static final String XS_BOOLEAN = "xs:boolean";
    public static final String XS_INT = "xs:int";
    public static final String XS_FLOAT = "xs:float";
    public static final String XS_STRING = "xs:string";
    public static final String NODE = "node()";

    public void setErrorListener(ErrorListener err){
        _err = err;
    }

    public void transform(String sourceFile, String xsltFile, String resultFile, String... parameters)
        throws TransformerException
    {
        String validate = Arrays.stream(parameters).anyMatch(n -> n.toUpperCase().startsWith("DTD_VALIDATION") && n.toLowerCase().contains("true")) ?
                "true" : "false";
        transform(sourceFile, xsltFile, resultFile, validate,
                Arrays.stream(parameters).filter(n -> !n.toUpperCase().startsWith("DTD_VALIDATION")).toArray(String[]::new));
    }

    private void transform(String sourceFile, String xsltFile, String resultFile, String validateDTD, String... parameters)
        throws TransformerException
    {
/*        if (validateDTD.equals("false")) {
            createDummyDTDifNE(sourceFile);
        }*/
        TransformerFactory tFactory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
        try {
            tFactory.setAttribute(FeatureKeys.MESSAGE_EMITTER_CLASS, "net.sf.saxon.serialize.MessageWarner");
            tFactory.setAttribute(FeatureKeys.DTD_VALIDATION, validateDTD);
            tFactory.setAttribute(FeatureKeys.EXPAND_ATTRIBUTE_DEFAULTS, "false");

            if(_err != null) tFactory.setErrorListener(_err);
        } catch(Exception e) {
            System.out.println("Error setting transformer factory attributes " + e.getMessage());
        }
//        try {
            Transformer transformer =  tFactory.newTransformer(new StreamSource(new File(xsltFile)));

            if (parameters.length > 0) {
                for (String singleParam : parameters) {
                    String param[] = singleParam.split("=");
                    transformer.setParameter(param[0], param[1]);
                }
            }
            transformer.transform(new StreamSource(new File(sourceFile)), new StreamResult(new File(resultFile)));
//        }
    }

    private void createDummyDTDifNE(String xmlFile) {
        String[] dtd = extractDTD(xmlFile);
        System.out.println("#########   DTD ##############  " + dtd[2]);
    }

    /**
     * Extracts document type name, public identifier or null and system identifier or null of the DTD
     * @param xmlFile name of the XML file of which the DTD information shall be extracted
     */
    public String[] extractDTD(String xmlFile) {
        DocTypeExtractionHandler handler = new DocTypeExtractionHandler();
        File file = new File(xmlFile);
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            parser.parse(file, handler);
        } catch (SAXException | IOException | ParserConfigurationException pc) {/**/}
        return handler.getDocType();
    }

    public static boolean validateXML(String xmlFile, String ngFile){
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = parser.parse(new File(xmlFile));
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.RELAXNG_NS_URI);
            Schema schema = factory.newSchema(new File(ngFile));
            Validator validator = schema.newValidator();
            validator.validate(new DOMSource(document));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    public static String standardOutputFile(String xsltPath) {
        String file = IOUtils.filenameFromPath(xsltPath);
        int pos = file.contains(".") ? file.indexOf(".") : file.length();
        return file.substring(0, pos) + ".xml";
    }

    private static Document getDOM() throws SaxonApiException {
        DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
        try {
            return dFactory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new SaxonApiException(e);
        }
    }

    public static Document loadDOM(String file) throws ParserConfigurationException, SAXException, IOException {
        File xmlFile = new File(file);
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return builder.parse(xmlFile);
    }

    public static Document stringToDOM(String xml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return builder.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));
    }

    /**
     * Transform a Node to it's String representation
     * @param node XML node
     * @param omitXmlDeclaration omit XML declaration in String, set to "yes|no" or "true|false" or "1|0"
     * @param indent indent, set to "yes" or "no"
     * @return
     */
    public static String nodeToString(Node node, String omitXmlDeclaration, String indent) throws TransformerException {
        StringWriter sw = new StringWriter();
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration);
        t.setOutputProperty(OutputKeys.INDENT, indent);
        t.transform(new DOMSource(node), new StreamResult(sw));
        return sw.toString();
    }

    /**
     * Transform a Node to it's String representation
     * @param nodeList XML node
     * @param omitXmlDeclaration omit XML declaration in String, set to "yes|no" or "true|false" or "1|0"
     * @param indent indent, set to "yes" or "no"
     * @return
     */
    public static String nodeListToString(NodeList nodeList, String omitXmlDeclaration, String indent) throws TransformerException {
        StringWriter sw = new StringWriter();
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration);
        t.setOutputProperty(OutputKeys.INDENT, indent);
        for (int i = 0, len = nodeList.getLength(); i < len; i++) {
            t.transform(new DOMSource(nodeList.item(i)), new StreamResult(sw));
        }
        return sw.toString();
    }

    private static List xqueryListOutput(XdmValue result, String type) throws SaxonApiException {
        List output = new ArrayList();
        for (XdmItem item : result) {
            switch (type) {
                case XS_BOOLEAN: output.add(new Boolean(((XdmAtomicValue) item).getBooleanValue()));
                    break;
                case XS_FLOAT: output.add(new Double(((XdmAtomicValue) item).getDoubleValue()));
                    break;
                case XS_INT: output.add(new Long(((XdmAtomicValue) item).getLongValue()));
                    break;
                default: output.add(item.getStringValue());
            }
        }
        return output;
    }

    public Object execXQuery(String query, XdmItem context, Map<QName, XdmItem> bindings, String outputType) throws SaxonApiException {
        Object output;
        XQueryCompiler comp = processor.newXQueryCompiler();
        XQueryExecutable exp = comp.compile(query);
        XQueryEvaluator qe = exp.load();
        for (Map.Entry<QName, XdmItem> binding : bindings.entrySet()) {
            qe.setExternalVariable(binding.getKey(), binding.getValue());
        }
        if (context != null) {
            qe.setContextItem(context);
        }
        if (outputType.equals(NODE)) {
            Document dom = getDOM();
            qe.run(new DOMDestination(dom));
            output = dom;
        } else {
            XdmValue result = qe.evaluate();
            output = xqueryListOutput(result, outputType);
        }
        return output;
    }

    /**
     *
     * @param query The query, possibly containing binding definitions
     * @param contextFile An XML file's path which will be loaded as context of the query or, if none provided, the
     *                   constant <code>Saxon.XQUERY_NO_CONTEXT</code>.
     * @param params Variable bindings and output type in a new-line separated String. Each line is expected to contain
     *               a binding definition in the form <code>varName = value as type</code>,
     *               the optional output definition in the form <code>Saxon.XQUERY_OUTPUT as type</code>.
     *               The standard output type is DOM, denoted by Saxon.NODE.
     * @return Result of the query, this can be a list of the basic types String, Integer, Boolean, or an XML DOM
     * @throws SaxonApiException If the query is not a valid XQuery or the input does not match the expected types
     */
    public Object xquery(String query, String contextFile, String... params) throws SaxonApiException{
        List<String> bindings = new ArrayList<>(Arrays.asList(params));
        String outputType = NODE;
        for (int i = bindings.size() - 1; i >= 0; i--) {
            String line = bindings.get(i);
            if (line.startsWith(XQUERY_OUTPUT)) {
                outputType = line.substring(line.lastIndexOf(" as ") + 4);
                bindings.remove(i);
            }
        }
        return xquery(query, contextFile, bindings, outputType);
    }

    public Object xquery(String query, String contextFile, List<String> bindingStrings, String outputType) throws SaxonApiException{
        if (StringUtils.isNoStringOrEmpty(outputType)) {
            outputType = NODE;
        }
        Map<QName, XdmItem> bindings = new HashMap<>();
        for (String line : bindingStrings) {
            String name = line.split(" *= *")[0];
            int endPos = line.lastIndexOf(" as ");
            String type = endPos == -1 ? XS_STRING : line.substring(endPos + 4);
            String val = line.substring(0, endPos).split(" *= *")[1];
            bindings.put(new QName(name), bindingVal(val, type));
        }
        XdmItem context = null;
        if (!contextFile.equals(XQUERY_NO_CONTEXT)) {
            context = processor.newDocumentBuilder().build(new StreamSource(new File(contextFile)));
        }
        return execXQuery(query, context, bindings, outputType);
    }

    private XdmItem bindingVal(String val, String type) throws SaxonApiException {
        switch (type) {
            case XS_BOOLEAN: return val.toLowerCase().contains("true") ? new XdmAtomicValue(true) : new XdmAtomicValue(false);
            case XS_FLOAT: return new XdmAtomicValue(Float.parseFloat(val));
            case XS_INT: return new XdmAtomicValue(Integer.parseInt(val));
            case NODE: return processor.newDocumentBuilder().build(new StreamSource(new File(val)));
            default: return new XdmAtomicValue(val);
        }
    }

    public static void saveDOM(Document dom, String fileName, String... charset) throws TransformerException, IOException {
        String encoding = (charset.length > 0) ? charset[0] : "UTF-8";
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
        StreamResult fileResult = new StreamResult(new FileWriter(fileName));
        transformer.transform(new DOMSource(dom), fileResult);

/*        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
        StringWriter stringWriter = new StringWriter();
        transformer.transform(new DOMSource(dom), new StreamResult(stringWriter));
        String domString = stringWriter.getBuffer().toString();
        IOUtils.saveStringToFile(domString, fileName);*/
    }
}
