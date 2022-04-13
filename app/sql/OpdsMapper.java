package sql;

import model.TBook;
import org.apache.ibatis.annotations.Param;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 25.03.2022 14:05
 * tfs ☭ sweat and blood
 */
public interface OpdsMapper {
    void insertBook(@Param("b") TBook b);

    TBook findBook(@Param("id") String tag, @Param("fb") boolean fb, @Param("epub") boolean epub);
}
