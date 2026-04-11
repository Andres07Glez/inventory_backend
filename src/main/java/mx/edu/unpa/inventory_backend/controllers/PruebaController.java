package mx.edu.unpa.inventory_backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test")
public class PruebaController {

    @GetMapping("/ok")
    public ResponseEntity<List<?>> myContacts() {
        return ResponseEntity.ok().build();
    }
}
