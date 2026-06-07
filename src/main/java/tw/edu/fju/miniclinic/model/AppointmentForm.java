package tw.edu.fju.miniclinic.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AppointmentForm {

    @NotBlank(message = "請輸入病歷號")
    @Pattern(regexp = "TEST\\d{5}", message = "病歷號格式為 TESTxxxxx")
    private String chartNo;      // 病歷號

    @NotBlank(message = "請選擇醫師")
    private String doctorId;     // 掛號的醫師

    @NotBlank(message = "請選擇日期")
    private String apptDate;     // 日期

    @NotBlank(message = "請選擇時段")
    private String timeSlot;     // 時段

    // 建構子 (無參，表單綁定需要)
    public AppointmentForm() {}

    // Getters & Setters
    public String getChartNo() { return chartNo; }
    public void setChartNo(String chartNo) { this.chartNo = chartNo; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getApptDate() { return apptDate; }
    public void setApptDate(String apptDate) { this.apptDate = apptDate; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
}
