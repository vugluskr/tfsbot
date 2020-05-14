package utils;

/**
 * @author Denis Danilin | denis@danilin.name
 * 14.05.2020
 * tfs ☭ sweat and blood
 */
public enum CallCmd {
    mkDir("mk"),
    ls("ls_"),
    cd("cd_"),
    rm("rm_"),
    mv("mv_"),
    rename("rn_"),
    get("gt_"),
    fullLs("mr_"),
    search("sr"),
    editMode("ed"),
    cancelDialog("cn"),
    ;

    public final String id;

    CallCmd(final java.lang.String id) {this.id = id;}

    public String of(final long id) {
        return this.id + id;
    }


    @Override
    public String toString() {
        return id;
    }
}
