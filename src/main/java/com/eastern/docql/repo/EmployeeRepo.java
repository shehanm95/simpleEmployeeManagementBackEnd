package com.eastern.docql.repo;

import com.eastern.docql.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepo extends JpaRepository <Employee,Integer> {
}
