package services.impl;

import model.opds.Book;
import services.OpdsSearch;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 06.04.2022 12:51
 * tfs ☭ sweat and blood
 */
public class OpdsSearchFlibusta implements OpdsSearch {
    // https://flibusta.is/opds/search?searchType=books&searchTerm=золото
    private static final String searchUrl = "https://flibusta.is/opds/search?searchType=books&searchTerm=";

    @Override
    public CompletionStage<List<Book>> search(final String query) {
        return null;
    }
}
