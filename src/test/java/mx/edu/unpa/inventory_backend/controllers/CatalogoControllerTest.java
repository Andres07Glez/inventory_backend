package mx.edu.unpa.inventory_backend.controllers;

import mx.edu.unpa.inventory_backend.domains.Category;
import mx.edu.unpa.inventory_backend.domains.Location;
import mx.edu.unpa.inventory_backend.dtos.brand.response.BrandResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.invoice.response.InvoiceResponseDTO;
import mx.edu.unpa.inventory_backend.enums.Campus;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.repositories.CategoryRepository;
import mx.edu.unpa.inventory_backend.repositories.LocationRepository;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.BrandService;
import mx.edu.unpa.inventory_backend.services.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@Import(GlobalExceptionHandler.class)
@WebMvcTest(
        controllers = CatalogoController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CatalogoController")
class CatalogoControllerTest {

    private static final String BASE_URL = "/v1/catalogs";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;


    @MockitoBean private CategoryRepository categoryRepository;
    @MockitoBean private LocationRepository locationRepository;

    // ── Servicios ─────────────────────────────────────────────────────────────
    @MockitoBean private BrandService brandService;
    @MockitoBean private InvoiceService invoiceService;

    // ── Seguridad ─────────────────────────────────────────────────────────────
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Category activeCategoryComputo;
    private Category activeCategoryMobiliario;
    private Category inactiveCategory;

    private Location activeLocationLab;
    private Location activeLocationAula;
    private Location inactiveLocation;

    private BrandResponseDTO dellBrand;
    private BrandResponseDTO hpBrand;

    private InvoiceResponseDTO invoiceOne;
    private InvoiceResponseDTO invoiceTwo;

    @BeforeEach
    void setUp() {
        activeCategoryComputo   = buildCategory(1, "Cómputo",    true);
        activeCategoryMobiliario = buildCategory(2, "Mobiliario", true);
        inactiveCategory        = buildCategory(3, "Obsoleto",   false);

        activeLocationLab  = buildLocation(1, "Laboratorio 1", "Edificio A", Campus.LOMA_BONITA, true);
        activeLocationAula = buildLocation(2, "Aula 201",      "Edificio B", Campus.TUXTEPEC,    true);
        inactiveLocation   = buildLocation(3, "Almacén viejo", "Bodega",     Campus.LOMA_BONITA, false);

        dellBrand = new BrandResponseDTO(1, "Dell", true);
        hpBrand   = new BrandResponseDTO(2, "HP",   true);

        invoiceOne = buildInvoice(1L, "FAC-001");
        invoiceTwo = buildInvoice(2L, "FAC-002");
    }

    // ── Helpers de construcción ───────────────────────────────────────────────

    private Category buildCategory(Integer id, String name, boolean active) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setIsActive(active);
        return c;
    }

    private Location buildLocation(Integer id, String name, String building, Campus campus, boolean active) {
        Location l = new Location();
        l.setId(id);
        l.setName(name);
        l.setBuilding(building);
        l.setCampus(campus);
        l.setIsActive(active);
        return l;
    }

    private InvoiceResponseDTO buildInvoice(Long id, String number) {
        return new InvoiceResponseDTO(
                id, number, 10L, "Proveedor Test",
                LocalDate.of(2024, 1, 15), BigDecimal.valueOf(5000.00),
                "invoices/" + id + "/doc.pdf",
                "http://localhost:8080/uploads/invoices/" + id + "/doc.pdf",
                "Notas de prueba",
                LocalDateTime.of(2024, 1, 15, 10, 0),"Admin"
        );
    }

    // =========================================================================
    // GET /v1/catalogs/categories
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/catalogs/categories")
    class GetCategories {

        @Test
        @DisplayName("should_return200WithActiveCategories_when_mixedCategoriesExist")
        void should_return200WithActiveCategories_when_mixedCategoriesExist() throws Exception {
            // El controlador filtra isActive en memoria — el repo devuelve todas.
            when(categoryRepository.findAll())
                    .thenReturn(List.of(activeCategoryComputo, activeCategoryMobiliario, inactiveCategory));

            mockMvc.perform(get(BASE_URL + "/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Cómputo"))
                    .andExpect(jsonPath("$.data[0].isActive").value(true))
                    .andExpect(jsonPath("$.data[1].id").value(2))
                    .andExpect(jsonPath("$.data[1].name").value("Mobiliario"));

            verify(categoryRepository).findAll();
        }

        @Test
        @DisplayName("should_return200WithEmptyList_when_allCategoriesAreInactive")
        void should_return200WithEmptyList_when_allCategoriesAreInactive() throws Exception {
            when(categoryRepository.findAll()).thenReturn(List.of(inactiveCategory));

            mockMvc.perform(get(BASE_URL + "/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("should_return200WithEmptyList_when_noCategoriesExist")
        void should_return200WithEmptyList_when_noCategoriesExist() throws Exception {
            when(categoryRepository.findAll()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        // Edge case: isActive = null no debe colarse como activa (Boolean.TRUE.equals(null) == false)
        @Test
        @DisplayName("should_excludeCategory_when_isActiveIsNull")
        void should_excludeCategory_when_isActiveIsNull() throws Exception {
            Category nullActiveCategory = buildCategory(4, "Sin estado", false);
            nullActiveCategory.setIsActive(null); // simula dato corrupto en BD

            when(categoryRepository.findAll())
                    .thenReturn(List.of(activeCategoryComputo, nullActiveCategory));

            mockMvc.perform(get(BASE_URL + "/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Cómputo"));
        }
    }

    // =========================================================================
    // GET /v1/catalogs/locations
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/catalogs/locations")
    class GetLocations {

        @Test
        @DisplayName("should_return200WithActiveLocations_when_mixedLocationsExist")
        void should_return200WithActiveLocations_when_mixedLocationsExist() throws Exception {
            when(locationRepository.findAll())
                    .thenReturn(List.of(activeLocationLab, activeLocationAula, inactiveLocation));

            mockMvc.perform(get(BASE_URL + "/locations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Laboratorio 1"))
                    .andExpect(jsonPath("$.data[0].building").value("Edificio A"))
                    .andExpect(jsonPath("$.data[0].campus").value("LOMA_BONITA"))
                    .andExpect(jsonPath("$.data[1].id").value(2))
                    .andExpect(jsonPath("$.data[1].campus").value("TUXTEPEC"));

            verify(locationRepository).findAll();
        }

        @Test
        @DisplayName("should_return200WithEmptyList_when_allLocationsAreInactive")
        void should_return200WithEmptyList_when_allLocationsAreInactive() throws Exception {
            when(locationRepository.findAll()).thenReturn(List.of(inactiveLocation));

            mockMvc.perform(get(BASE_URL + "/locations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("should_return200WithEmptyList_when_noLocationsExist")
        void should_return200WithEmptyList_when_noLocationsExist() throws Exception {
            when(locationRepository.findAll()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/locations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        // Edge case: isActive = null en Location (misma guardia Boolean.TRUE.equals)
        @Test
        @DisplayName("should_excludeLocation_when_isActiveIsNull")
        void should_excludeLocation_when_isActiveIsNull() throws Exception {
            Location nullActiveLocation = buildLocation(5, "Sin estado", "?", Campus.LOMA_BONITA, false);
            nullActiveLocation.setIsActive(null);

            when(locationRepository.findAll())
                    .thenReturn(List.of(activeLocationLab, nullActiveLocation));

            mockMvc.perform(get(BASE_URL + "/locations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Laboratorio 1"));
        }
    }

    // =========================================================================
    // GET /v1/catalogs/brands
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/catalogs/brands")
    class GetBrands {

        @Test
        @DisplayName("should_return200WithBrandList_when_activeBrandsExist")
        void should_return200WithBrandList_when_activeBrandsExist() throws Exception {
            when(brandService.getAllActive()).thenReturn(List.of(dellBrand, hpBrand));

            mockMvc.perform(get(BASE_URL + "/brands"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Dell"))
                    .andExpect(jsonPath("$.data[0].isActive").value(true))
                    .andExpect(jsonPath("$.data[1].name").value("HP"));

            verify(brandService).getAllActive();
        }

        @Test
        @DisplayName("should_return200WithEmptyList_when_noActiveBrandsExist")
        void should_return200WithEmptyList_when_noActiveBrandsExist() throws Exception {
            when(brandService.getAllActive()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/brands"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // =========================================================================
    // GET /v1/catalogs/invoices
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/catalogs/invoices")
    class GetInvoices {

        @Test
        @DisplayName("should_return200WithInvoiceList_when_invoicesExist")
        void should_return200WithInvoiceList_when_invoicesExist() throws Exception {
            when(invoiceService.getAllUnpaged()).thenReturn(List.of(invoiceOne, invoiceTwo));

            mockMvc.perform(get(BASE_URL + "/invoices"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].invoiceNumber").value("FAC-001"))
                    .andExpect(jsonPath("$.data[0].supplierName").value("Proveedor Test"))
                    .andExpect(jsonPath("$.data[0].totalAmount").value(5000.0))
                    // Verifica que la URL pública se serializa correctamente
                    .andExpect(jsonPath("$.data[0].documentUrl")
                            .value("http://localhost:8080/uploads/invoices/1/doc.pdf"))
                    .andExpect(jsonPath("$.data[1].invoiceNumber").value("FAC-002"));

            verify(invoiceService).getAllUnpaged();
        }

        @Test
        @DisplayName("should_return200WithEmptyList_when_noInvoicesExist")
        void should_return200WithEmptyList_when_noInvoicesExist() throws Exception {
            when(invoiceService.getAllUnpaged()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/invoices"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        // Edge case: LocalDate e LocalDateTime deben serializarse con JavaTimeModule.
        // Si el ObjectMapper del contexto no tiene JavaTimeModule registrado, este test
        // fallará con un error de serialización — señal de que falta @Import(JacksonConfig.class).
        @Test
        @DisplayName("should_serializeLocalDateCorrectly_when_invoiceHasDate")
        void should_serializeLocalDateCorrectly_when_invoiceHasDate() throws Exception {
            when(invoiceService.getAllUnpaged()).thenReturn(List.of(invoiceOne));

            mockMvc.perform(get(BASE_URL + "/invoices"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].invoiceDate").value("2024-01-15"))
                    .andExpect(jsonPath("$.data[0].createdAt").value("2024-01-15T10:00:00"));
        }
    }
}