package com.gighub.auth.mapper;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gighub.config.RootConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("database")
class WorkplaceCountMapperIntegrationTest {

    private AnnotationConfigApplicationContext context;
    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(RootConfig.class);
        sqlSessionFactory = context.getBean(SqlSessionFactory.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void returnsZeroForAUserWithNoWorkplaces() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            WorkplaceCountMapper mapper = session.getMapper(WorkplaceCountMapper.class);

            int count = mapper.countActiveByOwnerUserId(999999999L);

            assertTrue(count >= 0);
            assertEquals(0, count);
        }
    }
}
