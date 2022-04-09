package model.request;

import com.fasterxml.jackson.databind.JsonNode;
import model.ContentType;
import model.TFile;

import java.util.TreeMap;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 02.04.2022 14:51
 * tfs ☭ sweat and blood
 */
public class FileRequest extends TgRequest {
    private final TFile file;
    private final JsonNode srcNode;
    private JsonNode attachNode;
    private boolean fb2;

    public FileRequest(final JsonNode node, final ContentType type) {
        super(node.get("from"));

        srcNode = node;
        file = new TFile();
        file.type = type;

        attachNode = node.get(type.getParamName());

        if (type == ContentType.PHOTO) {
            if (attachNode.size() == 1)
                attachNode = attachNode.get(0);
            else {
                final TreeMap<Long, JsonNode> map = new TreeMap<>();

                for (int i = 0; i < attachNode.size(); i++)
                    map.put(attachNode.get(i).get("file_size").asLong(), attachNode.get(i));

                attachNode = map.lastEntry().getValue();
            }
        } else if (type == ContentType.DOCUMENT) {
            file.setName(attachNode.get("file_name").asText());
            file.refId = attachNode.get("file_id").asText();
            final String mimeType = attachNode.get("mime_type").asText();
            fb2 = mimeType.equalsIgnoreCase("application/x-zip-compressed-fb2") || mimeType.equalsIgnoreCase("application/x-fictionbook+xml");
        } else if (type == ContentType.CONTACT) {
            if (!attachNode.has("user_id"))
                attachNode = null;
            else {
                file.setOwner(attachNode.get("user_id").asLong());
                final String f = attachNode.has("first_name") ? attachNode.get("first_name").asText() : "";
                final String l = attachNode.has("last_name") ? attachNode.get("last_name").asText() : "";
                final String u = attachNode.has("username") ? attachNode.get("username").asText() : "";
                final String p = attachNode.has("phone_number") ? attachNode.get("phone_number").asText() : "";
                file.uniqId = node.has("file_unique_id") ? node.get("file_unique_id").asText() : p;
                file.refId = node.has("file_id") ? node.get("file_id").asText() : p;
                file.setName(notNull((notNull(f) + " " + notNull(l)), notNull(u, notNull(p, "u" + attachNode.get("user_id").asText()))));
            }
        }
    }

    public boolean isFb2() {
        return fb2;
    }

    @Override
    public boolean isCrooked() {
        return attachNode == null;
    }

    public TFile getFile() {
        return file;
    }

    public JsonNode getAttachNode() {
        return attachNode;
    }

    public JsonNode getSrcNode() {
        return srcNode;
    }
}

