# 🏥 Smart Hospital Records System (SHRMS)

A Java Swing-based desktop application for managing hospital operations such as **patients, doctors, appointments, prescriptions, billing, and feedback** using lightweight **file-based storage**.

This project is designed for academic and portfolio purposes and demonstrates **role-based access control, Java Swing UI development, and file handling without using a database**.

---

## ✨ Features

### 🔐 Authentication
- Role-based login (Admin, Doctor, Patient)
- Secure logout and session handling

### 👨‍💼 Admin Module
- Add, edit, activate/deactivate, and remove doctors
- Register new patients
- Approve or reject appointments
- Mark bills as paid
- View prescriptions and hospital statistics

### 🩺 Doctor Module
- View assigned appointments
- Accept, reject, or complete appointments
- Generate prescriptions

### 🧑‍⚕️ Patient Module
- Book appointments by department and doctor
- View appointment history
- Submit ratings and feedback

---

## 🛠 Technologies Used

- Java 11+
- Java Swing
- File Handling
- Object-Oriented Programming

---

## 📁 Project Structure

```text
SHRMS/
├── src/
│   └── com/hospital/
│       ├── MainWindow.java
│       ├── LoginPanel.java
│       ├── AdminDashboardPanel.java
│       ├── DoctorDashboardPanel.java
│       ├── PatientDashboardPanel.java
│       └── DataStore.java
└── data/
    ├── doctors.txt
    ├── patients.txt
    ├── appointments.txt
    ├── bills.txt
    ├── prescriptions.txt
    ├── feedback.txt
    └── logs.txt
```

---

## ⚙️ Requirements

- Java JDK 11 or higher
- IntelliJ / Eclipse / NetBeans / VS Code

Check Java version:

```bash
java -version
```

---

## 🚀 How to Run

### IntelliJ IDEA
1. Open IntelliJ IDEA
2. File → Open → Select `SHRMS`
3. Open `MainWindow.java`
4. Click **Run ▶**

### Eclipse
1. Create a Java Project
2. Import the `SHRMS` folder
3. Open `MainWindow.java`
4. Run as **Java Application**

### NetBeans
1. Open the project folder
2. Right-click the project
3. Select **Run**

---

## 🔑 Sample Login Credentials

### Admin

| Username | Password |
|----------|----------|
| admin | admin123 |

### Doctor

| Username | Password |
|----------|----------|
| arjun.reddy | doc1pass |

### Patient

| Username | Password |
|----------|----------|
| ravi.kumar | rpass1 |

---

## 💾 Data Storage

All information is stored in UTF-8 plain-text files inside the `data/` directory.

| File | Description |
|------|-------------|
| doctors.txt | Doctor details |
| patients.txt | Patient records |
| appointments.txt | Appointment information |
| bills.txt | Billing records |
| prescriptions.txt | Prescriptions |
| feedback.txt | Ratings and comments |
| logs.txt | Activity logs |

---

## 👩‍💻 Author

**Golla Varsha**

---

## 📄 License

This project is for **educational and portfolio purposes only**.
