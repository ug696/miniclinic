package tw.edu.fju.miniclinic.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import tw.edu.fju.miniclinic.model.Appointment;
import tw.edu.fju.miniclinic.model.AppointmentRepository;
import tw.edu.fju.miniclinic.model.DoctorRepository;
import tw.edu.fju.miniclinic.model.PatientRepository;

@Controller
public class StatsController {

    @Autowired
    private DoctorRepository doctorRepo;

    @Autowired
    private PatientRepository patientRepo;

    @Autowired
    private AppointmentRepository appointmentRepo;

    @GetMapping("/stats")
    public String showStats(Model model) {
        Map<String, Long> appointmentCountsByDepartment = new LinkedHashMap<>();

        for (Appointment appt : appointmentRepo.findAll()) {
            String department = appt.getDoctor().getDepartment();
            appointmentCountsByDepartment.merge(department, 1L, Long::sum);
        }

        model.addAttribute("doctorCount", doctorRepo.count());
        model.addAttribute("patientCount", patientRepo.count());
        model.addAttribute("appointmentCount", appointmentRepo.count());
        model.addAttribute("appointmentCountsByDepartment", appointmentCountsByDepartment);

        return "stats";
    }

    @GetMapping("/api/stats")
    @ResponseBody
    public Map<String, Object> getStats() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("BOOKED", appointmentRepo.countByStatus("BOOKED"));
        byStatus.put("COMPLETED", appointmentRepo.countByStatus("COMPLETED"));
        byStatus.put("CANCELLED", appointmentRepo.countByStatus("CANCELLED"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalDoctors", doctorRepo.count());
        result.put("totalPatients", patientRepo.count());
        result.put("totalAppointments", appointmentRepo.count());
        result.put("byStatus", byStatus);

        return result;
    }
}
