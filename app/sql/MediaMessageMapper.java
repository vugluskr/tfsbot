package sql;

import model.MediaMessage;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MediaMessageMapper {

    void insertMessageId(@Param("messageId") long messageId,
                         @Param("user_id") long user_id);

    void deleteMessages(@Param("dateTime") LocalDateTime dateTime);

    void deleteMessageById(@Param("messageId") long messageId,
                           @Param("userId") long userId);

    List<MediaMessage> selectMessagesByTime(@Param("dateTime") LocalDateTime dateTime);
}
