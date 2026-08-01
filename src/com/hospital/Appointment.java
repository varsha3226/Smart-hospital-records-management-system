package com.hospital;

/**
 * Appointment model.
 *
 * Faults fixed
 * ─────────────
 * 1. fromData() called AppointmentStatus.valueOf(v) with no try-catch.
 *    An unknown/corrupt status string throws IllegalArgumentException and
 *    crashes the entire loadAppointments() call.  Fixed: catch and default to REQUESTED.
 * 2. toData() did symptoms.replace(";",",") but did NOT restore "," back to ";"
 *    on read (fromData just stores the raw value with commas).  This is
 *    asymmetric but acceptable as long as it is consistent – added a note.
 * 3. No setSymptoms() setter even though symptoms can be updated.  Added.
 * 4. No setDate() setter – needed for rescheduling feature.  Added.
 */
public class Appointment {
    private String id;
    private String patientId;
    private String doctorId;
    private String department;
    private String date;       // yyyy-MM-dd
    private String timeSlot;
    private String symptoms;
    private AppointmentStatus status;

    public Appointment(String id, String patientId, String doctorId,
                       String department, String date, String timeSlot,
                       String symptoms, AppointmentStatus status) {
        this.id         = id;
        this.patientId  = patientId;
        this.doctorId   = doctorId;
        this.department = department;
        this.date       = date;
        this.timeSlot   = timeSlot;
        this.symptoms   = (symptoms != null) ? symptoms : "";
        this.status     = (status   != null) ? status   : AppointmentStatus.REQUESTED;
    }

    // Getters
    public String            getId()         { return id; }
    public String            getPatientId()  { return patientId; }
    public String            getDoctorId()   { return doctorId; }
    public String            getDepartment() { return department; }
    public String            getDate()       { return date; }
    public String            getTimeSlot()   { return timeSlot; }
    public String            getSymptoms()   { return symptoms; }
    public AppointmentStatus getStatus()     { return status; }

    // Setters
    public void setDoctorId(String doctorId)         { this.doctorId = doctorId; }
    public void setStatus(AppointmentStatus status)  { this.status   = status; }
    public void setSymptoms(String symptoms)         { this.symptoms  = symptoms; }
    public void setDate(String date)                 { this.date      = date; }

    public static Appointment fromData(String line) {
        String[] parts = line.split(";");
        String id = "", patientId = "", doctorId = "", department = "",
               date = "", timeSlot = "", symptoms = "";
        AppointmentStatus status = AppointmentStatus.REQUESTED;

        for (String p : parts) {
            String[] kv = p.split("=", 2);
            if (kv.length != 2) continue;
            String k = kv[0].trim();
            String v = kv[1].trim();
            switch (k) {
                case "id"         -> id         = v;
                case "patientId"  -> patientId  = v;
                case "doctorId"   -> doctorId   = v;
                case "department" -> department = v;
                case "date"       -> date       = v;
                case "timeSlot"   -> timeSlot   = v;
                case "symptoms"   -> symptoms   = v;
                case "status"     -> {
                    try { status = AppointmentStatus.valueOf(v); }
                    catch (IllegalArgumentException ex) {
                        System.err.println("Unknown appointment status '" + v + "', defaulting to REQUESTED");
                    }
                }
            }
        }
        return new Appointment(id, patientId, doctorId, department, date, timeSlot, symptoms, status);
    }

    public String toData() {
        return "id="         + id +
               ";patientId=" + patientId +
               ";doctorId="  + doctorId +
               ";department="+ department +
               ";date="      + date +
               ";timeSlot="  + timeSlot +
               ";symptoms="  + symptoms.replace(";", ",") +
               ";status="    + status.name();
    }

    @Override
    public String toString() {
        return id + " | " + date + " | " + timeSlot;
    }
}
