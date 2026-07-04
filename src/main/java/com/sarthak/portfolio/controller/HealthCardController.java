package com.sarthak.portfolio.controller;

import com.sarthak.portfolio.model.MedicalRecord;
import com.sarthak.portfolio.model.Patient;
import com.sarthak.portfolio.repository.MedicalRecordRepository;
import com.sarthak.portfolio.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/health-cards")
@CrossOrigin(origins = "*")
public class HealthCardController {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    // GET /api/health-cards/stats/summary - Aggregate metrics for registration analytics
    @GetMapping("/stats/summary")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        Map<String, Object> response = new HashMap<>();
        List<Patient> patients = patientRepository.findAll();

        Map<String, Integer> bloodGroupStats = new HashMap<>();
        Map<String, Integer> genderStats = new HashMap<>();
        genderStats.put("Male", 0);
        genderStats.put("Female", 0);
        genderStats.put("Other", 0);

        for (Patient p : patients) {
            // Group by blood type
            bloodGroupStats.put(p.getBloodGroup(), bloodGroupStats.getOrDefault(p.getBloodGroup(), 0) + 1);
            
            // Group by gender
            String gender = p.getGender();
            if (genderStats.containsKey(gender)) {
                genderStats.put(gender, genderStats.get(gender) + 1);
            } else {
                genderStats.put(gender, genderStats.getOrDefault(gender, 0) + 1);
            }
        }

        // Fetch recent 5 patients sorted by creation date descending
        List<Patient> recent = patients.stream()
                .sorted(Comparator.comparing(Patient::getCreatedAt).reversed())
                .limit(5)
                .collect(Collectors.toList());

        Map<String, Object> statsData = new HashMap<>();
        statsData.put("totalCards", patients.size());
        statsData.put("bloodGroupStats", bloodGroupStats);
        statsData.put("genderStats", genderStats);
        statsData.put("recentPatients", recent);

        response.put("success", true);
        response.put("data", statsData);

        return ResponseEntity.ok(response);
    }

    // POST /api/health-cards - Enroll a new patient and issue a card ID
    @PostMapping
    public ResponseEntity<Map<String, Object>> registerPatient(@RequestBody Patient patient) {
        Map<String, Object> response = new HashMap<>();

        if (patient.getName() == null || patient.getName().trim().isEmpty() ||
            patient.getDob() == null || patient.getDob().trim().isEmpty() ||
            patient.getGender() == null || patient.getGender().trim().isEmpty() ||
            patient.getBloodGroup() == null || patient.getBloodGroup().trim().isEmpty() ||
            patient.getPhone() == null || patient.getPhone().trim().isEmpty()) {
            
            response.put("error", "Missing required patient fields (name, dob, gender, bloodGroup, phone).");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Generate unique card ID format: HC-YYYY-XXXX (e.g. HC-2026-AE54)
        int currentYear = Year.now().getValue();
        String uniqueId = "";
        boolean isUnique = false;

        while (!isUnique) {
            String randHex = String.format("%04X", (int) (Math.random() * 0x10000));
            uniqueId = "HC-" + currentYear + "-" + randHex;
            isUnique = !patientRepository.existsById(uniqueId);
        }

        patient.setId(uniqueId);
        patient.setCreatedAt(LocalDateTime.now());
        
        // Defaults for clinical fields
        if (patient.getAllergies() == null || patient.getAllergies().trim().isEmpty()) patient.setAllergies("None");
        if (patient.getChronicDiseases() == null || patient.getChronicDiseases().trim().isEmpty()) patient.setChronicDiseases("None");
        if (patient.getMedications() == null || patient.getMedications().trim().isEmpty()) patient.setMedications("None");

        Patient saved = patientRepository.save(patient);

        response.put("success", true);
        response.put("message", "Health card created successfully!");
        response.put("data", saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/health-cards/{id} - Get card details
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPatientCard(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        Optional<Patient> patientOpt = patientRepository.findById(id);

        if (patientOpt.isEmpty()) {
            response.put("error", "Health card ID \"" + id + "\" not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        response.put("success", true);
        response.put("data", patientOpt.get());
        return ResponseEntity.ok(response);
    }

    // GET /api/health-cards/{id}/records - Retrieve medical visit history
    @GetMapping("/{id}/records")
    public ResponseEntity<Map<String, Object>> getMedicalHistory(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        
        if (!patientRepository.existsById(id)) {
            response.put("error", "Health card ID \"" + id + "\" not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        List<MedicalRecord> records = medicalRecordRepository.findByPatientIdOrderByTimestampDesc(id);
        
        response.put("success", true);
        response.put("data", records);
        return ResponseEntity.ok(response);
    }

    // POST /api/health-cards/{id}/records - Doctors add prescription consult log
    @PostMapping("/{id}/records")
    public ResponseEntity<Map<String, Object>> addMedicalRecord(@PathVariable String id, @RequestBody MedicalRecord record) {
        Map<String, Object> response = new HashMap<>();

        if (record.getDiagnosis() == null || record.getDiagnosis().trim().isEmpty() ||
            record.getPrescription() == null || record.getPrescription().trim().isEmpty()) {
            
            response.put("error", "Diagnosis and prescription are required field data.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (!patientRepository.existsById(id)) {
            response.put("error", "Health card ID \"" + id + "\" not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        record.setPatientId(id);
        record.setTimestamp(LocalDateTime.now());
        
        if (record.getDoctorName() == null || record.getDoctorName().trim().isEmpty()) {
            record.setDoctorName("General Practitioner");
        }
        if (record.getHospitalName() == null || record.getHospitalName().trim().isEmpty()) {
            record.setHospitalName("Central Clinic Hub");
        }

        MedicalRecord saved = medicalRecordRepository.save(record);

        response.put("success", true);
        response.put("message", "Medical record added successfully!");
        response.put("data", saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
