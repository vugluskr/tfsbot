package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import model.telegram.ContentType;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.10.2017 09:23
 * SIRBot ☭ sweat and blood
 */
public class ContactRef implements TeleFile {
    private long id;
    @JsonProperty("is_bot")
    private boolean bot;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;
    private String username;
    @JsonProperty("language_code")
    private String languageCode;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("user_id")
    private long userId;

    public long getUserId() {
        return userId;
    }

    public void setUserId(final long userId) {
        this.userId = userId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public boolean isBot() {
        return bot;
    }

    public void setBot(final boolean bot) {
        this.bot = bot;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(final String languageCode) {
        this.languageCode = languageCode;
    }

    @Override
    public ContentType getType() {
        return ContentType.CONTACT;
    }

    @Override
    public long getFileSize() {
        return 0;
    }

    @Override
    public String getFileId() {
        return phoneNumber;
    }

    @Override
    public String getUniqId() {
        return phoneNumber;
    }

    @Override
    public String toString() {
        return "ContactRef{" +
                "id=" + id +
                ", bot=" + bot +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", username='" + username + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}
