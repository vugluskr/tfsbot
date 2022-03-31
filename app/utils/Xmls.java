package utils;

import model.opds.Book;
import model.opds.Folder;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static utils.TextUtils.getInt;
import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 25.03.2022 13:26
 * tfs ☭ sweat and blood
 */
public class Xmls {
    public static Collection<Folder> getFolders(final NodeList nodes) {
        final List<Folder> list = new ArrayList<>();

        OUTER:
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node el = nodes.item(i);

            final Folder f = new Folder();

            for (int j = 0; j < el.getChildNodes().getLength(); j++) {
                final Node child = el.getChildNodes().item(j);

                switch (child.getNodeName()) {
                    case "category":
                    case "author":
                    case "dc:format":
                    case "dc:language":
                    case "dc:issued":
                        continue OUTER;
                    case "id":
                        f.setTag(child.getTextContent());
                        break;
                    case "title":
                        f.setTitle(child.getTextContent());
                        break;
                    case "content":
                        f.setDesc(child.getTextContent());
                        break;
                    case "link":
                        for (int k = 0; k < child.getAttributes().getLength(); k++)
                            if (child.getAttributes().item(k).getNodeName().equals("rel") && (child.getAttributes().item(k).getNodeValue().endsWith("/sort/new") || child.getAttributes().item(k).getNodeValue().endsWith("/sort/popular")))
                                continue OUTER;
                            else if (child.getAttributes().item(k).getNodeName().equals("href"))
                                f.setPath(child.getAttributes().item(k).getNodeValue());

                        break;
                }
            }

            list.add(f);
        }

        return list;
    }

    public static Collection<Book> getBooks(final NodeList nodes) {
        final List<Book> list = new ArrayList<>();

        for (int i = 0; i < nodes.getLength(); i++) {
            final Node el = nodes.item(i);

            final Book b = new Book();

            for (int j = 0; j < el.getChildNodes().getLength(); j++) {
                final Node child = el.getChildNodes().item(j);

                switch (child.getNodeName()) {
                    case "author":
                        for (int z = 0; z < child.getChildNodes().getLength(); z++)
                            if (child.getChildNodes().item(z).getNodeName().equals("name")) {
                                b.addAuthor(child.getChildNodes().item(z).getTextContent());
                                break;
                            }

                        break;
                    case "dc:issued":
                        b.setYear(getInt(child.getTextContent()));
                        break;
                    case "id":
                        b.setOpdsTag(child.getTextContent());
                        break;
                    case "title":
                        b.setTitle(child.getTextContent());
                        break;
                    case "link":
                        String type = "", href = "", rel = "";

                        for (int k = 0; k < child.getAttributes().getLength(); k++) {
                            final Node a = child.getAttributes().item(k);

                            switch (a.getNodeName()) {
                                case "type":
                                    type = a.getNodeValue();
                                    break;
                                case "href":
                                    href = a.getNodeValue();
                                    break;
                                case "rel":
                                    rel = a.getNodeValue();
                                    break;
                            }
                        }

                        if (rel.equals("http://opds-spec.org/acquisition/open-access")) {
                            if (type.contains("/fb2"))
                                b.setFbLink(href);
                            else if (type.contains("/epub"))
                                b.setEpubLink(href);
                        }

                        break;
                }
            }

            if (b.getYear() > 0 && (!isEmpty(b.getFbLink()) || !isEmpty(b.getEpubLink())))
                list.add(b);
        }

        return list;
    }

}
