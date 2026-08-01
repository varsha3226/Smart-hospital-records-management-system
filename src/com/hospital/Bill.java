package com.hospital;

/**
 * Bill model.
 *
 * Faults fixed
 * ─────────────
 * 1. fromData() called Double.parseDouble() with no guard – corrupt line
 *    crashes entire loadBills().  Fixed per-field.
 * 2. taxAmount was stored AND loaded but is always derivable as
 *    baseAmount * taxPercent / 100. Kept for backward-compat but noted.
 * 3. No validation that totalAmount == baseAmount + taxAmount.  Added
 *    a static factory method that calculates correctly.
 * B7. isPaid flag added — previously "Mark as Paid" only tracked in an
 *    in-memory HashSet which was wiped on re-login.  Now persisted to file.
 */
public class Bill {
    private String id;
    private String appointmentId;
    private String patientId;
    private String doctorId;
    private double baseAmount;
    private double taxPercent;
    private double taxAmount;
    private double totalAmount;
    private String createdAt;
    private boolean paid;   // B7: persisted paid flag

    public Bill(String id, String appointmentId, String patientId, String doctorId,
                double baseAmount, double taxPercent, double taxAmount,
                double totalAmount, String createdAt) {
        this(id, appointmentId, patientId, doctorId,
             baseAmount, taxPercent, taxAmount, totalAmount, createdAt, false);
    }

    public Bill(String id, String appointmentId, String patientId, String doctorId,
                double baseAmount, double taxPercent, double taxAmount,
                double totalAmount, String createdAt, boolean paid) {
        this.id            = id;
        this.appointmentId = appointmentId;
        this.patientId     = patientId;
        this.doctorId      = doctorId;
        this.baseAmount    = baseAmount;
        this.taxPercent    = taxPercent;
        this.taxAmount     = taxAmount;
        this.totalAmount   = totalAmount;
        this.createdAt     = createdAt;
        this.paid          = paid;
    }

    /** Factory: calculates tax and total automatically. */
    public static Bill create(String id, String appointmentId, String patientId,
                              String doctorId, double baseAmount, double taxPercent,
                              String createdAt) {
        double taxAmt   = Math.round(baseAmount * taxPercent) / 100.0;
        double total    = Math.round((baseAmount + taxAmt) * 100.0) / 100.0;
        return new Bill(id, appointmentId, patientId, doctorId,
                        baseAmount, taxPercent, taxAmt, total, createdAt, false);
    }

    // Getters
    public String getId()            { return id; }
    public String getAppointmentId() { return appointmentId; }
    public String getPatientId()     { return patientId; }
    public String getDoctorId()      { return doctorId; }
    public double getBaseAmount()    { return baseAmount; }
    public double getTaxPercent()    { return taxPercent; }
    public double getTaxAmount()     { return taxAmount; }
    public double getTotalAmount()   { return totalAmount; }
    public String getCreatedAt()     { return createdAt; }
    public boolean isPaid()          { return paid; }

    // B7: setter for marking paid
    public void setPaid(boolean paid) { this.paid = paid; }

    public static Bill fromData(String line) {
        String[] parts = line.split(";");
        String id = "", appointmentId = "", patientId = "", doctorId = "", createdAt = "";
        double base = 0, taxPct = 0, taxAmt = 0, total = 0;
        boolean paid = false;

        for (String p : parts) {
            String[] kv = p.split("=", 2);
            if (kv.length != 2) continue;
            String k = kv[0].trim();
            String v = kv[1].trim();
            switch (k) {
                case "id"            -> id            = v;
                case "appointmentId" -> appointmentId = v;
                case "patientId"     -> patientId     = v;
                case "doctorId"      -> doctorId      = v;
                case "createdAt"     -> createdAt     = v;
                case "paid"          -> paid          = Boolean.parseBoolean(v);
                case "baseAmount"    -> { try { base   = Double.parseDouble(v); } catch (Exception ignored) {} }
                case "taxPercent"    -> { try { taxPct = Double.parseDouble(v); } catch (Exception ignored) {} }
                case "taxAmount"     -> { try { taxAmt = Double.parseDouble(v); } catch (Exception ignored) {} }
                case "totalAmount"   -> { try { total  = Double.parseDouble(v); } catch (Exception ignored) {} }
            }
        }
        return new Bill(id, appointmentId, patientId, doctorId,
                        base, taxPct, taxAmt, total, createdAt, paid);
    }

    public String toData() {
        return "id="            + id +
               ";appointmentId="+ appointmentId +
               ";patientId="    + patientId +
               ";doctorId="     + doctorId +
               ";baseAmount="   + baseAmount +
               ";taxPercent="   + taxPercent +
               ";taxAmount="    + taxAmount +
               ";totalAmount="  + totalAmount +
               ";createdAt="    + createdAt +
               ";paid="         + paid;   // B7: persisted
    }
}
