package com.eastern.docql.controller;

import com.eastern.docql.model.Employee;
import com.eastern.docql.repo.EmployeeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

@CrossOrigin(origins = "http://127.0.0.1:5500")
// Allow CORS for this controller

@RestController
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    EmployeeRepo employeeRepo;

    @GetMapping("/all")
    public List<Employee> getAllEmployees() {
        return employeeRepo.findAll();
    }

    @GetMapping("")
    public String emp() {
        return "Employee";
    }

    private static final String UPLOAD_DIR = "src/main/resources/static/images/employee/";

    @PostMapping("/add")
    public Employee allEmployee(
            @RequestParam String name,
            @RequestParam Integer age,
            @RequestParam MultipartFile profile) {
        Employee emp = new Employee(0, name, age);
        Employee savedEmployee = employeeRepo.save(emp);

        // Check if the profile image exists
        if (!profile.isEmpty()) {
            // Create the target directory if it doesn't exist
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            Path targetPath = Paths.get(UPLOAD_DIR +
                    savedEmployee.getId()
                    + getFileExtension(profile.getOriginalFilename()));

            // Use Files.copy to save the file
            try {
                Files.copy(profile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return savedEmployee;
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

    @DeleteMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable int id) {
        // Delete the employee record from the database
        employeeRepo.deleteById(id);

        // Search for the file with the employee ID as the filename
        Path dirPath = Paths.get(UPLOAD_DIR);
        try (Stream<Path> files = Files.list(dirPath)) {
            files.filter(file -> file.getFileName().toString().startsWith(String.valueOf(id)))
                    .findFirst()
                    .ifPresent(file -> {
                        try {
                            Files.delete(file); // Delete the file
                            System.out.println("Deleted file: " + file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return "Employee and image deleted successfully.";
    }

}
