package services;

import model.TFile;
import model.User;
import sql.UserMapper;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 02.06.2020
 * tfs ☭ sweat and blood
 */
public class UserService {
    @Inject
    private TfsService tfsService;

    @Inject
    private UserMapper userMapper;

    public User resolveUser(final User preUser) {
        User user = userMapper.getUser(preUser.getId());

        if (user == null) {
            tfsService.initUserTables(preUser.getId());
            userMapper.insertUser(preUser.getId(), tfsService.findRoot(preUser.getId()).getId());

            user = userMapper.getUser(preUser.getId());
        }

        user.lang = notNull(preUser.lang, "en");
        user.name = preUser.name;
        return user;
    }

    public User update(final User user) {
        userMapper.updateUser(user.getCurrentDirId(), user.getQuery(), user.getViewOffset(), user.getOptions(), user.getId());

        return user;
    }

    public User entrySelectedSolo(final TFile entry, final User user) {
        user.selection.clear();
        userMapper.resetSelection(user.getId());
        return entrySelected(entry, user);
    }

    public User entryInversSelection(final TFile entry, final User user) {
        if (user.selection.add(entry.getId()))
            userMapper.selectEntry(entry.getId(), user.getId());
        else if (user.selection.remove(entry.getId()))
            userMapper.deselectEntry(entry.getId(), user.getId());

        return user;
    }

    public User entrySelected(final TFile entry, final User user) {
        if (user.selection.add(entry.getId()))
            userMapper.selectEntry(entry.getId(), user.getId());

        return user;
    }

    public User entryDeselected(final TFile entry, final User user) {
        if (user.selection.remove(entry.getId()))
            userMapper.deselectEntry(entry.getId(), user.getId());

        return user;
    }

    public User dirChange(final UUID newDir, final User user) {
        user.setCurrentDirId(newDir);
        user.setViewOffset(0);
        user.selection.clear();
        userMapper.resetSelection(user.getId());
        return update(user);
    }

    public User pageUp(final User user) {
        user.deltaSearchOffset(10);
        return update(user);
    }

    public User pageDown(final User user) {
        user.deltaSearchOffset(-10);
        return update(user);
    }

    public User searched(final String query, final User user) {
        user.setQuery(query);
        user.setViewOffset(0);
        user.selection.clear();
        user.setSearching();
        userMapper.resetSelection(user.getId());
        return update(user);
    }

    public void resetSelection(final User user) {
        user.selection.clear();
        userMapper.resetSelection(user.getId());
    }

    public void bulkSelection(final List<TFile> list, final User user) {
        if (user.selection.isEmpty() && list.isEmpty())
            return;

        userMapper.resetSelection(user.getId());

        if (list.size() == user.selection.size())
            user.selection.clear();
        else {
            user.selection.addAll(list.stream().map(TFile::getId).collect(Collectors.toSet()));
            userMapper.selectEntries(user.selection, user.getId());
        }
    }
}
