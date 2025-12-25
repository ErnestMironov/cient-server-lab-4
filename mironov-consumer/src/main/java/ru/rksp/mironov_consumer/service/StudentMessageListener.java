package ru.rksp.mironov_consumer.service;

import java.sql.Timestamp;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.rksp.mironov_consumer.dto.StudentMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentMessageListener {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void handleStudentMessage(StudentMessage message) {
        log.info("Received student message: {}", message);
        
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(false);
        
        transactionTemplate.execute(status -> {
            int rows = jdbcTemplate.update(
                "INSERT INTO utmn.student_message (full_name, passport, created_at) VALUES (?, ?, ?)",
                message.getFullName(),
                message.getPassport(),
                Timestamp.from(message.getCreatedAt())
            );
            log.info("Saved student message to database, rows affected: {}", rows);
            return rows;
        });
    }
}