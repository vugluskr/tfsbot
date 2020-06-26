package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import utils.LangMap;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.06.2020
 * tfs ☭ sweat and blood
 */
public interface Role {
    void doView();

    LangMap.Value helpValue();

    JsonNode dump();
}
