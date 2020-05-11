package model.telegram.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.10.2017 09:25
 * SIRBot ☭ sweat and blood
 */
public class ChatRef {
    private long id;
    @JsonProperty("is_bot")
    private boolean bot;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;
    private String username;
    private String type;

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

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }
}
