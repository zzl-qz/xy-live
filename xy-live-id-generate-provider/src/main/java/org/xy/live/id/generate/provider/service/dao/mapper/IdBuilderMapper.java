package org.xy.live.id.generate.provider.service.dao.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.xy.live.id.generate.provider.service.dao.po.IdBuilderPO;

import java.util.List;

@Mapper
public interface IdBuilderMapper extends BaseMapper<IdBuilderPO> {

    @Update("update t_id_generate_config set current_start = next_threshold,next_threshold=current_start+step,version=#{version}+1 where id=#{code}")
    Integer updateNewVersion(@Param("code") Integer code, @Param("version") Integer version);

    @Update("UPDATE t_id_generate_config set next_threshold=#{nextThreshold},current_start=#{currentStart},version=version+1 where id=#{id} and version=#{version}")
    Integer updateCurrentThreshold(@Param("nextThreshold") long nextThreshold, @Param("currentStart") long currentStart, @Param("id") int id, @Param("version") int version);

    /**
     * 查询所有数据
     * @return
     */
    @Select("select * from t_id_generate_config")
    List<IdBuilderPO> selectAll();


}
