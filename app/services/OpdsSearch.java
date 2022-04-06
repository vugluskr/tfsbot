package services;

import com.google.inject.ImplementedBy;
import model.opds.Book;
import services.impl.OpdsSearchFlibusta;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 06.04.2022 12:51
 * tfs ☭ sweat and blood
 */
@ImplementedBy(OpdsSearchFlibusta.class)
public interface OpdsSearch {
    CompletionStage<List<Book>> search(String query);
}
