package services;

import com.google.inject.ImplementedBy;
import model.opds.OpdsBook;
import model.opds.OpdsPage;
import services.impl.OpdsSearchImpl;

import java.io.File;
import java.util.List;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 06.04.2022 12:51
 * tfs ☭ sweat and blood
 */
@ImplementedBy(OpdsSearchImpl.class)
public interface OpdsSearch {
    OpdsPage search(String query, int pageNum);

    File loadFile(OpdsBook book, boolean fb2, boolean epub);

    List<String> resolveGenrePath(String path);

    String resolveGenreName(String genreId);
}
