package tw.edu.fju.miniclinic.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class LoginForm {

    @NotBlank(message = "請輸入醫師編號")
    @Pattern(regexp = "D\\d{3}", message = "醫師編號格式錯誤")
    private String doctorId;

    @NotBlank(message = "請輸入密碼")
    private String password;

    public LoginForm() {
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
