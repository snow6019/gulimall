package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author wangsn
 * @email wangsn2015@gmail.com
 * @date 2024-05-30 15:40:46
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
