package org.xy.live.user.provider.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.xy.live.user.provider.dao.po.UserPhonePO;

import java.util.List;

@Mapper
public interface IUserPhoneMapper extends BaseMapper<UserPhonePO> {

    /**
     * 插入索引表
     *
     * @param userPhone
     */
    @Insert("INSERT INTO t_user_phone_index (user_phone, user_id) VALUES (#{userPhone}, #{userId})")
    void insertToIndexTable(@Param("userPhone") String userPhone, @Param("userId") Long userId);


    /**
     * 查询所有的用户手机号
     *
     * @return
     */
    @Select("select user_phone from t_user_phone_index")
    List<String> selectAllUserPhonesFromIndex();

    /**
     * 根据用户phone 查询对应userId
     * @param userPhone
     * @return
     */
    @Select("select user_id from t_user_phone_index where user_phone = #{userPhone}")
    Long getUserIdByPhone(String userPhone);

}
