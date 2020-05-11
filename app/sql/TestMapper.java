package sql;

import org.apache.ibatis.annotations.Param;

/**
 * @author Denis Danilin | denis@danilin.name
 * 05.05.2020
 * tfs ☭ sweat and blood
 */
public interface TestMapper {
    void dropFsStruct(@Param("owner") long owner);

    void dropFsTree(@Param("owner") long owner);

    void dropUser(long id);
}
