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
            map.put(Gear.class.getSimpleName(), Gear.class.getConstructor());
            map.put(Renaming.class.getSimpleName(), Renaming.class.getConstructor());
            map.put(Moving.class.getSimpleName(), Moving.class.getConstructor());
            map.put(MkDir.class.getSimpleName(), MkDir.class.getConstructor());
            map.put(MkLabel.class.getSimpleName(), MkLabel.class.getConstructor());
            map.put(Search.class.getSimpleName(), Search.class.getConstructor());
            map.put(SearchGear.class.getSimpleName(), SearchGear.class.getConstructor());
            map.put(OpenFile.class.getSimpleName(), OpenFile.class.getConstructor());
            map.put(MkFile.class.getSimpleName(), MkFile.class.getConstructor());
            map.put("", MkFile.class.getConstructor());
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }


    protected GUI gui;
    protected UserService userService;
    protected FsService fsService;
    protected TgApi tgApi;
    protected User user;

    protected long dirId;
    protected final Consumer<Long> dialogIdConsumer = dialogId -> {
        user.setLastDialogId(dialogId);
        userService.updateOpts(user);
    };

    public void setDirId(final long dirId) {
        this.dirId = dirId;
    }

    public abstract boolean isCallbackAppliable(final String callbackData);

    public abstract CallbackAnswer applyCallback(final String callbackData);

    public abstract void refreshView();

    abstract JsonNode toJson();

    public abstract void fromJson(JsonNode node);

    public static void freshInit(final User user, final TgApi tgApi, final GUI gui, final UserService userService, final FsService fsService) {
        final State state = new View();
        state.dirId = 1;
        state.userService = userService;
        state.gui = gui;
        state.fsService = fsService;
        state.tgApi = tgApi;
        state.user = user;

        user.setState(state);
    }

    public static JsonNode stateToJson(final State state) {
        final ObjectNode node = Json.newObject();
        node.put("type", state.getClass().getSimpleName());
        node.put("dir_id", state.dirId);
        node.set("data", state.toJson());

        return node;
    }

    public <T extends State> T switchTo(T state) {
        user.setState(state);
        state.dirId = dirId;
        state.user = user;
        state.fsService = fsService;
        state.userService = userService;
        state.gui = gui;
        state.tgApi = tgApi;

        return state;
    }

    @SuppressWarnings("unchecked")
    public static <T extends State> T stateFromJson(final JsonNode node, final User user, final TgApi tgApi, final GUI gui, final UserService userService,
                                                    final FsService fsService) {
        try {
            final State state = map.getOrDefault(node.get("type").asText(), map.get("")).newInstance();

            state.fromJson(node.get("data"));
            state.dirId = node.get("dir_id").asInt();
            state.user = user;
            state.gui = gui;
            state.userService = userService;
            state.fsService = fsService;
            state.tgApi = tgApi;

            return (T) state;
        } catch (final Exception e) {
            logger.error("Cant restore state: " + e.getMessage(), e);
        }

        return null;
    }

    protected <T extends State> T fromMyBackup(final JsonNode node) {
        return State.stateFromJson(node, user, tgApi, gui, userService, fsService);
    }

    public static class View extends State implements Callback {
        private static final Logger.ALogger logger = Logger.of(View.class);
        private static final Set<String> appliableCallbacks =
                Arrays.stream(new String[]{goUp, mkLabel, searchStateInit, mkDir, gearStateInit, rewind, forward}).collect(Collectors.toSet());
        private int offset;

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();
            node.put("dir_id", dirId);
            node.put("offset", offset);

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || node.size() <= 0)
                return;

            dirId = node.has("dir_id") ? node.get("dir_id").asInt() : 1;
            offset = node.has("offset") ? node.get("offset").asInt() : 0;
        }

        @Override
        public boolean isCallbackAppliable(final String callbackData) {
            return appliableCallbacks.contains(callbackData) || callbackData.startsWith(openEntry);
        }

        @Override
        public CallbackAnswer applyCallback(final String callbackData) {
            String answer = "";
            switch (callbackData) {
                case goUp:
                    if (dirId > 1) {
                        final TFile dir = fsService.get(dirId, user);
                        dirId = dir.getParentId();
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
                    switchTo(new MkLabel()).setRecoil(this);
                    break;
                case searchStateInit:
                    switchTo(new Search()).setFallback(this);
                    break;
                case mkDir:
                    switchTo(new MkDir()).setRecoil(this);
                    break;
                case gearStateInit:
                    final Gear gear = switchTo(new Gear());
                    gear.setFallback(this);
                    gear.offset = offset;
                    answer = v(LangMap.Names.EDIT_MODE, user);
                    break;
                default:
                    final long id = getLong(callbackData);

                    if (id <= 0)
                        break;

                    final TFile entry = fsService.get(id, user);
                    if (entry.isDir()) {
                        dirId = id;
                        answer = v(LangMap.Names.CD, user, entry.getName());
                        break;
                    } else if (entry.isLabel())
                        break;

                    final OpenFile openFile = switchTo(new OpenFile());
                    openFile.itemId = id;
                    openFile.fallback = this;
                    break;
            }

            return new CallbackAnswer(0, answer);
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

    public static class Gear extends State implements Fallbackable {
        private static final Logger.ALogger logger = Logger.of(Gear.class);
        private static final Set<String> appliableCallbacks =
                Arrays.stream(new String[]{inversCheck, renameEntry, move, drop, cancel, rewind, forward}).collect(Collectors.toSet());

        private final Set<Long> selection = new HashSet<>();
        private int offset;
        private State fallback;

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();

            node.put("offset", offset);
            selection.forEach(i -> node.withArray("selection").add(i));
            node.set("fallback", State.stateToJson(fallback));

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || node.size() <= 0) return;

            offset = node.has("offset") ? node.get("offset").asInt() : 0;
            if (node.has("selection"))
                node.get("selection").forEach(j -> selection.add(j.asLong()));
            fallback = node.has("fallback") ? fromMyBackup(node.get("fallback")) : new View();
        }

        @Override
        public boolean isCallbackAppliable(final String callbackData) {
            return appliableCallbacks.contains(callbackData) || callbackData.startsWith(renameEntry) || callbackData.startsWith(inversCheck);
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
                    moving.setFallback(this);
                    break;
                case drop:
                    fsService.rm(selection, user);
                    answer = v(LangMap.Names.DELETED_MANY, user, selection.size());
                    selection.clear();
                    break;
                case cancel:
                    switchTo(fallback());
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
                    if (itemId <= 0)
                        break;

                    if (callbackData.startsWith(renameEntry)) {
                        final Renaming renaming = switchTo(new Renaming());
                        renaming.setRecoil(this);
                        renaming.itemId = itemId;
                        break;
                    } else // check/uncheck
                        if (selection.remove(itemId))
                            answer = v(LangMap.Names.DESELECTED, user);
                        else if (selection.add(itemId))
                            answer = v(LangMap.Names.SELECTED, user);
            }

            return new CallbackAnswer(0, answer);
        }

        @Override
        public State fallback() {
            return fallback;
        }

        private void setFallback(final State state) {
            this.fallback = state;
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
                    upper.add(GUI.Buttons.cancelButton);
                }

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

    public static class Renaming extends State implements RequireInput, OneStep {
        private long itemId;
        private State recoil;

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();

            node.put("item_id", itemId);
            node.set("recoil", State.stateToJson(recoil));

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || node.size() <= 0)
                return;

            itemId = node.has("item_id") ? node.get("item_id").asLong() : 0;
            recoil = node.has("recoil") ? fromMyBackup(node.get("recoil")) : new View();
        }

        @Override
        public void refreshView() {
            switchTo(recoil).refreshView();
        }

        @Override
        public boolean isCallbackAppliable(final String callbackData) { return false; }

        @Override
        public CallbackAnswer applyCallback(final String callbackData) { return null; }

        @Override
        public String prompt() {
            return v(LangMap.Names.TYPE_RENAME, user, fsService.get(itemId, user).getName());
        }

        @Override
        public void accept(final String input) {
            if (isEmpty(input))
                return;

            if (fsService.findAt(input, dirId, user) != null) {
                tgApi.sendPlainText(v(LangMap.Names.CANT_RN_TO, user, input), user.getId(), dialogIdConsumer);
                return;
            }

            final TFile file = fsService.get(itemId, user);

            if (file.getName().equals(input))
                return;

            file.setName(input);
            fsService.updateMeta(file, user);
        }

        @Override
        public State recoil() {
            return recoil;
        }

        private void setRecoil(final State state) {
            this.recoil = state;
        }
    }

    public static class Moving extends State implements Fallbackable {
        private static final Logger.ALogger logger = Logger.of(Moving.class);
        private static final Set<String> appliableCallbacks =
                Arrays.stream(new String[]{put, goUp, cancel, rewind, forward}).collect(Collectors.toSet());

        private final Set<Long> ids = new HashSet<>(0);
        private State fallback;
        private int offset;

        @Override
        public void refreshView() {
            try {
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
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);

                switchTo(fallback).refreshView();
            }
        }

        @Override
        public State fallback() {
            return fallback;
        }

        private void setFallback(final State state) {
            this.fallback = state;
        }

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();

            node.put("offset", offset);
            ids.forEach(i -> node.withArray("ids").add(i));
            node.set("fallback", State.stateToJson(fallback));

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || node.size() <= 0) return;

            offset = node.has("offset") ? node.get("offset").asInt() : 0;
            if (node.has("ids"))
                node.get("ids").forEach(j -> ids.add(j.asLong()));
            fallback = node.has("fallback") ? fromMyBackup(node.get("fallback")) : new View();
        }

        @Override
        public boolean isCallbackAppliable(final String callbackData) {
            return appliableCallbacks.contains(callbackData) || callbackData.startsWith(openEntry);
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
                    switchTo(fallback);
                    break;
                default:
                    final TFile dir = fsService.get(getLong(callbackData), user);
                    if (dir == null || !dir.isDir())
                        switchTo(fallback);
                    else {
                        this.dirId = dir.getId();
                        answer = v(LangMap.Names.CD, user, dir.getName());
                    }
                    break;
            }

            return new CallbackAnswer(0, answer);
        }
    }

    public static class MkDir extends State implements RequireInput, OneStep {
        private State recoil;

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();
            if (recoil != null)
                node.set("recoil", State.stateToJson(recoil));

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || !node.has("recoil"))
                return;

            recoil = fromMyBackup(node.get("recoil"));
        }

        @Override
        public boolean isCallbackAppliable(final String callbackData) { return false; }

        @Override
        public CallbackAnswer applyCallback(final String callbackData) { return null; }

        @Override
        public void refreshView() {
            if (recoil != null)
                recoil.refreshView();
        }

        @Override
        public String prompt() {
            return v(LangMap.Names.TYPE_FOLDER, user);
        }

        @Override
        public void accept(final String input) {
            if (isEmpty(input)) return;

            fsService.mkdir(input, dirId, user.getId());
        }

        @Override
        public State recoil() {
            return recoil;
        }

        private void setRecoil(final State state) {
            this.recoil = state;
        }
    }

    public static class MkLabel extends State implements RequireInput, OneStep {
        private State recoil;

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();
            if (recoil != null)
                node.set("recoil", State.stateToJson(recoil));

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || !node.has("recoil"))
                return;

            recoil = fromMyBackup(node.get("recoil"));
        }

        @Override
        public boolean isCallbackAppliable(final String callbackData) { return false; }

        @Override
        public CallbackAnswer applyCallback(final String callbackData) { return null; }

        @Override
        public void refreshView() {
            if (recoil != null)
                recoil.refreshView();
        }

        @Override
        public String prompt() {
            return v(LangMap.Names.TYPE_LABEL, user);
        }

        @Override
        public void accept(final String input) {
            if (isEmpty(input)) return;

            fsService.upload(TFileFactory.label(input, dirId), user);
        }

        @Override
        public State recoil() {
            return recoil;
        }

        private void setRecoil(final State state) {
            this.recoil = state;
        }
    }

    public static class Search extends State implements RequireInput, Fallbackable {
        private static final Logger.ALogger logger = Logger.of(View.class);
        private static final Set<String> appliableCallbacks =
                Arrays.stream(new String[]{searchStateInit, gearStateInit, cancel, rewind, forward, openEntry}).collect(Collectors.toSet());

        private String query = null, body = null;
        private int offset;
        private State fallback;

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();

            node.put("query", query);
            node.put("body", body);
            node.put("offset", offset);
            node.set("fallback", State.stateToJson(fallback));

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || node.size() <= 0)
                return;

            query = node.has("query") ? node.get("query").asText() : null;
            body = node.has("body") ? node.get("body").asText() : null;
            offset = node.has("offset") ? node.get("offset").asInt() : 0;
            fallback = node.has("fallback") ? fromMyBackup(node.get("fallback")) : new View();
        }

        @Override
        public boolean isCallbackAppliable(final String callbackData) {
            return appliableCallbacks.contains(callbackData) || callbackData.startsWith(openEntry);
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
                    sg.setFallback(this);
                    sg.offset = offset;
                    sg.query = query;
                    break;
                case cancel:
                    switchTo(fallback);
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
                    final TFile file = fsService.get(getLong(callbackData), user);
                    if (file == null || file.isLabel())
                        break;
                    if (file.isDir())
                        switchTo(new View()).dirId = file.getId();
                    else {
                        final OpenFile openFile = switchTo(new OpenFile());
                        openFile.itemId = file.getId();
                        openFile.fallback = this;
                    }
                    break;
            }

            return new CallbackAnswer(0, answer);
        }

        @Override
        public void accept(final String input) {
            query = input;

            final int found = fsService.findChildsByName(dirId, input.toLowerCase(), user);
            if (found > 0)
                body = escapeMd(v(LangMap.Names.SEARCHED, user, input, found));
            else {
                tgApi.sendPlainText(v(LangMap.Names.NO_RESULTS, user, input), user.getId(), dialogId -> {
                    user.setLastDialogId(dialogId);
                    userService.updateOpts(user);
                });

                switchTo(fallback);
            }
        }

        @Override
        public String prompt() {
            return v(LangMap.Names.TYPE_LABEL, user);
        }

        @Override
        public State fallback() {
            return fallback;
        }

        private void setFallback(final State state) {
            this.fallback = state;
        }

        @Override
        public void refreshView() {
            try {
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
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public static class SearchGear extends State implements Fallbackable {
        private static final Logger.ALogger logger = Logger.of(SearchGear.class);
        private static final Set<String> appliableCallbacks =
                Arrays.stream(new String[]{inversCheck, renameEntry, move, drop, cancel, rewind, forward}).collect(Collectors.toSet());

        private final Set<Long> selection = new HashSet<>();
        private int offset;
        private State fallback;
        private String query;

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();

            node.put("offset", offset);
            node.put("query", query);
            selection.forEach(i -> node.withArray("selection").add(i));
            node.set("fallback", State.stateToJson(fallback));

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || node.size() <= 0) return;

            query = node.has("query") ? node.get("query").asText() : "";
            offset = node.has("offset") ? node.get("offset").asInt() : 0;
            if (node.has("selection"))
                node.get("selection").forEach(j -> selection.add(j.asLong()));
            fallback = node.has("fallback") ? fromMyBackup(node.get("fallback")) : new View();
        }

        @Override
        public boolean isCallbackAppliable(final String callbackData) {
            return appliableCallbacks.contains(callbackData) || callbackData.startsWith(renameEntry) || callbackData.startsWith(inversCheck);
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
                    moving.setFallback(this);
                    break;
                case drop:
                    if (!selection.isEmpty()) {
                        fsService.rm(selection, user);
                        answer = v(LangMap.Names.DELETED_MANY, user, selection.size());
                        selection.clear();
                    }
                    break;
                case cancel:
                    switchTo(fallback());
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
                    if (itemId <= 0)
                        break;

                    if (callbackData.startsWith(renameEntry)) {
                        final Renaming renaming = switchTo(new Renaming());
                        renaming.setRecoil(this);
                        renaming.itemId = itemId;
                        break;
                    } else // check/uncheck
                        if (selection.remove(itemId))
                            answer = v(LangMap.Names.DESELECTED, user);
                        else if (selection.add(itemId))
                            answer = v(LangMap.Names.SELECTED, user);
            }

            return new CallbackAnswer(0, answer);
        }

        @Override
        public State fallback() {
            return fallback;
        }

        private void setFallback(final State state) {
            this.fallback = state;
        }

        @Override
        public void refreshView() {
            try {
                final List<TFile> scope = fsService.getFound(user);
                final List<InlineButton> upper = new ArrayList<>(0), bottom = new ArrayList<>(0);
                if (!selection.isEmpty()) {
                    if (selection.size() == 1)
                        upper.add(GUI.Buttons.renameButton);

                    upper.add(new InlineButton(Uni.move + "(" + selection.size() + ")", move));
                    upper.add(new InlineButton(Uni.drop + "(" + selection.size() + ")", drop));
                    upper.add(GUI.Buttons.cancelButton);
                }

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
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public static class OpenFile extends State implements Fallbackable {
        private static final Set<String> appliableCallbacks =
                Arrays.stream(new String[]{renameEntry, move, drop, cancel}).collect(Collectors.toSet());

        private long itemId;
        private State fallback;

        @Override
        public void refreshView() {
            gui.makeFileDialog(fsService.get(itemId, user), user.getId(), dialogIdConsumer);
        }

        @Override
        public JsonNode toJson() {
            final ObjectNode node = Json.newObject();

            node.put("item_id", itemId);
            node.set("fallback", State.stateToJson(fallback));

            return node;
        }

        @Override
        public void fromJson(final JsonNode node) {
            if (node == null || node.size() <= 0) return;

            itemId = node.has("item_id") ? node.get("item_id").asLong() : 0;
            fallback = node.has("fallback") ? fromMyBackup(node.get("fallback")) : new View();
        }

        @Override
        public State fallback() {
            return fallback;
        }

        @Override
        public CallbackAnswer applyCallback(final String callbackData) {
            String answer = "";

            switch (callbackData) {
                case renameEntry:
                    final Renaming renaming = switchTo(new Renaming());
                    renaming.setRecoil(fallback());
                    renaming.itemId = itemId;
                    break;
                case move:
                    final Moving moving = switchTo(new Moving());
                    moving.setFallback(fallback());
                    moving.ids.add(itemId);
                    break;
                case drop:
                    fsService.rm(itemId, user);
                    answer = v(LangMap.Names.DELETED, user);
                case cancel:
                    switchTo(fallback());
                    break;
            }

            return new CallbackAnswer(0, answer);
        }

        @Override
        public boolean isCallbackAppliable(final String callbackData) {
            return appliableCallbacks.contains(callbackData);
        }
    }

    public static class MkFile extends State {
        public void accept(final TeleFile file) {
            if (file == null)
                return;

            fsService.upload(TFileFactory.file(file, dirId), user);
            switchTo(new View());
        }

        @Override
        public boolean isCallbackAppliable(final String callbackData) { return false; }

        @Override
        public CallbackAnswer applyCallback(final String callbackData) { return null; }

        @Override
        public void refreshView() { }

        @Override
        public JsonNode toJson() { return null; }

        @Override
        public void fromJson(final JsonNode node) { }
    }

    public interface RequireInput {
        String prompt();

        void accept(String input);
    }

    public interface Fallbackable {
        State fallback();
    }

    public interface OneStep {
        State recoil();
    }
}
