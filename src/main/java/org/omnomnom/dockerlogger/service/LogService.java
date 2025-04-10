package org.omnomnom.dockerlogger.service;

import jakarta.annotation.Resource;
import org.omnomnom.dockerlogger.db.Logentity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
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

    private static String getLocalHostAddress() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            return localhost.getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }
}
