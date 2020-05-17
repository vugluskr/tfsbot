package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import model.User;

import java.util.List;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.10.2017 09:22
 * SIRBot ☭ sweat and blood
 */
public class MessageRef {
    @JsonProperty("message_id")
    private long messageId;

    @JsonProperty("chat")
    private ChatRef chatRef;

    @JsonProperty("from")
    private ContactRef contactRef;

    @JsonProperty("date")
    private long dateUnixTime;

    private String caption;

    private List<PhotoRef> photo;

    @JsonProperty("caption_entities")
    private List<CaptionEntityRef> captionEntities;

    private VideoRef video;

    private StickerRef sticker;

    private List<EntityRef> entities;

    @JsonProperty("edit_date")
    private long editDateUnixTime;

    private String text;

    private DocumentRef document;

    private AudioRef audio;
    private VoiceRef voice;

    // todo contact
    // {"contact":{"phone_number":"+79807486512","first_name":"Кисюк","user_id":69436249}}}

    @JsonProperty("contact")
    private ContactRef tgUser;

    public ContactRef getTgUser() {
        return tgUser;
    }

    public void setTgUser(final ContactRef tgUser) {
        this.tgUser = tgUser;
    }

    public AudioRef getAudio() {
        return audio;
    }

    public void setAudio(final AudioRef audio) {
        this.audio = audio;
    }

    public VoiceRef getVoice() {
        return voice;
    }

    public void setVoice(final VoiceRef voice) {
        this.voice = voice;
    }

    public DocumentRef getDocument() {
        return document;
    }

    public void setDocument(final DocumentRef document) {
        this.document = document;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public long getEditDateUnixTime() {
        return editDateUnixTime;
    }

    public void setEditDateUnixTime(final long editDateUnixTime) {
        this.editDateUnixTime = editDateUnixTime;
    }

    public List<EntityRef> getEntities() {
        return entities;
    }

    public void setEntities(final List<EntityRef> entities) {
        this.entities = entities;
    }

    public StickerRef getSticker() {
        return sticker;
    }

    public void setSticker(final StickerRef sticker) {
        this.sticker = sticker;
    }

    public VideoRef getVideo() {
        return video;
    }

    public void setVideo(final VideoRef video) {
        this.video = video;
    }

    public List<CaptionEntityRef> getCaptionEntities() {
        return captionEntities;
    }

    public void setCaptionEntities(final List<CaptionEntityRef> captionEntities) {
        this.captionEntities = captionEntities;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(final String caption) {
        this.caption = caption;
    }

    public List<PhotoRef> getPhoto() {
        return photo;
    }

    public void setPhoto(final List<PhotoRef> photo) {
        this.photo = photo;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(final long messageId) {
        this.messageId = messageId;
    }

    public ChatRef getChatRef() {
        return chatRef;
    }

    public void setChatRef(final ChatRef chatRef) {
        this.chatRef = chatRef;
    }

    public ContactRef getContactRef() {
        return contactRef;
    }

    public void setContactRef(final ContactRef contactRef) {
        this.contactRef = contactRef;
    }

    public long getDateUnixTime() {
        return dateUnixTime;
    }

    public void setDateUnixTime(final long dateUnixTime) {
        this.dateUnixTime = dateUnixTime;
    }

    public PhotoRef getMaxPhoto() {
        if (photo == null || photo.isEmpty())
            return null;

        if (photo.size() > 1)
            photo.sort((o1, o2) -> Long.compare(o2.getFileSize(), o1.getFileSize()));

        return photo.get(0);
    }

    public TeleFile getTeleFile() {
        if (video != null)
            return video;
        if (document != null)
            return document;
        if (audio != null)
            return audio;
        if (voice != null)
            return voice;
        if (sticker != null)
            return sticker;
        if (photo != null)
            return getMaxPhoto();
        if (tgUser != null)
            return tgUser;

        return null;
    }
}
