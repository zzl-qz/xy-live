package org.xy.live.user.provider.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.xy.live.user.provider.dao.po.UserTagPo;

@Mapper
public interface IUserTagMapper extends BaseMapper<UserTagPo> {

    /**
     * 设置进标签
     * ${fieldName} & #{tag} = 0 表示的意思是这个标签原先是没有的，
     * 也就是说只有当原本是没有这个标签的，才会去进行设置进去的操作
     *
     * @param userId
     * @param fieldName
     * @param tag
     * @return
     */
    @Update("update t_user_tag set ${fieldName} = ${fieldName} | #{tag} where user_id = #{userId} and ${fieldName} & #{tag} = 0")
    int setTag(Long userId, String fieldName, long tag);

    /**
     * 取消标签
     * ${fieldName} & #{tag} = #{tag} 表示的意思是原本就是包含这个标签的，也就是说只有包含这个标签的情况下才能去取消这个标签
     *
     * @param userId
     * @param fieldName
     * @param tag
     * @return
     */
    @Update("update t_user_tag set ${fieldName} = ${fieldName} &~ #{tag} where user_id = #{userId} and ${fieldName} & #{tag} = #{tag}")
    int cancelTag(Long userId, String fieldName, long tag);
}
