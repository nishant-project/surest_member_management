package com.surest.surest_member_management.repository;

import com.surest.surest_member_management.entity.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
class MemberRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("surestDB")
            .withUsername("postgres")
            .withPassword("root");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MemberRepository repo;

    @Test
    void shouldSaveAndFind(){
        Member m = Member.builder()
                .id(java.util.UUID.randomUUID())
                .firstName("Ravi")
                .lastName("Kumar")
                .dateOfBirth(LocalDate.of(1995,1,20))
                .email("ravi@example.com")
                .build();
        repo.save(m);

        var result = repo.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase("Ravi","Kumar", PageRequest.of(0,5));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("ravi@example.com");
    }
}
