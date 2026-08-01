package com.hospital;

import java.io.*;
import java.util.*;

/**
 * DataStore – single source of truth for all file I/O.
 *
 * Faults fixed
 * ─────────────
 * 1. nextDoctorId/nextPatientId used list.size()+1 → ID COLLISION after any deletion.
 *    Fixed: scan max numeric suffix.
 * 2. nextBillId/nextPrescriptionId had no null/format guard → crash on bad line.
 * 3. Every loadX() had no per-line crash guard → one bad line kills entire load.
 *    Fixed: try-catch per line, skip + log bad lines.
 * 4. saveBills() wrote a daily backup on every single save → silent disk fill. Removed.
 * 5. FileReader/FileWriter used platform charset → garbled ₹ and special chars.
 *    Fixed: explicit UTF-8 everywhere.
 * 6. appendAppointment used a throwaway List<> – replaced with singletonList.
 * 7. Stale comment ("REPLACE THE WHOLE BROKEN BLOCK") removed.
 */
public class DataStore {

    private static final String DATA_DIR          = "data";
    private static final String DOCTORS_FILE      = DATA_DIR + "/doctors.txt";
    private static final String PATIENTS_FILE     = DATA_DIR + "/patients.txt";
    private static final String APPOINTMENTS_FILE = DATA_DIR + "/appointments.txt";
    private static final String BILLS_FILE        = DATA_DIR + "/bills.txt";
    private static final String FEEDBACK_FILE     = DATA_DIR + "/feedback.txt";
    private static final String PRES_FILE         = DATA_DIR + "/prescriptions.txt";
    private static final String LOGS_FILE         = DATA_DIR + "/logs.txt";

    static {
        ensureDataDir();
        ensureSampleData();
    }

    private static void ensureDataDir() {
        new File(DATA_DIR).mkdirs();
    }

    private static void ensureSampleData() {
        try {
            File df = new File(DOCTORS_FILE);
            if (!df.exists() || df.length() == 0) {
                List<Doctor> sample = new ArrayList<>();
                sample.add(new Doctor("DOC1", "Dr. Arjun Reddy",  "arjun", "pass123",
                        "Cardiology", "MBBS, MD", 7, "MORNING", true, 4.5, 10));
                sample.add(new Doctor("DOC2", "Dr. Meera Sharma", "meera", "pass123",
                        "Neurology",  "MBBS, DM", 5, "EVENING", true, 4.7, 8));
                saveDoctors(sample);
            }
            File pf = new File(PATIENTS_FILE);
            if (!pf.exists() || pf.length() == 0) {
                List<Patient> sample = new ArrayList<>();
                sample.add(new Patient("PAT1","Ravi Kumar","9999999999","ravi","pass123",30,"Male","Chest pain and shortness of breath"));
                sample.add(new Patient("PAT2","Sita Devi","8888888888","sita","pass123",25,"Female","Severe headache and nausea"));
                sample.add(new Patient("PAT3","Amit Sharma","7777777777","amit","pass123",42,"Male","Joint pain"));
                sample.add(new Patient("PAT4","Priya Singh","6666666666","priya","pass123",28,"Female","Fever and fatigue"));
                savePatients(sample);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── I/O helpers (UTF-8) ───────────────────────────────────────────
    private static List<String> readLines(String filename) {
        List<String> lines = new ArrayList<>();
        File file = new File(filename);
        if (!file.exists()) return lines;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) lines.add(line);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lines;
    }

    private static void writeLines(String filename, List<String> lines, boolean append) {
        File file = new File(filename);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, append), java.nio.charset.StandardCharsets.UTF_8))) {
            for (String line : lines) { bw.write(line); bw.newLine(); }
            bw.flush();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── DOCTORS ───────────────────────────────────────────────────────
    public static List<Doctor> loadDoctors() {
        List<Doctor> result = new ArrayList<>();
        for (String line : readLines(DOCTORS_FILE)) {
            try { result.add(Doctor.fromData(line)); }
            catch (Exception e) { System.err.println("Bad doctor line skipped: " + line); }
        }
        return result;
    }
    public static void saveDoctors(List<Doctor> doctors) {
        List<String> lines = new ArrayList<>();
        for (Doctor d : doctors) lines.add(d.toData());
        writeLines(DOCTORS_FILE, lines, false);
    }

    // ── PATIENTS ──────────────────────────────────────────────────────
    public static List<Patient> loadPatients() {
        List<Patient> result = new ArrayList<>();
        for (String line : readLines(PATIENTS_FILE)) {
            try { result.add(Patient.fromData(line)); }
            catch (Exception e) { System.err.println("Bad patient line skipped: " + line); }
        }
        return result;
    }
    public static void savePatients(List<Patient> list) {
        List<String> lines = new ArrayList<>();
        for (Patient p : list) lines.add(p.toData());
        writeLines(PATIENTS_FILE, lines, false);
    }

    // ── APPOINTMENTS ──────────────────────────────────────────────────
    public static List<Appointment> loadAppointments() {
        List<Appointment> result = new ArrayList<>();
        for (String line : readLines(APPOINTMENTS_FILE)) {
            try { result.add(Appointment.fromData(line)); }
            catch (Exception e) { System.err.println("Bad appointment line skipped: " + line); }
        }
        return result;
    }
    public static void saveAppointments(List<Appointment> list) {
        List<String> lines = new ArrayList<>();
        for (Appointment a : list) lines.add(a.toData());
        writeLines(APPOINTMENTS_FILE, lines, false);
    }
    public static void appendAppointment(Appointment apt) {
        writeLines(APPOINTMENTS_FILE, Collections.singletonList(apt.toData()), true);
    }

    // ── BILLS ─────────────────────────────────────────────────────────
    public static List<Bill> loadBills() {
        List<Bill> result = new ArrayList<>();
        for (String line : readLines(BILLS_FILE)) {
            try { result.add(Bill.fromData(line)); }
            catch (Exception e) { System.err.println("Bad bill line skipped: " + line); }
        }
        return result;
    }
    public static void saveBills(List<Bill> list) {
        List<String> lines = new ArrayList<>();
        for (Bill b : list) lines.add(b.toData());
        writeLines(BILLS_FILE, lines, false);
        // Daily backup removed: was writing on EVERY save, silently filling disk.
    }
    public static void appendBill(Bill b) {
        writeLines(BILLS_FILE, Collections.singletonList(b.toData()), true);
    }

    // ── FEEDBACK ──────────────────────────────────────────────────────
    public static List<Feedback> loadFeedback() {
        List<Feedback> result = new ArrayList<>();
        for (String line : readLines(FEEDBACK_FILE)) {
            try { result.add(Feedback.fromData(line)); }
            catch (Exception e) { System.err.println("Bad feedback line skipped: " + line); }
        }
        return result;
    }
    public static void appendFeedback(Feedback fb) {
        writeLines(FEEDBACK_FILE, Collections.singletonList(fb.toData()), true);
    }

    // ── PRESCRIPTIONS ─────────────────────────────────────────────────
    public static List<Prescription> loadPrescriptions() {
        List<Prescription> result = new ArrayList<>();
        for (String line : readLines(PRES_FILE)) {
            try { result.add(Prescription.fromData(line)); }
            catch (Exception e) { System.err.println("Bad prescription line skipped: " + line); }
        }
        return result;
    }
    public static void appendPrescription(Prescription p) {
        writeLines(PRES_FILE, Collections.singletonList(p.toData()), true);
    }

    // ── LOGS ──────────────────────────────────────────────────────────
    public static void appendLog(TransactionLog log) {
        writeLines(LOGS_FILE, Collections.singletonList(log.toData()), true);
    }

    // ── ID generators (max-scan, collision-safe after deletions) ──────
    private static int maxSuffix(List<String> ids, String prefix) {
        return ids.stream()
            .filter(id -> id != null && id.startsWith(prefix))
            .mapToInt(id -> { try { return Integer.parseInt(id.substring(prefix.length())); } catch (Exception e) { return 0; } })
            .max().orElse(0);
    }

    public static String nextDoctorId() {
        List<String> ids = new ArrayList<>();
        loadDoctors().forEach(d -> ids.add(d.getId()));
        return "DOC" + (maxSuffix(ids, "DOC") + 1);
    }
    public static String nextPatientId() {
        List<String> ids = new ArrayList<>();
        loadPatients().forEach(p -> ids.add(p.getId()));
        return "PAT" + (maxSuffix(ids, "PAT") + 1);
    }
    public static String nextAppointmentId() {
        List<String> ids = new ArrayList<>();
        loadAppointments().forEach(a -> ids.add(a.getId()));
        return "APT" + String.format("%03d", maxSuffix(ids, "APT") + 1);
    }
    public static String nextBillId() {
        List<String> ids = new ArrayList<>();
        loadBills().forEach(b -> ids.add(b.getId()));
        return "BILL" + (maxSuffix(ids, "BILL") + 1);
    }
    public static String nextPrescriptionId() {
        List<String> ids = new ArrayList<>();
        loadPrescriptions().forEach(p -> ids.add(p.getId()));
        return "PRS" + (maxSuffix(ids, "PRS") + 1);
    }
}
