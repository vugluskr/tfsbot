package utils;

import model.User;

import java.util.EnumMap;

import static utils.TextUtils.*;

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

        init(Value.LS_HELP,
                mdBold("Folder view mode.") + "\n" +
                        escapeMd("First row show folder path, if exists below are shown all notes which are placed here.\nFirst row buttons:\n") +
                        escapeMd(Strings.Uni.goUp + " - go to the parent folder. ") + mdItalic("doesnt shown if you're in a root folder") + "\n" +
                        escapeMd(Strings.Uni.label + " - make note in the current folder. ") + mdItalic("you have to type note's text after click") + "\n" +
                        escapeMd(Strings.Uni.mkdir + " - make subfolder in the current folder. ") + mdItalic("you have to type subfolder's name after click") + "\n" +
                        escapeMd(Strings.Uni.gear + " - go to 'folder edit mode'. ") + mdItalic("provides possibility to rename, delete or share access to the current folder") +
                        "\n\n" +
                        escapeMd("Entire content of the current folder (subfolders and files) is displayed with buttons (max. 10 buttons per page).\n\n" +
                                "If entry is folder it will have " + Strings.Uni.folder + " icon before its name. Click the button to get into the folder.\n" +
                                "Click the file's button to view the file.\n\n " +
                                "If the current folder contains more than 10 childs then last row of buttons will contain navigation buttons: " + Strings.Uni.rewind + " - 10 " +
                                "entries back and " + Strings.Uni.forward + " - 10 entries further"),
                mdBold("Режим просмотра папки.") + "\n" +
                        escapeMd("Первой строкой указан путь к текущей папке, ниже, если есть, показаны все заметки, которые были заведены в ней.\nПервая строка кнопок:\n") +
                        escapeMd(Strings.Uni.goUp + " - переход в родительскую папку. ") + mdItalic("не показывается, если текущая директория - корневая") + "\n" +
                        escapeMd(Strings.Uni.label + " - создание заметки в текущей папке. ") + mdItalic("после нажатия нужно будет ввести текст заметки") + "\n" +
                        escapeMd(Strings.Uni.mkdir + " - создание подпапки в текущей папке. ") + mdItalic("после нажатия нужно будет ввести имя новой папки") + "\n" +
                        escapeMd(Strings.Uni.gear + " - переход в режим управления папкой. ") + mdItalic("предоставляет доступ к переименованию, удалению и предоставлению публичного и " +
                        "персонального доступа к текущей папке") + "\n\n" +
                        escapeMd("Содержимое текущей папки (подпапки и файлы) отображено кнопками (макс. 10 штук).\n\n" +
                                "Если элемент - папка, то перед его именем стоит иконка " + Strings.Uni.folder + ". Нажатие на кнопку приводит к переходу в эту папку.\n" +
                                "Нажатие на кнопку файла позволяет просмотреть этот файл.\n\n Если текущая папка содержит больше 10 элементов, то последней строкой отображаются " +
                                "две кнопки перехода по страницам: " + Strings.Uni.rewind + " - назад на 10 файлов и " + Strings.Uni.forward + " - вперёд на 10 файлов"));
        init(Value.FILE_HELP, mdBold("File view mode\n") +
                        escapeMd("First of all file's body is shown. Below is file's path, after that control buttons are:\n" +
                                Strings.Uni.goUp + " - go to the parent folder.\n" +
                                Strings.Uni.keyLock + " - set/clear password on file.\n" +
                                Strings.Uni.share + " - go to the access share management.\n" +
                                Strings.Uni.edit + " - rename the file. ") + mdItalic("you have to type new file's name after click. Be aware that file wont be actually renamed " +
                        "in a cloud. Only display name will be changed after that.\n") +
                        escapeMd(Strings.Uni.drop + " - drop the file. ") + mdItalic("File will be permanently deleted."),
                mdBold("Режим просмотра файла\n") +
                        escapeMd("Первым отображается тело самого файла. Ниже указан путь файла, затем идёт ряд кнопок управления:\n" +
                                Strings.Uni.goUp + " - переход в родительскую папку.\n" +
                                Strings.Uni.keyLock + " - установить/снять пароль на доступ к файлу.\n" +
                                Strings.Uni.share + " - переход к управлению правами доступа к файлу.\n" +
                                Strings.Uni.edit + " - переименование файла. ") + mdItalic("после нажатия нужно будет ввести новое имя файла. Внимание, сам файл физически не будет " +
                        "переименован, будет изменено только имя под которым он отображается в боте.\n") +
                        escapeMd(Strings.Uni.drop + " - удаление файла. ") + mdItalic("после нажатия файл будет безвозвратно удалён."));
        init(Value.SEARCHED_HELP, mdBold("Search results mode\n") +
                        escapeMd("Search query and search folder are show at first, followed by search results quantity.\nNext is button " + Strings.Uni.goUp + ": exit search - " +
                                "after click you will get into 'view mode' of the element where you started searching.\n\nIf bot found more than nothing then all results are " +
                                "shown as buttons. Each button is signed with entry's relative path, relative to the your current folder\n\n") +
                        mdItalic("Be aware that found notes, unlike the 'view mode', are also shown with buttons.\n\n") +
                        escapeMd("Click to any result button takes you to the view mode according to the element's type, but return button (" + Strings.Uni.goUp + ") will " +
                                "lead back to the search results.\n"),
                mdBold("Просмотр результатов поиска\n") +
                        escapeMd("Первой строкой показаны поисковый запрос и папка начала поиска, затем общее количество результатов поиска.\nЗатем следует строка с кнопкой выхода из " +
                                "режима поиска: " + Strings.Uni.goUp + " - нажатие приводит к возврату в режим просмотра того элемента, где был начат поиск.\n\nЕсли количество " +
                                "результатов отлично от нуля, то все они показываются ниже кнопками. Каждая кнопка подписана согласно локальному пути элемента, относительно текущей папки\n\n") +
                        mdItalic("Обратите внимание, что найденные заметки, в отличие от обычного режима, здесь показываются тоже кнопками, наравне с файлами и папками.\n\n") +
                        escapeMd("Нажатие на любую кнопку приводит к переходу в режим обычного просмотра выбранного элемента, согласно его типу, но в этом просмотре кнопка выхода " +
                                "из просмотра (" + Strings.Uni.goUp + ") будет вести обратно к результатам поиска.\n"));
        init(Value.ROOT_HELP, mdBold("Root folder\n") +
                        escapeMd("You are in the root folder of the TeleFS.\n\n" +
                                "Bot allows to create, edit and delete folders and files just like you do it on your home PC, only hierarchy is available immediately via all " +
                                "your devices with your telegram account.\n" +
                                "Physically files are in the telegram's cloud and its real content is unaccessible to neither bot nor somebody else, untill you decide to share " +
                                "it.\n" +
                                "Bot follows 'one window' policy, that means, that any time you see single message from it where content depends on your mode and " +
                                "performed commands; e.g. if you searched for something then you see search results, if you changed a folder - you see folder's content in same " +
                                "message and so on.\n\n" +
                                "Regardless of modes and actions, four actions are available to you at any given time:\n") +
                        mdBold("1. ") + mdItalic("files upload") + escapeMd(" bot's main goal is to keep access to your folders and files according to your structure. Any time " +
                        "you can extend your collection just simple send any number of files, all of it will be stored in a current folder, where you are while send it.. " +
                        "No special command or action is required, just send files to bot and thats it. ") + mdItalic(
                                "Be aware, simple 'documents' are saved with original filenames, but its not so for media files - unfortunately, telegram loses its filenames, so" +
                                        " bot have to construct it from the file's types.\n" +
                        "Hint: if you will apply a comment while sending a file, then bot will use it as filename\n\n") +
                        mdBold("2. ") + escapeMd("'/reset' command - reset bot's state if it is unconscious and takes you here, to the root folder.\n\n") +
                        mdBold("3. ") + escapeMd("'/help' - context help, reflects on your actions and current position. If you're missed and dont know what to do - use this " +
                        "command.\n\n") +
                        mdBold("4. ") + mdItalic("search by filename") + escapeMd(" - you can start search anywhere at full hierarchy depth from where you are, just send a " +
                        "query text. ") + mdItalic("The search is always case-insensitive and for any part of the name\n\n") +
                        escapeMd("These buttons are available at the root folder:\n") +
                        escapeMd(Strings.Uni.label + " - make a note. ") + mdItalic("you will have to type a note's text after click") + "\n" +
                        escapeMd(Strings.Uni.mkdir + " - make a subfolder. ") + mdItalic("you will have to type a new subfolder's name after click") + "\n" +
                        escapeMd(Strings.Uni.gear + " - go to the 'notes manage mode'. ") + mdItalic("you will be able to manage existed notes in the root folder"),
                mdBold("Домашняя папка\n") +
                        escapeMd("Вы находитесь в домашней папке вашей файловой системы TeleFS.\n\n" +
                                "Бот позволяет создавать, изменять и удалять папки и файлы также, как вы делаете это на своём компьютере, только эта иерархия сразу доступна на всех" +
                                " ваших устройствах, где запущен telegram под вашим аккаунтом.\n" +
                                "Файлы физически находятся в облаке telegram и их содержимое недоступно ни боту, ни кому-либо ещё, до тех пор, пока вы не решите поделиться доступом с " +
                                "кем-либо.\n" +
                                "Бот работает в режиме 'единого окна', это означает, что в любой момент времени вы видите только одно сообщение от бота, в котором содержимое " +
                                "соответствует режиму и командам, принятым от вас; то есть, если вы что-то искали - вы видите результаты поиска, если вы перешли в директорию - " +
                                "содержимое сообщения изменится на список содержимого этой директории и так далее.\n\n" +
                                "Независимо от режимов и действий, в любой момент времени вам доступны четыре действия:\n") +
                        mdBold("1. ") + mdItalic("добавление файлов") + escapeMd(" главная задача бота - хранить доступ к вашим файлам, согласно созданной вами структуре. Вы в любой " +
                        "момент можете пополнить вашу коллекцию просто отправив любое количество файлов боту, они будут сохранены в той папке, где вы находитесь в момент отправки. " +
                        "Никаких специальных комманд или действий не требуется - просто посылайте файлы. ") + mdItalic("Обратите внимание, что документы сохраняются с теми " +
                        "именами, с которыми они были посланы, но это не касается фотографий, видео и аудио материалов - к сожалению, telegram не передаёт их имена, потому бот называет " +
                        "полученные файлы согласно их типам.\n" +
                        "Подсказка: если вы посылаете файл и в комментарии напишете что-либо, то комментарий будет использован в качестве имени файла\n\n") +
                        mdBold("2. ") + escapeMd("команда '/reset' - если отклик бота неадекватен или отсутствует. Это сбрасывает ваше взаимодействие с ботом и возвращает вас " +
                        "на начальную точку - сюда, в домашнюю папку.\n\n") +
                        mdBold("3. ") + escapeMd("команда '/help' - контекстная помощь, в зависимости от того, где вы сейчас находитесь и что делали. Если вы запутались и не знаете " +
                        "куда нажать - отправьте эту команду.\n\n") +
                        mdBold("4. ") + mdItalic("поиск по имени") + escapeMd(" - в любом месте файловой системы вы можете искать файлы и папки по имени, вглубь по всей иерархии, " +
                        "начиная с того места, где вы находитесь. Просто отправьте сообщение с частью искомого имени из любого места. ") + mdItalic("Поиск всегда происходит без учёта " +
                        "регистра и по любой части имени\n\n") +
                        escapeMd("В домашней папке вам доступны следующие кнопки управления:\n") +
                        escapeMd(Strings.Uni.label + " - создание заметки в текущей папке. ") + mdItalic("после нажатия нужно будет ввести текст заметки") + "\n" +
                        escapeMd(Strings.Uni.mkdir + " - создание подпапки в текущей папке. ") + mdItalic("после нажатия нужно будет ввести имя новой папки") + "\n" +
                        escapeMd(Strings.Uni.gear + " - переход в режим управления метками. ") + mdItalic("предоставляет доступ к редактированию и удалению меток в домашней папке")
            );
        init(Value.SHARE_DIR_HELP, mdBold("Manage folder's access sharing\n") +
                        escapeMd("In TeleFS you can share folder in two ways: public anonymous link and personal grants..\n\n" +
                                "Anonymous access is granted via public http-link. You can achieve it with click on " + Strings.Uni.link + " button. Second click removes public " +
                                        "link." +
                                "With clicking on this link a user achieves a 'read-only' access to your folder.\n\n" +
                                "Personal access is granted individually for every user. If you click a " + Strings.Uni.mkGrant + " button, then bot will ask you to share with " +
                                "it contact of the person you want grant access to. It means that a person must be telegram user and you must have him in contacts. " +
                                "After contact received bot will display a button with person's name in a share management list.\n" +
                                "Personal access can be 'read-only' or 'full'. 'Full' means that a person could change or even delete your folder's entire content, be careful " +
                                "with it. You can change access type with simple clicking person's button.\n" +
                                "Right of each personal access's button there is drop button with " + Strings.Uni.drop + " icon - if you click it this person's access will be " +
                                "immediately removed.\n" +
                                Strings.Uni.cancel + " - return to the folder view mode."),
                mdBold("Управление доступом к папке\n") +
                        escapeMd("В TeleFS вы можете предоставлять доступ к своим папкам и файлам двумя способами: публичная ссылка и персональный доступ.\n\n" +
                                "Публичный анонимный доступ предоставляется путём создания http-ссылки на папку. Это делается нажатием на кнопку " + Strings.Uni.link +
                                ". Повторное нажатие удаляет " +
                                "ранее созданную ссылку. При переходе по данной ссылке любой пользователь telegram получает доступ 'только для чтения' к вашей папке.\n\n" +
                                "Персональный доступ предоставляется индивидуально для каждого человека. После нажатия кнопки " + Strings.Uni.mkGrant + " вам будет " +
                                "предложено прислать боту контакт человека, с которым вы хотите поделиться содержимым папки. Это означает, что человек должен быть " +
                                "пользователем telegram и он должен быть у вас в контактах. После получения контакта, кнопка с именем пользователя будет отображена в " +
                                "списке управления доступом. Персональный доступ может быть как 'только для чтения' так и 'полный доступ'; второй вариант означает, что" +
                                " этот пользователь сможет изменять и удалять содержимое папки, будьте осторожны в предоставлении такого варианта. Для изменения режима " +
                                "предоставления доступа достаточно один раз кликнуть на кнопку с именем пользователя.\n" +
                                "Справа от каждой кнопки персонального доступа находится кнопка удаления с иконкой " + Strings.Uni.drop + " - при клике на неё доступ к " +
                                "данной папке для данного пользователя будет сразу же аннулирован.\n" +
                                Strings.Uni.cancel + " - кнопка возврата к режиму просмотра папки.")
            );
        init(Value.SHARE_FILE_HELP, mdBold("Manage file's access sharing\n") +
                        escapeMd("In TeleFS you can share file in two ways: public anonymous link and personal grants..\n\n" +
                                "Anonymous access is granted via public http-link. You can achieve it with click on " + Strings.Uni.link + " button. Second click removes public " +
                                "link." +
                                "With clicking on this link a user achieves a 'read-only' access to your file.\n\n" +
                                "Personal access is granted individually for every user. If you click a " + Strings.Uni.mkGrant + " button, then bot will ask you to share with " +
                                "it contact of the person you want grant access to. It means that a person must be telegram user and you must have him in contacts. " +
                                "After contact received bot will display a button with person's name in a share management list.\n" +
                                "Personal access can be 'read-only' or 'full'. 'Full' means that a person could change or even delete your file, be careful " +
                                "with it. You can change access type with simple clicking person's button.\n" +
                                "Right of each personal access's button there is drop button with " + Strings.Uni.drop + " icon - if you click it this person's access will be " +
                                "immediately removed.\n" +
                                Strings.Uni.cancel + " - return to the file view mode."),
                mdBold("Управление доступом к файлу\n") +
                        escapeMd("В TeleFS вы можете предоставлять доступ к своим папкам и файлам двумя способами: публичная ссылка и персональный доступ.\n\n" +
                                "Публичный анонимный доступ предоставляется путём создания http-ссылки на файл. Это делается нажатием на кнопку " + Strings.Uni.link +
                                ". Повторное нажатие удаляет " +
                                "ранее созданную ссылку. При переходе по данной ссылке любой пользователь telegram получает доступ 'только для чтения' к вашему файлу" +
                                ".\n\nПерсональный доступ предоставляется индивидуально для каждого человека. После нажатия кнопки " + Strings.Uni.mkGrant + " вам будет " +
                                "предложено прислать боту контакт человека, с которым вы хотите поделиться данным файлом. Это означает, что человек должен быть " +
                                "пользователем telegram и он должен быть у вас в контактах. После получения контакта, кнопка с именем пользователя будет отображена в " +
                                "списке управления доступом. Персональный доступ может быть как 'только для чтения' так и 'полный доступ'; второй вариант означает, что" +
                                " этот пользователь сможет переименовать или удалить файл, будьте осторожны в предоставлении такого варианта. Для изменения режима " +
                                "предоставления доступа достаточно один раз кликнуть на кнопку с именем пользователя.\n" +
                                "Справа от каждой кнопки персонального доступа находится кнопка удаления с иконкой " + Strings.Uni.drop + " - при клике на неё доступ к " +
                                "данному файлу для данного пользователя будет сразу же аннулирован.\n" +
                                Strings.Uni.cancel + " - кнопка возврата к режиму просмотра файла.")
            );
        init(Value.GEAR_HELP, mdBold("Folder manage mode\n") +
                        escapeMd("Navigation is unavailable in this mode, only folder's management.\n\nFirst row is a control buttons:" +
                                Strings.Uni.keyLock + " - set/clear access password.\n" +
                                Strings.Uni.share + " - share folder's access.\n" +
                                Strings.Uni.edit + " - rename the folder. ") + mdItalic("you will have to type new folder's name after click.\n") +
                        escapeMd(Strings.Uni.drop + " - delete the folder. ") + mdItalicU("folder will be deleted after click " + mdBold("with its entire content\n")) +
                        escapeMd(Strings.Uni.cancel + " - back to the view mode\n\n" +
                                        "If notes are found in the folder it will be displayed with buttons, below control buttons.\n" +
                                        "Click on the note button will take you in the note's management mode."
                                ),
                mdBold("Режим управления папкой\n") +
                        escapeMd("В данном режиме недоступна навигация ни вверх ни вниз по иерархии.\n\nВ первой строке находятся кнопки управления самой папкой:" +
                                Strings.Uni.keyLock + " - задать/снять пароль на доступ к папке.\n" +
                                Strings.Uni.share + " - управление правами доступа к папке.\n" +
                                Strings.Uni.edit + " - переименование папки. ") + mdItalic("после нажатия нужно будет ввести новое имя папки.\n") +
                        escapeMd(Strings.Uni.drop + " - удаление папки. ") + mdItalicU("после нажатия папка будет безвозвратно удалена " + mdBold("вместе со всем содержимым\n")) +
                        escapeMd(Strings.Uni.cancel + " - выход из режима управления\n\n" +
                                        "Если в папке имеются заметки, то они будут отображены кнопками, ниже ряда кнопок управления.\n" +
                                        "Клик по кнопке заметки приводит к переходу в режим управления этой заметкой."
                                ));
        init(Value.LABEL_HELP, mdBold("Note management mode\n") +
                        escapeMd("Here you can edit or delete a note with these buttons:\n" +
                                Strings.Uni.edit + " - edit note. ") + mdItalic("You will have to type new note's text after click\n") +
                        escapeMd(Strings.Uni.drop + " - delete note. ") + mdItalic("Note will be permanently deleted after click.\n") +
                        escapeMd(Strings.Uni.goUp + " - back to the parent's folder view mode. "),
                mdBold("Режим управления заметкой\n") +
                        escapeMd("Здесь вы можете отредактировать или удалить заметку, с помощью соответствующих кнопок:\n" +
                                Strings.Uni.edit + " - редактирование заметки. ") + mdItalic("После нажатия нужно будет ввести новый текст заметки\n") +
                        escapeMd(Strings.Uni.drop + " - удаление заметки. ") + mdItalic("После нажатия заметка будет безвозвратно удалена.\n") +
                        escapeMd(Strings.Uni.goUp + " - выход из режима управления. ") + mdItalic("После нажатаия вы будете возвращены в режим просмотра родительской папки.")
            );
        init(Value.GEARING, "Manage folder '%s'", "Управление папкой '%s'");
        init(Value.CANT_MKDIR, "cannot create directory ‘%s’: File exists", "Невозможно создать папку '%s': файл уже существует");
        init(Value.CANT_RN_TO, "cannot rename to ‘%s’: File exists", "Невозможно переименовать в '%s': файл уже существует");
        init(Value.CANT_MKLBL, "cannot create label ‘%s’: File exists", "Невозможно создать заметку '%s': файл уже существует");
        init(Value.NO_RESULTS, "Nothing found", "Ничего не найдено");
        init(Value.RESULTS_FOUND, "Found %s item(s)", "позиций найдено: %s");
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
        init(Value.SEARCHED, "Search for '%s' in '%s'", "Поиск '%s' в '%s'");
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
        init(Value.SEND_CONTACT_DIR, "Send me a contact of the person you want to grant access to folder '%s'",
                "Пришли мне контакт того, кому хочешь предоставить доступ к папке '%s'");
        init(Value.SEND_CONTACT_FILE, "Send me a contact of the person you want to grant access to file '%s'",
                "Пришли мне контакт того, кому хочешь предоставить доступ к файлу '%s'");
        init(Value.CANT_GRANT, "Access already granted to %s", "%s: доступ уже предоставлен");
        init(Value.SHARE_RW, Strings.Uni.mkGrant + " %s [Read/Write]", Strings.Uni.mkGrant + " %s [полный доступ]");
        init(Value.SHARE_RO, Strings.Uni.mkGrant + " %s [Read only]", Strings.Uni.mkGrant + " %s [только чтение]");
        init(Value.SHARES, Strings.Uni.share + " network", Strings.Uni.share + " сеть");
        init(Value.SHARES_ANONYM, "common", "общие");
        init(Value.NOT_ALLOWED, "You're not allowed to do it here", "Это действие запрещено в текущей папке");
        init(Value.NOT_ALLOWED_THIS, "You're not allowed to do it to this entry", "Это действие запрещено в для данного элемента");
        init(Value.FILE_ACCESS, "Grant access to file %s", "Доступ к файлу %s");
        init(Value.DIR_ACCESS, "Grant access to folder %s", "Доступ к папке %s");
        init(Value.TYPE_REWRITE, "Write new text for label:", "Напиши новый текст заметки:");
        init(Value.TYPE_LOCK_DIR, "Type a new password for folder '%s'", "Напиши новый пароль для папки '%s'");
        init(Value.TYPE_LOCK_FILE, "Type a new password for file '%s'", "Напиши новый пароль для файла '%s'");
        init(Value.TYPE_PASSWORD_FILE, "File '%s' is protected with password, type it:", "Доступ к Файлу '%s' ограничен, напиши пароль:");
        init(Value.TYPE_PASSWORD_DIR, "Folder '%s' is protected with password, type it:", "Доступ к папке '%s' ограничен, напиши пароль:");
        init(Value.PASSWORD_FAILED, "Wrong password", "Неверный пароль");
    }

    private static void init(final Value key, final String en, final String ru) {
        enData.put(key, en);
        ruData.put(key, ru);
    }

    public enum Value {
        SEARCHED_HELP, ROOT_HELP, SHARE_FILE_HELP, GEAR_HELP, LS_HELP, FILE_HELP, LABEL_HELP, CANT_MKDIR, CANT_RN_TO, CANT_MKLBL, NO_RESULTS, TYPE_RENAME,
        TYPE_FOLDER, CD, PAGE, NORMAL_MODE, EDIT_MODE, DELETED, DELETED_MANY, MOVE_DEST, DESELECTED, SELECTED, MOVED, TYPE_LABEL, SEARCHED, NO_CONTENT, LANG_SWITCHED,
        RESULTS_FOUND, UPLOADED, None, CHECK_ALL, NO_GLOBAL_LINK, NO_PERSONAL_GRANTS, GEARING, PASS_RESET, PASS_DROP, PASSWORD_SET, PASSWORD_NOT_SET, VALID_ONETIME,
        VALID_UNTILL, VALID_CANCEL, VALID_NOT_SET, VALID_SET_OTU, VALID_SET_UNTILL, LINK_DELETED, LINK_SAVED, PASS_SET, TYPE_PASSWORD, TYPE_PASSWORD2, PASSWORD_SET_TXT,
        PASSWORD_NOT_MATCH, PASSWORD_CLEARED, VALID_CLEARED, OTU_SET, SEND_CONTACT_DIR, SEND_CONTACT_FILE, CANT_GRANT, SHARE_RW, SHARE_RO, SHARES, SHARES_ANONYM, NOT_ALLOWED,
        NOT_ALLOWED_THIS, LINK, FILE_ACCESS, TYPE_REWRITE, SHARE_DIR_HELP, TYPE_LOCK_DIR, TYPE_LOCK_FILE, TYPE_PASSWORD_FILE, TYPE_PASSWORD_DIR, PASSWORD_FAILED, DIR_ACCESS
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
