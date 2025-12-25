package ru.rksp.mironov_consumer.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class StudentMessage {
    private String fullName;
    private String passport;
    private Instant createdAt;
}

