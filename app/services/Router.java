package services;

import com.google.inject.ImplementedBy;
import model.request.TgRequest;
import services.impl.RouterImpl;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 03.04.2022 12:35
 * tfs ☭ sweat and blood
 */
@ImplementedBy(RouterImpl.class)
public interface Router {

    void handle(TgRequest request);
}
