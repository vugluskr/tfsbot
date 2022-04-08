package model.opds;

import java.util.ArrayList;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 06.04.2022 15:17
 * tfs ☭ sweat and blood
 */
public class OpdsPage {
    private boolean hasPrev, hasNext;
    private ArrayList<OpdsBook> books;

    public ArrayList<OpdsBook> getBooks() {
        return books;
    }

    public void setBooks(final ArrayList<OpdsBook> books) {
        this.books = books;
    }

    public boolean isHasPrev() {
        return hasPrev;
    }

    public void setHasPrev(final boolean hasPrev) {
        this.hasPrev = hasPrev;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(final boolean hasNext) {
        this.hasNext = hasNext;
    }
}
