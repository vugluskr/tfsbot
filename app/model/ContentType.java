package model;

import utils.TextUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author Denis Danilin | denis@danilin.name
 * 29.10.2017 07:27
 * SIRBot ☭ sweat and blood
 */
public enum ContentType {
    DIR, AUDIO(".mp3"), DOCUMENT, PHOTO(".jpg"), STICKER, VIDEO(".mp4"), VOICE(".ogg"), LABEL, CONTACT, SOFTLINK;

    public static List<ContentType> media = Arrays.asList(AUDIO, DOCUMENT, PHOTO, STICKER, VIDEO, VOICE, CONTACT);

    public final String ext;

    ContentType() {
        ext = "";
    }

    ContentType(final String ext) {
        this.ext = ext;
    }

    public String getUrlPath() {
        return "send" + TextUtils.capitalize(name());
    }

    public String getParamName() {
        return name().toLowerCase();
    }

}
