package states;

import model.Callback;
import model.TFile;
import model.User;
import model.telegram.ContentType;
import model.telegram.api.InlineButton;
import services.GUI;
import states.actions.MkDirAction;
import states.actions.MkLabelAction;
import states.actions.SearchAction;
import utils.FlowBox;
import utils.LangMap;
import utils.Strings;

import java.util.List;
import java.util.function.Predicate;

import static utils.LangMap.v;
import static utils.Strings.Callback.rewind;
import static utils.TextUtils.escapeMd;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.05.2020
 * tfs ☭ sweat and blood
 */
public class LsState extends AState {
    public static final String NAME = LsState.class.getSimpleName();

    public LsState() {
        super(NAME);
    }

    @Override
    protected void handleCallback(final Callback cb, final User user, final CallReply reply) {
        switch (cb.type()) {
            case goUp:
                user.current = user.parent == null ? fsService.getRoot(user) : user.parent;
                user.parent = user.current.getParentId() == null ? null : fsService.get(user.current.getParentId(), user);
                reply.reply(LangMap.Value.CD, user.getPath());
                break;
            case rewind:
            case forward:
                user.deltaOffset(cb.type().equals(rewind) ? -10 : 10);
                reply.reply(LangMap.Value.PAGE, (user.getOffset() / 10) + 1);
                break;
            case open:
                final TFile entry = fsService.get(cb.entryId, user);

                if (entry.isDir()) {
                    user.current = entry;
                    user.parent = user.current.getParentId() == null ? null : fsService.get(user.current.getParentId(), user);
                    reply.reply(LangMap.Value.CD, entry.getName());
                } else {
                    user.selection.clear();
                    user.selection.add(entry);
                    doView(ViewFileState.NAME, user);
                    return;
                }
                break;
            case search:
                user.setQuery(null);
                doAction(SearchAction.NAME, user);
                return;
            case mkDir:
                doAction(MkDirAction.NAME, user);
                return;
            case mkLabel:
                doAction(MkLabelAction.NAME, user);
                return;
            case gearLs:
                user.selection.clear();
                doView(GearLsState.NAME, user);
                return;
        }

        handle(user);
    }

    @Override
    protected void handle(final User user) {
        final List<TFile> scope = fsService.list(user);

        final FlowBox box = new FlowBox()
                .md2()
                .body(notNull(user.getPath(), "/"));

        if (scope.isEmpty())
            box.body("\n\n_" + escapeMd(v(LangMap.Value.NO_CONTENT, user)) + "_");

        if (scope.stream().anyMatch(e -> e.getType() == ContentType.LABEL)) {
            box.body("\n\n");
            scope.stream().filter(e -> e.getType() == ContentType.LABEL).forEach(e -> box.body('`' + escapeMd(e.getName()) + "`\n\n"));
        }

        if (user.parent != null)
            box.button(GUI.Buttons.goUpButton);

        if (user.current.isRw()) {
            box.button(GUI.Buttons.mkLabelButton)
                    .button(GUI.Buttons.mkDirButton);
        }
        box.button(GUI.Buttons.searchButton);
        if (user.current.isRw()) box.button(GUI.Buttons.gearButton);

        final Predicate<TFile> viewable = e -> e.getType() != ContentType.LABEL;

        final long count = scope.stream().filter(viewable).count();

        user.newView();

        if (count > 0)
            scope.stream().filter(viewable)
                    .sorted((o1, o2) -> {
                        final int res = Boolean.compare(o2.isDir(), o1.isDir());
                        return res != 0 ? res : o1.getName().compareTo(o2.getName());
                    })
                    .skip(user.getOffset())
                    .limit(10)
                    .forEach(f -> {
                        user.viewAdd(f);
                        box.row().button(new InlineButton((f.isDir() ? Strings.Uni.folder + " " : "") + f.getName(), Strings.Callback.open.toString() + user.viewIdx()));
                    });

        gui.sendBox(box.setListing(user.getOffset() > 0, user.getOffset() + 10 < count), user);
    }
}
