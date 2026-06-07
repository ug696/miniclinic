package tw.edu.fju.miniclinic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import tw.edu.fju.miniclinic.model.Doctor;
import tw.edu.fju.miniclinic.model.DoctorRepository;
import java.util.Optional;
import java.util.List;

@Controller
public class DoctorPageController {

    @Autowired
    private DoctorRepository doctorRepo;

    @GetMapping("/doctors")
    public String listDoctors(
            @RequestParam(required = false) String department,
            Model model) {

        List<Doctor> doctors;
        if (department == null || department.isBlank()) {
            // 如果沒有指定科別，就找全部的醫生
            doctors = doctorRepo.findAll();
        } else {
            // 如果有指定科別，就只找那個科別的醫生
            doctors = doctorRepo.findByDepartment(department);
        }

        // 把資料打包準備送到前端網頁
        model.addAttribute("doctors", doctors);
        model.addAttribute("departments", doctorRepo.findAllDepartments());
        model.addAttribute("selectedDept", department);

        return "doctors"; 
    }
    @GetMapping("/doctors/{doctorId}")
    public String doctorDetail(@PathVariable String doctorId, Model model) {
        // 透過網址傳來的 doctorId 去資料庫找這位醫師
        Optional<Doctor> doctor = doctorRepo.findById(doctorId);

        if (doctor.isEmpty()) {
            return "redirect:/doctors"; // 如果亂打網址找不到人，就把它踢回清單頁
        }

        // 把找到的醫師資料打包，準備送到下一個網頁
        model.addAttribute("doctor", doctor.get());
        return "doctor-detail"; // 這會去尋找 templates/doctor-detail.html
    }
}
