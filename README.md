# 🏥 SHRMS — Smart Hospital Resource Management System

A desktop-based Hospital Management System built with **Java Swing**, featuring role-based dashboards for Admins, Doctors, and Patients. All data is stored in plain-text flat files — no database required.

---

## 📋 Table of Contents

- [Features](#-features)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [How to Run](#-how-to-run)
- [Default Login Credentials](#-default-login-credentials)
- [Data Files](#-data-files)
- [Departments Covered](#-departments-covered)
- [Screenshots Overview](#-screenshots-overview)

---

## ✨ Features

### 👨‍💼 Admin Dashboard
- Add, edit, toggle active/inactive, and remove doctors
- Register new patients
- Approve or reject appointment requests
- Mark bills as paid
- View all prescriptions with detailed dialog
- Analytics dashboard — total doctors, patients, appointments, revenue, average rating
- Live search/filter on doctor and patient tables

### 🩺 Doctor Dashboard
- View assigned appointments with patient details
- Accept, reject, or mark appointments as completed
- Create prescriptions for completed appointments (multi-line diagnosis and medicines)
- Profile card showing qualification, experience, shift, rating, and patient count

### 🧑‍⚕️ Patient Dashboard
- Book appointments by department and doctor with real-time slot availability check
- View full appointment history with status labels
- Submit star-rated feedback for completed appointments
- Profile overview showing appointment statistics

### 🔐 Authentication
- Role-based login — Admin, Doctor, Patient
- Session management with clean logout and toast notification

---

## 📁 Project Structure

```
SHRMS/
│
├── src/
│   └── com/
│       └── hospital/
│           ├── MainWindow.java            # App entry point, JFrame host
│           ├── LoginPanel.java            # Login screen for all roles
│           ├── HomePanel.java             # Public landing/home screen
│           ├── AdminDashboardPanel.java   # Full admin management UI
│           ├── DoctorDashboardPanel.java  # Doctor appointments & prescriptions
│           ├── PatientDashboardPanel.java # Patient booking, history, feedback
│           │
│           ├── UIUtils.java              # Design system: colours, fonts, dialogs, tables
│           │
│           ├── Doctor.java               # Doctor model
│           ├── Patient.java              # Patient model
│           ├── Appointment.java          # Appointment model
│           ├── Bill.java                 # Billing model
│           ├── Prescription.java         # Prescription model
│           ├── Feedback.java             # Feedback/rating model
│           ├── TransactionLog.java       # Audit log model
│           ├── AppointmentStatus.java    # Enum: REQUESTED → COMPLETED flow
│           ├── Role.java                 # Enum: ADMIN, DOCTOR, PATIENT
│           │
│           └── DataStore.java            # All file I/O — load, save, append, ID generation
│
└── data/                                 # Flat-file database (auto-created on first run)
    ├── doctors.txt
    ├── patients.txt
    ├── appointments.txt
    ├── bills.txt
    ├── prescriptions.txt
    ├── feedback.txt
    └── logs.txt
```

---

## ✅ Prerequisites

| Requirement | Version |
|-------------|---------|
| Java JDK    | 11 or higher |
| OS          | Windows / macOS / Linux |

No external libraries or frameworks are needed. The project uses only the Java standard library and Swing.

---

## 🚀 How to Run

### Option 1 — Command Line (Recommended)

**Step 1 — Clone the repository**
```bash
git clone https://github.com/your-username/SHRMS.git
cd SHRMS
```

**Step 2 — Compile all source files**
```bash
javac -d out src/com/hospital/*.java
```

**Step 3 — Run the application**
```bash
java -cp out com.hospital.MainWindow
```

> The `data/` folder is created automatically in your working directory on first launch.

---

### Option 2 — IntelliJ IDEA

1. Open IntelliJ → **File → Open** → select the `SHRMS` folder
2. Right-click `src/` → **Mark Directory as → Sources Root**
3. Open `MainWindow.java`
4. Click the green ▶ **Run** button next to the `main()` method

---

### Option 3 — Eclipse

1. **File → New → Java Project** → uncheck "Use default location" → point to the `SHRMS` folder
2. Right-click the project → **Build Path → Configure Build Path** → confirm `src` is listed as a source folder
3. Open `MainWindow.java`
4. **Run → Run As → Java Application**

---

### Option 4 — VS Code

1. Install the **Extension Pack for Java** from the VS Code marketplace
2. Open the `SHRMS` folder: **File → Open Folder**
3. Open `MainWindow.java`
4. Click **Run** above the `main()` method or press `F5`

---

### Option 5 — Build a runnable JAR

```bash
# Compile
javac -d out src/com/hospital/*.java

# Package
jar cfe SHRMS.jar com.hospital.MainWindow -C out .

# Run
java -jar SHRMS.jar
```

> ⚠️ Make sure the `data/` folder is in the **same directory** as where you run the JAR from.

---

## 🔑 Default Login Credentials

| Role    | Username  | Password   |
|---------|-----------|------------|
| Admin   | `admin`   | `admin123` |
| Doctor  | *(any username from doctors.txt)* | *(matching password)* |
| Patient | *(any username from patients.txt)* | *(matching password)* |

**Sample doctor login:** `arjun.reddy` / `doc1pass`  
**Sample patient login:** `ravi.kumar` / `rpass1`

---

## 🗂️ Data Files

All data is stored in the `data/` directory as semicolon-delimited text files. Place the pre-populated sample files in a `data/` folder next to your compiled output before running if you want demo data loaded at startup.

| File | Description |
|------|-------------|
| `doctors.txt` | Doctor profiles with specialty, shift, rating |
| `patients.txt` | Patient records with contact and medical history |
| `appointments.txt` | All bookings with status tracking |
| `bills.txt` | Billing records with base amount, tax, and paid status |
| `prescriptions.txt` | Doctor-issued prescriptions with medicines and follow-up |
| `feedback.txt` | Patient ratings and comments per appointment |
| `logs.txt` | Auto-generated audit trail of all actions |

**File format example (`doctors.txt`):**
```
id=DOC1;name=Dr. Arjun Reddy;username=arjun.reddy;password=doc1pass;department=Cardiology;qualification=MBBS MD DM Cardiology;experienceYears=16;shift=MORNING;active=true;rating=4.8;ratingsCount=320
```

---

## 🏥 Departments Covered

The sample data includes doctors across **33 medical specialties**:

Cardiology · Cardiothoracic Surgery · Neurology · Neurosurgery · Orthopedics · General Surgery · Plastic Surgery · Vascular Surgery · Gynecology · Reproductive Medicine · Neonatology · Pediatrics · Dermatology · Psychiatry · Endocrinology · Ophthalmology · Pulmonology · Nephrology · Gastroenterology · Rheumatology · Oncology · ENT · Urology · Hematology · Radiology · Anesthesiology · Emergency Medicine · Infectious Disease · Geriatrics · Sports Medicine · Palliative Care · Sleep Medicine · Allergy and Immunology

---

## 📸 Screenshots Overview

| Screen | Description |
|--------|-------------|
| Login  | Role selector with credential validation |
| Admin — Doctors | Searchable table, add/edit/remove with styled dialogs |
| Admin — Appointments | Approve/reject with friendly status labels |
| Admin — Billing | Mark as paid, full billing table |
| Admin — Analytics | 6-card stats dashboard |
| Doctor — Appointments | Accept/reject/complete with state guards |
| Doctor — Prescriptions | Multi-line diagnosis form, combobox patient selector |
| Patient — Book | Department → Doctor → Date → Slot with live availability |
| Patient — History | Sortable appointment history |
| Patient — Feedback | 5-star rating with comments |

---

## 🛠️ Tech Stack

- **Language:** Java 11+
- **UI Framework:** Java Swing
- **Storage:** Plain-text flat files (UTF-8, semicolon-delimited)
- **Architecture:** MVC-lite — model classes, DataStore I/O layer, panel-based views
- **No external dependencies**

---

## 📄 License

This project is for educational purposes.
