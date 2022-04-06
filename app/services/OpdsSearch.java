package services;

import com.google.inject.ImplementedBy;
import model.opds.OpdsPage;
import services.impl.OpdsSearchFlibusta;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 06.04.2022 12:51
 * tfs ☭ sweat and blood
 */
@ImplementedBy(OpdsSearchFlibusta.class)
public interface OpdsSearch {
    OpdsPage search(String query, int pageNum);
}
