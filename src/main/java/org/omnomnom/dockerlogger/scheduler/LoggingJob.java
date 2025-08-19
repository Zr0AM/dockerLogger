package org.omnomnom.dockerlogger.scheduler;

import jakarta.annotation.Resource;
import org.omnomnom.dockerlogger.db.Logentity;
import org.omnomnom.dockerlogger.service.LogService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class LoggingJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(LoggingJob.class);

    @Resource
    LogService logService;

    @Value("${spring.application.name}")
    private String appName;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String message = context.getMergedJobDataMap().getString("message");
        logger.info("Executing logging job with message: {}", message);

        try {
            Logentity log = new Logentity();
            log.setLogApp(appName);
            log.setLogAppComp("QuartzScheduler");
            log.setLogMsg(message);
            log.setLogError(false);
            log.setLogType("INFO");
            log.setLogSrcUser(System.getProperty("user.name"));

            try {
                log.setLogSrcIp(InetAddress.getLocalHost().getHostAddress());
            } catch (UnknownHostException e) {
                logger.warn("Could not determine local IP address. Defaulting to 127.0.0.1.", e);
                log.setLogSrcIp("127.0.0.1");
            }

            logService.insertLog(log);

        } catch (Exception e) {
            // Throwing JobExecutionException tells Quartz there was a failure
            throw new JobExecutionException("Failed to send log", e);
        }
    }
}
