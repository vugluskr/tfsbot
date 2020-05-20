package services;

import actors.StateActor;
import actors.protocol.GotCallback;
import actors.protocol.GotInput;
import actors.protocol.WakeUpNeo;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import model.User;
import model.telegram.api.TeleFile;
import play.Logger;
import utils.Strings;
import utils.TFileFactory;

import javax.inject.Inject;

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
    private FsService fsService;

    @Inject
    private ActorSystem actorSystem;

    public void accept(final User user, final TeleFile file, final String input, final String callbackData, final long msgId, final long callbackId) {
        try {
            if (user.getLastDialogId() > 0) {
                tgApi.deleteMessage(user.getLastDialogId(), user.getId());
                user.setLastDialogId(0);
            }

            if (input != null || file != null)
                tgApi.deleteMessage(msgId, user.getId());

            if (notNull(input).equals("/start")) {
                if (user.getLastMessageId() > 0) {
                    tgApi.deleteMessage(user.getLastMessageId(), user.getId());
                    user.setLastMessageId(0);
                }

                user.setState(Strings.Actors.View);
            }

            if (file != null) {
                fsService.upload(TFileFactory.file(file, input, user.getDirId()), user);
                user.setState(Strings.Actors.View);
            }

            actorSystem.actorOf(StateActor.props.get(user.getState()),
                    user.getState()).tell(
                    callbackData != null
                            ? new GotCallback(callbackId, callbackData, user)
                            : !isEmpty(input)
                            ? new GotInput(input, user)
                            : new WakeUpNeo(user),
                    ActorRef.noSender());
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
