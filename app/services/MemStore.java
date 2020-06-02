package services;

import akka.actor.ActorSystem;
import model.User;
import model.telegram.api.ContactRef;
import play.Logger;
import play.libs.Json;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.FiniteDuration;
import sql.UserMapper;
import states.LsState;
import utils.TFileFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static utils.TextUtils.isEmpty;
import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 29.05.2020
 * tfs ☭ sweat and blood
 */
@Singleton
public class MemStore {
    private static final Logger.ALogger logger = Logger.of(MemStore.class);
    private static final long periodSeconds = 15, expireThresholdMinutes = 5;
    private final Map<Long, User> userMap;
    private final Map<Long, AtomicLong> hitMap;
    private final Set<Long> expiredProcessing;
    private final List<CompletableFuture<User>> waiters;

    @Inject
    private UserMapper mapper;

    @Inject
    private TfsService tfsService;

    @Inject
    public MemStore(final ActorSystem system, final ExecutionContext ec) {
        userMap = new ConcurrentHashMap<>(0);
        hitMap = new ConcurrentHashMap<>(0);
        expiredProcessing = ConcurrentHashMap.newKeySet();
        waiters = new ArrayList<>(0);

        system.scheduler().schedule(new FiniteDuration(periodSeconds, TimeUnit.SECONDS), new FiniteDuration(periodSeconds, TimeUnit.SECONDS), this::cleanUp, ec);
    }

    private void cleanUp() {
        final long threshold = System.currentTimeMillis() - (expireThresholdMinutes * 60000L);

        expiredProcessing.clear();
        expiredProcessing.addAll(hitMap.entrySet().stream().filter(e -> e.getValue().get() < threshold).map(Map.Entry::getKey).collect(Collectors.toList()));

        for (final long id : expiredProcessing)
            if (hitMap.get(id).get() < threshold) {
                final User user = userMap.remove(id);
                mapper.update(user.getId(), Json.toJson(user).toString());
                hitMap.remove(id);
            }

        expiredProcessing.clear();
        waiters.forEach(w -> w.complete(null));
    }

    public CompletionStage<User> getUser(final ContactRef cr) {
        User user;
        if ((user = userMap.get(cr.getId())) == null) {
            if (expiredProcessing.contains(cr.getId())) {
                final CompletableFuture<User> waiter = new CompletableFuture<>();
                waiters.add(waiter);
                return waiter.thenCompose(any -> getUser(cr));
            }

            user = findUser(cr);
            userMap.put(user.getId(), user);
            hitMap.put(user.getId(), new AtomicLong(System.currentTimeMillis()));
        } else
            hitMap.get(user.getId()).set(System.currentTimeMillis());

        return CompletableFuture.completedFuture(user);
    }

    private User findUser(final ContactRef cr) {
        final String db = mapper.selectUser(cr.getId());
        final User user;

        if (db == null) {
            final boolean ru = notNull(cr.getLanguageCode()).equalsIgnoreCase("ru");
            tfsService.initUser(cr.getId());

            user = new User();
            user.setId(cr.getId());
            user.setLang(notNull(cr.getLanguageCode(), "en"));
            user.setState(LsState.NAME);
            user.current = tfsService.getRoot(user);
            mapper.insertUser(user.getId(), Json.toJson(user).toString());

            final UUID rootId = tfsService.getRoot(user).getId();

            tfsService.mk(TFileFactory.dir(ru ? "Документы" : "Documents", rootId, user.getId()));
            tfsService.mk(TFileFactory.dir(ru ? "Фото" : "Photos", rootId, user.getId()));
            tfsService.mk(TFileFactory.label(ru ? "Пример заметки" : "Example note", tfsService.mk(TFileFactory.dir(ru ? "Заметки" : "Notes", rootId, user.getId())).getId(),
                    user.getId()));
            logger.info("New user: " + cr);
        } else {
            if (!notNull(db).startsWith("{")) { // old timer
                tfsService.validateFs(cr.getId());
                try {
                    user = mapper.selectUserOldWay(cr.getId());
                    mapper.update(user.getId(), Json.toJson(user).toString());
                } catch (final Exception e) {
                    logger.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            } else
                try {
                    user = Json.fromJson(Json.parse(db), User.class);
                } catch (final Exception e) {
                    logger.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }

            if (isEmpty(user.getLang()))
                user.setLang(notNull(cr.getLanguageCode(), "ru"));
        }

        user.setName(cr.name());
        if (user.current == null)
            user.current = tfsService.getRoot(user);

        return user;
    }

}
