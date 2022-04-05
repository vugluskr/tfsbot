package services;

import com.google.inject.ImplementedBy;
import model.Share;
import model.TFile;
import model.user.TgUser;
import model.user.UDbData;
import services.impl.DataStoreImpl;

import java.util.List;
import java.util.UUID;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 15:43
 * tfs ☭ sweat and blood
 */
@ImplementedBy(DataStoreImpl.class)
public interface DataStore {
    int countFolder(UUID folderId, long userId);

    List<String> listFolderLabels(UUID folderId, long userId);

    List<TFile> listFolder(UUID folderId, int offset, int limit, long userId);

    TFile getEntry(UUID id, long userId);

    boolean isPasswordOk(UUID entryId, String password);

    List<TFile> listFolderLabelsAsFiles(UUID folderId, long userId, int offset, int limit);

    int countFolderLabels(UUID folderId, long userId);

    void reinitUserTables(long userId);

    TFile mk(TFile entry);

    boolean isEntryMissed(UUID folderId, String name, TgUser user);

    void updateEntry(TFile entry);

    Share getPublicShare(String id);

    void rm(UUID entryId, TgUser user);

    void rm(UUID entryId, TgUser user, boolean selfIncluded);

    void lockEntry(TFile entry, String salt, String password);

    void unlockEntry(TFile entry);

    int countSearch(UUID folderId, String query, long userId);

    List<TFile> searchFolder(UUID folderId, String query, int offset, int limit, long userId);

    int countEntryGrants(UUID entryId);

    void changeEntryGrantRw(UUID entryId, int idx, int offset, long userId);

    Share getEntryLink(UUID entryId);

    List<Share> selectEntryGrants(UUID entryId, int offset, int limit, long userId);

    String getBotName();

    TFile getSingleFolderEntry(UUID dirId, int offset, long userId);

    TFile getSingleFolderLabel(UUID dirId, int offset, long userId);

    void dropEntryLink(UUID entryId, long userId);

    void makeEntryLink(UUID entryId, TgUser user);

    void dropEntryGrant(UUID entryId, Share share);

    boolean entryNotGrantedTo(UUID entryId, long targetUserId, long ownerUserId);

    void entryGrantTo(UUID entryId, TgUser target, TgUser owner);

    UDbData getUser(long id);

    UUID initUserTables(long userId);

    void insertUser(UDbData data);

    void updateUser(UDbData u);
}
