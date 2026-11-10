package com.careos.hms.common.audit;

import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.config.JpaAuditingConfig;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@EntityScan(basePackageClasses = JpaAuditingRepositoryTest.AuditTestEntity.class)
@EnableJpaRepositories(considerNestedRepositories = true, basePackageClasses = JpaAuditingRepositoryTest.AuditTestRepository.class)
class JpaAuditingRepositoryTest {

    @Autowired
    private AuditTestRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void populatesAuditColumnsAndSupportsSoftDelete() {
        AuditTestEntity entity = new AuditTestEntity("foundation-check", 1L);
        AuditTestEntity saved = repository.saveAndFlush(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getHospitalId()).isEqualTo(1L);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo(ApiConstants.SYSTEM_AUDITOR);
        assertThat(saved.getUpdatedBy()).isEqualTo(ApiConstants.SYSTEM_AUDITOR);
        assertThat(saved.isDeleted()).isFalse();

        saved.markDeleted("ADMIN_TEST");
        AuditTestEntity deleted = repository.saveAndFlush(saved);
        entityManager.clear();

        AuditTestEntity reloaded = repository.findById(deleted.getId()).orElseThrow();
        assertThat(reloaded.isDeleted()).isTrue();
        assertThat(reloaded.getDeletedAt()).isNotNull();
        assertThat(reloaded.getDeletedBy()).isEqualTo("ADMIN_TEST");
    }

    @Entity
    @Table(name = "test_audit_records")
    static class AuditTestEntity extends TenantAwareEntity {

        @Column(name = "code", nullable = false, length = 64)
        private String code;

        protected AuditTestEntity() {
        }

        AuditTestEntity(String code, Long hospitalId) {
            this.code = code;
            assignHospitalId(hospitalId);
        }

        String getCode() {
            return code;
        }
    }

    interface AuditTestRepository extends JpaRepository<AuditTestEntity, Long> {
    }
}
