package model.user;

import java.util.UUID;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 02.04.2022 16:45
 * tfs ☭ sweat and blood
 */
public class UDbData {
    private long id;
    private long msgId;
    private String s1, s2, s3, s4;

    public UDbData() {
    }

    public UDbData(final long id, final UUID rootId) {
        setId(id);
        setS1(rootId.toString());
    }

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public long getMsgId() {
        return msgId;
    }

    public void setMsgId(final long msgId) {
        this.msgId = msgId;
    }

    public String getS1() {
        return s1;
    }

    public void setS1(final String s1) {
        this.s1 = s1;
    }

    public String getS2() {
        return s2;
    }

    public void setS2(final String s2) {
        this.s2 = s2;
    }

    public String getS3() {
        return s3;
    }

    public void setS3(final String s3) {
        this.s3 = s3;
    }

    public String getS4() {
        return s4;
    }

    public void setS4(final String s4) {
        this.s4 = s4;
    }
}
