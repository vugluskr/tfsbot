package model.telegram;

import utils.TextUtils;

/**
 * @author Denis Danilin | denis@danilin.name
 * 29.10.2017 07:27
 * SIRBot ☭ sweat and blood
 */
public enum ContentType {
    DIR, AUDIO(".mp3"), DOCUMENT, PHOTO(".jpg"), STICKER, VIDEO(".mp4"), VOICE(".ogg"), LABEL, CONTACT;

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
