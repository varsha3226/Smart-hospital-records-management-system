package com.hospital;

/**
 * Prescription model.
 *
 * Faults fixed
 * ─────────────
 * 1. No null guards in constructor — null fields caused NullPointerException
 *    in toData() when calling .replace() on a null string.
 * 2. No guard in fromData() — any corrupt field silently left as empty string
 *    (acceptable) but no per-field try-catch for safety.
 */
public class Prescription {
    private String id;
    private String appointmentId;
    private String patientId;
    private String doctorId;
    private String diagnosis;
    private String medicines;
    private String duration;
    private String followUpDate;
    private String doctorSignature;

    public Prescription(String id, String appointmentId, String patientId, String doctorId,
                        String diagnosis, String medicines, String duration,
                        String followUpDate, String doctorSignature) {
        this.id              = id              != null ? id              : "";
        this.appointmentId   = appointmentId   != null ? appointmentId   : "";
        this.patientId       = patientId       != null ? patientId       : "";
        this.doctorId        = doctorId        != null ? doctorId        : "";
        this.diagnosis       = diagnosis       != null ? diagnosis       : "";
        this.medicines       = medicines       != null ? medicines       : "";
        this.duration        = duration        != null ? duration        : "";
        this.followUpDate    = followUpDate    != null ? followUpDate    : "";
        this.doctorSignature = doctorSignature != null ? doctorSignature : "";
    }

    public String getId()              { return id; }
    public String getAppointmentId()   { return appointmentId; }
    public String getPatientId()       { return patientId; }
    public String getDoctorId()        { return doctorId; }
    public String getDiagnosis()       { return diagnosis; }
    public String getMedicines()       { return medicines; }
    public String getDuration()        { return duration; }
    public String getFollowUpDate()    { return followUpDate; }
    public String getDoctorSignature() { return doctorSignature; }

    public static Prescription fromData(String line) {
        String[] parts = line.split(";");
        String id = "", appointmentId = "", patientId = "", doctorId = "",
               diagnosis = "", medicines = "", duration = "", follow = "", sig = "";
        for (String p : parts) {
            String[] kv = p.split("=", 2);
            if (kv.length != 2) continue;
            String k = kv[0].trim();
            String v = kv[1].trim();
            switch (k) {
                case "id"              -> id            = v;
                case "appointmentId"   -> appointmentId = v;
                case "patientId"       -> patientId     = v;
                case "doctorId"        -> doctorId      = v;
                case "diagnosis"       -> diagnosis     = v;
                case "medicines"       -> medicines     = v;
                case "duration"        -> duration      = v;
                case "followUpDate"    -> follow        = v;
                case "doctorSignature" -> sig           = v;
            }
        }
        return new Prescription(id, appointmentId, patientId, doctorId,
                                diagnosis, medicines, duration, follow, sig);
    }

    public String toData() {
        return "id="              + id +
               ";appointmentId="  + appointmentId +
               ";patientId="      + patientId +
               ";doctorId="       + doctorId +
               ";diagnosis="      + diagnosis.replace(";", ",") +
               ";medicines="      + medicines.replace(";", ",") +
               ";duration="       + duration +
               ";followUpDate="   + followUpDate +
               ";doctorSignature="+ doctorSignature;
    }
}
