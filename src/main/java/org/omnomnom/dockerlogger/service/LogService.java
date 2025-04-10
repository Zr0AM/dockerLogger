package org.omnomnom.dockerlogger.service;

import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogService {

    @Resource
    JdbcTemplate dbJdbcTemplate;

    public void insertLog(LocalDateTime logDateTime, String logType, String logApp, String logAppComp, String logSrcIP,
                          String logSrcUser, String logMsg, boolean logError, String logErrorStack) {

        String storedProcedureCall = "{call dbo.InsertLog(?, ?, ?, ?, ?, ?, ?, ?, ?)}";

        dbJdbcTemplate.update(storedProcedureCall,
                logDateTime,
                logType,
                logApp,
                logAppComp,
                logSrcIP,
                logSrcUser,
                logMsg,
                logError,
                logErrorStack);
    }

}
