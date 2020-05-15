package utils;

import model.User;

import java.util.EnumMap;

/**
 * @author Denis Danilin | denis@danilin.name
 * 15.05.2020
 * tfs ☭ sweat and blood
 */
public class LangMap {
    private static final EnumMap<Names, String> enData, ruData;

    static {
        ruData = new EnumMap<>(Names.class);
        enData = new EnumMap<>(Names.class);

        init(Names.CMD_LIST, "Commands list", "Список команд");
        init(Names.CMD, "Command", "Команда");
        init(Names.DESC, "Description", "Описание");
        init(Names.CD_HELP, "cd <dir>", "cd <дир>");
        init(Names.CD_HELP2, "Change directory", "Переход в директорию");
        init(Names.GET_HELP, "get <file>", "get <файл>");
        init(Names.GET_HELP2, "Get previously stored file", "Получить ранее сохранённый файл");
        init(Names.LS_HELP, "ls", "ls");
        init(Names.LS_HELP2, "Listing of current directory", "Показать содержимое текущей директории");
        init(Names.MKD_HELP, "mkdir <name>", "mkdir <название>");
        init(Names.MKD_HELP2, "Make directory", "Создать директорию");
        init(Names.MV_HELP, "mv <src> <trg>", "mv <истотчник> <цель>");
        init(Names.MV_HELP2, "Move source to target", "Переместить источник в целевой каталог");
        init(Names.PWD_HELP, "pwd", "pwd");
        init(Names.PWD_HELP2, "Current directory full path", "Полный путь к текущей директории");
        init(Names.RM_HELP, "rm <name>", "rm <имя>");
        init(Names.RM_HELP2, "Remove file or dir (recursively)", "Удалить файл или директорию (рекурсивно)");
        init(Names.LBL_HELP, "label <text>", "label <текст>");
        init(Names.LBL_HELP2, "Add label to current dir", "Добавить заметку в текущую директорию");
        init(Names.ALS_HELP, "alias <als>=<cmd>", "alias <псевдоним>=<команда>");
        init(Names.ALS_HELP2, "Alias any command", "Создать псевдоним для любой команды");

        init(Names.CANT_MKDIR, "cannot create directory ‘{}’: File exists", "Невозможно создать директорию '{}': файл с таким именем уже существует");
        init(Names.CANT_RN_TO, "cannot rename to ‘{}’: File exists", "Невозможно переименовать в '{}': файл с таким именем уже существует");
        init(Names.CANT_MKLBL, "cannot create label ‘{}’: File exists", "Невозможно создать заметку '{}': файл уже существует");
        init(Names.NO_RESULTS, "Nothing found for ‘{}’", "Ничего не найдено по запросу '{}'");
        init(Names.TYPE_QUERY, "Type query:", "Запрос:");
        init(Names.TYPE_RENAME, "Type new name for '{}':", "Новое название для '{}':");
        init(Names.TYPE_FOLDER, "Type new folder name:", "Название новой директории:");
        init(Names.CD, "cd {}", "переход в {}");
        init(Names.PAGE, "page #{}", "страница #'{}'");
        init(Names.NORMAL_MODE, "Normal mode", "Нормальный режим");
        init(Names.EDIT_MODE, "Edit mode. Select entries to move or delete. Hit '" + Uni.back + "' to cancel.", "Режим редактирования. Можно отметить файлы и/или директории для " +
                "их удаления или переноса. Кликнуть '" + Uni.back + "' для возврата в нормальный режим.");
        init(Names.DELETED_MANY, "{} entry(s) deleted", "{} файл(ов) удалено");
        init(Names.DELETED, "Entry deleted", "Файл удалён");
        init(Names.MOVE_DEST, "Choose destination folder. Hit '" + Uni.back + "' to cancel moving. Hit '" + Uni.target + "' to put files in current dir.", "Необходимо выбрать " +
                "директорию для переноса. Клик на '"+Uni.back+"' для отмены переноса. Клик на '"+Uni.target+"' для размещения файлов в текущей директории.");
        init(Names.DESELECTED, "deselected", "Отмена выбора");
        init(Names.SELECTED, "selected", "Файл выбран");
        init(Names.MOVED, "Moved {} entry(s)", "Перенесено {} файл(ов)");
        init(Names.TYPE_LABEL, "Type label:", "Текст заметки:");
        init(Names.SEARCHED, "searched '{}' at {}", "Поиск '{}' в {}");
        init(Names.NO_CONTENT, "No content here yet. Send me some files.", "В этой директории пока ничего нет.");
        init(Names.LANG_SWITCHED, "Switched to English", "Используется русский язык");
    }

    private static void init(final Names key, final String en, final String ru) {
        enData.put(key, en);
        ruData.put(key, ru);
    }

    public enum Names {
        CMD, DESC, CD_HELP, CD_HELP2, GET_HELP, GET_HELP2, LS_HELP, LS_HELP2, MKD_HELP, MKD_HELP2, MV_HELP, MV_HELP2, PWD_HELP, PWD_HELP2, RM_HELP, RM_HELP2, LBL_HELP, LBL_HELP2, ALS_HELP, ALS_HELP2, CANT_MKDIR, CANT_RN_TO, CANT_MKLBL, NO_RESULTS, TYPE_QUERY, TYPE_RENAME, TYPE_FOLDER, CD, PAGE, NORMAL_MODE, EDIT_MODE, DELETED, DELETED_MANY, MOVE_DEST, DESELECTED, SELECTED, MOVED, TYPE_LABEL, SEARCHED, NO_CONTENT, LANG_SWITCHED, CMD_LIST
    }

    public static String v(final Names name, final User user, final Object... args) {
        try {
            final String value = (UOpts.Russian.is(user) ? ruData : enData).getOrDefault(name, name.name());

            return TextUtils.isEmpty(args) ? value : String.format(value, args);
        } catch (final Exception ignore) { }

        return name.name();
    }

    public static String vru(final Names name) {
        return ruData.getOrDefault(name, name.name());
    }

    public static String ven(final Names name) {
        return enData.getOrDefault(name, name.name());
    }
}
