package com.atguigu.gulimall.product.controller;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class IndexController {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedissonClient redisson;
    @Autowired
    StringRedisTemplate redisTemplate;

    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model) {
        List<CategoryEntity> categoryEntityList = categoryService.getLevelOneCategorys();
        model.addAttribute("catagories", categoryEntityList);
        return "index";
    }

    @GetMapping("/categoryEntityList")
    @ResponseBody
    @Cacheable(value = "categoryEntityList",key = "'getCategoryEntityList'")
    public List<CategoryEntity> getCategoryEntityList() {
        List<CategoryEntity> categoryEntityList = categoryService.getLevelOneCategorys();
        return null;
    }

    /**
     * 查询二级分类和三级分类
     * @return
     */
    @ResponseBody
    @GetMapping("/index/json/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        Map<String, List<Catelog2Vo>> catalogJson = categoryService.getCatalogJson();
        return catalogJson;
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        RLock lock = redisson.getLock("my-lock");

        lock.lock();
        try {
            System.out.println("加锁成功..."+Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (Exception e) {

        } finally {
            lock.unlock();
            System.out.println("释放锁..."+Thread.currentThread().getId());
        }
        return "hello";
    }

    @ResponseBody
    @GetMapping("write")
    public String writeLock() {
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("rw-lock");
        RLock rLock = readWriteLock.writeLock();
        rLock.lock();
        String uuid = "";
        try {
            System.out.println("写锁加锁成功..."+Thread.currentThread().getId());
            uuid = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set("uuid",uuid);
            Thread.sleep(30000);
        } catch (Exception e) {

        } finally {
            rLock.unlock();
            System.out.println("写锁释放成功..."+Thread.currentThread().getId());
        }

        return uuid;
    }

    @ResponseBody
    @GetMapping("read")
    public String readLock() {
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("rw-lock");
        RLock rLock = readWriteLock.readLock();
        rLock.lock();
        String uuid = "";
        try {
            System.out.println("读锁加锁成功..."+Thread.currentThread().getId());
            uuid = redisTemplate.opsForValue().get("uuid");
            Thread.sleep(30000);
        } catch (Exception e) {

        } finally {
            rLock.unlock();
            System.out.println("读锁释放成功..."+Thread.currentThread().getId());
        }
        return uuid;
    }

    @ResponseBody
    @GetMapping("park")
    public String park() throws InterruptedException {
        RSemaphore park = redisson.getSemaphore("park");
        park.acquire();
        return "ok";
    }

    @ResponseBody
    @GetMapping("go")
    public String go() throws InterruptedException {
        RSemaphore park = redisson.getSemaphore("park");
        park.release();
        return "ok";
    }

    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch countDownLatch = redisson.getCountDownLatch("lockDoor");
        countDownLatch.trySetCount(5);
        countDownLatch.await();
        return "放假了...";
    }

    @GetMapping("/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id) throws InterruptedException {
        RCountDownLatch countDownLatch = redisson.getCountDownLatch("lockDoor");
        countDownLatch.countDown();
        return id+"班人都走了...";
    }
}
