package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author wangsn
 * @email wangsn2015@gmail.com
 * @date 2024-05-28 17:20:14
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
