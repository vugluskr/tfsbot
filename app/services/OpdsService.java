package services;

import com.google.inject.ImplementedBy;
import services.impl.OpdsServiceImpl;

import java.util.UUID;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 25.03.2022 14:06
 * tfs ☭ sweat and blood
 */
@ImplementedBy(OpdsServiceImpl.class)
public interface OpdsService {
    boolean requestOpds(String url, String title, UUID dir, long userId, String lang);
}
