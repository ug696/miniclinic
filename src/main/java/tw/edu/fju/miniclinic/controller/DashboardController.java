package tw.edu.fju.miniclinic.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import tw.edu.fju.miniclinic.model.Appointment;
import tw.edu.fju.miniclinic.model.AppointmentRepository;
import tw.edu.fju.miniclinic.model.Doctor;
import tw.edu.fju.miniclinic.model.DoctorRepository;

import java.time.LocalDate;
import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private DoctorRepository doctorRepo;

    @Autowired
    private AppointmentRepository appointmentRepo;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String doctorId = (String) session.getAttribute("loggedInDoctorId");
        if (doctorId == null) {
            return "redirect:/login";
        }

        Doctor doctor = doctorRepo.findById(doctorId).orElse(null);

        // Session 裡的 doctorId 查不到對應醫師（資料被刪除等異常情況）
        if (doctor == null) {
            session.invalidate();
            return "redirect:/login";
        }

        LocalDate today = LocalDate.now();
        List<Appointment> myAppointments = appointmentRepo.findByDoctorAndApptDate(doctor, today);

        model.addAttribute("doctor", doctor);
        model.addAttribute("appointments", myAppointments);
        model.addAttribute("today", today);

        return "dashboard";
    }
}
