package com.cms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * PostgreSQL-backed job queue — replaces all Redis list queues.
 * Queues: file:process, embedding:process, search:index, ai:process,
 *         audit:buffer, webhook:deliver
 */
@Service
@Slf4j
public class JobQueueService {

    private final JdbcTemplate pgJdbc;
    private final ObjectMapper objectMapper;

    public JobQueueService(@Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbc,
                           ObjectMapper objectMapper) {
        this.pgJdbc = pgJdbc;
        this.objectMapper = objectMapper;
    }

    public void push(String queueName, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            pgJdbc.update(
                    "INSERT INTO job_queue (queue_name, payload) VALUES (?, ?)",
                    queueName, json);
            log.debug("Pushed job to queue '{}': {}", queueName, json);
        } catch (Exception e) {
            log.error("Failed to push job to queue '{}': {}", queueName, e.getMessage());
        }
    }

    public void push(String queueName, String payloadJson) {
        try {
            pgJdbc.update(
                    "INSERT INTO job_queue (queue_name, payload) VALUES (?, ?)",
                    queueName, payloadJson);
            log.debug("Pushed job to queue '{}'", queueName);
        } catch (Exception e) {
            log.error("Failed to push job to queue '{}': {}", queueName, e.getMessage());
        }
    }
}
