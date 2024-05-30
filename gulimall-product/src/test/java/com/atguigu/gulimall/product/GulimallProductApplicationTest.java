package com.atguigu.gulimall.product;

import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = GulimallProductApplication.class)
public class GulimallProductApplicationTest {
    @Autowired
    private BrandService brandService;

    @Test
    public void test1() {
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setName("sony");
        brandEntity.setDescript("sony大法好");
        brandService.save(brandEntity);
    }
}
