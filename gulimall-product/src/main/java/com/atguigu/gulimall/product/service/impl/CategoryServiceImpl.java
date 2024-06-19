package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> treeList() {
        List<CategoryEntity> allList = this.list();
        List<CategoryEntity> collect = allList.stream().filter(x -> Objects.equals(0L, x.getParentCid()))
                .map(x -> {x.setChildren(setChildRen(x, allList));
                return x;}).sorted((x1,x2)-> (Objects.nonNull(x1.getSort())? x1.getSort():0) - (Objects.nonNull(x2.getSort())?x2.getSort():0))
                .collect(Collectors.toList());
        return collect;
    }

    private List<CategoryEntity> setChildRen(CategoryEntity categoryEntity, List<CategoryEntity> allList) {
        return allList.stream().filter(x -> Objects.equals(x.getParentCid(), categoryEntity.getCatId()))
                .map(x -> {x.setChildren(setChildRen(x, allList));
                return x;}).sorted((x1,x2)-> (Objects.nonNull(x1.getSort())? x1.getSort():0) - (Objects.nonNull(x2.getSort())?x2.getSort():0))
                .collect(Collectors.toList());
    }
}