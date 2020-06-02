package states;

import model.Callback;
import model.TFile;
import model.User;
import model.telegram.api.InlineButton;
import services.GUI;
import utils.FlowBox;
import utils.LangMap;
import utils.Strings;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static utils.Strings.Callback.goUp;
import static utils.Strings.Callback.rewind;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.05.2020
 * tfs ☭ sweat and blood
 */
public class MoveState extends AState {
    public static final String NAME = MoveState.class.getSimpleName();

    public MoveState() {
        super(NAME);
    }

    @Override
    protected void handleCallback(final Callback cb, final User user, final CallReply reply) {
        switch (cb.type()) {
            case rewind:
            case forward:
                user.deltaOffset(cb.type().equals(rewind) ? -10 : 10);
                reply.reply(LangMap.Value.PAGE, (user.getOffset() / 10) + 1);
                break;
            case put:
                if (!user.current.isRw()) {
                    gui.notify(LangMap.Value.NOT_ALLOWED, user);
                    return;
                }

                final Set<UUID> predictors = fsService.getPredictors(user).stream().map(TFile::getId).collect(Collectors.toSet());
                final AtomicInteger counter = new AtomicInteger(0);
                user.selection.stream().filter(f -> f.isRw() && (!f.isDir() || !predictors.contains(f.getId()))).peek(e -> counter.incrementAndGet()).forEach(f -> f.setParentId(user.current.getId()));
                fsService.updateMetas(user.selection);
                user.selection.clear();
                reply.reply(LangMap.Value.MOVED, counter.get());
                doView(LsState.NAME, user);
                return;
            case cancelCb:
                doView(LsState.NAME, user);
                return;
            case goUp:
            case open:
                user.current = cb.type() == goUp ? user.parent : fsService.get(cb.entryId, user);
                user.parent = user.current.getParentId() == null ? null : fsService.get(user.current.getParentId(), user);
                reply.reply(LangMap.Value.CD, user.getPath());
                break;
            default:
                doView(LsState.NAME, user);
                return;
        }

        handle(user);
    }

    @Override
    protected void handle(final User user) {
        final List<TFile> scope = fsService.listFolders(user);

        final FlowBox box = new FlowBox()
                .md2()
                .body(user.getPath())
                .row();

        if (user.parent != null)
            box.button(GUI.Buttons.goUpButton);

        box.button(GUI.Buttons.putButton)
                .button(GUI.Buttons.cancelButton);

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
                    box.row().button(new InlineButton(Strings.Uni.folder + "  " + f.getName(), Strings.Callback.open.toString() + user.viewIdx()));
                });

        gui.sendBox(box.setListing(user.getOffset() > 0, user.getOffset() + 10 < scope.size()), user);
    }
}
