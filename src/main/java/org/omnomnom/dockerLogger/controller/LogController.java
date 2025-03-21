package org.omnomnom.dockerLogger.controller;

import org.omnomnom.dockerLogger.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.Inet4Address;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/")
public class LogController {

    @Autowired
    LogService logService;

    @GetMapping("test")
    public void testService() {
        logService.insertLog(LocalDateTime.now(), "INFO", "Test", "test", getLocalHostAddress(), "testUser", "this is a test", false, null);
    }

    private String getLocalHostAddress() {
        try {
            return Inet4Address.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "";
        }
    }

}
