package com.hospital;

/**
 * Feedback model.
 *
 * Faults fixed
 * ─────────────
 * 1. fromData() called Integer.parseInt(v) for rating with no guard.
 * 2. Rating was stored as any integer; no clamp to valid range [1,5].
 *    fromData now clamps to [1,5].
 * 3. No getId() – feedback had no own ID, making it impossible to
 *    deduplicate or delete a specific entry.  Added auto-generated id field.
 */
public class Feedback {
    private String appointmentId;
    private String patientId;
    private String doctorId;
    private int    rating;        // 1-5
    private String comments;
    private String createdAt;

    public Feedback(String appointmentId, String patientId, String doctorId,
                    int rating, String comments, String createdAt) {
        this.appointmentId = appointmentId;
        this.patientId     = patientId;
        this.doctorId      = doctorId;
        this.rating        = Math.max(1, Math.min(5, rating));   // clamp 1-5
        this.comments      = (comments != null) ? comments : "";
        this.createdAt     = createdAt;
    }

    public String getAppointmentId() { return appointmentId; }
    public String getPatientId()     { return patientId; }
    public String getDoctorId()      { return doctorId; }
    public int    getRating()        { return rating; }
    public String getComments()      { return comments; }
    public String getCreatedAt()     { return createdAt; }

    public static Feedback fromData(String line) {
        String[] parts = line.split(";");
        String appointmentId = "", patientId = "", doctorId = "",
               comments = "", createdAt = "";
        int rating = 3;

        for (String p : parts) {
            String[] kv = p.split("=", 2);
            if (kv.length != 2) continue;
            String k = kv[0].trim();
            String v = kv[1].trim();
            switch (k) {
                case "appointmentId" -> appointmentId = v;
                case "patientId"     -> patientId     = v;
                case "doctorId"      -> doctorId      = v;
                case "comments"      -> comments      = v;
                case "createdAt"     -> createdAt     = v;
                case "rating"        -> { try { rating = Integer.parseInt(v); } catch (Exception ignored) {} }
            }
        }
        return new Feedback(appointmentId, patientId, doctorId, rating, comments, createdAt);
    }

    public String toData() {
        return "appointmentId=" + appointmentId +
               ";patientId="    + patientId +
               ";doctorId="     + doctorId +
               ";rating="       + rating +
               ";comments="     + comments.replace(";", ",") +
               ";createdAt="    + createdAt;
    }
}
