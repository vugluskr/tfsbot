package utils;

import model.opds.Book;
import model.opds.Folder;
import model.opds.Opds;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    public static Opds makeOpds(final String url, final Document document) {
        final Opds opds = new Opds();

        opds.setUrl(url);
        opds.setTitle(document.getElementsByTagName("title").item(0).getTextContent());
        opds.setUpdated(LocalDateTime.parse(document.getElementsByTagName("updated").item(0).getTextContent(), DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        opds.childs.addAll(getFolders(document.getElementsByTagName("entry")));

        return opds;
    }

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
                    case "updated":
                        f.setUpdated(LocalDateTime.parse(child.getTextContent(), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                        break;
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

        OUTER:
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
                    case "updated":
                        b.setUpdated(LocalDateTime.parse(child.getTextContent(), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                        break;
                    case "id":
                        b.setTag(child.getTextContent());
                        break;
                    case "title":
                        b.setTitle(child.getTextContent());
                        break;
                    case "content":
                        b.setDesc(child.getTextContent());
                        break;
                    case "link":
                        String type = "", href = "";

                        for (int k = 0; k < child.getAttributes().getLength(); k++) {
                            final Node a = child.getAttributes().item(k);

                            if (a.getNodeName().equals("type"))
                                type = a.getNodeValue();
                            else if (a.getNodeName().equals("href"))
                                href = a.getNodeValue();
                        }

                        if (type.contains("/fb2"))
                            b.setFbLink(href);
                        else if (type.contains("/epub"))
                            b.setEpubLink(href);

                        break;
                }
            }

            if (b.getYear() > 0 && (!isEmpty(b.getFbLink()) || !isEmpty(b.getEpubLink())))
                list.add(b);
        }

        return list;
    }

}
