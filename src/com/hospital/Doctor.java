package com.hospital;

/**
 * Doctor model.
 *
 * Faults fixed
 * ─────────────
 * 1. fromData() had no try-catch around Integer/Double.parseInt – a corrupt
 *    line crashes the whole loadDoctors() call.  Fixed per-field.
 * 2. No setPassword() setter – needed for password-change feature. Added.
 * 3. toString() returned "name (dept)" which is fine for comboboxes, but
 *    omitted the Dr. prefix if the stored name lacks it. Left as-is
 *    (name is stored verbatim as entered).
 * 4. toData() / fromData() used raw semicolon-split; qualification and
 *    department can theoretically contain commas but not semicolons – safe.
 */
public class Doctor {
    private String id;
    private String name;
    private String username;
    private String password;
    private String department;
    private String qualification;
    private int    experienceYears;
    private String shift;          // MORNING / EVENING / NIGHT
    private boolean active;
    private double rating;
    private int    ratingsCount;

    public Doctor(String id, String name, String username, String password,
                  String department, String qualification, int experienceYears,
                  String shift, boolean active, double rating, int ratingsCount) {
        this.id              = id;
        this.name            = name;
        this.username        = username;
        this.password        = password;
        this.department      = department;
        this.qualification   = qualification;
        this.experienceYears = experienceYears;
        this.shift           = shift;
        this.active          = active;
        this.rating          = rating;
        this.ratingsCount    = ratingsCount;
    }

    // Getters
    public String  getId()             { return id; }
    public String  getName()           { return name; }
    public String  getUsername()       { return username; }
    public String  getPassword()       { return password; }
    public String  getDepartment()     { return department; }
    public String  getQualification()  { return qualification; }
    public int     getExperienceYears(){ return experienceYears; }
    public String  getShift()          { return shift; }
    public boolean isActive()          { return active; }
    public double  getRating()         { return rating; }
    public int     getRatingsCount()   { return ratingsCount; }

    // Setters
    public void setName(String name)                  { this.name            = name; }
    public void setPassword(String password)          { this.password        = password; }
    public void setDepartment(String department)      { this.department      = department; }
    public void setQualification(String qualification){ this.qualification   = qualification; }
    public void setExperienceYears(int years)         { this.experienceYears = years; }
    public void setShift(String shift)                { this.shift           = shift; }
    public void setActive(boolean active)             { this.active          = active; }
    public void setRating(double rating)              { this.rating          = rating; }
    public void setRatingsCount(int count)            { this.ratingsCount    = count; }

    public static Doctor fromData(String line) {
        String[] parts = line.split(";");
        String id = "", name = "", username = "", password = "",
               department = "", qualification = "", shift = "MORNING";
        int     exp    = 0;
        boolean active = true;
        double  rating = 0.0;
        int     rc     = 0;

        for (String p : parts) {
            String[] kv = p.split("=", 2);
            if (kv.length != 2) continue;
            String k = kv[0].trim();
            String v = kv[1].trim();
            switch (k) {
                case "id"             -> id            = v;
                case "name"           -> name          = v;
                case "username"       -> username      = v;
                case "password"       -> password      = v;
                case "department"     -> department    = v;
                case "qualification"  -> qualification = v;
                case "shift"          -> shift         = v;
                case "active"         -> active        = Boolean.parseBoolean(v);
                case "experienceYears"-> { try { exp    = Integer.parseInt(v); } catch (Exception ignored) {} }
                case "rating"         -> { try { rating = Double.parseDouble(v); } catch (Exception ignored) {} }
                case "ratingsCount"   -> { try { rc     = Integer.parseInt(v); } catch (Exception ignored) {} }
            }
        }
        return new Doctor(id, name, username, password, department,
                          qualification, exp, shift, active, rating, rc);
    }

    public String toData() {
        return "id="             + id +
               ";name="          + name +
               ";username="      + username +
               ";password="      + password +
               ";department="    + department +
               ";qualification=" + qualification +
               ";experienceYears="+ experienceYears +
               ";shift="         + shift +
               ";active="        + active +
               ";rating="        + rating +
               ";ratingsCount="  + ratingsCount;
    }

    @Override
    public String toString() { return name + " (" + department + ")"; }
}
