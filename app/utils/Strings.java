package utils;


import java.time.format.DateTimeFormatter;

/**
 * @author Denis Danilin | denis@danilin.name
 * 19.05.2020
 * tfs ☭ sweat and blood
 */
public interface Strings {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");

    interface State {
        String MkLabel = "MkLabel";
        String Search = "Search";
        String MkDir = "MkDir";
        String Gear = "Gear";
        String OpenFile = "OpenFile";
        String View = "View";
        String SearchGear = "SearchGear";
        String Move = "Move";
        String Rename = "Rename";
        String MkShare = "MkShare";
        String PubShareWizard = "PubShareWizard";
        String PasswWizard = "PasswordWizard";
    }

    interface Callback {
        // main buttons
        String goUp = "goUp";
        String mkLabel = State.MkLabel;
        String searchStateInit = State.Search;
        String mkDir = State.MkDir;
        String gearStateInit = State.Gear;
        String cancelCb = "cncl";
        String move = State.Move;
        String drop = "rm";

        // entry buttons
        String inversCheck = "ichk_";
        String openEntry = State.OpenFile + '.';
        String renameEntry = State.Rename + '.';

        // flow nav buttons
        String rewind = "rwd";
        String forward = "fwd";

        String put = "pt";
        String checkAll = "ca";

        String share = "sha";
        String mkLink = "lnk";
        String mkGrant = "grnt";
        String resetPassword = "rpss";
        String dropPassword = "dpss";
        String save = "sv";
        String resetValid = "vld_rst";
        String makeOtuValid = "vld_otu";
        String makeUntillValid = "vld_unt";
    }

    interface Uni {
        String rewind = "\u25C0\uFE0F"; // ◀️
        String forward = "\u25B6\uFE0F"; // ▶️
        String updir = "\u2B05\uFE0F"; // ⬅️
        String search = "\uD83D\uDD0D"; // 🔍
        String gear = "\u2699"; // ⚙
        String mkdir = "\uD83D\uDCC1"; //
        String folder = "\uD83D\uDCC2"; //
        String drop = "\uD83D\uDDD1"; // 🗑
        String rename = "\u270F\uFE0F"; // ✏️
        String move = "\u2934\uFE0F"; // ⤴️
        String cancel = "\u274C"; // ❌
        String checked = "\u2714\uFE0F"; // ✔️
        String checkAll = "\u2611\uFE0F"; // ☑️
        String put = "\u2705"; // ✅
        String label = "\uD83C\uDFF7"; // 🏷
        String share = "\uD83C\uDF10"; // 🌐
        String keyLock = "\uD83D\uDD10"; // 🔐
        String lock = "\uD83D\uDD12"; // 🔒
        String Link = "\uD83D\uDD17"; // 🔗
        String People = "\uD83D\uDC65"; // 👥
        String Person = "\uD83D\uDC64"; // 👤
        String save = "\uD83D\uDCBE"; // 💾
        String uno = "1\uFE0F\u20E3"; // 1️⃣
    }

}
