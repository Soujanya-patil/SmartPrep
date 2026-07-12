package com.smartprep.controller;

import com.smartprep.dto.DashboardDTO;
import com.smartprep.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/{userId}")
    public ResponseEntity<DashboardDTO> getDashboard(@PathVariable int userId) {
        DashboardDTO dashboard = dashboardService.getDashboard(userId);
        return ResponseEntity.ok(dashboard);
    }
}