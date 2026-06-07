package tw.edu.fju.miniclinic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tw.edu.fju.miniclinic.model.Doctor;
import tw.edu.fju.miniclinic.model.DoctorRepository;

import java.util.List;
import java.util.Optional;

@RestController
public class DoctorApiController {

    @Autowired
    private DoctorRepository doctorRepo;

    @GetMapping("/api/doctors")
    public List<Doctor> getDoctors(
            @RequestParam(required = false) String department) {
        if (department == null || department.isBlank()) {
            return doctorRepo.findAll();
        }
        return doctorRepo.findByDepartment(department);
    }

    @GetMapping("/api/doctors/{doctorId}")
    public ResponseEntity<Doctor> getDoctor(@PathVariable String doctorId) {
        Optional<Doctor> doctor = doctorRepo.findById(doctorId);
        return doctor
                .map(d -> ResponseEntity.ok(d))                 // 有找到 -> 回傳 200 OK + 資料
                .orElse(ResponseEntity.notFound().build());     // 沒找到 -> 回傳 404
    }

    @GetMapping("/api/departments")
    public List<String> getDepartments() {
        return doctorRepo.findAllDepartments();
    }

    @PostMapping("/api/doctors")
    public ResponseEntity<Doctor> createDoctor(@RequestBody Doctor doctor) {
        Doctor saved = doctorRepo.save(doctor);
        return ResponseEntity.status(201).body(saved);
    }
    @PutMapping("/api/doctors/{doctorId}")
    public ResponseEntity<Doctor> updateDoctor(
            @PathVariable String doctorId,
            @RequestBody Doctor updated) {

        return doctorRepo.findById(doctorId)
            .map(existing -> {
                existing.setName(updated.getName());
                existing.setDepartment(updated.getDepartment());
                existing.setSpecialty(updated.getSpecialty());
                return ResponseEntity.ok(doctorRepo.save(existing));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/api/doctors/{doctorId}")
    public ResponseEntity<Void> deleteDoctor(@PathVariable String doctorId) {
        if (!doctorRepo.existsById(doctorId)) {
            return ResponseEntity.notFound().build();
        }
        doctorRepo.deleteById(doctorId);
        return ResponseEntity.noContent().build();  // 204 No Content
    }
}