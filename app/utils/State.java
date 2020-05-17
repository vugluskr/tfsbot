package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.TFile;
import model.User;
import model.telegram.ContentType;
import model.telegram.api.CallbackAnswer;
import model.telegram.api.InlineButton;
import model.telegram.api.TeleFile;
import play.Logger;
import play.libs.Json;
import services.FsService;
import services.GUI;
import services.TgApi;
import services.UserService;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static utils.Callback.*;
import static utils.LangMap.v;
import static utils.TextUtils.*;

/**
 * @author Denis Danilin | denis@danilin.name
 * 17.05.2020
 * tfs ☭ sweat and blood
 */
public abstract class State {
    private static final Logger.ALogger logger = Logger.of(State.class);
    private static final Map<String, Constructor<? extends State>> map = new HashMap<>(0);

    static {
        try {
            map.put(Gear.class.getSimpleName(), Gear.class.getDeclaredConstructor());
            map.put(Renaming.class.getSimpleName(), Renaming.class.getDeclaredConstructor());
            map.put(Moving.class.getSimpleName(), Moving.class.getDeclaredConstructor());
            map.put(MkDir.class.getSimpleName(), MkDir.class.getDeclaredConstructor());
            map.put(MkLabel.class.getSimpleName(), MkLabel.class.getDeclaredConstructor());
            map.put(Search.class.getSimpleName(), Search.class.getDeclaredConstructor());
            map.put(SearchGear.class.getSimpleName(), SearchGear.class.getDeclaredConstructor());
            map.put(OpenFile.class.getSimpleName(), OpenFile.class.getDeclaredConstructor());
            map.put("", View.class.getDeclaredConstructor());
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void freshInit(final User user, final TgApi tgApi, final GUI gui, final UserService userService, final FsService fsService) {
        final State state = new View();
        state.dirId = 1;
        state.userService = userService;
        state.gui = gui;
        state.fsService = fsService;
        state.tgApi = tgApi;
        state.user = user;

        user.setState(state);
        logger.debug("Built fresh state for user #" + user.getId());
    }

    public static JsonNode stateToJson(final State state) {
        final ObjectNode node = Json.newObject();
        node.put("type", state.getClass().getSimpleName());
        node.put("dir_id", state.dirId);
        node.set("data", state.toJson());
        if (state.routeBack != null) {
            final ObjectNode rb = node.with("routeback");
            rb.put("type", state.routeBack.getClass().getSimpleName());
            rb.put("dir_id", state.routeBack.dirId);
            rb.set("data", state.routeBack.toJson());
        }

        return node;
    }

    @SuppressWarnings("unchecked")
    public static <T extends State> T stateFromJson(final JsonNode node, final User user, final TgApi tgApi, final GUI gui, final UserService userService,
                                                    final FsService fsService) {
        try {
            logger.debug("Rebuild session from: " + node);
            final State state = map.getOrDefault(node.get("type").asText(), map.get("")).newInstance();
            logger.debug("Built: " + state.getClass().getSimpleName());

            state.fromJson(node.get("data"));
            state.dirId = node.get("dir_id").asInt();
            state.user = user;
            state.gui = gui;
            state.userService = userService;
            state.fsService = fsService;
            state.tgApi = tgApi;

            if (node.has("routeback"))
                state.routeBack = stateFromJson(node.get("routeback"), user, tgApi, gui, userService, fsService);

            return (T) state;
        } catch (final Exception e) {
            logger.error("Cant restore state: " + e.getMessage(), e);
        }

        return null;
    }

    protected GUI gui;
    protected UserService userService;
    protected FsService fsService;
    protected TgApi tgApi;
    protected User user;
    protected State routeBack;

    protected long dirId;
    protected final Consumer<Long> dialogIdConsumer = dialogId -> {
        user.setLastDialogId(dialogId);
        userService.updateOpts(user);
    };

    public abstract void refreshView();

    protected abstract JsonNode toJson();

    protected abstract void fromJson(JsonNode node);

    protected <T extends State> T chainBack(final T forward) {
        return switchTo(forward, routeBack != null ? routeBack : new View());
    }

    protected final void goBack() {
        switchTo(routeBack != null ? routeBack : new View(), null);
    }

    protected <T extends State> T switchTo(T state) {
        return switchTo(state, this);
    }

    private <T extends State> T switchTo(T state, final State routeBack) {
        logger.debug("Switching from '" + getClass().getSimpleName() + "' to '" + state.getClass().getSimpleName() + "'");
        user.setState(state);
        state.dirId = dirId;
        state.user = user;
        state.fsService = fsService;
        state.userService = userService;
        state.gui = gui;
        state.tgApi = tgApi;
        state.routeBack = routeBack;

        return state;
    }

    public final CallbackAnswer apply(final TeleFile file, final String input, final String callbackData) {
        if (file != null) {
            fsService.upload(TFileFactory.file(file, input, dirId), user);
            return new CallbackAnswer(v(LangMap.Names.UPLOADED, user, file.getFileName()));
        }

        if (callbackData != null)
            return applyCallback(callbackData);

        handleInput(input);

        return null;
    }

    protected CallbackAnswer applyCallback(final String callback) { return null; }

    protected void handleInput(final String input) {
        if (!isEmpty(input))
            fsService.upload(TFileFactory.label(input, dirId), user);
    }

    public static class View extends State implements Callback {
        private static final Logger.ALogger logger = Logger.of(View.class);
        private int offset;

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();
            node.put("offset", offset);

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || node.size() <= 0)
                return;

            offset = node.has("offset") ? node.get("offset").asInt() : 0;
        }

        @Override
        protected CallbackAnswer applyCallback(final String callbackData) {
            String answer = "";
            switch (callbackData) {
                case goUp:
                    if (dirId > 1) {
                        final TFile dir = fsService.get(dirId, user);
                        dirId = dir.getParentId();
                        offset = 0;
                        answer = v(LangMap.Names.CD, user, dir.getPath());
                    }
                    break;
                case rewind:
                    offset = Math.max(0, offset - 10);
                    answer = v(LangMap.Names.PAGE, user, (offset / 10) + 1);
                    break;
                case forward:
                    offset += 10;
                    answer = v(LangMap.Names.PAGE, user, (offset / 10) + 1);
                    break;
                case mkLabel:
                    switchTo(new MkLabel());
                    break;
                case searchStateInit:
                    switchTo(new Search());
                    break;
                case mkDir:
                    switchTo(new MkDir());
                    break;
                case gearStateInit:
                    final Gear gear = switchTo(new Gear());
                    gear.offset = offset;
                    answer = v(LangMap.Names.EDIT_MODE, user);
                    break;
                default:
                    if (!callbackData.startsWith(openEntry))
                        return null;

                    final long id = getLong(callbackData);

                    if (id <= 0)
                        break;

                    final TFile entry = fsService.get(id, user);
                    if (entry.isDir()) {
                        dirId = id;
                        offset = 0;
                        answer = v(LangMap.Names.CD, user, entry.getName());
                        break;
                    } else if (entry.isLabel())
                        break;

                    switchTo(new OpenFile()).itemId = id;
                    break;
            }

            return new CallbackAnswer(answer);
        }

        @Override
        public void refreshView() {
            try {
                final List<TFile> scope = fsService.list(dirId, user);
                final List<InlineButton> upper = new ArrayList<>(0), bottom = new ArrayList<>(0);
                if (dirId > 1) upper.add(GUI.Buttons.goUpButton);
                upper.add(GUI.Buttons.mkLabelButton);
                upper.add(GUI.Buttons.mkDirButton);
                upper.add(GUI.Buttons.searchButton);
                upper.add(GUI.Buttons.gearButton);

                if (offset > 0)
                    bottom.add(GUI.Buttons.rewindButton);
                if (!scope.isEmpty() && offset + 10 < scope.stream().filter(e -> e.getType() != ContentType.LABEL).count())
                    bottom.add(GUI.Buttons.forwardButton);

                final TFile cd = fsService.get(dirId, user);

                gui.makeMainView(escapeMd(cd.getPath()) + (scope.isEmpty() ? "\n\n_" + escapeMd(v(LangMap.Names.NO_CONTENT, user)) + "_" : ""), scope, offset, upper, bottom,
                        user.getLastMessageId(), user.getId(), msgId -> {
                            if (msgId == 0 || msgId != user.getLastMessageId()) {
                                user.setLastMessageId(msgId);
                                userService.updateOpts(user);
                            }
                        });
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public static class Gear extends State {
        private static final Logger.ALogger logger = Logger.of(Gear.class);

        private final Set<Long> selection = new HashSet<>();
        private int offset;

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();

            node.put("offset", offset);
            selection.forEach(i -> node.withArray("selection").add(i));

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || node.size() <= 0) return;

            offset = node.has("offset") ? node.get("offset").asInt() : 0;
            if (node.has("selection"))
                node.get("selection").forEach(j -> selection.add(j.asLong()));
        }

        @Override
        public CallbackAnswer applyCallback(final String callbackData) {
            String answer = "";

            switch (callbackData) {
                case move:
                    if (isEmpty(selection))
                        break;
                    final Moving moving = switchTo(new Moving());
                    moving.ids.addAll(selection);
                    break;
                case drop:
                    fsService.rm(selection, user);
                    answer = v(LangMap.Names.DELETED_MANY, user, selection.size());
                    selection.clear();
                    break;
                case cancel:
                    goBack();
                    break;
                case rewind:
                    offset = Math.max(0, offset - 10);
                    answer = v(LangMap.Names.PAGE, user, (offset / 10) + 1);
                    break;
                case forward:
                    offset += 10;
                    answer = v(LangMap.Names.PAGE, user, (offset / 10) + 1);
                    break;
                default:
                    final long itemId = getLong(callbackData);

                    if (callbackData.startsWith(renameEntry) && (itemId > 0 || !selection.isEmpty())) {
                        switchTo(new Renaming()).itemId = itemId > 0 ? itemId : selection.iterator().next();
                        break;
                    } else if (callbackData.startsWith(inversCheck)) {
                        if (selection.remove(itemId))
                            answer = v(LangMap.Names.DESELECTED, user);
                        else if (selection.add(itemId))
                            answer = v(LangMap.Names.SELECTED, user);
                    } else
                        return null;
            }

            return new CallbackAnswer(answer);
        }

        @Override
        public void refreshView() {
            try {
                final List<TFile> scope = fsService.list(dirId, user);
                final List<InlineButton> upper = new ArrayList<>(0), bottom = new ArrayList<>(0);
                if (!selection.isEmpty()) {
                    if (selection.size() == 1)
                        upper.add(GUI.Buttons.renameButton);

                    upper.add(new InlineButton(Uni.move + "(" + selection.size() + ")", move));
                    upper.add(new InlineButton(Uni.drop + "(" + selection.size() + ")", drop));
                }

                upper.add(GUI.Buttons.cancelButton);

                if (offset > 0)
                    bottom.add(GUI.Buttons.rewindButton);
                if (!scope.isEmpty() && offset + 10 < scope.stream().filter(e -> e.getType() != ContentType.LABEL).count())
                    bottom.add(GUI.Buttons.forwardButton);

                final TFile cd = fsService.get(dirId, user);

                gui.makeGearView(escapeMd(cd.getPath()) + (scope.isEmpty() ? "\n\n_" + escapeMd(v(LangMap.Names.NO_CONTENT, user)) + "_" : ""), selection, scope, upper,
                        bottom, offset, user, msgId -> {
                            if (msgId == 0 || msgId != user.getLastMessageId()) {
                                user.setLastMessageId(msgId);
                                userService.updateOpts(user);
                            }
                        });
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public static class Renaming extends State {
        private long itemId;

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();

            node.put("item_id", itemId);

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || node.size() <= 0)
                return;

            itemId = node.has("item_id") ? node.get("item_id").asLong() : 0;
        }

        @Override
        public void refreshView() {
            tgApi.ask(v(LangMap.Names.TYPE_RENAME, user, fsService.get(itemId, user).getName()), user.getId(), dialogIdConsumer);
        }

        @Override
        protected CallbackAnswer applyCallback(final String callback) {
            goBack();
            return null;
        }

        @Override
        protected void handleInput(final String input) {
            if (!isEmpty(input)) {
                if (fsService.findAt(input, dirId, user) != null) {
                    tgApi.sendPlainText(v(LangMap.Names.CANT_RN_TO, user, input), user.getId(), dialogIdConsumer);
                    return;
                }

                final TFile file = fsService.get(itemId, user);

                if (!file.getName().equals(input)) {
                    file.setName(input);
                    fsService.updateMeta(file, user);
                }
            }

            goBack();
        }
    }

    public static class Moving extends State {
        private final Set<Long> ids = new HashSet<>(0);
        private int offset;

        @Override
        public void refreshView() {
            final List<TFile> scope = fsService.listFolders(dirId, user);
            final List<InlineButton> upper = new ArrayList<>(0), bottom = new ArrayList<>(0);
            if (dirId > 1)
                upper.add(GUI.Buttons.goUpButton);
            upper.add(GUI.Buttons.putButton);
            upper.add(GUI.Buttons.cancelButton);

            if (offset > 0)
                bottom.add(GUI.Buttons.rewindButton);
            if (!scope.isEmpty() && offset + 10 < scope.stream().filter(e -> e.getType() != ContentType.LABEL).count())
                bottom.add(GUI.Buttons.forwardButton);

            final TFile cd = fsService.get(dirId, user);

            gui.makeMovingView(escapeMd(cd.getPath()), scope, upper, bottom, offset, user, msgId -> {
                if (msgId == 0 || msgId != user.getLastMessageId()) {
                    user.setLastMessageId(msgId);
                    userService.updateOpts(user);
                }
            });
        }

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();

            node.put("offset", offset);
            ids.forEach(i -> node.withArray("ids").add(i));

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || node.size() <= 0) return;

            offset = node.has("offset") ? node.get("offset").asInt() : 0;
            if (node.has("ids"))
                node.get("ids").forEach(j -> ids.add(j.asLong()));
        }

        @Override
        public CallbackAnswer applyCallback(final String callbackData) {
            String answer = "";

            switch (callbackData) {
                case rewind:
                    offset = Math.max(0, offset - 10);
                    answer = v(LangMap.Names.PAGE, user, (offset / 10) + 1);
                    break;
                case forward:
                    offset += 10;
                    answer = v(LangMap.Names.PAGE, user, (offset / 10) + 1);
                    break;
                case goUp:
                    if (dirId <= 1)
                        break;
                    final TFile parent = fsService.getParentOf(dirId, user);
                    this.dirId = parent.getId();
                    answer = v(LangMap.Names.CD, user, parent.getName());
                    break;
                case put:
                    if (ids.isEmpty())
                        break;

                    final List<TFile> selection = fsService.getByIds(ids, user);
                    final Set<Long> predictors = fsService.getPredictors(dirId, user).stream().map(TFile::getId).collect(Collectors.toSet());
                    final AtomicInteger counter = new AtomicInteger(0);
                    selection.stream().filter(f -> !f.isDir() || !predictors.contains(f.getId())).peek(e -> counter.incrementAndGet()).forEach(f -> f.setParentId(dirId));
                    fsService.updateMetas(selection, user);
                    answer = v(LangMap.Names.MOVED, user, counter.get());
                case cancel:
                    goBack();
                    break;
                default:
                    if (!callbackData.startsWith(openEntry))
                        return null;

                    final TFile dir = fsService.get(getLong(callbackData), user);
                    if (dir == null || !dir.isDir())
                        goBack();
                    else {
                        this.dirId = dir.getId();
                        answer = v(LangMap.Names.CD, user, dir.getName());
                    }
                    break;
            }

            return new CallbackAnswer(answer);
        }
    }

    public static class MkDir extends State {
        @Override
        public JsonNode toJson() {
            return Json.newObject();
        }

        @Override
        public void fromJson(final JsonNode node) { }

        @Override
        public void refreshView() {
            tgApi.ask(v(LangMap.Names.TYPE_FOLDER, user), user.getId(), dialogIdConsumer);
        }

        @Override
        protected void handleInput(final String input) {
            if (isEmpty(input)) return;

            if (fsService.findAt(input, dirId, user) != null)
                tgApi.sendPlainText(v(LangMap.Names.CANT_MKDIR, user, input), user.getId(), dialogIdConsumer);
            else {
                fsService.mkdir(input, dirId, user.getId());
                goBack();
            }
        }

        @Override
        protected CallbackAnswer applyCallback(final String callback) {
            goBack();
            return null;
        }
    }

    public static class MkLabel extends State {
        @Override
        public JsonNode toJson() {
            return Json.newObject();
        }

        @Override
        public void fromJson(final JsonNode node) { }

        @Override
        public void refreshView() {
            tgApi.ask(v(LangMap.Names.TYPE_LABEL, user), user.getId(), dialogIdConsumer);
        }

        @Override
        public void handleInput(final String input) {
            if (isEmpty(input)) return;

            if (fsService.findAt(input, dirId, user) != null)
                tgApi.sendPlainText(v(LangMap.Names.CANT_MKLBL, user, input), user.getId(), dialogIdConsumer);
            else {
                fsService.upload(TFileFactory.label(input, dirId), user);
                goBack();
            }
        }

        @Override
        protected CallbackAnswer applyCallback(final String callback) {
            goBack();
            return null;
        }
    }

    public static class Search extends State {
        private String query = null, body = null;
        private int offset;

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();

            node.put("query", query);
            node.put("body", body);
            node.put("offset", offset);

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || node.size() <= 0)
                return;

            query = node.has("query") ? node.get("query").asText() : null;
            body = node.has("body") ? node.get("body").asText() : null;
            offset = node.has("offset") ? node.get("offset").asInt() : 0;
        }

        @Override
        public CallbackAnswer applyCallback(final String callbackData) {
            String answer = "";

            switch (callbackData) {
                case searchStateInit:
                    tgApi.ask(prompt(), user.getId(), dialogIdConsumer);
                    break;
                case gearStateInit:
                    final SearchGear sg = switchTo(new SearchGear());
                    sg.offset = offset;
                    sg.query = query;
                    break;
                case cancel:
                    goBack();
                    break;
                case rewind:
                    offset = Math.max(0, offset - 10);
                    answer = v(LangMap.Names.PAGE, user, (offset / 10) + 1);
                    break;
                case forward:
                    offset += 10;
                    answer = v(LangMap.Names.PAGE, user, (offset / 10) + 1);
                    break;
                default:
                    if (!callbackData.startsWith(openEntry))
                        return null;

                    final TFile file = fsService.get(getLong(callbackData), user);
                    if (file == null || file.isLabel())
                        break;

                    if (file.isDir())
                        switchTo(new View()).dirId = file.getId();
                    else
                        switchTo(new OpenFile()).itemId = file.getId();

                    break;
            }

            return new CallbackAnswer(answer);
        }

        @Override
        public void handleInput(final String input) {
            query = input;

            final int found = fsService.findChildsByName(dirId, input.toLowerCase(), user);
            if (found > 0)
                body = escapeMd(v(LangMap.Names.SEARCHED, user, input, found));
            else
                body = escapeMd(v(LangMap.Names.NO_RESULTS, user, input));
        }

        public String prompt() {
            return v(LangMap.Names.TYPE_LABEL, user);
        }

        @Override
        public void refreshView() {
            if (isEmpty(query)) {
                tgApi.ask(prompt(), user.getId(), dialogIdConsumer);
                return;
            }

            final List<TFile> scope = fsService.getFound(user);
            final List<InlineButton> upper = new ArrayList<>(0), bottom = new ArrayList<>(0);
            upper.add(GUI.Buttons.searchButton);
            upper.add(GUI.Buttons.gearButton);
            upper.add(GUI.Buttons.cancelButton);

            if (offset > 0)
                bottom.add(GUI.Buttons.rewindButton);
            if (!scope.isEmpty() && offset + 10 < scope.stream().filter(e -> e.getType() != ContentType.LABEL).count())
                bottom.add(GUI.Buttons.forwardButton);

            gui.makeMainView(body, scope, offset, upper, bottom, user.getLastMessageId(), user.getId(), msgId -> {
                if (msgId == 0 || msgId != user.getLastMessageId()) {
                    user.setLastMessageId(msgId);
                    userService.updateOpts(user);
                }
            });
        }
    }

    public static class SearchGear extends State {
        private final Set<Long> selection = new HashSet<>();
        private int offset;
        private String query;

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();

            node.put("offset", offset);
            node.put("query", query);
            selection.forEach(i -> node.withArray("selection").add(i));

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || node.size() <= 0) return;

            query = node.has("query") ? node.get("query").asText() : "";
            offset = node.has("offset") ? node.get("offset").asInt() : 0;
            if (node.has("selection"))
                node.get("selection").forEach(j -> selection.add(j.asLong()));
        }

        @Override
        public CallbackAnswer applyCallback(final String callbackData) {
            String answer = "";

            switch (callbackData) {
                case move:
                    if (isEmpty(selection))
                        break;
                    switchTo(new Moving()).ids.addAll(selection);
                    break;
                case drop:
                    if (!selection.isEmpty()) {
                        fsService.rm(selection, user);
                        answer = v(LangMap.Names.DELETED_MANY, user, selection.size());
                        selection.clear();
                    }
                    break;
                case cancel:
                    goBack();
                    break;
                case rewind:
                    offset = Math.max(0, offset - 10);
                    answer = v(LangMap.Names.PAGE, user, (offset / 10) + 1);
                    break;
                case forward:
                    offset += 10;
                    answer = v(LangMap.Names.PAGE, user, (offset / 10) + 1);
                    break;
                default:
                    if (!callbackData.startsWith(renameEntry) && !callbackData.startsWith(inversCheck))
                        return null;

                    final long itemId = getLong(callbackData);
                    if (itemId <= 0)
                        break;

                    if (callbackData.startsWith(renameEntry)) {
                        switchTo(new Renaming()).itemId = itemId;
                        break;
                    } else // check/uncheck
                        if (selection.remove(itemId))
                            answer = v(LangMap.Names.DESELECTED, user);
                        else if (selection.add(itemId))
                            answer = v(LangMap.Names.SELECTED, user);
            }

            return new CallbackAnswer(answer);
        }

        @Override
        public void refreshView() {
            final List<TFile> scope = fsService.getFound(user);
            final List<InlineButton> upper = new ArrayList<>(0), bottom = new ArrayList<>(0);

            if (!selection.isEmpty()) {
                if (selection.size() == 1)
                    upper.add(GUI.Buttons.renameButton);

                upper.add(new InlineButton(Uni.move + "(" + selection.size() + ")", move));
                upper.add(new InlineButton(Uni.drop + "(" + selection.size() + ")", drop));
            }

            upper.add(GUI.Buttons.cancelButton);

            if (offset > 0)
                bottom.add(GUI.Buttons.rewindButton);
            if (!scope.isEmpty() && offset + 10 < scope.stream().filter(e -> e.getType() != ContentType.LABEL).count())
                bottom.add(GUI.Buttons.forwardButton);

            gui.makeGearView("_" + escapeMd(v(LangMap.Names.SEARCHED, user, query, scope.size())) + "_", selection, scope, upper,
                    bottom, offset, user, msgId -> {
                        if (msgId == 0 || msgId != user.getLastMessageId()) {
                            user.setLastMessageId(msgId);
                            userService.updateOpts(user);
                        }
                    });
        }
    }

    public static class OpenFile extends State {
        private long itemId;

        @Override
        public void refreshView() {
            gui.makeFileDialog(fsService.get(itemId, user), user.getId(), dialogIdConsumer);
        }

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();

            node.put("item_id", itemId);

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || node.size() <= 0) return;

            itemId = node.has("item_id") ? node.get("item_id").asLong() : 0;
        }

        @Override
        public CallbackAnswer applyCallback(final String callbackData) {
            String answer = "";

            switch (callbackData) {
                case renameEntry:
                    chainBack(new Renaming()).itemId = itemId;
                    break;
                case move:
                    chainBack(new Moving()).ids.add(itemId);
                    break;
                case drop:
                    fsService.rm(itemId, user);
                    answer = v(LangMap.Names.DELETED, user);
                default:
                    goBack();
                    break;
            }

            return new CallbackAnswer(answer);
        }
    }
}
