package sql;

import model.Share;
import org.apache.ibatis.annotations.Param;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author Denis Danilin | denis@danilin.name
 * 28.05.2020
 * tfs ☭ sweat and blood
 */
public interface ShareMapper {
    boolean isIdAvailable(@Param("id") String id);
    void insertShare(@Param("share") Share share);
    void dropShare(@Param("id") String id);
    boolean isShareExists(@Param("entryId") UUID entryId, @Param("sharedTo") long sharedTo, @Param("owner") long owner);
    Share selectPublicShare(@Param("id") String id);
    List<Share> selectEntryGrants(@Param("entryId") UUID entryId, @Param("offset") int offset, @Param("limit") int limit, @Param("owner") long owner);
    void dropEntryLink(@Param("entryId") UUID entryId, @Param("owner") long owner);
    int countEntryGrants(@Param("entryId") UUID entryId);
    Share getEntryLink(@Param("entryId") UUID entryId);
    List<Share> getEntryShares(@Param("entryId") UUID entryId, @Param("owner") long owner);

    void changeGrantRw(@Param("entryId") UUID entryId, @Param("offset") int offset, @Param("owner") long owner);

    List<Share> selectById(@Param("ids") List<String> ids);
    List<Share> getDirectSharesByConsumerId(@Param("userId") long userId);
}
