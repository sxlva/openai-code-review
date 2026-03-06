package cn.fcr.middleware.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author 傅崇睿
 * @date 2026/03/05 21:07
 * @description ApiTest
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiTest {

    @Test
    public void test() {

        System.out.println(Integer.parseInt("aaaa123"));

    }

}
