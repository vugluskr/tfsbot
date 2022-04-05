package model.request;

import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 02.04.2022 15:23
 * tfs ☭ sweat and blood
 */
public class CmdRequest extends TgRequest {
    private static final Logger.ALogger logger = Logger.of(CmdRequest.class);
    private static final String shareMark = "/start shared-", fbsMark = "/fbs ";
    private final String text;
    private Cmd cmd;
    private String arg;

    public CmdRequest(final JsonNode node, final String text0) {
        super(node.get("from"));
        this.text = notNull(text0);
        cmd = null;
        arg = null;

        if (text.equalsIgnoreCase("/start"))
            cmd = Cmd.Start;
        else if (text.equalsIgnoreCase("/help"))
            cmd = Cmd.Help;
        else if (text.equalsIgnoreCase("/reset"))
            cmd = Cmd.Reset;
        else if (text.toLowerCase().startsWith(shareMark) && text.length() > shareMark.length()) {
            cmd = Cmd.JoinShare;
            arg = text.substring(shareMark.length());
        } else if (text.toLowerCase().startsWith(fbsMark) && text.length() > fbsMark.length()) {
            cmd = Cmd.FbSearch;
            arg = text.substring(fbsMark.length());
        }

        if (cmd == null)
            logger.debug("Cant resolve '"+text+"' to command. From user " + user);
    }

    @Override
    public boolean isCrooked() {
        return cmd == null;
    }

    public String getText() {
        return text;
    }

    public Cmd getCmd() {
        return cmd;
    }

    public String getArg() {
        return arg;
    }

    public enum Cmd {Start, Help, Reset, JoinShare, FbSearch}
}
