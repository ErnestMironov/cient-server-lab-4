package ru.rksp.mironov_consumer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.rksp.mironov_consumer.entity.StudentMessageEntity;

@Repository
public interface StudentMessageRepository extends JpaRepository<StudentMessageEntity, Long> {
}

