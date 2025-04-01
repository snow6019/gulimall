package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    @Resource
    private CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    RedissonClient redisson;

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
                .map(x -> {
                    x.setChildren(setChildRen(x, allList));
                    return x;
                }).sorted((x1, x2) -> (Objects.nonNull(x1.getSort()) ? x1.getSort() : 0) - (Objects.nonNull(x2.getSort()) ? x2.getSort() : 0))
                .collect(Collectors.toList());
        return collect;
    }

    @Override // CategoryServiceImpl
    public Long[] findCateLogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        paths = findParentPath(catelogId, paths);
        // 收集的时候是顺序 前端是逆序显示的 所以用集合工具类给它逆序一下
        Collections.reverse(paths);
        return paths.toArray(new Long[paths.size()]);
    }

    @CacheEvict(value = "categoryEntityList",key = "'getCategoryEntityList'")
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    @Override
    public List<CategoryEntity> getLevelOneCategorys() {
        System.out.println("getLevelOneCategorys...");
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0L));
    }

    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (StringUtils.isBlank(catalogJson)) {
            log.debug("未在缓存中查询到数据，进入数据库查询数据...");
            Map<String, List<Catelog2Vo>> catalogJsonFormDB = getCatalogJsonFormDBWithRedisLock();
//            String jsonString = JSON.toJSONString(catalogJsonFormDB);
//            redisTemplate.opsForValue().set("catalogJson",jsonString,1, TimeUnit.DAYS);
            return catalogJsonFormDB;
        }
        log.debug("在缓存中查询到数据...");
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
        return result;
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonFormDBWithRedissonLock() {
        RLock lock = redisson.getLock("catalogJson-lock");
        lock.lock();
        Map<String, List<Catelog2Vo>> dataFromDB;
        try {
            dataFromDB = getDataFromDB();
        } finally {
            lock.unlock();
        }
        return dataFromDB;
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonFormDBWithRedisLock() {
        //占分布式锁
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 30, TimeUnit.SECONDS);
        if (lock) {
            log.debug("获取分布式锁成功");
            //加锁成功
            /**
             *
             * 问题一：程序执行到这步getDataFromDB()方法出现异常 导致程序没有正常解锁
             * 或者程序执行完getDataFromDB()方法后机器突然宕机导致程序没有正常解锁
             * 解决方法：设置过期时间
             * 问题二：程序执行到redisTemplate.expire("lock", 30, TimeUnit.SECONDS);前突然宕机
             * 解决方法：保证加锁设置过期时间是一个原子操作
             * 问题三：业务执行超时，此时锁已经过期超时，此时已经有其他用户进行加锁操作，如果这个时候
             * 进行删除操作就会将其他用户的锁给删除掉，从而导致更多的用户拥进来加锁导致程序崩溃
             */
//            redisTemplate.expire("lock", 30, TimeUnit.SECONDS);
//            String lock1 = redisTemplate.opsForValue().get("lock");
//            if (uuid.equals(lock1)) {
//                redisTemplate.delete("lock");
//            }
            Map<String, List<Catelog2Vo>> dataFromDB;
            try {
                dataFromDB = getDataFromDB();
            } finally {
                // 删除也必须是原子操作 Lua脚本操作 删除成功返回1 否则返回0
                String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                // 原子删锁
                redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList("lock"), uuid);
            }
            return dataFromDB;
        } else {
            log.debug("加锁失败...等待重试");
            //加锁失败...重试 自旋
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatalogJsonFormDBWithRedisLock();
        }
    }

    private Map<String, List<Catelog2Vo>> getDataFromDB() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (StringUtils.isNotBlank(catalogJson)) {
            //如果缓存不为空直接返回
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }
        log.debug("查询了数据库...");
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        // 查询所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
        // 封装数据
        Map<String, List<Catelog2Vo>> listMap = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 每一个一级分类，查到这个一级分类的二级分类
            List<CategoryEntity> level2Catelog = getParent_cid(selectList, v.getCatId());
            // 封装上面的结果集
            List<Catelog2Vo> catelog2Vos = null;
            if (level2Catelog != null) {
                catelog2Vos = level2Catelog.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());

                    // 找到当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                            // 封装成指定格式
                            Catelog2Vo.catelog3Vo catelog3Vo = new Catelog2Vo.catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));

        String jsonString = JSON.toJSONString(listMap);
        redisTemplate.opsForValue().set("catalogJson", jsonString, 1, TimeUnit.DAYS);
        return listMap;
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonFormDBWithLocalLock() {
        synchronized (this) {
            return getDataFromDB();
        }
    }

    // 抽取方法
    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
        return collect;
    }

    /**
     * 递归收集所有父节点
     */
    private List<Long> findParentPath(Long catlogId, List<Long> paths) {
        // 1、收集当前节点id
        paths.add(catlogId);
        CategoryEntity byId = this.getById(catlogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

    private List<CategoryEntity> setChildRen(CategoryEntity categoryEntity, List<CategoryEntity> allList) {
        return allList.stream().filter(x -> Objects.equals(x.getParentCid(), categoryEntity.getCatId()))
                .map(x -> {
                    x.setChildren(setChildRen(x, allList));
                    return x;
                }).sorted((x1, x2) -> (Objects.nonNull(x1.getSort()) ? x1.getSort() : 0) - (Objects.nonNull(x2.getSort()) ? x2.getSort() : 0))
                .collect(Collectors.toList());
    }
}