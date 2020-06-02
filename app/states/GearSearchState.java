package states;

import model.Callback;
import model.TFile;
import model.User;
import model.telegram.api.InlineButton;
import services.GUI;
import states.actions.RenameAction;
import utils.FlowBox;
import utils.LangMap;
import utils.Strings;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static utils.LangMap.v;
import static utils.Strings.Callback.*;
import static utils.TextUtils.escapeMd;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.05.2020
 * tfs ☭ sweat and blood
 */
public class GearSearchState extends AState {
    public static final String NAME = GearSearchState.class.getSimpleName();

    public GearSearchState() {
        super(NAME);
    }

    @Override
    protected void handleCallback(final Callback cb, final User user, final CallReply reply) {
        switch (cb.type()) {
            case drop:
                reply.reply(LangMap.Value.DELETED_MANY, fsService.rmSelected(user));
                break;
            case cancelCb:
                user.selection.clear();
                doView(SearchLsState.NAME, user);
                return;
            case rewind:
            case forward:
                user.deltaSearchOffset(cb.type().equals(rewind) ? -10 : 10);
                reply.reply(LangMap.Value.PAGE, (user.getSearchOffset() / 10) + 1);
                break;
            case checkAll:
                reply.reply(LangMap.Value.CHECK_ALL, (user.getOffset() / 10) + 1);
                if (user.selection.size() != user.searchResults.size()) {
                    user.selection.clear();
                    user.selection.addAll(user.searchResults);
                } else
                    user.selection.clear();
                break;
            case rename:
                user.selection.clear();
                user.selection.add(user.searchResults.iterator().next());
                doAction(RenameAction.NAME, user);
                return;
            case move:
                user.selection.clear();
                user.selection.addAll(user.searchResults);
                doView(MoveState.NAME, user);
                return;
            case inversCheck:
                final AtomicBoolean found = new AtomicBoolean(false);
                user.selection.stream().filter(e -> e.getId().equals(cb.entryId)).findAny().ifPresent(e -> {
                    user.selection.remove(e);
                    found.set(true);
                });

                if (!found.get())
                    user.selection.add(fsService.get(cb.entryId, user));
                break;
            default:
                doView(LsState.NAME, user);
                return;
        }

        handle(user);
    }

    @Override
    protected void handle(final User user) {
        final List<TFile> scope = user.searchResults;

        final FlowBox box = new FlowBox()
                .md2()
                .body(user.getPath() + "\n")
                .body("_" + escapeMd(v(LangMap.Value.SEARCHED, user, user.getQuery(), user.searchResults.size())) + "_")
                .row();

        if (!user.selection.isEmpty()) {
            if (user.selection.size() == 1)
                box.button(GUI.Buttons.renameButton);

            box.button(new InlineButton(Strings.Uni.move + "(" + user.selection.size() + ")", move))
                    .button(new InlineButton(Strings.Uni.drop + "(" + user.selection.size() + ")", drop));

            scope.forEach(e -> e.selected = user.selection.stream().anyMatch(f -> f.getId() == e.getId() && f.getOwner() == e.getOwner()));
        }

        box.button(GUI.Buttons.checkAllButton)
                .button(GUI.Buttons.cancelButton);

        user.newView();
        scope.stream()
                .sorted((o1, o2) -> {
                    final int res = Boolean.compare(o2.isDir(), o1.isDir());
                    return res != 0 ? res : o1.getName().compareTo(o2.getName());
                })
                .skip(user.getSearchOffset())
                .limit(10)
                .forEach(f -> {
                    user.viewAdd(f);
                    box.row().button(new InlineButton((f.isShared() ? Strings.Uni.share + " " : "") + (f.isDir() ? Strings.Uni.folder + " " : "") + f.getName() +
                            (f.selected ? " " + Strings.Uni.checked : ""), Strings.Callback.inversCheck.toString() + user.viewIdx()));

                });

        gui.sendBox(box.setListing(user.getSearchOffset() > 0, user.getSearchOffset() + 10 < scope.size()), user);
    }
}
