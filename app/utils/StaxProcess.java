package utils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 25.03.2022 13:44
 * tfs ☭ sweat and blood
 */
public class StaxProcess implements AutoCloseable {
    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    private final XMLStreamReader reader;

    public StaxProcess(final InputStream is) throws XMLStreamException {
        reader = FACTORY.createXMLStreamReader(is);
    }

    public XMLStreamReader getReader() {
        return reader;
    }

    public String getLocalName() {return reader.getLocalName();}

    @Override
    public void close() {
        if (reader != null) try {reader.close();} catch (XMLStreamException ignore) {}
    }

    public int next() throws XMLStreamException {return reader.next();}

    public boolean hasNext() throws XMLStreamException {return reader.hasNext();}

    public boolean doUntil(final int stopEvent, final String value) throws XMLStreamException {
        while (reader.hasNext())
            if (reader.next() == stopEvent && value.equals(reader.getLocalName()))
                return true;

        return false;
    }

    public boolean startElement(final String element, final String parent) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();

            if (parent != null && event == XMLEvent.END_ELEMENT && parent.equals(reader.getLocalName()))
                return false;

            if (event == XMLEvent.START_ELEMENT && element.equals(reader.getLocalName()))
                return true;
        }

        return false;
    }

    public String getAttribute(final String name) {
        return getAttribute(name, null);
    }

    public String getAttribute(final String name, final String namespace) {
        return reader.getAttributeValue(namespace, name);
    }

    public String getText() throws XMLStreamException {
        return reader.getElementText();
    }
}
