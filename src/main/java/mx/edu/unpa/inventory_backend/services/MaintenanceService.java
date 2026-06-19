package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.maintenance.request.MaintenanceCreateRequest;
import mx.edu.unpa.inventory_backend.dtos.maintenance.response.MaintenanceResponse;
import mx.edu.unpa.inventory_backend.dtos.maintenance.response.MaintenanceSummary;
import mx.edu.unpa.inventory_backend.enums.MaintenanceType;

import java.util.List;

public interface MaintenanceService {

    /**
     * Registra un nuevo mantenimiento. Operación inmutable: no hay update ni delete.
     *
     * @param request   datos del mantenimiento a registrar
     * @param createdById ID del usuario autenticado (extraído del JWT, nunca del body)
     * @return respuesta completa del registro creado
     */
    MaintenanceResponse create(MaintenanceCreateRequest request, Long createdById);

    /**
     * Lista los mantenimientos de un bien específico, ordenados por fecha desc.
     *
     * @param assetId ID del bien
     * @return lista de resúmenes
     */
    List<MaintenanceSummary> getByAssetId(Long assetId);

    /**
     * Lista global con filtro opcional por tipo.
     *
     * @param type filtro de tipo; 
     * @return lista de resúmenes
     */
    List<MaintenanceSummary> getAll(MaintenanceType type);

    /**
     * Detalle completo de un registro por ID.
     *
     * @param id identificador del registro
     * @return respuesta completa
     */
    MaintenanceResponse getById(Long id);

    // Agrega esto al final de la interfaz
    /**
     * Elimina un registro de mantenimiento.
     * Solo accesible para usuarios con rol ADMIN.
     *
     * @param id identificador del registro a eliminar
     */
    void delete(Long id);
}