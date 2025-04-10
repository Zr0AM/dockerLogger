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

        String storedProcedureCall = "{call dbo.InsertLog(?, ?, ?, ?, ?, ?, ?, ?, ?)}";

        dbJdbcTemplate.update(storedProcedureCall,
                LocalDateTime.now(),
                log.getLogType(),
                log.getLogApp(),
                log.getLogAppComp(),
                getLocalHostAddress(),
                log.getLogSrcUser(),
                log.getLogMsg(),
                log.getLogError(),
                log.getLogErrorStack());
    }

    private String getLocalHostAddress() {
        try {
            return java.net.Inet4Address.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "";
        }
    }
}
