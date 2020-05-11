package model;

/**
 * @author Denis Danilin | denis@danilin.name
 * 02.05.2020
 * tfs ☭ sweat and blood
 */
public class UserAlias implements Comparable<UserAlias> {
    private String alias;
    private String cmd;

    public UserAlias() {
    }

    public UserAlias(final String alias) {
        this.alias = alias;
    }

    public UserAlias(final String alias, final String cmd) {
        this.alias = alias;
        this.cmd = cmd;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(final String alias) {
        this.alias = alias;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(final String cmd) {
        this.cmd = cmd;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final UserAlias userAlias = (UserAlias) o;

        return alias.equals(userAlias.alias);
    }

    @Override
    public int hashCode() {
        return alias.hashCode();
    }

    @Override
    public int compareTo(final UserAlias o) {
        return alias.compareTo(o.alias);
    }
}
