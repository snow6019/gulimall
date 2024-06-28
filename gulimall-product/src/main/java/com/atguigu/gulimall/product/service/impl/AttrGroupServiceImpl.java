package com.atguigu.gulimall.product.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.service.AttrGroupService;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override // 根据分类返回属性分组 // AttrGroupServiceImpl.java // 按关键字或者按id查
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        String key = (String) params.get("key");
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<>();
        // select * from AttrGroup where attr_group_id=key or attr_group_name like key
        if(!StringUtils.isEmpty(key)){
            // 传入consumer
            wrapper.and((obj)->
                    obj.eq("attr_group_id", key).or().like("attr_group_name", key)
            );
        }

        if(catelogId == 0){//  0的话查所有
            // Query可以把map封装为IPage // this.page(IPage,QueryWrapper)
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params),// Query自己封装方法返回Page对象
                    wrapper);
            return new PageUtils(page);
        }else {
            wrapper.eq("catelog_id",catelogId);
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return new PageUtils(page);
        }
    }

}