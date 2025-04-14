package org.omnomnom.dockerlogger.service;

import jakarta.annotation.Resource;
import org.omnomnom.dockerlogger.db.Logentity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class LogService {

    @Resource
    JdbcTemplate dbJdbcTemplate;

    public void insertLog(Logentity log) {

        dbJdbcTemplate.update("{call dbo.InsertLog(?, ?, ?, ?, ?, ?, ?, ?, ?)}",
                LocalDateTime.now(),
                log.getLogType(),
                log.getLogApp(),
                log.getLogAppComp(),
                log.getLogSrcIp(),
                log.getLogSrcUser(),
                log.getLogMsg(),
                log.getLogError(),
                log.getLogErrorStack()
        );
    }
}
