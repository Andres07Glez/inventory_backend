package mx.edu.unpa.inventory_backend.services;


import mx.edu.unpa.inventory_backend.dtos.brand.request.BrandRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.brand.response.BrandResponseDTO;

import java.util.List;

public interface BrandService {

    List<BrandResponseDTO> getAllActive();
    BrandResponseDTO       create(BrandRequestDTO request);
    BrandResponseDTO       update(Integer id, BrandRequestDTO request);
    void                   delete(Integer id);   // soft delete

}
