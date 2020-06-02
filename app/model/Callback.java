package model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import play.libs.Json;
import utils.Strings;

import java.util.UUID;

/**
 * @author Denis Danilin | denis@danilin.name
 * 29.05.2020
 * tfs ☭ sweat and blood
 */
public class Callback {
    public final Strings.Callback type;
    public final int idx;
    public final long id;
    public UUID entryId;
    public String shareId;

    public Callback(final Strings.Callback type, final long id, final int idx) {
        this.type = type;
        this.id = id;
        this.idx = idx;
    }

    public Strings.Callback type() {
        return type;
    }
}
