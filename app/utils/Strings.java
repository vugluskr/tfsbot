package utils;


/**
 * @author Denis Danilin | denis@danilin.name
 * 19.05.2020
 * tfs ☭ sweat and blood
 */
public interface Strings {
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
    }

    interface Params {
        String offset = "offset";
        String dirId = "dirId";
        String query = "query";
        String fileId = "fileId";
    }

    interface Callback {
        // main buttons
        String goUp = "goUp";
        String mkLabel = State.MkLabel;
        String searchStateInit = State.Search;
        String mkDir = State.MkDir;
        String gearStateInit = State.Gear;
        String cancel = "cncl";
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
    }

}
