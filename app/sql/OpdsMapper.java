package sql;

import model.opds.Book;
import model.opds.Folder;
import model.opds.Opds;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 25.03.2022 14:05
 * tfs ☭ sweat and blood
 */
public interface OpdsMapper {
    void insertOpds(@Param("o") Opds o);

    void updateFolder(@Param("f") Folder f);

    void insertFolder(@Param("f") Folder f);

    void updateBook(@Param("b") Book b);

    void insertBook(@Param("b") Book b);

    boolean opdsExists(@Param("url") String url);

    boolean opdsExhausted(@Param("url") String url, @Param("time") LocalDateTime now);

    Opds findRawOpdsByUrl(@Param("url") String url);

    List<Folder> selectChilds(@Param("id") long id);

    List<Book> selectBooks(@Param("folderId") long id);
}