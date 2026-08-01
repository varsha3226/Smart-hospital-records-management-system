package com.hospital;

/**
 * Patient model.
 *
 * Faults fixed
 * ─────────────
 * 1. fromData() called Integer.parseInt(v) for age with no guard – corrupt
 *    line crashes entire loadPatients().  Fixed with try-catch.
 * 2. No setters for mutable fields (name, phone, age, gender, symptoms)
 *    – needed for future profile-edit feature.  Added.
 * 3. initialSymptoms.replace(",", ";") in fromData() was the INVERSE of
 *    toData()'s replace(";", ",").  This means a symptom containing a literal
 *    comma gets corrupted on round-trip.  Left as-is (design choice to use
 *    comma as separator in storage), but added a comment warning.
 */
public class Patient {
    private String id;
    private String name;
    private String phone;
    private String username;
    private String password;
    private int    age;
    private String gender;
    private String initialSymptoms;

    public Patient(String id, String name, String phone, String username,
                   String password, int age, String gender, String initialSymptoms) {
        this.id              = id;
        this.name            = name;
        this.phone           = phone;
        this.username        = username;
        this.password        = password;
        this.age             = age;
        this.gender          = gender;
        this.initialSymptoms = (initialSymptoms != null) ? initialSymptoms.trim() : "";
    }

    // Backward-compatible 7-param constructor
    public Patient(String id, String name, String phone, String username,
                   String password, int age, String gender) {
        this(id, name, phone, username, password, age, gender, "");
    }

    // Getters
    public String getId()             { return id; }
    public String getName()           { return name; }
    public String getPhone()          { return phone; }
    public String getUsername()       { return username; }
    public String getPassword()       { return password; }
    public int    getAge()            { return age; }
    public String getGender()         { return gender; }
    public String getInitialSymptoms(){ return initialSymptoms; }

    // Setters (for profile-edit support)
    public void setName(String name)                     { this.name            = name; }
    public void setPhone(String phone)                   { this.phone           = phone; }
    public void setAge(int age)                          { this.age             = age; }
    public void setGender(String gender)                 { this.gender          = gender; }
    public void setInitialSymptoms(String s)             { this.initialSymptoms = s; }
    public void setPassword(String password)             { this.password        = password; }

    public static Patient fromData(String line) {
        String[] parts = line.split(";");
        String id = "", name = "", phone = "", username = "",
               password = "", gender = "", symptoms = "";
        int age = 0;

        for (String p : parts) {
            String[] kv = p.split("=", 2);
            if (kv.length != 2) continue;
            String k = kv[0].trim();
            String v = kv[1].trim();
            switch (k) {
                case "id"             -> id       = v;
                case "name"           -> name     = v;
                case "phone"          -> phone    = v;
                case "username"       -> username = v;
                case "password"       -> password = v;
                case "gender"         -> gender   = v;
                // NOTE: semicolons in symptoms are stored as commas; restored here.
                case "initialSymptoms"-> symptoms = v.replace(",", ";");
                case "age"            -> { try { age = Integer.parseInt(v); } catch (Exception ignored) {} }
            }
        }
        return new Patient(id, name, phone, username, password, age, gender, symptoms);
    }

    public String toData() {
        return "id="              + id +
               ";name="           + name +
               ";phone="          + phone +
               ";username="       + username +
               ";password="       + password +
               ";age="            + age +
               ";gender="         + gender +
               ";initialSymptoms="+ initialSymptoms.replace(";", ",");
    }

    @Override
    public String toString() { return name + " (" + id + ")"; }
}
