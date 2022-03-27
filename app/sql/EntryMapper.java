package sql;

import model.TFile;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

/**
 * @author Denis Danilin | denis@danilin.name
 * 24.06.2020
 * tfs ☭ sweat and blood
 */
public interface EntryMapper {
    int countSearch(@Param("query") String query, @Param("dirId") UUID dirId, @Param("v1") String v1);

    int countDirLs(@Param("dirId") UUID dirId, @Param("v1") String v1);

    int countDirGear(@Param("dirId") UUID dirId, @Param("v1") String v1);

    List<TFile> lsDirContent(@Param("dirId") UUID dirId, @Param("offset") int offset, @Param("limit") int limit, @Param("v1") String v1, @Param("v2") String v2);

    List<TFile> gearDirContent(@Param("dirId") UUID dirId, @Param("offset") int offset, @Param("limit") int limit, @Param("v1") String v1, @Param("v2") String v2);

    List<String> lsDirLabels(@Param("dirId") UUID dirId, @Param("v1") String v1);

    List<TFile> searchContent(@Param("query") String query, @Param("dirId") UUID dirId, @Param("offset") int offset, @Param("limit") int limit, @Param("v1") String v1,
                              @Param("v2") String v2);

    TFile getEntry(@Param("id") UUID id, @Param("v1") String v1, @Param("v2") String v2);

    TFile findEntry(@Param("id") UUID id, @Param("name") String name, @Param("v1") String v1, @Param("v2") String v2);

    void rmList(@Param("uuids") List<UUID> uuids, @Param("t") String t);

    List<TFile> getTree(@Param("id") UUID id, @Param("v1") String v1);

    TFile getParent(@Param("id") UUID id, @Param("v1") String v1, @Param("v2") String v2);
}
