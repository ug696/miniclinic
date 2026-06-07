package tw.edu.fju.miniclinic.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    //  Part A
    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "ok",
            "service", "miniclinic"
        );
    }

    //  Part B 
    @GetMapping("/api/about")
    public Map<String, String> about() {
        return Map.of(
            "student_id", "414570148", 
            "student_name", "謝宥祺", 
            "project", "MiniClinic",
            "version", "0.1.0",
            "chapter", "Ch09-A"
        );
    }
}