package utils;

import model.User;

import java.util.EnumMap;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 15.05.2020
 * tfs ☭ sweat and blood
 */
public class LangMap {
    private static final EnumMap<Value, String> enData, ruData;

    static {
        ruData = new EnumMap<>(Value.class);
        enData = new EnumMap<>(Value.class);

        init(Value.CMD_LIST, "Commands list", "Список команд");
        init(Value.CMD, "Command", "Команда");
        init(Value.DESC, "Description", "Описание");
        init(Value.CD_HELP, "cd <dir>", "cd <дир>");
        init(Value.CD_HELP2, "Change directory", "Переход в директорию");
        init(Value.GET_HELP, "get <file>", "get <файл>");
        init(Value.GET_HELP2, "Get previously stored file", "Получить ранее сохранённый файл");
        init(Value.LS_HELP, "ls", "ls");
        init(Value.LS_HELP2, "Listing of current directory", "Показать содержимое текущей директории");
        init(Value.MKD_HELP, "mkdir <name>", "mkdir <название>");
        init(Value.MKD_HELP2, "Make directory", "Создать директорию");
        init(Value.MV_HELP, "mv <src> <trg>", "mv <истотчник> <цель>");
        init(Value.MV_HELP2, "Move source to target", "Переместить источник в целевой каталог");
        init(Value.PWD_HELP, "pwd", "pwd");
        init(Value.PWD_HELP2, "Current directory full path", "Полный путь к текущей директории");
        init(Value.RM_HELP, "rm <name>", "rm <имя>");
        init(Value.RM_HELP2, "Remove file or dir (recursively)", "Удалить файл или директорию (рекурсивно)");
        init(Value.LBL_HELP, "label <text>", "label <текст>");
        init(Value.LBL_HELP2, "Add label to current dir", "Добавить заметку в текущую директорию");
        init(Value.ALS_HELP, "alias <als>=<cmd>", "alias <псевдоним>=<команда>");
        init(Value.ALS_HELP2, "Alias any command", "Создать псевдоним для любой команды");

        init(Value.CANT_MKDIR, "cannot create directory ‘%s’: File exists", "Невозможно создать папку '%s': файл уже существует");
        init(Value.CANT_RN_TO, "cannot rename to ‘%s’: File exists", "Невозможно переименовать в '%s': файл уже существует");
        init(Value.CANT_MKLBL, "cannot create label ‘%s’: File exists", "Невозможно создать заметку '%s': файл уже существует");
        init(Value.NO_RESULTS, "Nothing found for ‘%s’", "Ничего не найдено по запросу '%s'");
        init(Value.NO_RESULTS_AFTER, "No search results", "Результатов поиска нет");
        init(Value.TYPE_QUERY, "Type query:", "Поиск:");
        init(Value.TYPE_RENAME, "Type new name for '%s':", "Напиши новое имя для '%s':");
        init(Value.TYPE_FOLDER, "Type new folder name:", "Напиши имя новой папки");
        init(Value.CD, "cd %s", "переход в %s");
        init(Value.PAGE, "page #%s", "страница #'%s'");
        init(Value.NORMAL_MODE, "Normal mode", "Нормальный режим");
        init(Value.EDIT_MODE, "Edit mode.", "Режим редактирования.");
        init(Value.DELETED_MANY, "%s entry(s) deleted", "%s файл(ов) удалено");
        init(Value.DELETED, "Entry deleted", "Файл удалён");
        init(Value.MOVE_DEST, "Choose destination folder.", "Необходимо выбрать папку для переноса.");
        init(Value.DESELECTED, "deselected", "Отмена выбора");
        init(Value.SELECTED, "selected", "Файл(ы) выбран(ы)");
        init(Value.MOVED, "Moved %s entry(s)", "Перенесено %s файл(ов)");
        init(Value.TYPE_LABEL, "Type label:", "Текст заметки:");
        init(Value.NO_CONTENT, "No content here yet. Send me some files.", "В этой папке пока ничего нет.");
        init(Value.LANG_SWITCHED, "Switched to English", "Используется русский язык");
        init(Value.SEARCHED, "Search for '%s': %s entry(s)", "Поиск '%s': %s результат(ов)");
        init(Value.UPLOADED, "File stored '%s'", "Файл сохранён '%s'");
        init(Value.CHECK_ALL, "*", "*");
        init(Value.NO_GLOBAL_LINK, "No public link", "Нет публичной ссылки");
        init(Value.NO_PERSONAL_GRANTS, "No personal grants", "Нет личных приглашений");
        init(Value.LINK, "Link: %s", "Ссылка: %s");
        init(Value.PASS_RESET, "Reset passw", "Сбросить пароль");
        init(Value.PASS_SET, "Set passw", "Установить пароль");
        init(Value.PASS_DROP, "Drop passw", "Удалить пароль");
        init(Value.PASSWORD_SET, "Password: _set_", "Пароль: _установлен_");
        init(Value.PASSWORD_SET_TXT, "Password setted", "Пароль установлен");
        init(Value.PASSWORD_NOT_SET, "Password: _not set_", "Пароль: _не установлен_");
        init(Value.VALID_ONETIME, "Validity: _one time access_", "Срок действия: _одноразовая ссылка_");
        init(Value.VALID_UNTILL, "Validity: _open untill %s_", "Срок действия: _ссылка действительна до %s_");
        init(Value.VALID_CANCEL, "Reset validity", "Удалить срок");
        init(Value.VALID_NOT_SET, "Validity: _unlimited_", "Срок действия: _без ограничений_");
        init(Value.VALID_SET_OTU, "Set OneTime", "Сделать одноразовой");
        init(Value.VALID_SET_UNTILL, "Set expiration", "Установить срок");
        init(Value.LINK_DELETED, "Link deleted", "Ссылка удалена");
        init(Value.LINK_SAVED, "Link saved", "Ссылка сохранена");
        init(Value.TYPE_PASSWORD, "Type password:", "Новый пароль:");
        init(Value.TYPE_PASSWORD2, "Type password again:", "Новый пароль (повтор):");
        init(Value.PASSWORD_NOT_MATCH, "Password doesnt match", "Пароль не подходит");
        init(Value.PASSWORD_CLEARED, "Password removed", "Пароль выключен");
        init(Value.VALID_CLEARED, "Validity limit removed", "Ограничения сняты");
        init(Value.OTU_SET, "Validity limited up to one time", "Ссылка ограничена одним срабатыванием");
        init(Value.DROP_PUBLINK_DIR, "%s\n\nRemove public share link for this folder?", "%s\n\nУдалить публичную ссылку на эту папку?");
        init(Value.DROP_PUBLINK_FILE, "%s\n\nRemove public share link for this file?", "%s\n\nУдалить публичную ссылку на этот файл?");
        init(Value.CREATE_PUBLINK_DIR, "%s\n\nCreate public share link for this folder?", "%s\n\nСоздать публичную ссылку на эту папку?");
        init(Value.SEND_CONTACT_DIR, "%s\n\nSend me a contact of the person you want to grant access to this folder",
                "%s\n\nПришли мне контакт того, кому хочешь предоставить доступ к этой папке");
        init(Value.CREATE_PUBLINK_FILE, "%s\n\nCreate public share link for this file?", "%s\n\nСоздать публичную ссылку на этот файл?");
        init(Value.SEND_CONTACT_FILE, "%s\n\nSend me a contact of the person you want to grant access to this file",
                "%s\n\nПришли мне контакт того, кому хочешь предоставить доступ к этому файлу");
        init(Value.CANT_GRANT, "Access already granted to %s", "%s: доступ уже предоставлен");
        init(Value.SHARE_ACCESS, Strings.Uni.Person + " %s [Read/Write]", Strings.Uni.Person + " %s [полный доступ]");
        init(Value.SHARE_ACCESS_RO, Strings.Uni.Person + " %s [Read only]", Strings.Uni.Person + " %s [только чтение]");
        init(Value.SHARES, Strings.Uni.share + " network", Strings.Uni.share + " сеть");
        init(Value.SHARES_ANONYM, "common", "общие");
        init(Value.NOT_ALLOWED, "You're not allowed to do it here", "Это действие запрещено в текущей папке");
        init(Value.NOT_ALLOWED_THIS, "You're not allowed to do it to this entry", "Это действие запрещено в для данного элемента");
    }

    private static void init(final Value key, final String en, final String ru) {
        enData.put(key, en);
        ruData.put(key, ru);
    }

    public enum Value {
        CMD, DESC, CD_HELP, CD_HELP2, GET_HELP, GET_HELP2, LS_HELP, LS_HELP2, MKD_HELP, MKD_HELP2, MV_HELP, MV_HELP2, PWD_HELP, PWD_HELP2, RM_HELP, RM_HELP2, LBL_HELP,
        LBL_HELP2, ALS_HELP, ALS_HELP2, CANT_MKDIR, CANT_RN_TO, CANT_MKLBL, NO_RESULTS, TYPE_QUERY, TYPE_RENAME, TYPE_FOLDER, CD, PAGE, NORMAL_MODE, EDIT_MODE, DELETED,
        DELETED_MANY, MOVE_DEST, DESELECTED, SELECTED, MOVED, TYPE_LABEL, SEARCHED, NO_CONTENT, LANG_SWITCHED, NO_RESULTS_AFTER, UPLOADED, None, CHECK_ALL, NO_GLOBAL_LINK,
        NO_PERSONAL_GRANTS, CMD_LIST,
        PASS_RESET, PASS_DROP, PASSWORD_SET, PASSWORD_NOT_SET, VALID_ONETIME, VALID_UNTILL, VALID_CANCEL, VALID_NOT_SET, VALID_SET_OTU, VALID_SET_UNTILL, LINK_DELETED,
        LINK_SAVED, PASS_SET, TYPE_PASSWORD, TYPE_PASSWORD2, PASSWORD_SET_TXT, PASSWORD_NOT_MATCH, PASSWORD_CLEARED, VALID_CLEARED, OTU_SET, DROP_PUBLINK_DIR, DROP_PUBLINK_FILE, CREATE_PUBLINK_DIR, SEND_CONTACT_DIR, CREATE_PUBLINK_FILE, SEND_CONTACT_FILE, CANT_GRANT, SHARE_ACCESS, SHARE_ACCESS_RO, SHARES, SHARES_ANONYM, NOT_ALLOWED, NOT_ALLOWED_THIS, LINK

    }

    public static String v(final Value name, final User user, final Object... args) {
        return v(name, notNull(user.getLang(), "ru"), args);
    }

    public static String v(final Value name, final String langTag, final Object... args) {
        try {
            if (name == Value.None)
                return "";

            final String value = (langTag.equalsIgnoreCase("ru") ? ruData : enData).getOrDefault(name, name.name());

            return TextUtils.isEmpty(args) ? value : String.format(value, args);
        } catch (final Exception ignore) { }

        return name.name();
    }
}
