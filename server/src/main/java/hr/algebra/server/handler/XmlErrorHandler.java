package hr.algebra.server.handler;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import java.util.ArrayList;
import java.util.List;

public class XmlErrorHandler implements ErrorHandler {

    private final List<SAXParseException> exceptions = new ArrayList<>();

    @Override
    public void warning(SAXParseException exception) {
        exceptions.add(exception);
    }

    @Override
    public void error(SAXParseException exception) {
        exceptions.add(exception);
    }

    @Override
    public void fatalError(SAXParseException exception) {
        exceptions.add(exception);
    }

    public List<SAXParseException> getExceptions() {
        return exceptions;
    }
}
