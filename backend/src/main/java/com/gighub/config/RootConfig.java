package com.gighub.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;

/**
 * 웹 계층을 제외한 애플리케이션 공통 Bean을 관리하는 Root Context 설정입니다.
 *
 * <p>Service와 공통 Component는 이 Context에서 관리합니다. Controller는 DispatcherServlet이
 * 사용하는 Servlet Context에서만 생성하여 같은 Bean이 중복 등록되지 않게 합니다.</p>
 *
 * <p>TODO: MySQL 환경 규격이 확정되면 DataSource, MyBatis SqlSessionFactory와
 * TransactionManager 설정을 추가합니다.</p>
 */
@Configuration
@ComponentScan(
        basePackages = "com.gighub",
        excludeFilters = {
            @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Controller.class),
            @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Configuration.class)
        }
)
public class RootConfig {
    // 현재는 공통 Bean이 없으므로 Component Scan 경계만 선언합니다.
}

