package org.xy.live.user.provider.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.xy.live.user.provider.dao.po.UserPO;

import java.util.List;

@Mapper
public interface IUserMapper extends BaseMapper<UserPO> {


    // 插入索引表
    @Insert("INSERT INTO t_user_index (user_id) VALUES (#{userId})")
    void insertToIndexTable(@Param("userId") Long userId);

    // 查询索引表中所有的用户Id
    @Select("SELECT user_id FROM t_user_index")
    List<Long> selectAllUserIdsFromIndex();


}
