package org.omnomnom.dockerLogger.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogService {

    @Autowired
    JdbcTemplate dbJdbcTemplate;

    public void insertLog(LocalDateTime logDttm, String logType, String logApp, String logAppComp, String logSrcIP,
                          String logSrcUser, String logMsg, boolean logError, String logErrorStack) {

        String storedProcedureCall = "{call dbo.InsertLog(?, ?, ?, ?, ?, ?, ?, ?, ?)}";

        dbJdbcTemplate.update(storedProcedureCall,
                logDttm,
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
