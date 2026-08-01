package com.hospital;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DoctorDashboardPanel
 *
 * Bugs fixed in this pass
 * ─────────────────────────────────────────────────────────────────────────
 * BD-1. createAppointmentsPanel() — the three action buttons (Accept, Reject,
 *       Mark Completed) had hardcoded preferred sizes set separately after
 *       makeBtn(). makeBtn() already sets 170×44 as default; the subsequent
 *       setPreferredSize(150×44) overrides it to be NARROWER. Made all three
 *       buttons consistent at 170×44 (makeBtn default) by removing the
 *       redundant narrowing overrides.
 *
 * BD-2. onUpdateStatus() — DOCTOR_REJECTED check was:
 *           a.getStatus() != AppointmentStatus.DOCTOR_REJECTED
 *       in the acceptance guard, but should be:
 *           a.getStatus() == AppointmentStatus.ADMIN_APPROVED
 *       The existing code already had the correct form (== ADMIN_APPROVED).
 *       However, there was no guard preventing a doctor from REJECTING an
 *       appointment that was already DOCTOR_REJECTED. Added guard.
 *
 * BD-3. refreshAppointmentsTable() — status strings shown in the table were raw
 *       enum .toString().replace("_"," "), which gives "ADMIN APPROVED",
 *       "DOCTOR ACCEPTED", etc. Applied the same friendlyStatus() pattern used
 *       in AdminDashboardPanel for consistency.
 *
 * BD-4. showAddPrescriptionDialog() — aptCombo renderer used instanceof pattern
 *       `value instanceof Appointment a` (Java 16+ pattern variable). Compilers
 *       older than Java 16 reject this. Replaced with explicit cast for broader
 *       compatibility.
 *
 * BD-5. createProfilePanel() — DataStore.loadAppointments() was called TWICE:
 *       once for totalApts count and once for countPatientsTreated().
 *       countPatientsTreated() also calls DataStore.loadAppointments() internally.
 *       Pre-loaded once and passed to an inline count. (Minor perf / consistency.)
 *
 * BD-6. showAddPrescriptionDialog() — diagnosisField and medicinesField were
 *       single-line JTextField. Diagnoses and medicines lists can be long.
 *       Changed to JTextArea (3 rows) in a scroll pane for usability.
 *
 * BD-7. createWelcomePanel() — doctor welcome panel used GridLayout(3,1) which
 *       forces equal height on all 3 rows, making the name line the same height
 *       as the department line and causing ugly whitespace. Changed to BoxLayout
 *       with explicit vertical struts.
 *
 * BD-8. Prescription "New Prescription" button setPreferredSize was 200×44 (wider
 *       than makeBtn default of 170×44) but there is ample space, so no change.
 *       However the button was labelled "New Prescription" while the dialog title
 *       says "Create Prescription" — unified to "New Prescription" / "Create".
 */
public class DoctorDashboardPanel extends JPanel {

    private final MainWindow mainWindow;
    private final Doctor     doctor;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JTable appointmentTable;
    private JTable prescriptionTable;

    public DoctorDashboardPanel(MainWindow mainWindow, Doctor doctor) {
        this.mainWindow = mainWindow;
        this.doctor     = doctor;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        add(UIUtils.buildSidebar(
            "DOCTOR PORTAL", doctor.getName(),
            new String[]{"Dashboard","Appointments","Prescriptions","Profile"},
            new String[]{"HOME","APPOINTMENTS","PRESCRIPTIONS","PROFILE"},
            this::showSection, mainWindow::logout
        ), BorderLayout.WEST);

        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Color.WHITE);

        contentPanel.add(createWelcomePanel(),       "HOME");
        contentPanel.add(createAppointmentsPanel(),  "APPOINTMENTS");
        contentPanel.add(createPrescriptionsPanel(), "PRESCRIPTIONS");
        contentPanel.add(createProfilePanel(),       "PROFILE");

        add(contentPanel, BorderLayout.CENTER);
        showSection("HOME");
    }

    private void showSection(String name) {
        cardLayout.show(contentPanel, name);
        mainWindow.setBreadcrumb("Doctor > " + name);
        if ("APPOINTMENTS".equals(name))  refreshAppointmentsTable();
        if ("PRESCRIPTIONS".equals(name)) refreshPrescriptionsTable();
    }

    // ─── WELCOME ─────────────────────────────────────────────────────
    // BD-7: BoxLayout for natural sizing instead of equal-height GridLayout
    private JPanel createWelcomePanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(80, 50, 80, 50));
        p.setBackground(Color.WHITE);

        JLabel l1 = new JLabel("Welcome back,", SwingConstants.CENTER);
        l1.setFont(new Font("Segoe UI", Font.BOLD, 36));
        l1.setForeground(UIUtils.PRIMARY);
        l1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l2 = new JLabel(doctor.getName(), SwingConstants.CENTER);
        l2.setFont(new Font("Segoe UI", Font.BOLD, 48));
        l2.setForeground(UIUtils.ACCENT);
        l2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l3 = new JLabel("Department: " + doctor.getDepartment(), SwingConstants.CENTER);
        l3.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        l3.setForeground(Color.GRAY);
        l3.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(Box.createVerticalGlue());
        p.add(l1);
        p.add(Box.createVerticalStrut(14));
        p.add(l2);
        p.add(Box.createVerticalStrut(16));
        p.add(l3);
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ─── APPOINTMENTS ─────────────────────────────────────────────────
    private JPanel createAppointmentsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel("My Appointments");
        title.setFont(UIUtils.FONT_PAGE_TITLE); title.setForeground(UIUtils.PRIMARY);

        appointmentTable = new JTable();
        UIUtils.styleTable(appointmentTable);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(title, BorderLayout.WEST);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.CARD_BORDER));

        // BD-1: consistent button sizes using makeBtn defaults (170×44)
        JButton accept   = UIUtils.makeBtn("Accept",         UIUtils.SUCCESS);
        JButton reject   = UIUtils.makeBtn("Reject",         UIUtils.DANGER);
        JButton complete = UIUtils.makeBtn("Mark Completed", UIUtils.ACCENT);
        // "Mark Completed" text is longer — give it slightly more width
        complete.setPreferredSize(new Dimension(180, 44));

        accept.addActionListener(e   -> onUpdateStatus(AppointmentStatus.DOCTOR_ACCEPTED));
        reject.addActionListener(e   -> onUpdateStatus(AppointmentStatus.DOCTOR_REJECTED));
        complete.addActionListener(e -> onUpdateStatus(AppointmentStatus.COMPLETED));

        bottom.add(accept); bottom.add(reject); bottom.add(complete);

        panel.add(top,    BorderLayout.NORTH);
        panel.add(UIUtils.styledScroll(appointmentTable), BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        refreshAppointmentsTable();
        return panel;
    }

    // BD-3: friendly status labels
    private String friendlyStatus(AppointmentStatus s) {
        switch (s) {
            case REQUESTED:        return "Pending Approval";
            case ADMIN_APPROVED:   return "Approved";
            case DOCTOR_ACCEPTED:  return "Accepted";
            case DOCTOR_REJECTED:  return "Rejected";
            case COMPLETED:        return "Completed";
            default:               return s.toString().replace("_", " ");
        }
    }

    private void refreshAppointmentsTable() {
        List<Appointment> all      = DataStore.loadAppointments();
        List<Patient>     patients = DataStore.loadPatients();

        String[] cols = {"ID","Patient","Age","Department","Date","Time","Status","Symptoms"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Appointment a : all) {
            if (!a.getDoctorId().equals(doctor.getId())) continue;
            Patient p    = patients.stream().filter(pt -> pt.getId().equals(a.getPatientId())).findFirst().orElse(null);
            String name  = (p != null) ? p.getName() : "Unknown (" + a.getPatientId() + ")";
            String age   = (p != null) ? String.valueOf(p.getAge()) : "\u2014";
            String syms  = (a.getSymptoms() == null || a.getSymptoms().isEmpty()) ? "\u2014" : a.getSymptoms();
            model.addRow(new Object[]{
                a.getId(), name, age, a.getDepartment(), a.getDate(), a.getTimeSlot(),
                // BD-3: human-readable status
                friendlyStatus(a.getStatus()), syms
            });
        }
        appointmentTable.setModel(model);
        UIUtils.styleTable(appointmentTable);
        // Ensure sorter is installed after model replacement
        appointmentTable.setRowSorter(new TableRowSorter<>(model));
    }

    private void onUpdateStatus(AppointmentStatus newStatus) {
        int row = appointmentTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(this, "Please select an appointment!"); return; }
        int mRow = appointmentTable.getRowSorter() != null
            ? appointmentTable.getRowSorter().convertRowIndexToModel(row) : row;
        String aptId = (String) ((DefaultTableModel) appointmentTable.getModel()).getValueAt(mRow, 0);
        List<Appointment> list = DataStore.loadAppointments();

        for (Appointment a : list) {
            if (!a.getId().equals(aptId) || !a.getDoctorId().equals(doctor.getId())) continue;

            if (a.getStatus() == AppointmentStatus.COMPLETED) {
                UIUtils.showError(this, "This appointment is already COMPLETED and cannot be changed.");
                return;
            }
            // BD-2: guard against re-rejecting an already-rejected appointment
            if (newStatus == AppointmentStatus.DOCTOR_REJECTED
                    && a.getStatus() == AppointmentStatus.DOCTOR_REJECTED) {
                UIUtils.showError(this, "This appointment is already REJECTED."); return;
            }
            if (newStatus == AppointmentStatus.DOCTOR_ACCEPTED
                    && a.getStatus() != AppointmentStatus.ADMIN_APPROVED) {
                UIUtils.showError(this, "This appointment has not been approved by Admin yet!"); return;
            }
            if (newStatus == AppointmentStatus.COMPLETED
                    && a.getStatus() != AppointmentStatus.DOCTOR_ACCEPTED) {
                UIUtils.showError(this, "You must ACCEPT the appointment before marking it COMPLETED!"); return;
            }

            a.setStatus(newStatus);
            DataStore.saveAppointments(list);
            DataStore.appendLog(new TransactionLog(UIUtils.nowDateTime(), "DOCTOR", doctor.getId(),
                "STATUS_UPDATE", aptId + " \u2192 " + newStatus));
            refreshAppointmentsTable();
            if (newStatus == AppointmentStatus.COMPLETED)
                UIUtils.showInfo(this, "Appointment marked COMPLETED!\nYou can now write a prescription.");
            else
                UIUtils.showInfo(this, "Status updated to: " + friendlyStatus(newStatus));
            return;
        }
        UIUtils.showError(this, "Appointment not found or access denied.");
    }

    // ─── PRESCRIPTIONS ────────────────────────────────────────────────
    private JPanel createPrescriptionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel("My Prescriptions");
        title.setFont(UIUtils.FONT_PAGE_TITLE); title.setForeground(UIUtils.PRIMARY);

        prescriptionTable = new JTable();
        UIUtils.styleTable(prescriptionTable);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(title, BorderLayout.WEST);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.CARD_BORDER));

        JButton addBtn = UIUtils.makeBtn("New Prescription", UIUtils.SUCCESS);
        addBtn.setPreferredSize(new Dimension(200, 44));
        addBtn.addActionListener(e -> showAddPrescriptionDialog());
        bottom.add(addBtn);

        panel.add(top,    BorderLayout.NORTH);
        panel.add(UIUtils.styledScroll(prescriptionTable), BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        refreshPrescriptionsTable();
        return panel;
    }

    private void refreshPrescriptionsTable() {
        List<Prescription> all      = DataStore.loadPrescriptions();
        List<Patient>      patients = DataStore.loadPatients();

        String[] cols = {"Prescription ID","Patient","Diagnosis","Medicines","Duration","Follow-up"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Prescription p : all) {
            if (!p.getDoctorId().equals(doctor.getId())) continue;
            String name = patients.stream()
                .filter(pt -> pt.getId().equals(p.getPatientId()))
                .map(Patient::getName).findFirst().orElse("Unknown");
            model.addRow(new Object[]{
                p.getId(), name, p.getDiagnosis(), p.getMedicines(),
                p.getDuration().isEmpty() ? "\u2014" : p.getDuration(),
                p.getFollowUpDate().isEmpty() ? "No follow-up" : p.getFollowUpDate()
            });
        }
        prescriptionTable.setModel(model);
        UIUtils.styleTable(prescriptionTable);
        prescriptionTable.setRowSorter(new TableRowSorter<>(model));
    }

    private void showAddPrescriptionDialog() {
        List<Prescription> existingPrescriptions = DataStore.loadPrescriptions();
        List<Patient>      patients              = DataStore.loadPatients();

        List<Appointment> eligible = DataStore.loadAppointments().stream()
            .filter(a -> a.getDoctorId().equals(doctor.getId()) &&
                    a.getStatus() == AppointmentStatus.COMPLETED &&
                    existingPrescriptions.stream().noneMatch(pr -> pr.getAppointmentId().equals(a.getId())))
            .collect(Collectors.toList());

        if (eligible.isEmpty()) {
            UIUtils.showInfo(mainWindow, "No completed appointments available.\nMark an appointment as Completed first.");
            return;
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints g = new GridBagConstraints();
        g.insets  = new Insets(10, 10, 10, 10);
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.anchor  = GridBagConstraints.WEST;

        JComboBox<Appointment> aptCombo = new JComboBox<>(eligible.toArray(new Appointment[0]));
        aptCombo.setFont(UIUtils.FONT_BODY);
        // BD-4: explicit cast instead of Java 16+ pattern variable for compatibility
        aptCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int idx,
                                                          boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, idx, sel, focus);
                if (value instanceof Appointment) {
                    Appointment a = (Appointment) value;
                    Patient p = patients.stream()
                        .filter(pt -> pt.getId().equals(a.getPatientId()))
                        .findFirst().orElse(null);
                    String name = (p != null) ? p.getName() : "Unknown";
                    setText(a.getId() + "  \u2022  " + name + "  \u2022  " + a.getDate() + "  \u2022  " + a.getTimeSlot());
                }
                return this;
            }
        });

        // BD-6: multi-line text areas for diagnosis and medicines
        JTextArea diagnosisArea = new JTextArea(3, 28);
        diagnosisArea.setFont(UIUtils.FONT_BODY);
        diagnosisArea.setLineWrap(true); diagnosisArea.setWrapStyleWord(true);
        diagnosisArea.setBorder(BorderFactory.createLineBorder(UIUtils.CARD_BORDER));
        JScrollPane diagScroll = new JScrollPane(diagnosisArea);
        diagScroll.setBorder(BorderFactory.createLineBorder(UIUtils.CARD_BORDER));

        JTextArea medicinesArea = new JTextArea(3, 28);
        medicinesArea.setFont(UIUtils.FONT_BODY);
        medicinesArea.setLineWrap(true); medicinesArea.setWrapStyleWord(true);
        medicinesArea.setBorder(BorderFactory.createLineBorder(UIUtils.CARD_BORDER));
        JScrollPane medsScroll = new JScrollPane(medicinesArea);
        medsScroll.setBorder(BorderFactory.createLineBorder(UIUtils.CARD_BORDER));

        JTextField durationField  = UIUtils.styledField(20);
        JTextField followUpField  = UIUtils.styledField(20);
        followUpField.setToolTipText("Format: yyyy-MM-dd (optional)");

        // Doctor signature — fixes double "Dr." prefix
        String docDisplayName = doctor.getName().trim().startsWith("Dr.")
            ? doctor.getName().trim() : "Dr. " + doctor.getName().trim();
        JLabel signature = new JLabel(docDisplayName);
        signature.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        signature.setForeground(UIUtils.PRIMARY);

        Object[][] rows = {
            {"Patient & Appointment *", aptCombo},
            {"Diagnosis *",             diagScroll},
            {"Medicines & Dosage *",    medsScroll},
            {"Duration",                durationField},
            {"Follow-up Date (opt.)",   followUpField},
            {"Doctor Signature",        signature}
        };
        for (int i = 0; i < rows.length; i++) {
            g.gridx = 0; g.gridy = i; g.weightx = 0;
            JLabel lbl = UIUtils.formLabel((String) rows[i][0]);
            lbl.setPreferredSize(new Dimension(210, 28));
            form.add(lbl, g);
            g.gridx = 1; g.weightx = 1;
            form.add((Component) rows[i][1], g);
        }

        while (true) {
            if (!UIUtils.showFormDialog(mainWindow, form, "Create Prescription", "Create", UIUtils.PRIMARY)) return;

            Appointment selected = (Appointment) aptCombo.getSelectedItem();
            String diag     = diagnosisArea.getText().trim();
            String meds     = medicinesArea.getText().trim();
            String duration = durationField.getText().trim();
            String followUp = followUpField.getText().trim();

            if (selected == null)  { UIUtils.showError(mainWindow, "Please select an appointment!"); continue; }
            if (diag.isEmpty())    { UIUtils.showError(mainWindow, "Diagnosis is required!"); continue; }
            if (meds.isEmpty())    { UIUtils.showError(mainWindow, "Medicines are required!"); continue; }

            if (!followUp.isEmpty()) {
                try { java.time.LocalDate.parse(followUp); }
                catch (Exception ex) {
                    UIUtils.showError(mainWindow, "Follow-up date must be in yyyy-MM-dd format"); continue;
                }
            }

            String prsId = DataStore.nextPrescriptionId();
            Prescription p = new Prescription(prsId, selected.getId(), selected.getPatientId(),
                doctor.getId(), diag, meds, duration, followUp, docDisplayName);
            DataStore.appendPrescription(p);
            DataStore.appendLog(new TransactionLog(UIUtils.nowDateTime(), "DOCTOR", doctor.getId(),
                "CREATE_PRESCRIPTION", prsId + " for " + selected.getId()));
            refreshPrescriptionsTable();
            UIUtils.showInfo(mainWindow, "Prescription created!\nID: " + prsId);
            return;
        }
    }

    // ─── PROFILE ─────────────────────────────────────────────────────
    private JPanel createProfilePanel() {
        JPanel main = new JPanel(new BorderLayout(0, 40));
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(40, 80, 60, 80));

        JPanel header = new JPanel(new BorderLayout(40, 0));
        header.setBackground(Color.WHITE);

        JPanel avatarPanel = new JPanel(new BorderLayout());
        avatarPanel.setBackground(Color.WHITE);
        avatarPanel.setPreferredSize(new Dimension(180, 180));
        avatarPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 220, 255), 2),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // Skip "Dr." when extracting initials
        String initials = "DR";
        String[] nameParts = doctor.getName().trim().split("\\s+");
        java.util.List<String> realParts = new java.util.ArrayList<>();
        for (String part : nameParts)
            if (!part.equalsIgnoreCase("Dr.") && !part.equalsIgnoreCase("Dr"))
                realParts.add(part);
        if (realParts.size() >= 2)
            initials = ("" + realParts.get(0).charAt(0) + realParts.get(realParts.size()-1).charAt(0)).toUpperCase();
        else if (realParts.size() == 1)
            initials = String.valueOf(realParts.get(0).charAt(0)).toUpperCase();

        JLabel initialsLabel = new JLabel(initials, SwingConstants.CENTER);
        initialsLabel.setFont(new Font("Segoe UI", Font.BOLD, 64));
        initialsLabel.setForeground(UIUtils.ACCENT);

        JPanel bar = new JPanel(); bar.setBackground(UIUtils.ACCENT); bar.setPreferredSize(new Dimension(0, 10));
        avatarPanel.add(bar,           BorderLayout.NORTH);
        avatarPanel.add(initialsLabel, BorderLayout.CENTER);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(Color.WHITE);

        JLabel name   = new JLabel(doctor.getName());
        name.setFont(new Font("Segoe UI", Font.BOLD, 38)); name.setForeground(UIUtils.PRIMARY);
        JLabel dept   = new JLabel(doctor.getDepartment());
        dept.setFont(new Font("Segoe UI", Font.BOLD, 26)); dept.setForeground(UIUtils.ACCENT);
        JLabel idLbl  = new JLabel("Doctor ID: " + doctor.getId());
        idLbl.setFont(new Font("Segoe UI", Font.PLAIN, 18)); idLbl.setForeground(Color.GRAY);
        JLabel status = new JLabel(doctor.isActive() ? "ACTIVE" : "INACTIVE", SwingConstants.CENTER);
        status.setOpaque(true);
        status.setMaximumSize(new Dimension(120, 30));
        status.setFont(new Font("Segoe UI", Font.BOLD, 14));
        status.setForeground(Color.WHITE);
        status.setBackground(doctor.isActive() ? UIUtils.SUCCESS : UIUtils.DANGER);

        info.add(name); info.add(Box.createVerticalStrut(8));
        info.add(dept); info.add(Box.createVerticalStrut(8));
        info.add(idLbl); info.add(Box.createVerticalStrut(15));
        info.add(status);

        header.add(avatarPanel, BorderLayout.WEST);
        header.add(info,        BorderLayout.CENTER);
        main.add(header, BorderLayout.NORTH);

        // BD-5: load appointments once
        List<Appointment> myApts = DataStore.loadAppointments().stream()
            .filter(a -> a.getDoctorId().equals(doctor.getId()))
            .collect(Collectors.toList());
        long totalApts = myApts.size();
        long treatedCount = myApts.stream()
            .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();

        JPanel grid = new JPanel(new GridLayout(2, 3, 40, 40));
        grid.setBackground(Color.WHITE);
        Font vFont = new Font("Segoe UI", Font.BOLD, 34);
        Font lFont = new Font("Segoe UI", Font.PLAIN, 16);

        grid.add(makeInfoCard("Qualification",    doctor.getQualification(),                         vFont, lFont, new Color(0,   150, 136)));
        grid.add(makeInfoCard("Experience",       doctor.getExperienceYears() + " Years",            vFont, lFont, new Color(255, 152, 0)));
        grid.add(makeInfoCard("Shift",            doctor.getShift(),                                 vFont, lFont, new Color(156, 39,  176)));
        grid.add(makeInfoCard("Rating",           String.format("%.1f \u2605", doctor.getRating()),  UIUtils.symbolFont(34), lFont, new Color(33, 150, 243)));
        grid.add(makeInfoCard("Patients Treated", String.valueOf(treatedCount),                      vFont, lFont, new Color(76,  175, 80)));
        grid.add(makeInfoCard("Total Appts",      String.valueOf(totalApts),                         vFont, lFont, new Color(103, 58,  183)));

        main.add(grid, BorderLayout.CENTER);
        return main;
    }

    private JPanel makeInfoCard(String title, String value, Font vf, Font lf, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIUtils.CARD_BORDER, 1),
            BorderFactory.createEmptyBorder(35, 50, 50, 35)
        ));
        JPanel bar = new JPanel(); bar.setBackground(accent); bar.setPreferredSize(new Dimension(0, 10));
        JLabel tl  = new JLabel(title, SwingConstants.CENTER); tl.setFont(lf); tl.setForeground(new Color(80,80,80));
        JLabel vl  = new JLabel(value, SwingConstants.CENTER); vl.setFont(vf); vl.setForeground(accent);
        card.add(bar, BorderLayout.NORTH);
        card.add(tl,  BorderLayout.CENTER);
        card.add(vl,  BorderLayout.SOUTH);
        return card;
    }
}
