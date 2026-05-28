package mx.edu.unpa.inventory_backend.dtos.dashboard.response;

import java.util.List;

public record DashboardStatsResponse(
        // ── KPIs ──────────────────────────────────────────────────────────────
        long totalAssets,       // bienes no dados de baja
        long availableAssets,   // lifecycle = AVAILABLE
        long assignedAssets,    // lifecycle = ASSIGNED

        // ── Distribución por condición (donut chart) ──────────────────────────
        long goodCondition,
        long regularCondition,
        long badCondition,

        // ── Módulos pendientes (placeholder — se actualizan cuando existan) ───
        long openIncidents,        // siempre 0 hasta implementar el módulo
        long maintenanceThisMonth, // siempre 0 hasta implementar el módulo

        // ── Top 5 ubicaciones (bar chart) ────────────────────────────────────
        List<LocationStatDTO> topLocations
) {}
