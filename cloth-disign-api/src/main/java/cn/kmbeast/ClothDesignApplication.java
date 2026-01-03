package cn.kmbeast;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * 项目启动类
 */
@MapperScan("cn.kmbeast.mapper")
@ServletComponentScan("cn.kmbeast.servlet")
@SpringBootApplication
public class ClothDesignApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClothDesignApplication.class, args);
    }
}
