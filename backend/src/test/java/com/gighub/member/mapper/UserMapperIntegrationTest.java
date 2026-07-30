package com.gighub.member.mapper;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gighub.config.RootConfig;
import com.gighub.member.domain.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("database")
class UserMapperIntegrationTest {

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
    void insertsThenCountsByLoginIdAndEmail() {
        String loginId = "mapper" + System.currentTimeMillis() % 100000;
        String email = loginId + "@example.com";

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);

            assertEquals(0, mapper.countByLoginId(loginId));
            assertEquals(0, mapper.countByEmail(email));

            User user = User.builder()
                    .loginId(loginId)
                    .email(email)
                    .passwordHash("{bcrypt}dummy")
                    .name("매퍼 테스트")
                    .phone(null)
                    .role("WORKER")
                    .build();

            int inserted = mapper.insert(user);

            assertEquals(1, inserted);
            assertNotNull(user.getId());
            assertEquals(1, mapper.countByLoginId(loginId));
            assertEquals(1, mapper.countByEmail(email));

            User found = mapper.findByLoginId(loginId);
            assertNotNull(found);
            assertEquals(email, found.getEmail());
            assertEquals("WORKER", found.getRole());

            assertNull(mapper.findByLoginId("no-such-login-id-xyz"));
        }
    }
}
