package org.omnomnom.dockerLogger.controller;

import jakarta.annotation.Resource;
import org.apache.hc.core5.http.HttpStatus;
import org.omnomnom.dockerLogger.db.LogEntity;
import org.omnomnom.dockerLogger.service.LogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.Inet4Address;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/")
public class LogController {

    @Resource
    LogService logService;

    @PostMapping("addLog")
    public ResponseEntity<String> addLog(@RequestBody LogEntity log) {
        try {
            logService.insertLog(LocalDateTime.now(), log.getLogType(), log.getLogApp(), log.getLogAppComp(), getLocalHostAddress(), log.getLogSrcUser(), log.getLogMsg(), log.getLogError(), log.getLogErrorStack());
            return ResponseEntity.ok("Log added successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body("Error adding log: " + e.getMessage());
        }
    }

    private String getLocalHostAddress() {
        try {
            return Inet4Address.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "";
        }
    }

}
