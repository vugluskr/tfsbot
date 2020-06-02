package services;

import model.Callback;
import model.Share;
import model.TFile;
import model.User;
import model.telegram.api.TeleFile;
import play.Logger;
import states.*;
import states.actions.*;
import utils.LangMap;
import utils.TFileFactory;

import javax.inject.Inject;
import java.nio.file.Paths;

import static utils.TextUtils.isEmpty;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 16.05.2020
 * tfs ☭ sweat and blood
 */
public class HeadQuarters {
    private static final Logger.ALogger logger = Logger.of(HeadQuarters.class);

    @Inject
    private TgApi tgApi;

    @Inject
    private TfsService fsService;

    @Inject
    private ViewFileState viewEntryState;
    @Inject
    private GearLsState gearLsState;
    @Inject
    private LsState lsState;
    @Inject
    private MoveState movingState;
    @Inject
    private ShareViewState viewSharesState;
    @Inject
    private SearchLsState searchedState;
    @Inject
    private GearSearchState searchedGearState;

    @Inject
    private MkDirAction mkDirAction;
    @Inject
    private MkLabelAction mkLabelAction;
    @Inject
    private RenameAction renameAction;
    @Inject
    private SearchAction searchAction;
    @Inject
    private MkPubLinkAction pubLinkAction;
    @Inject
    private MkGrantAction grantAction;

    public void accept(final User user, final TeleFile file, String input, final Callback callback, final long msgId) {
        try {
            if (callback != null && callback.idx != -1 && !isEmpty(user.view))
                try {
                    if (user.view.get(0) instanceof TFile)
                        callback.entryId = ((TFile) user.view.get(callback.idx)).getId();
                    else if (user.view.get(0) instanceof Share)
                        callback.shareId = ((Share) user.view.get(0)).getId();
                } catch (final Exception ignore) { }

            if (user.getLastDialogId() > 0) {
                tgApi.deleteMessage(user.getLastDialogId(), user.getId());
                user.setLastDialogId(0);
            }

            if (!isEmpty(input) || file != null)
                tgApi.deleteMessage(msgId, user.getId());

            if (notNull(input).equals("/start")) {
                if (user.getLastMessageId() > 0) {
                    tgApi.deleteMessage(user.getLastMessageId(), user.getId());
                    user.setLastMessageId(0);
                }
                user.current = fsService.getRoot(user);
                user.setFallback(null);
                user.setState(LsState.NAME);
                lsState.doView(user, this::resolveState, this::resolveAction);
                return;
            } else if (notNull(input).startsWith("/start shared-")) {
                final String id = notNull(input).substring(14);

                final Share share;
                if (!id.isEmpty() && (share = fsService.getPublicShare(id)) != null && share.getOwner() != user.getId()) {
                    user.setOffset(0);
                    user.setFallback(null); // todo переход в шару

                    fsService.applyShareByLink(share, user);
                }

                input = null;
                user.setState(LsState.NAME);
            }

            final AInputAction preAction = isEmpty(user.getFallback()) ? null : resolveAction(user.getFallback());

            if (preAction != null && preAction.interceptAny() && preAction.intercepted(file, input, callback, user)) {
                if (callback != null)
                    tgApi.sendCallbackAnswer(LangMap.Value.None, callback.id, user);
            } else {
                if (file != null) {
                    fsService.mk(TFileFactory.file(file, input, user.current.getId(), user.getId()));
                    user.setState(LsState.NAME);
                } else if (callback != null)
                    try {
                        resolveState(user.getState()).onCallback(callback, user, this::resolveState, this::resolveAction);
                    } catch (final Exception e) {
                        logger.error("Error on callback: " + e.getMessage(), e);
                        if (user.getLastMessageId() > 0) {
                            try { tgApi.deleteMessage(user.getLastMessageId(), user.getId()); } catch (final Exception ignore) { }
                            user.setLastMessageId(0);
                        }
                        resolveState(user.getState()).doView(user, this::resolveState, this::resolveAction);
                    }
                else if (input != null && preAction != null) {
                    preAction.onInput(input, user);

                    if (user.skipTail)
                        return;

                    resolveState(user.getState()).doView(user, this::resolveState, this::resolveAction);
                } else
                    resolveState(user.getState()).doView(user, this::resolveState, this::resolveAction);
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends AInputAction> T resolveAction(final String name) {
        if (name == null)
            return null;

        if (name.equals(MkDirAction.NAME)) return (T) mkDirAction;
        if (name.equals(MkLabelAction.NAME)) return (T) mkLabelAction;
        if (name.equals(RenameAction.NAME)) return (T) renameAction;
        if (name.equals(SearchAction.NAME)) return (T) searchAction;
        if (name.equals(MkPubLinkAction.NAME)) return (T) pubLinkAction;
        if (name.equals(MkGrantAction.NAME)) return (T) grantAction;

        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends AState> T resolveState(final String path) {
        final String name = notNull(Paths.get(path).getFileName().toString(), LsState.NAME);

        if (name.equals(LsState.NAME)) return (T) lsState;
        if (name.equals(ViewFileState.NAME)) return (T) viewEntryState;
        if (name.equals(GearLsState.NAME)) return (T) gearLsState;
        if (name.equals(ShareViewState.NAME)) return (T) viewSharesState;
        if (name.equals(MoveState.NAME)) return (T) movingState;
        if (name.equals(SearchLsState.NAME)) return (T) searchedState;
        if (name.equals(GearSearchState.NAME)) return (T) searchedGearState;

        return (T) lsState;
    }
}
