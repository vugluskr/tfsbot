package sql;

import model.Share;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

/**
 * @author Denis Danilin | denis@danilin.name
 * 28.05.2020
 * tfs ☭ sweat and blood
 */
public interface ShareMapper {
    List<Share> selectSharesByDir(@Param("entryId") UUID id, @Param("owner") long owner);

    boolean isShareIdAvailable(@Param("id") String id);

    void insertShare(@Param("share") Share share);

    void dropShare(@Param("id") String id, @Param("owner") long owner);

    boolean selectShareExist(@Param("entryId") UUID id, @Param("sharedTo") long sharedTo, @Param("owner") long owner);

    void changeShareRo(@Param("shareId") String shareId, @Param("owner") long owner);

    boolean isAnyShareExist(@Param("entryId") UUID id, @Param("owner") long owner);

    Share selectPublicShare(@Param("id") String id);

    Share selectShare(@Param("id") String id);

    void dropGlobalShareByEntry(@Param("entryId") UUID entryId, @Param("owner") long owner);
}
