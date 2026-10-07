package com.smartprep.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight health check for uptime/cron pings. Touches no database.
 */
@RestController
public class PingController {

    @GetMapping("/api/ping")
    public String ping() {
        return "ok";
    }
}
