package com.open.spring.mvc.capstone;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Small REST controller exposing demo endpoints for the capstone project.
 */
@RestController
@RequestMapping(path = "/api/capstones/demo", produces = MediaType.APPLICATION_JSON_VALUE)
public class CapstoneServiceController {

    @GetMapping("/list")
    public ResponseEntity<List<String>> listOps() {
        return new ResponseEntity<>(CapstoneController.demoListOps(), HttpStatus.OK);
    }

    @GetMapping("/mapset")
    public ResponseEntity<Map<String, Integer>> mapSet() {
        return new ResponseEntity<>(CapstoneController.demoMapSet(), HttpStatus.OK);
    }

    @GetMapping("/stackqueue")
    public ResponseEntity<Map<String, Object>> stackQueue() {
        return new ResponseEntity<>(CapstoneController.demoStackQueue(), HttpStatus.OK);
    }

    @GetMapping("/tree")
    public ResponseEntity<List<String>> tree() {
        return new ResponseEntity<>(CapstoneController.demoTree(), HttpStatus.OK);
    }
}
