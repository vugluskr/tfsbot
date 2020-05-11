package services;

import com.google.inject.ImplementedBy;
import model.TFile;
import model.telegram.api.TextRef;
import services.impl.TgApiReal;

/**
 * @author Denis Danilin | denis@danilin.name
 * 05.05.2020
 * tfs ☭ sweat and blood
 */
@ImplementedBy(TgApiReal.class)
public interface TgApi {
    void sendFile(TFile file, long chatId);

    void sendMessage(TextRef text);
}
