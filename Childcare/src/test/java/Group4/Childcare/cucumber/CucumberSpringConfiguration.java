package Group4.Childcare.cucumber;

import Group4.Childcare.ChildcareApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Cucumber Spring 配置類
 * 用於整合 Cucumber 與 Spring Boot 測試環境
 */
@CucumberContextConfiguration
@SpringBootTest(
    classes = ChildcareApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class CucumberSpringConfiguration {
    // 此類用於配置 Cucumber 與 Spring 的整合
    // Cucumber 測試需要完整的應用程式上下文，包括資料庫連接
}

