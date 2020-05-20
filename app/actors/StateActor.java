package actors;

import actors.protocol.WakeUpNeo;
import akka.actor.AbstractActor;
import akka.actor.Props;
import model.User;
import services.FsService;
import services.GUI;
import services.TgApi;
import services.UserService;
import utils.Strings.Actors;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 19.05.2020
 * tfs ☭ sweat and blood
 */
public abstract class StateActor extends AbstractActor {
    public static final Map<String, Props> props = new HashMap<>();

    static {
        props.put(Actors.View, Props.create(ViewActor.class));
        props.put(Actors.MkLabel, Props.create(MkLabelActor.class));
        props.put(Actors.Search, Props.create(SearchActor.class));
        props.put(Actors.MkDir, Props.create(MkDirActor.class));
        props.put(Actors.Gear, Props.create(GearActor.class));
        props.put(Actors.OpenFile, Props.create(OpenFileActor.class));
        props.put(Actors.Rename, Props.create(RenameActor.class));
        props.put(Actors.SearchGear, Props.create(SearchGearActor.class));
        props.put(Actors.Move, Props.create(MoveActor.class));
    }

    @Inject
    protected TgApi tgApi;

    @Inject
    protected UserService userService;

    @Inject
    protected FsService fsService;

    @Inject
    protected GUI gui;

    protected void switchTo(final Object msg, final String name) {
        getContext().actorOf(props.getOrDefault(name, props.get(Actors.View))).tell(msg, getSelf());
    }

    protected void switchBack(final User user) {
        user.setState(notNull(user.getFallback(), Actors.View));
        saveState(user);

        switchTo(new WakeUpNeo(user), user.getState());
    }

    protected void saveState(final User user) {
        user.setState(getSelf().path().name());
        user.setFallback(user.getState().equals(Actors.View) || !props.containsKey(getSelf().path().parent().name()) ? Actors.View : getSelf().path().parent().name());

        userService.update(user);
    }
}
