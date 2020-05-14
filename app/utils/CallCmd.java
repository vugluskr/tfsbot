package utils;

import java.util.Arrays;

/**
 * @author Denis Danilin | denis@danilin.name
 * 14.05.2020
 * tfs ☭ sweat and blood
 */
public enum CallCmd {
    mkDir("mk_"),
    ls("ls_"),
    cd("cd_"),
    rm("rm_"),
    mv("mv_"),
    rename("rn_"),
    get("gt_"),
    fullLs("mr_"),
    search("sr"),
    editMode("ed_"),
    cancelDialog("cn_"),
    ;

    public final String id;

    CallCmd(final java.lang.String id) {this.id = id;}

    public String of(final long id) {
        return this.id + id;
    }

    public static CallCmd ofId(final String id) {
        return TextUtils.isEmpty(id) ? null : Arrays.stream(values()).filter(e -> e.id.equals(id) || id.startsWith(e.id)).findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return id;
    }
}
