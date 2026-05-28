package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.unpa.inventory_backend.dtos.dashboard.response.DashboardStatsResponse;
import mx.edu.unpa.inventory_backend.dtos.dashboard.response.LocationStatDTO;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.services.DashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AssetRepository assetRepository;


    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        log.debug("Calculando estadísticas del dashboard");

        long totalAssets     = assetRepository.countByLifecycleStatusNot(LifecycleStatus.DECOMMISSIONED);
        long availableAssets = assetRepository.countByLifecycleStatus(LifecycleStatus.AVAILABLE);
        long assignedAssets  = assetRepository.countByLifecycleStatus(LifecycleStatus.ASSIGNED);

        // Condición — excluimos bienes dados de baja para no distorsionar el donut
        long goodCondition    = assetRepository.countByConditionStatusAndLifecycleStatusNot(
                ConditionStatus.GOOD,    LifecycleStatus.DECOMMISSIONED);
        long regularCondition = assetRepository.countByConditionStatusAndLifecycleStatusNot(
                ConditionStatus.REGULAR, LifecycleStatus.DECOMMISSIONED);
        long badCondition     = assetRepository.countByConditionStatusAndLifecycleStatusNot(
                ConditionStatus.BAD,     LifecycleStatus.DECOMMISSIONED);

        // Top 5 ubicaciones — PageRequest.of(0, 5) = primera página, 5 elementos
        List<LocationStatDTO> topLocations =
                assetRepository.findTopLocationsByAssignedAssets(PageRequest.of(0, 5));

        return new DashboardStatsResponse(
                totalAssets,
                availableAssets,
                assignedAssets,
                goodCondition,
                regularCondition,
                badCondition,
                0L,  // openIncidents — módulo pendiente
                0L,  // maintenanceThisMonth — módulo pendiente
                topLocations
        );
    }
}
