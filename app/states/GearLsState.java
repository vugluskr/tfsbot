package states;

import model.Callback;
import model.TFile;
import model.User;
import model.telegram.api.InlineButton;
import services.GUI;
import states.actions.RenameAction;
import states.actions.SearchAction;
import utils.FlowBox;
import utils.LangMap;
import utils.Strings;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static utils.LangMap.v;
import static utils.Strings.Callback.*;
import static utils.TextUtils.escapeMd;
import static utils.TextUtils.isEmpty;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.05.2020
 * tfs ☭ sweat and blood
 */

public class GearLsState extends AState {
    public static String NAME = GearLsState.class.getSimpleName();

    public GearLsState() {
        super(NAME);
    }

    @Override
    protected void handleCallback(final Callback cb, final User user, final CallReply reply) {
        switch (cb.type()) {
            case cancelCb:
                user.getSelection().clear();
                doView(LsState.NAME, user);
                return;
            case rewind:
            case forward:
                user.deltaOffset(cb.type().equals(rewind) ? -10 : 10);
                reply.reply(LangMap.Value.PAGE, (user.getOffset() / 10) + 1);
                break;
            case drop:
                reply.reply(LangMap.Value.DELETED_MANY, fsService.rmSelected(user));
                break;
            case checkAll:
                reply.reply(LangMap.Value.CHECK_ALL);
                final List<TFile> list = fsService.list(user);

                if (user.selection.isEmpty())
                    user.selection.addAll(list);
                else {
                    final boolean refill = user.selection.size() == list.size();

                    user.selection.clear();

                    if (!refill)
                        user.selection.addAll(list);
                }

                break;
            case inversCheck:
                final AtomicBoolean wasSelected = new AtomicBoolean(false);
                user.selection.stream().filter(e -> e.getId() == cb.entryId).findAny().ifPresent(tFile -> {
                    user.selection.remove(tFile);
                    wasSelected.set(true);
                });

                if (!wasSelected.get())
                    user.selection.add(fsService.get(cb.entryId, user));
                break;
            case search:
                doAction(SearchAction.NAME, user);
                return;
            case rename:
                doAction(RenameAction.NAME, user);
                return;
            case move:
                doView(MoveState.NAME, user);
                return;
            case share:
                doView(ShareViewState.NAME, user);
                return;
            default:
                doView(LsState.NAME, user); // мущина, в очередь
                return;
        }

        handle(user);
    }

    @Override
    protected void handle(final User user) {
        final List<TFile> scope = fsService.list(user);
        final FlowBox box = new FlowBox()
                .md2()
                .body(user.getPath());
        if (scope.isEmpty())
            box.body("\n\n_" + escapeMd(v(LangMap.Value.NO_CONTENT, user)) + "_");

        box.row();
        if (!isEmpty(user.selection)) {
            if (user.selection.size() == 1) {
                if (user.selection.get(0).isSharable()) box.button(GUI.Buttons.shareButton);
                box.button(GUI.Buttons.renameButton);
            }

            if (user.hasMovable()) box.button(new InlineButton(Strings.Uni.move + "(" + user.selection.size() + ")", move));
            box.button(new InlineButton(Strings.Uni.drop + "(" + user.selection.size() + ")", drop))
                    .button(GUI.Buttons.checkAllButton);

            scope.forEach(f -> f.selected = user.selection.stream().anyMatch(s -> s.getOwner() == f.getOwner() && s.getId() == f.getId()));
        }
        box.button(GUI.Buttons.cancelButton);

        user.newView();
        scope.stream()
                .sorted((o1, o2) -> {
                    final int res = Boolean.compare(o2.isDir(), o1.isDir());
                    return res != 0 ? res : o1.getName().compareTo(o2.getName());
                })
                .skip(user.getOffset())
                .limit(10)
                .forEach(f -> {
                    user.viewAdd(f);
                    box.row().button(new InlineButton((f.isShared() ? Strings.Uni.share + " " : "") + (f.isDir() ? Strings.Uni.folder + " " : "") + f.getName() + (f.selected ?
                            " " + Strings.Uni.checked : ""), Strings.Callback.inversCheck.toString() + user.viewIdx()));

                });

        gui.sendBox(box.setListing(user.getOffset() > 0, scope.size() > user.getOffset() + 10), user);
    }
}
