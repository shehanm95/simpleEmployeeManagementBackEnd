package com.eastern.docql.controller;

import com.eastern.docql.model.Employee;
import com.eastern.docql.repo.EmployeeRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@CrossOrigin // Allow CORS for this controller
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    private static final String UPLOAD_DIR = "src/main/resources/static/images/employee/";
    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    @Autowired
    private EmployeeRepo employeeRepo;

    @GetMapping("/all")
    public List<Employee> getAllEmployees() {
        return employeeRepo.findAll();
    }

    @PostMapping("/add")
    public Employee addEmployee(
            @RequestParam String name,
            @RequestParam Integer age,
            @RequestParam MultipartFile profile) {
        Employee emp = new Employee(0, name, null, age);
        Employee savedEmployee = employeeRepo.save(emp);

        // Save the image if the profile exists
        if (!profile.isEmpty()) {
            try {
                String imageName = savedEmployee.getId() + getFileExtension(profile.getOriginalFilename());
                saveImage(profile, imageName);
                savedEmployee.setImgName(imageName); // Update employee with image name
                employeeRepo.save(savedEmployee); // Save the employee again with image name
                log.info("Profile image saved: " + imageName);
            } catch (IOException e) {
                log.error("Error saving profile image", e);
                throw new RuntimeException(e);
            }
        }

        return savedEmployee;
    }

    @PutMapping("/update")
    public Employee updateEmployee(
            @RequestParam Integer id,
            @RequestParam String name,
            @RequestParam Integer age,
            @RequestParam MultipartFile profile) {
        Optional<Employee> existingEmployee = employeeRepo.findById(id);

        if (existingEmployee.isPresent()) {
            Employee emp = existingEmployee.get();
            emp.setName(name);
            emp.setAge(age);

            // Handle profile image update if provided
            if (!profile.isEmpty()) {
                try {
                    String imageName = emp.getId() + getFileExtension(profile.getOriginalFilename());
                    saveImage(profile, imageName);
                    emp.setImgName(imageName); // Update with new image name
                    log.info("Profile image updated: " + imageName);
                } catch (IOException e) {
                    log.error("Error updating profile image", e);
                    throw new RuntimeException(e);
                }
            }

            return employeeRepo.save(emp);
        } else {
            throw new RuntimeException("Employee not found with id: " + id);
        }
    }

    @DeleteMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable int id) {
        employeeRepo.deleteById(id);

        // Delete the associated profile image
        Path dirPath = Paths.get(UPLOAD_DIR);
        try (Stream<Path> files = Files.list(dirPath)) {
            files.filter(file -> file.getFileName().toString().startsWith(String.valueOf(id + ".")))
                    .findFirst()
                    .ifPresent(file -> {
                        try {
                            Files.delete(file);
                            log.info("Deleted file: " + file);
                        } catch (IOException e) {
                            log.error("Error deleting file", e);
                        }
                    });
        } catch (IOException e) {
            log.error("Error accessing image directory", e);
            throw new RuntimeException(e);
        }

        return "Employee and image deleted successfully.";
    }

    @GetMapping("/getImage/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable int id) {
        Optional<Employee> employee = employeeRepo.findById(id);
        if (employee.isPresent() && employee.get().getImgName() != null) {
            String imgName = employee.get().getImgName();
            Path imgPath = Paths.get(UPLOAD_DIR, imgName);

            try {
                UrlResource resource = new UrlResource(imgPath.toUri());
                String contentType = Files.probeContentType(imgPath);

                if (resource.exists() && resource.isReadable()) {
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + imgName + "\"")
                            .contentType(MediaType.parseMediaType(contentType))
                            .body(resource);
                }
            } catch (MalformedURLException e) {
                log.error("Malformed URL for image: " + imgName, e);
            } catch (IOException e) {
                log.error("Error reading image file: " + imgName, e);
            }
        }

        return ResponseEntity.notFound().build();
    }

    // Helper method to extract file extension
    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

    // Helper method to save image to directory
    private void saveImage(MultipartFile profile, String imageName) throws IOException {
        Path targetPath = Paths.get(UPLOAD_DIR, imageName);
        Files.copy(profile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
