package states;

import model.Callback;
import model.TFile;
import model.User;
import model.telegram.ContentType;
import model.telegram.api.InlineButton;
import play.Logger;
import services.GUI;
import states.actions.SearchAction;
import utils.FlowBox;
import utils.LangMap;
import utils.Strings;

import java.util.List;

import static utils.LangMap.v;
import static utils.Strings.Callback.rewind;
import static utils.TextUtils.escapeMd;

/**
 * @author Denis Danilin | denis@danilin.name
 * 27.05.2020
 * tfs ☭ sweat and blood
 */
public class SearchLsState extends AState {
    private static final Logger.ALogger logger = Logger.of(SearchLsState.class);
    public static final String NAME = SearchLsState.class.getSimpleName();

    public SearchLsState() {
        super(NAME);
    }

    @Override
    protected void handleCallback(final Callback cb, final User user, final CallReply reply) {
        switch (cb.type()) {
            case cancelCb:
                doView(LsState.NAME, user);
                return;
            case rewind:
            case forward:
                user.deltaSearchOffset(cb.type().equals(rewind) ? -10 : 10);
                reply.reply(LangMap.Value.PAGE, (user.getSearchOffset() / 10) + 1);
                break;
            case search:
                doAction(SearchAction.NAME, user);
                return;
            case gearLs:
                user.selection.clear();
                doView(GearSearchState.NAME, user);
                return;
            default:
                final TFile entry = fsService.get(cb.entryId, user);

                if (entry.isDir() || entry.isLabel()) {
                    user.current = entry.isLabel() ? fsService.get(entry.getParentId(), user) : entry;
                    user.parent = user.current.getParentId() == null ? null : fsService.get(user.current.getParentId(), user);
                    reply.reply(LangMap.Value.CD, user.getPath());
                    doView(LsState.NAME, user);
                    return;
                }

                user.selection.clear();
                user.selection.add(entry);
                doView(ViewFileState.NAME, user);
                break;
        }

        handle(user);
    }

    protected void handle(final User user) {
        try {
            final FlowBox box = new FlowBox()
                    .md2()
                    .body(user.getPath() + "\n")
                    .body(escapeMd(v(user.searchResults.isEmpty() ? LangMap.Value.NO_RESULTS : LangMap.Value.SEARCHED, user, user.getQuery(), user.searchResults.size())));

            final int cpl = user.getPath().length();
            final List<TFile> scope = user.searchResults;

            box.button(GUI.Buttons.searchButton);
            if (!scope.isEmpty())
                box.button(GUI.Buttons.gearButton);
            box.button(GUI.Buttons.cancelButton);

            if (scope.stream().anyMatch(e -> e.getType() == ContentType.LABEL)) {
                box.body("\n\n");
                scope.stream().filter(e -> e.getType() == ContentType.LABEL).forEach(e -> box.body('`' + escapeMd(e.getName()) + "`\n\n"));
            }

            user.newView();
            scope.stream().filter(e -> e.getType() != ContentType.LABEL)
                    .sorted((o1, o2) -> {
                        final int res = Boolean.compare(o2.isDir(), o1.isDir());
                        return res != 0 ? res : o1.getName().compareTo(o2.getName());
                    })
                    .skip(user.getSearchOffset())
                    .limit(10)
                    .forEach(f -> {
                        user.viewAdd(f);
                        box.row().button(new InlineButton((f.isDir() ? Strings.Uni.folder + " " : "") + f.getPath().substring(cpl),
                                Strings.Callback.open.toString() + user.viewIdx()));
                    });

            gui.sendBox(box.setListing(user.getSearchOffset() > 0, user.getSearchOffset() + 10 < scope.stream().filter(e -> e.getType() != ContentType.LABEL).count()), user);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
