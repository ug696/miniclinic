package tw.edu.fju.miniclinic.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, String> {

    // 透過科別找醫師
    List<Doctor> findByDepartment(String department);

    // 取得所有不重複的科別名單
    @Query("select distinct d.department from Doctor d")
    List<String> findAllDepartments();
}
