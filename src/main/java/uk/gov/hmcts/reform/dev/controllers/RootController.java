package uk.gov.hmcts.reform.dev.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@Hidden
public class RootController {

    @GetMapping("/")
    public ResponseEntity<String> root() {
        return ok("Task service is running. See /swagger-ui.html for API documentation.");
    }
}
