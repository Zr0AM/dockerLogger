package org.omnomnom.dockerlogger.configuration;

import org.omnomnom.dockerlogger.scheduler.LoggingJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Date;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail loggingJobDetail() {
        return JobBuilder.newJob(LoggingJob.class)
                .withIdentity("loggingJob")
                .withDescription("Sends periodic application logs")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger startupLogTrigger(JobDetail loggingJobDetail) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("message", "Service initialized.");

        return TriggerBuilder.newTrigger()
                .forJob(loggingJobDetail)
                .withIdentity("startupLogTrigger")
                .withDescription("Fires once on application startup")
                .usingJobData(jobDataMap)
                .startAt(Date.from(Instant.now().plusSeconds(60)))
                .build();
    }

    @Bean
    public Trigger periodicLogTrigger(JobDetail loggingJobDetail) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("message", "Periodic health check log.");

        return TriggerBuilder.newTrigger()
                .forJob(loggingJobDetail)
                .withIdentity("periodicLogTrigger")
                .withDescription("Fires every 5 minutes for health checks")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(5).repeatForever())
                .usingJobData(jobDataMap)
                .build();
    }
}