package tw.edu.fju.miniclinic.controller;

import java.time.LocalDate;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import tw.edu.fju.miniclinic.model.Appointment;
import tw.edu.fju.miniclinic.model.AppointmentForm;
import tw.edu.fju.miniclinic.model.AppointmentRepository;
import tw.edu.fju.miniclinic.model.Doctor;
import tw.edu.fju.miniclinic.model.DoctorRepository;
import tw.edu.fju.miniclinic.model.Patient;
import tw.edu.fju.miniclinic.model.PatientRepository;

@Controller
public class AppointmentController {

    @Autowired
    private DoctorRepository doctorRepo;

    @Autowired
    private PatientRepository patientRepo;

    @Autowired
    private AppointmentRepository appointmentRepo;

    // 1. 顯示預約表單頁面
    @GetMapping("/appointment/new")
    public String newAppointmentForm(Model model) {
        AppointmentForm form = new AppointmentForm();
        form.setApptDate(LocalDate.now().toString());

        model.addAttribute("form", form);
        model.addAttribute("doctors", doctorRepo.findAll());
        return "appointment-new";
    }

    @PostMapping("/appointment/new")
    public String submitAppointment(
            @Valid @ModelAttribute("form") AppointmentForm form,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("form", form);
            model.addAttribute("doctors", doctorRepo.findAll());
            return "appointment-new";
        }

        // 步驟 1：用表單的字串 ID，從資料庫查出真正的物件
        Patient patient = patientRepo.findById(form.getChartNo()).orElse(null);
        Doctor  doctor  = doctorRepo.findById(form.getDoctorId()).orElse(null);

        // 步驟 2：驗證——找不到就回表單顯示錯誤
        if (patient == null || doctor == null) {
            model.addAttribute("error", "查無此病歷號或醫師，請確認後重試");
            model.addAttribute("form", form);
            model.addAttribute("doctors", doctorRepo.findAll());
            return "appointment-new";   // ← 回到表單頁，不是跳轉
        }

        // 步驟 3：建立 Appointment Entity，設定關聯物件
        Appointment appt = new Appointment();
        appt.setPatient(patient);
        appt.setDoctor(doctor);
        appt.setApptDate(LocalDate.parse(form.getApptDate()));  // 字串 → LocalDate
        appt.setTimeSlot(form.getTimeSlot());
        appt.setStatus("BOOKED");

        // 步驟 4：存入資料庫，JPA 自動填入 apptId
        Appointment saved = appointmentRepo.save(appt);

        // 步驟 5：把儲存後的物件交給結果頁面
        model.addAttribute("appointment", saved);
        return "appointment-result";
    }
}
