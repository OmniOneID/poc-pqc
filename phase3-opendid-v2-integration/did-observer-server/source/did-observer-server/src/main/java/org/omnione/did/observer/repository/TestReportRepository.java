package org.omnione.did.observer.repository;

import org.omnione.did.observer.entity.TestReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestReportRepository extends JpaRepository<TestReportEntity, Long> {
    List<TestReportEntity> findAllByOrderByIdAsc();
}
