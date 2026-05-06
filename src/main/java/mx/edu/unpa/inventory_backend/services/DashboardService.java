package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.dashboard.response.DashboardStatsResponse;

public interface DashboardService {
    DashboardStatsResponse getStats();
}
