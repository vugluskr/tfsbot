package utils;


import java.time.format.DateTimeFormatter;

import static utils.TextUtils.getInt;

/**
 * @author Denis Danilin | denis@danilin.name
 * 19.05.2020
 * tfs ☭ sweat and blood
 */
public interface Strings {

    interface Uni {
        String rewind = "\u25C0\uFE0F"; // ◀️
        String forward = "\u25B6\uFE0F"; // ▶️
        String goUp = "\u2B05\uFE0F"; // ⬅️
//        String search = "\uD83D\uDD0D"; // 🔍
        String gear = "\u2699"; // ⚙
        String mkdir = "\uD83D\uDCC1"; //
        String folder = "\uD83D\uDCC2"; //
        String drop = "\uD83D\uDDD1"; // 🗑
        String edit = "\u270F\uFE0F"; // ✏️
        String move = "\u2934\uFE0F"; // ⤴️
        String cancel = "\u274C"; // ❌
        String checked = "\u2714\uFE0F"; // ✔️
        String checkAll = "\u2611\uFE0F"; // ☑️
        String put = "\u2705"; // ✅
        String label = "\uD83C\uDFF7\u0336"; // 🏷
        String share = "\uD83C\uDF10"; // 🌐
        String keyLock = "\uD83D\uDD10"; // 🔐
        String lock = "\uD83D\uDD12"; // 🔒
        String link = "\uD83D\uDD17"; // 🔗
        String People = "\uD83D\uDC65"; // 👥
        String mkGrant = "\uD83D\uDC64"; // 👤
        String save = "\uD83D\uDCBE"; // 💾
        String uno = "1\uFE0F\u20E3"; // 1️⃣
    }

}
