package sql;

import model.opds.Book;
import org.apache.ibatis.annotations.Param;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 25.03.2022 14:05
 * tfs ☭ sweat and blood
 */
public interface OpdsMapper {
    void insertBook(@Param("b") Book b);

    Book findBook(@Param("tag") String tag);

    void updateBookEpub(@Param("epubRef") String epubRef, @Param("url") String url);

    void updateBookFb(@Param("fbRef") String fbRef, @Param("url") String url);
}
