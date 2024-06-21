package com.atguigu.gulimall.thirdparty;

import com.aliyun.oss.OSSClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallThirdPartyApplicationTest {
    @Autowired
    private OSSClient ossClient;

    @Test
    public void testAliyunUpload() throws FileNotFoundException {

        InputStream inputStream = new FileInputStream("C:\\Users\\Administrator\\Desktop\\456.png");
        ossClient.putObject("gulimall-wangsn", "haha.png", inputStream);
        ossClient.shutdown();

        System.out.printf("上传完成。。。");
    }

}
