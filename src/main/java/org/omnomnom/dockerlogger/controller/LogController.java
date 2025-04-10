package org.omnomnom.dockerlogger.controller;

import jakarta.annotation.Resource;
import org.apache.hc.core5.http.HttpStatus;
import org.omnomnom.dockerlogger.db.Logentity;
import org.omnomnom.dockerlogger.service.LogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/")
public class LogController {

    @Resource
    LogService logService;

    @PostMapping("addLog")
    public ResponseEntity<String> addLog(@RequestBody Logentity log) {
        try {
            logService.insertLog(log);
            return ResponseEntity.ok("Log added successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body("Error adding log: " + e.getMessage());
        }
    }

}
