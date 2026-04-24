package mx.edu.unpa.inventory_backend.controllers;

import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.Category;
import mx.edu.unpa.inventory_backend.domains.Invoice;
import mx.edu.unpa.inventory_backend.domains.Location;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.repositories.CategoryRepository;
import mx.edu.unpa.inventory_backend.repositories.InvoiceRepository;
import mx.edu.unpa.inventory_backend.repositories.LocationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/catalogs")
@RequiredArgsConstructor
public class CatalogoController {

    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final InvoiceRepository invoiceRepository;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Category>>> getCategories() {
        List<Category> list = categoryRepository.findAll()
                .stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<List<Location>>> getLocations() {
        List<Location> list = locationRepository.findAll()
                .stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsActive()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<Invoice>>> getInvoices() {
        return ResponseEntity.ok(ApiResponse.ok(invoiceRepository.findAll()));
    }

}
