package states;

import com.typesafe.config.Config;
import model.Callback;
import model.Share;
import model.TFile;
import model.User;
import model.telegram.api.InlineButton;
import services.GUI;
import states.actions.MkGrantAction;
import states.actions.MkPubLinkAction;
import utils.FlowBox;
import utils.LangMap;
import utils.Strings;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static utils.LangMap.v;
import static utils.TextUtils.escapeMd;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.05.2020
 * tfs ☭ sweat and blood
 */
public class ShareViewState extends AState {
    public static final String NAME = ShareViewState.class.getSimpleName();

    @Inject
    private Config config;

    public ShareViewState() {
        super(NAME);
    }

    @Override
    protected void handleCallback(final Callback cb, final User user, final CallReply reply) {
        switch (cb.type()) {
            case mkGrant:
                doAction(MkGrantAction.NAME, user);
                return;
            case mkLink:
                doAction(MkPubLinkAction.NAME, user);
                return;
            case gearLs:
                doView(GearLsState.NAME, user);
                return;
            case changeRo:
                fsService.changeShareRo(cb.shareId, user);
                break;
            case drop:
                fsService.dropShare(cb.shareId, user);

                if (fsService.noSharesExist(user.selection.get(0).getId(), user)) {
                    user.selection.get(0).setUnshared();
                    fsService.updateMeta(user.selection.get(0), user);
                }

                break;
            default:
                doView(LsState.NAME, user);
                return;
        }

        handle(user);
    }

    @Override
    protected void handle(final User user) {
        final TFile dir = user.selection.get(0);
        final List<Share> scope = dir.isShared() ? fsService.listShares(dir.getId(), user) : Collections.emptyList();
        final Share glob = scope.stream().filter(s -> s.getSharedTo() == 0).findAny().orElse(null);
        final long countPers = scope.stream().filter(s -> s.getSharedTo() > 0).count();

        final FlowBox box = new FlowBox()
                .md2()
                .body("*" + escapeMd(dir.getPath()) + "*\n\n")
                .body(Strings.Uni.Link + ": _" +
                        escapeMd((glob != null
                                ? "https://t.me/" + config.getString("service.bot.nick") + "?start=shared-" + glob.getId()
                                : v(LangMap.Value.NO_GLOBAL_LINK, user)
                        )) + "_\n");

        if (countPers <= 0)
            box.body(Strings.Uni.People + ": _" + escapeMd(v(LangMap.Value.NO_PERSONAL_GRANTS, user)) + "_");

        box.button(GUI.Buttons.mkLinkButton)
                .button(GUI.Buttons.mkGrantButton)
                .button(GUI.Buttons.cancelButton);

        user.newView();
        scope.stream()
                .sorted(Comparator.comparing(Share::getName))
                .forEach(s -> {
                    user.viewAdd(s);
                    box.row()
                            .button(new InlineButton(v(s.isReadWrite() ? LangMap.Value.SHARE_ACCESS : LangMap.Value.SHARE_ACCESS_RO, user, s.getName()),
                                    Strings.Callback.changeRo.toString() + user.viewIdx()))
                            .button(new InlineButton(Strings.Uni.drop, Strings.Callback.drop.toString() + user.viewIdx()));
                });

        gui.sendBox(box, user);
    }
}
