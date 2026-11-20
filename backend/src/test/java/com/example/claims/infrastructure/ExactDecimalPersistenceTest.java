package com.example.claims.infrastructure;

import com.example.claims.domain.Claim;
import com.example.claims.domain.ClaimType;
import com.example.claims.domain.Role;
import com.example.claims.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/claims-persistence-test.db?foreign_keys=on",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExactDecimalPersistenceTest {
    @Autowired private ClaimRepository claims;
    @Autowired private UserRepository users;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void amountRoundTripsExactlyAndSqliteForeignKeysAreEnabled() {
        User claimant = users.saveAndFlush(new User(UUID.randomUUID(), "Test Claimant",
                "claimant@example.test", Role.CLAIMANT, Instant.now()));
        BigDecimal exactAmount = new BigDecimal("1234.56");
        UUID id = UUID.randomUUID();
        claims.saveAndFlush(new Claim(id, "CLM-ROUNDTRIP-1", claimant, ClaimType.PROPERTY,
                LocalDate.now(), "Persistence test", exactAmount, null, null, Instant.now()));

        BigDecimal restored = claims.findById(id).orElseThrow().getEstimatedAmount();
        Integer foreignKeysEnabled = jdbc.queryForObject("PRAGMA foreign_keys", Integer.class);
        assertThat(restored.toPlainString()).isEqualTo("1234.56");
        assertThat(foreignKeysEnabled).isEqualTo(1);
    }
}
