package com.hospital;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/**
 * AdminDashboardPanel – full hospital management interface.
 *
 * Bugs fixed in this pass
 * ─────────────────────────────────────────────────────────────────────────
 * BA-1. onRemoveDoctor() used JOptionPane.showConfirmDialog (raw, unstyled)
 *       for the delete-confirmation. Replaced with a styled confirmation dialog
 *       that matches the rest of the UI (consistent with all other dialogs).
 *
 * BA-2. createDoctorPanel() — TableRowSorter was created from the INITIAL model
 *       but refreshDoctorTable() replaces the entire model via setModel().
 *       After refresh the sorter still refers to the OLD model, so sorting and
 *       filtering stop working. Fixed: refreshDoctorTable() reinstalls the sorter
 *       on the new model, and the search bar's DocumentListener re-reads the
 *       sorter from doctorTable.getRowSorter() (not a captured local variable).
 *       Same bug applied to patients (no sorter was installed at all — added).
 *
 * BA-3. createPatientPanel() — patientTable had no row sorter, so search filtering
 *       set on a sorter that was never attached to the table. Fixed: sorter created
 *       and attached after refreshPatientTable().
 *
 * BA-4. onAddDoctor() — duplicate username check only looked in doctors list, not
 *       patients. A doctor and patient could share a username, causing silent
 *       login ambiguity. Fixed: check both lists.
 *
 * BA-5. onEditDoctor() — the name field allowed editing but name validation
 *       (letters/spaces/periods only) was NOT applied in onEditDoctor even though
 *       it is applied in onAddDoctor. Fixed: same regex applied.
 *
 * BA-6. onEditDoctor() — password length < 4 check was NOT applied on edit,
 *       allowing blank or single-char passwords to be saved. Fixed.
 *
 * BA-7. refreshAppointmentTable() — status display: ADMIN_APPROVED was mapped to
 *       "APPROVED" but DOCTOR_ACCEPTED, which is the status after a doctor accepts
 *       an ADMIN_APPROVED appointment, still showed as "DOCTOR ACCEPTED" (raw enum).
 *       Fixed: add a human-friendly label for each status enum value.
 *
 * BA-8. createBillingPanel() — billingTable had no row sorter. The Mark-as-Paid
 *       handler used convertRowIndexToModel() defensively but if sorter is null
 *       the guard `getRowSorter() != null ? ... : vRow` returned vRow directly,
 *       which is correct only when unsorted. Added sorter for full parity.
 *
 * BA-9. createPrescriptionsPanel() — VIEW DETAILS dialog used raw JOptionPane
 *       (unstyled). Replaced with a styled scrollable detail dialog.
 *
 * BA-10. showAddPatientDialog() — the "Symptoms" label was added outside the
 *        main rows[] loop using hard-coded gridy = rows.length (6), but the
 *        JScrollPane for symptoms used gridy = rows.length + 1 (7). If rows[]
 *        is ever reordered this will silently break. Refactored to use the
 *        same incremental row variable used in buildDoctorForm().
 *
 * BA-11. refreshDoctorTable() and refreshPatientTable() called after setModel()
 *        but before sorter re-installation means briefly there is no sorter.
 *        On EDT this is invisible to users, but structurally the sorter setup
 *        is now done once inside refreshXxxTable() to keep state consistent.
 *
 * BA-12. createDoctorPanel() search bar — the DocumentListener captured the
 *        sorter local variable. After refreshDoctorTable() replaces the model the
 *        local sorter reference is stale. Fixed: always call
 *        (TableRowSorter) doctorTable.getRowSorter() in the listener lambda.
 *
 * BA-13. onToggleDoctor() showed success info with UIUtils.showInfo(mainWindow,...)
 *        using mainWindow as parent. All other action handlers in this file use
 *        the local panel variable (which is the correct visible parent). Made
 *        consistent: use mainWindow as parent for actions triggered from
 *        the header area and use panel for actions triggered within a panel.
 *        (Minor UX — dialog centering — not a functional bug.)
 *
 * BA-14. createAnalyticsPanel() called DataStore.loadDoctors() twice:
 *        once for docCount and once inside the avgRating stream. Pre-loaded once.
 *
 * BA-15. Appointments panel — APPROVE button should only be available for
 *        REQUESTED status. Previous code checked correctly but displayed a
 *        generic "Only REQUESTED appointments can be approved" message even
 *        when the appointment was already APPROVED. Message improved to show
 *        the current status.
 */
public class AdminDashboardPanel extends JPanel {

    private MainWindow mainWindow;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JTable doctorTable;
    private JTable patientTable;
    private JTable appointmentTable;
    private JTable billingTable;
    private JTable prescriptionTable;

    public AdminDashboardPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        add(UIUtils.buildSidebar(
            "ADMIN PORTAL", null,
            new String[]{"Dashboard","Doctors","Patients","Appointments","Billing","Prescriptions","Analytics"},
            new String[]{"HOME","DOCTORS","PATIENTS","APPOINTMENTS","BILLING","PRESCRIPTIONS","ANALYTICS"},
            this::showSection, mainWindow::logout
        ), BorderLayout.WEST);

        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Color.WHITE);

        contentPanel.add(createWelcomePanel(),       "HOME");
        contentPanel.add(createDoctorPanel(),        "DOCTORS");
        contentPanel.add(createPatientPanel(),       "PATIENTS");
        contentPanel.add(createAppointmentPanel(),   "APPOINTMENTS");
        contentPanel.add(createBillingPanel(),       "BILLING");
        contentPanel.add(createPrescriptionsPanel(), "PRESCRIPTIONS");
        contentPanel.add(createAnalyticsPanel(),     "ANALYTICS");

        add(contentPanel, BorderLayout.CENTER);
        showSection("HOME");
    }

    private void showSection(String name) {
        cardLayout.show(contentPanel, name);
        String pretty = name.charAt(0) + name.substring(1).toLowerCase();
        mainWindow.setBreadcrumb("Admin > " + pretty);
    }

    // ─── WELCOME ──────────────────────────────────────────────────────
    private JPanel createWelcomePanel() {
        // BA-14: load doctors once
        List<Doctor> doctors = DataStore.loadDoctors();
        int docs = doctors.size();
        int pats = DataStore.loadPatients().size();
        int apts = DataStore.loadAppointments().size();
        double rev = DataStore.loadBills().stream().mapToDouble(Bill::getTotalAmount).sum();

        JPanel hero = UIUtils.makeDashboardHero(
            "Welcome to Admin Dashboard",
            "Manage all hospital operations efficiently",
            "Doctors: " + docs + "  \u00B7  Patients: " + pats + "  \u00B7  Appointments: " + apts
        );

        JPanel cards = new JPanel(new GridLayout(1, 4, 20, 0));
        cards.setOpaque(false);
        cards.setBorder(BorderFactory.createEmptyBorder(0, 40, 40, 40));
        cards.add(UIUtils.makeStatCard("Doctors",      String.valueOf(docs), UIUtils.ACCENT));
        cards.add(UIUtils.makeStatCard("Patients",     String.valueOf(pats), UIUtils.SUCCESS));
        cards.add(UIUtils.makeStatCard("Appointments", String.valueOf(apts), UIUtils.INFO));
        cards.add(UIUtils.makeStatCard("Revenue",      "\u20B9" + String.format("%,.0f", rev), UIUtils.WARNING));

        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UIUtils.BG_LIGHT);
        outer.add(hero,  BorderLayout.CENTER);
        outer.add(cards, BorderLayout.SOUTH);
        return outer;
    }

    // ─── DOCTORS ──────────────────────────────────────────────────────
    private JPanel createDoctorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        doctorTable = new JTable();
        UIUtils.styleTable(doctorTable);

        JLabel title = new JLabel("MANAGE DOCTORS", SwingConstants.CENTER);
        title.setFont(UIUtils.FONT_PAGE_TITLE);
        title.setForeground(UIUtils.PRIMARY);

        // BA-12: search listener fetches sorter from table, not a captured local
        JTextField search = UIUtils.buildSearchBar("Search by name, department, status...");

        JPanel top = new JPanel(new BorderLayout(10, 8));
        top.setBackground(Color.WHITE);
        top.add(title,  BorderLayout.NORTH);
        top.add(search, BorderLayout.CENTER);
        panel.add(top, BorderLayout.NORTH);

        refreshDoctorTable(); // BA-2: sorter also installed inside refresh

        // BA-12: always get live sorter from table, not captured variable
        search.getDocument().addDocumentListener(docListener(() -> {
            boolean[] ph = (boolean[]) search.getClientProperty("isPlaceholder");
            boolean isPlaceholder = (ph != null && ph[0]);
            String t = search.getText().trim();
            @SuppressWarnings("unchecked")
            TableRowSorter<DefaultTableModel> liveSorter =
                (TableRowSorter<DefaultTableModel>) doctorTable.getRowSorter();
            if (liveSorter != null)
                liveSorter.setRowFilter((isPlaceholder || t.isEmpty()) ? null : RowFilter.regexFilter("(?i)" + t));
        }));

        panel.add(UIUtils.styledScroll(doctorTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        buttons.setBackground(Color.WHITE);
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.CARD_BORDER));

        JButton add    = UIUtils.makeBtn("Add Doctor",    UIUtils.SUCCESS);
        JButton edit   = UIUtils.makeBtn("Edit Doctor",   UIUtils.PRIMARY);
        JButton toggle = UIUtils.makeBtn("Toggle Active", UIUtils.INFO);
        JButton remove = UIUtils.makeBtn("Remove Doctor", UIUtils.DANGER);

        add.addActionListener(e    -> onAddDoctor());
        edit.addActionListener(e   -> onEditDoctor());
        toggle.addActionListener(e -> onToggleDoctor());
        remove.addActionListener(e -> onRemoveDoctor());

        buttons.add(add); buttons.add(edit); buttons.add(toggle); buttons.add(remove);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    // BA-2 / BA-11: sorter installed/reinstalled here so it always matches current model
    private void refreshDoctorTable() {
        List<Doctor> list = DataStore.loadDoctors();
        String[] columns = {"ID","Name","Department","Qualification","Experience (Yrs)","Shift","Rating","Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Doctor d : list) {
            model.addRow(new Object[]{
                d.getId(), d.getName(), d.getDepartment(), d.getQualification(),
                d.getExperienceYears(), d.getShift(),
                String.format("%.1f (%d)", d.getRating(), d.getRatingsCount()),
                d.isActive() ? "Active" : "Inactive"
            });
        }
        doctorTable.setModel(model);
        UIUtils.styleTable(doctorTable);
        // BA-2: always reinstall sorter on new model
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        doctorTable.setRowSorter(sorter);
    }

    private JPanel buildDoctorForm(JTextField nameF, JTextField userF, JTextField passF,
                                   JTextField deptF, JTextField qualF, JTextField expF,
                                   JComboBox<String> shiftBox) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.fill   = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        Object[][] rows = {
            {"Full Name *",          nameF},
            {"Username *",           userF},
            {"Password *",           passF},
            {"Department *",         deptF},
            {"Qualification *",      qualF},
            {"Experience (Years) *", expF},
            {"Shift",                shiftBox}
        };
        for (int i = 0; i < rows.length; i++) {
            g.gridx = 0; g.gridy = i; g.weightx = 0;
            JLabel lbl = UIUtils.formLabel((String) rows[i][0]);
            lbl.setPreferredSize(new Dimension(190, 28));
            form.add(lbl, g);
            g.gridx = 1; g.weightx = 1;
            form.add((Component) rows[i][1], g);
        }
        return form;
    }

    private void onAddDoctor() {
        JTextField nameF  = UIUtils.styledField(22);
        JTextField userF  = UIUtils.styledField(22);
        JTextField passF  = UIUtils.styledField(22);
        JTextField deptF  = UIUtils.styledField(22);
        JTextField qualF  = UIUtils.styledField(22);
        JTextField expF   = UIUtils.styledField(22);
        JComboBox<String> shiftBox = new JComboBox<>(new String[]{"MORNING","EVENING","NIGHT"});
        JPanel form = buildDoctorForm(nameF, userF, passF, deptF, qualF, expF, shiftBox);

        while (true) {
            if (!UIUtils.showFormDialog(mainWindow, form, "Add New Doctor", "Save Doctor", UIUtils.SUCCESS)) return;
            try {
                String name   = nameF.getText().trim();
                String user   = userF.getText().trim();
                String pass   = passF.getText().trim();
                String dept   = deptF.getText().trim();
                String qual   = qualF.getText().trim();
                String expStr = expF.getText().trim();

                UIUtils.requireField(name,   "Full Name");
                UIUtils.requireField(user,   "Username");
                UIUtils.requireField(pass,   "Password");
                UIUtils.requireField(dept,   "Department");
                UIUtils.requireField(qual,   "Qualification");
                UIUtils.requireField(expStr, "Experience");

                if (!name.matches("[A-Za-z .]+"))
                    throw new IllegalArgumentException("Name must contain only letters, spaces and periods");
                if (pass.length() < 4)
                    throw new IllegalArgumentException("Password must be at least 4 characters");

                int exp = Integer.parseInt(expStr);
                if (exp < 0 || exp > 60)
                    throw new IllegalArgumentException("Experience must be between 0 and 60 years");

                // BA-4: check both doctors AND patients for duplicate username
                if (DataStore.loadDoctors().stream().anyMatch(d -> d.getUsername().equalsIgnoreCase(user)))
                    throw new IllegalArgumentException("Username '" + user + "' already exists as a doctor");
                if (DataStore.loadPatients().stream().anyMatch(p -> p.getUsername().equalsIgnoreCase(user)))
                    throw new IllegalArgumentException("Username '" + user + "' already exists as a patient");

                Doctor d = new Doctor(DataStore.nextDoctorId(), name, user, pass, dept, qual,
                    exp, (String) shiftBox.getSelectedItem(), true, 0.0, 0);
                List<Doctor> list = DataStore.loadDoctors();
                list.add(d);
                DataStore.saveDoctors(list);
                DataStore.appendLog(new TransactionLog(UIUtils.nowDateTime(), "ADMIN", "ADMIN", "ADD_DOCTOR", d.getId()));
                refreshDoctorTable();
                UIUtils.showInfo(mainWindow, "Doctor added!\nID: " + d.getId() + "\nUsername: " + user);
                return;
            } catch (NumberFormatException ex) {
                UIUtils.showError(mainWindow, "Experience must be a valid whole number!");
            } catch (Exception ex) {
                UIUtils.showError(mainWindow, ex.getMessage());
            }
        }
    }

    private void onEditDoctor() {
        int row = doctorTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(mainWindow, "Please select a doctor to edit"); return; }
        int modelRow = doctorTable.getRowSorter() != null
            ? doctorTable.getRowSorter().convertRowIndexToModel(row) : row;
        String id = (String) ((DefaultTableModel) doctorTable.getModel()).getValueAt(modelRow, 0);

        List<Doctor> list = DataStore.loadDoctors();
        Doctor doc = list.stream().filter(d -> d.getId().equals(id)).findFirst().orElse(null);
        if (doc == null) return;

        JTextField nameF  = UIUtils.styledField(22); nameF.setText(doc.getName());
        JTextField userF  = UIUtils.styledField(22); userF.setText(doc.getUsername()); userF.setEditable(false);
        JTextField passF  = UIUtils.styledField(22); passF.setText(doc.getPassword());
        JTextField deptF  = UIUtils.styledField(22); deptF.setText(doc.getDepartment());
        JTextField qualF  = UIUtils.styledField(22); qualF.setText(doc.getQualification());
        JTextField expF   = UIUtils.styledField(22); expF.setText(String.valueOf(doc.getExperienceYears()));
        JComboBox<String> shiftBox = new JComboBox<>(new String[]{"MORNING","EVENING","NIGHT"});
        shiftBox.setSelectedItem(doc.getShift());
        JPanel form = buildDoctorForm(nameF, userF, passF, deptF, qualF, expF, shiftBox);

        while (true) {
            if (!UIUtils.showFormDialog(mainWindow, form, "Edit Doctor \u2013 " + doc.getName(), "Save Changes", UIUtils.PRIMARY)) return;
            try {
                String name   = nameF.getText().trim();
                String pass   = passF.getText().trim();
                String dept   = deptF.getText().trim();
                String qual   = qualF.getText().trim();
                String expStr = expF.getText().trim();

                UIUtils.requireField(name,   "Full Name");
                UIUtils.requireField(pass,   "Password");
                UIUtils.requireField(dept,   "Department");
                UIUtils.requireField(qual,   "Qualification");
                UIUtils.requireField(expStr, "Experience");

                // BA-5: same name validation as onAddDoctor
                if (!name.matches("[A-Za-z .]+"))
                    throw new IllegalArgumentException("Name must contain only letters, spaces and periods");
                // BA-6: password length check on edit
                if (pass.length() < 4)
                    throw new IllegalArgumentException("Password must be at least 4 characters");

                int exp = Integer.parseInt(expStr);
                if (exp < 0 || exp > 60) throw new IllegalArgumentException("Experience must be 0-60 years");

                doc.setName(name);
                doc.setPassword(pass);
                doc.setDepartment(dept);
                doc.setQualification(qual);
                doc.setExperienceYears(exp);
                doc.setShift((String) shiftBox.getSelectedItem());
                DataStore.saveDoctors(list);
                DataStore.appendLog(new TransactionLog(UIUtils.nowDateTime(), "ADMIN", "ADMIN", "EDIT_DOCTOR", id));
                refreshDoctorTable();
                UIUtils.showInfo(mainWindow, "Doctor updated successfully!");
                return;
            } catch (NumberFormatException ex) {
                UIUtils.showError(mainWindow, "Experience must be a valid number!");
            } catch (Exception ex) {
                UIUtils.showError(mainWindow, ex.getMessage());
            }
        }
    }

    private void onToggleDoctor() {
        int row = doctorTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(mainWindow, "Please select a doctor"); return; }
        int modelRow = doctorTable.getRowSorter() != null
            ? doctorTable.getRowSorter().convertRowIndexToModel(row) : row;
        String id = (String) ((DefaultTableModel) doctorTable.getModel()).getValueAt(modelRow, 0);
        List<Doctor> list = DataStore.loadDoctors();
        for (Doctor d : list) {
            if (d.getId().equals(id)) {
                d.setActive(!d.isActive());
                DataStore.saveDoctors(list);
                refreshDoctorTable();
                UIUtils.showInfo(mainWindow, "Status updated: " + d.getName()
                    + " \u2192 " + (d.isActive() ? "Active" : "Inactive"));
                return;
            }
        }
    }

    // BA-1: styled confirm dialog instead of raw JOptionPane
    private void onRemoveDoctor() {
        int row = doctorTable.getSelectedRow();
        if (row < 0) { UIUtils.showError(mainWindow, "Please select a doctor"); return; }
        int modelRow = doctorTable.getRowSorter() != null
            ? doctorTable.getRowSorter().convertRowIndexToModel(row) : row;
        String id = (String) ((DefaultTableModel) doctorTable.getModel()).getValueAt(modelRow, 0);

        // Block deletion if doctor has active appointments
        long activeApts = DataStore.loadAppointments().stream()
            .filter(a -> a.getDoctorId().equals(id) &&
                    a.getStatus() != AppointmentStatus.COMPLETED &&
                    a.getStatus() != AppointmentStatus.DOCTOR_REJECTED)
            .count();
        if (activeApts > 0) {
            UIUtils.showError(mainWindow,
                "Cannot remove doctor — they have " + activeApts
                + " active/pending appointment(s).\n"
                + "Please resolve those appointments first.");
            return;
        }

        // BA-1: styled confirmation panel
        JPanel confirmPanel = new JPanel(new GridBagLayout());
        confirmPanel.setBackground(Color.WHITE);
        confirmPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        // Lookup doctor name for confirmation message
        List<Doctor> allDocs = DataStore.loadDoctors();
        String docName = allDocs.stream().filter(d -> d.getId().equals(id))
            .map(Doctor::getName).findFirst().orElse(id);

        JLabel warn = new JLabel("<html><b>Remove doctor: " + docName + "?</b><br>"
            + "<span style='color:red'>This action cannot be undone.</span></html>");
        warn.setFont(UIUtils.FONT_BODY);
        confirmPanel.add(warn, g);

        if (!UIUtils.showFormDialog(mainWindow, confirmPanel, "Confirm Doctor Removal", "Remove", UIUtils.DANGER)) return;

        List<Doctor> list = DataStore.loadDoctors();
        list.removeIf(d -> d.getId().equals(id));
        DataStore.saveDoctors(list);
        DataStore.appendLog(new TransactionLog(UIUtils.nowDateTime(), "ADMIN", "ADMIN", "REMOVE_DOCTOR", id));
        refreshDoctorTable();
        UIUtils.showInfo(mainWindow, "Doctor removed successfully");
    }

    // ─── PATIENTS ──────────────────────────────────────────────────────
    private JPanel createPatientPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("MANAGE PATIENTS", SwingConstants.CENTER);
        title.setFont(UIUtils.FONT_PAGE_TITLE);
        title.setForeground(UIUtils.PRIMARY);

        JTextField search = UIUtils.buildSearchBar("Search by name, phone, gender...");

        JPanel top = new JPanel(new BorderLayout(10, 8));
        top.setBackground(Color.WHITE);
        top.add(title,  BorderLayout.NORTH);
        top.add(search, BorderLayout.CENTER);
        panel.add(top, BorderLayout.NORTH);

        String[] columns = {"ID","Name","Phone","Username","Age","Gender","Symptoms"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        patientTable = new JTable(model);
        UIUtils.styleTable(patientTable);

        // BA-3: patient search sorter properly installed
        refreshPatientTable();
        search.getDocument().addDocumentListener(docListener(() -> {
            boolean[] ph = (boolean[]) search.getClientProperty("isPlaceholder");
            boolean isPlaceholder = (ph != null && ph[0]);
            String t = search.getText().trim();
            @SuppressWarnings("unchecked")
            TableRowSorter<DefaultTableModel> liveSorter =
                (TableRowSorter<DefaultTableModel>) patientTable.getRowSorter();
            if (liveSorter != null)
                liveSorter.setRowFilter((isPlaceholder || t.isEmpty()) ? null : RowFilter.regexFilter("(?i)" + t));
        }));

        panel.add(UIUtils.styledScroll(patientTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.CARD_BORDER));
        JButton addBtn = UIUtils.makeBtn("Add New Patient", UIUtils.SUCCESS);
        addBtn.setPreferredSize(new Dimension(200, 44));
        addBtn.addActionListener(e -> showAddPatientDialog());
        btnPanel.add(addBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // BA-3: sorter installed/reinstalled in refresh
    private void refreshPatientTable() {
        DefaultTableModel model = (DefaultTableModel) patientTable.getModel();
        model.setRowCount(0);
        for (Patient p : DataStore.loadPatients()) {
            String sym = p.getInitialSymptoms();
            if (sym == null || sym.trim().isEmpty()) sym = "\u2014";
            else if (sym.length() > 60) sym = sym.substring(0, 57) + "...";
            model.addRow(new Object[]{
                p.getId(), p.getName(), p.getPhone(),
                p.getUsername(), p.getAge(), p.getGender(), sym
            });
        }
        UIUtils.styleTable(patientTable);
        // BA-3: reinstall sorter on refreshed model
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        patientTable.setRowSorter(sorter);
    }

    // BA-10: use incremental row counter for all form rows
    private void showAddPatientDialog() {
        JTextField nameField  = UIUtils.styledField(22);
        JTextField phoneField = UIUtils.styledField(22);
        JTextField userField  = UIUtils.styledField(22);
        JPasswordField passField = UIUtils.styledPassField(22);
        JTextField ageField   = UIUtils.styledField(22);
        JComboBox<String> genderBox = new JComboBox<>(new String[]{"Male","Female","Other"});
        JTextArea symptomsArea = new JTextArea(4, 22);
        symptomsArea.setLineWrap(true); symptomsArea.setWrapStyleWord(true);
        symptomsArea.setFont(UIUtils.FONT_BODY);
        symptomsArea.setBorder(BorderFactory.createLineBorder(UIUtils.CARD_BORDER));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.fill   = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        // BA-10: use a single row counter for all rows including symptoms
        Object[][] rows = {
            {"Full Name *",          nameField},
            {"Phone (10 digits) *",  phoneField},
            {"Username *",           userField},
            {"Password *",           passField},
            {"Age *",                ageField},
            {"Gender",               genderBox}
        };
        int r = 0;
        for (; r < rows.length; r++) {
            g.gridx = 0; g.gridy = r; g.weightx = 0; g.gridwidth = 1;
            JLabel lbl = UIUtils.formLabel((String) rows[r][0]);
            lbl.setPreferredSize(new Dimension(190, 28));
            form.add(lbl, g);
            g.gridx = 1; g.weightx = 1;
            form.add((Component) rows[r][1], g);
        }
        // Symptoms label — row r
        g.gridx = 0; g.gridy = r; g.gridwidth = 2; g.weightx = 0;
        form.add(UIUtils.formLabel("Initial Symptoms / Reason for Visit:"), g);
        r++;
        // Symptoms text area — row r+1
        g.gridx = 0; g.gridy = r; g.gridwidth = 2; g.fill = GridBagConstraints.BOTH; g.weighty = 1.0;
        form.add(new JScrollPane(symptomsArea), g);

        while (true) {
            if (!UIUtils.showFormDialog(mainWindow, form, "Add New Patient", "Save Patient", UIUtils.SUCCESS)) return;
            try {
                String name     = nameField.getText().trim();
                String phone    = phoneField.getText().trim();
                String user     = userField.getText().trim();
                String pass     = new String(passField.getPassword());
                String ageStr   = ageField.getText().trim();
                String symptoms = symptomsArea.getText().trim();

                UIUtils.requireField(name,   "Full Name");
                UIUtils.requireField(phone,  "Phone");
                UIUtils.requireField(user,   "Username");
                UIUtils.requireField(pass,   "Password");
                UIUtils.requireField(ageStr, "Age");

                if (!UIUtils.isValidPhone(phone))
                    throw new IllegalArgumentException("Phone must be exactly 10 digits");
                if (pass.length() < 4)
                    throw new IllegalArgumentException("Password must be at least 4 characters");

                int age = Integer.parseInt(ageStr);
                if (!UIUtils.isValidAge(age))
                    throw new IllegalArgumentException("Age must be between 1 and 120");

                // BA-4 parity: check both patients AND doctors for duplicate username
                if (DataStore.loadPatients().stream().anyMatch(p -> p.getUsername().equalsIgnoreCase(user)))
                    throw new IllegalArgumentException("Username '" + user + "' already exists as a patient");
                if (DataStore.loadDoctors().stream().anyMatch(d -> d.getUsername().equalsIgnoreCase(user)))
                    throw new IllegalArgumentException("Username '" + user + "' already exists as a doctor");

                String id = DataStore.nextPatientId();
                Patient np = new Patient(id, name, phone, user, pass, age,
                    (String) genderBox.getSelectedItem(), symptoms);
                List<Patient> list = DataStore.loadPatients();
                list.add(np);
                DataStore.savePatients(list);
                DataStore.appendLog(new TransactionLog(UIUtils.nowDateTime(), "ADMIN", "ADMIN",
                    "ADD_PATIENT", id + " - " + name));
                refreshPatientTable();
                UIUtils.showInfo(mainWindow, "Patient registered!\nID: " + id + "\nUsername: " + user);
                return;
            } catch (NumberFormatException ex) {
                UIUtils.showError(mainWindow, "Age must be a valid number!");
            } catch (Exception ex) {
                UIUtils.showError(mainWindow, ex.getMessage());
            }
        }
    }

    // ─── APPOINTMENTS ─────────────────────────────────────────────────
    private JPanel createAppointmentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("APPOINTMENTS", SwingConstants.CENTER);
        title.setFont(UIUtils.FONT_PAGE_TITLE);
        title.setForeground(UIUtils.PRIMARY);
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"ID","Patient","Doctor","Department","Date","Time","Status","Symptoms"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        appointmentTable = new JTable(model);
        UIUtils.styleTable(appointmentTable);
        panel.add(UIUtils.styledScroll(appointmentTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.CARD_BORDER));

        JButton approveBtn = UIUtils.makeBtn("APPROVE", UIUtils.SUCCESS);
        JButton rejectBtn  = UIUtils.makeBtn("REJECT",  UIUtils.DANGER);
        approveBtn.setPreferredSize(new Dimension(150, 44));
        rejectBtn.setPreferredSize(new Dimension(150, 44));

        approveBtn.addActionListener(e -> {
            int vRow = appointmentTable.getSelectedRow();
            if (vRow == -1) { UIUtils.showError(panel, "Please select an appointment!"); return; }
            int mRow = appointmentTable.getRowSorter() != null
                ? appointmentTable.getRowSorter().convertRowIndexToModel(vRow) : vRow;
            String id = (String) model.getValueAt(mRow, 0);
            List<Appointment> list = DataStore.loadAppointments();
            for (Appointment a : list) {
                if (a.getId().equals(id)) {
                    if (a.getStatus() != AppointmentStatus.REQUESTED) {
                        // BA-15: show the current status in the error message
                        UIUtils.showError(panel, "Only REQUESTED appointments can be approved.\n"
                            + "Current status: " + friendlyStatus(a.getStatus())); return;
                    }
                    a.setStatus(AppointmentStatus.ADMIN_APPROVED);
                    DataStore.saveAppointments(list);
                    DataStore.appendLog(new TransactionLog(UIUtils.nowDateTime(), "ADMIN", "ADMIN",
                        "APPROVE_APPOINTMENT", id));
                    refreshAppointmentTable();
                    UIUtils.showInfo(panel, "Appointment " + id + " APPROVED");
                    return;
                }
            }
        });

        rejectBtn.addActionListener(e -> {
            int vRow = appointmentTable.getSelectedRow();
            if (vRow == -1) { UIUtils.showError(panel, "Please select an appointment!"); return; }
            int mRow = appointmentTable.getRowSorter() != null
                ? appointmentTable.getRowSorter().convertRowIndexToModel(vRow) : vRow;
            String id = (String) model.getValueAt(mRow, 0);
            List<Appointment> list = DataStore.loadAppointments();
            for (Appointment a : list) {
                if (a.getId().equals(id)) {
                    if (a.getStatus() == AppointmentStatus.COMPLETED) {
                        UIUtils.showError(panel, "Cannot reject a completed appointment."); return;
                    }
                    a.setStatus(AppointmentStatus.DOCTOR_REJECTED);
                    DataStore.saveAppointments(list);
                    DataStore.appendLog(new TransactionLog(UIUtils.nowDateTime(), "ADMIN", "ADMIN",
                        "REJECT_APPOINTMENT", id));
                    refreshAppointmentTable();
                    UIUtils.showInfo(panel, "Appointment " + id + " REJECTED");
                    return;
                }
            }
        });

        btnPanel.add(approveBtn); btnPanel.add(rejectBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        refreshAppointmentTable();
        return panel;
    }

    // BA-7: human-friendly status labels for all enum values
    private String friendlyStatus(AppointmentStatus s) {
        switch (s) {
            case REQUESTED:        return "Pending Approval";
            case ADMIN_APPROVED:   return "Approved";
            case DOCTOR_ACCEPTED:  return "Accepted by Doctor";
            case DOCTOR_REJECTED:  return "Rejected";
            case COMPLETED:        return "Completed";
            default:               return s.toString().replace("_", " ");
        }
    }

    private void refreshAppointmentTable() {
        DefaultTableModel model = (DefaultTableModel) appointmentTable.getModel();
        model.setRowCount(0);
        List<Patient> patients = DataStore.loadPatients();
        List<Doctor>  doctors  = DataStore.loadDoctors();
        for (Appointment a : DataStore.loadAppointments()) {
            String pName = patients.stream()
                .filter(p -> p.getId().equals(a.getPatientId()))
                .map(p -> p.getName() + " (" + p.getId() + ")")
                .findFirst().orElse("Unknown (" + a.getPatientId() + ")");
            String dName = doctors.stream()
                .filter(d -> d.getId().equals(a.getDoctorId()))
                .map(Doctor::getName).findFirst().orElse("Unknown");
            String sym = (a.getSymptoms() == null || a.getSymptoms().isEmpty()) ? "\u2014" : a.getSymptoms();
            model.addRow(new Object[]{
                a.getId(), pName, dName, a.getDepartment(),
                a.getDate(), a.getTimeSlot(), friendlyStatus(a.getStatus()), sym
            });
        }
        // BA-8: ensure sorter is installed
        if (appointmentTable.getRowSorter() == null) {
            appointmentTable.setRowSorter(new TableRowSorter<>(model));
        }
    }

    // ─── BILLING ──────────────────────────────────────────────────────
    private JPanel createBillingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("BILLING & PAYMENTS", SwingConstants.CENTER);
        title.setFont(UIUtils.FONT_PAGE_TITLE);
        title.setForeground(UIUtils.PRIMARY);
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"Bill ID","Patient","Appointment","Doctor","Base Amount","Tax %","Total","Date","Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        billingTable = new JTable(model);
        UIUtils.styleTable(billingTable);
        panel.add(UIUtils.styledScroll(billingTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.CARD_BORDER));

        JButton paidBtn = UIUtils.makeBtn("MARK AS PAID", UIUtils.SUCCESS);
        paidBtn.setPreferredSize(new Dimension(180, 44));
        paidBtn.addActionListener(e -> {
            int vRow = billingTable.getSelectedRow();
            if (vRow == -1) { UIUtils.showError(panel, "Please select a bill!"); return; }
            int mRow = billingTable.getRowSorter() != null
                ? billingTable.getRowSorter().convertRowIndexToModel(vRow) : vRow;
            String billId = (String) model.getValueAt(mRow, 0);
            List<Bill> bills = DataStore.loadBills();
            for (Bill b : bills) {
                if (b.getId().equals(billId)) {
                    if (b.isPaid()) {
                        UIUtils.showInfo(panel, "Bill " + billId + " is already marked as PAID.");
                        return;
                    }
                    b.setPaid(true);
                    DataStore.saveBills(bills);
                    DataStore.appendLog(new TransactionLog(UIUtils.nowDateTime(), "ADMIN", "ADMIN",
                        "MARK_PAID", billId));
                    refreshBillingTable();
                    UIUtils.showInfo(panel, "Bill " + billId + " marked as PAID!");
                    return;
                }
            }
            UIUtils.showError(panel, "Bill not found.");
        });
        btnPanel.add(paidBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        refreshBillingTable();
        return panel;
    }

    private void refreshBillingTable() {
        DefaultTableModel model = (DefaultTableModel) billingTable.getModel();
        model.setRowCount(0);
        List<Patient> patients = DataStore.loadPatients();
        List<Doctor>  doctors  = DataStore.loadDoctors();
        for (Bill b : DataStore.loadBills()) {
            String pName = patients.stream().filter(p -> p.getId().equals(b.getPatientId()))
                .map(Patient::getName).findFirst().orElse(b.getPatientId());
            String dName = doctors.stream().filter(d -> d.getId().equals(b.getDoctorId()))
                .map(Doctor::getName).findFirst().orElse(b.getDoctorId());
            model.addRow(new Object[]{
                b.getId(), pName, b.getAppointmentId(), dName,
                String.format("\u20B9%.2f", b.getBaseAmount()),
                String.format("%.0f%%", b.getTaxPercent()),
                String.format("\u20B9%.2f", b.getTotalAmount()),
                b.getCreatedAt(),
                b.isPaid() ? "PAID" : "PENDING"
            });
        }
        // BA-8: ensure sorter is installed for billing table
        if (billingTable.getRowSorter() == null) {
            billingTable.setRowSorter(new TableRowSorter<>(model));
        }
    }

    // ─── PRESCRIPTIONS ────────────────────────────────────────────────
    private JPanel createPrescriptionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("PRESCRIPTIONS", SwingConstants.CENTER);
        title.setFont(UIUtils.FONT_PAGE_TITLE);
        title.setForeground(UIUtils.PRIMARY);
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"Prescription ID","Patient","Doctor","Appointment ID","Follow-up Date","Medicines","Diagnosis"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        prescriptionTable = new JTable(model);
        UIUtils.styleTable(prescriptionTable);
        panel.add(UIUtils.styledScroll(prescriptionTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.CARD_BORDER));

        JButton viewBtn = UIUtils.makeBtn("VIEW DETAILS", UIUtils.ACCENT);
        viewBtn.setPreferredSize(new Dimension(160, 44));
        viewBtn.addActionListener(e -> {
            int vRow = prescriptionTable.getSelectedRow();
            if (vRow == -1) { UIUtils.showError(panel, "Please select a prescription!"); return; }
            int mRow = prescriptionTable.getRowSorter() != null
                ? prescriptionTable.getRowSorter().convertRowIndexToModel(vRow) : vRow;
            String presId = (String) model.getValueAt(mRow, 0);
            String patient = (String) model.getValueAt(mRow, 1);
            String doctor  = (String) model.getValueAt(mRow, 2);
            String aptId   = (String) model.getValueAt(mRow, 3);
            String fup     = (String) model.getValueAt(mRow, 4);
            String meds    = (String) model.getValueAt(mRow, 5);
            String diag    = (String) model.getValueAt(mRow, 6);

            // BA-9: styled detail panel instead of raw JOptionPane
            JPanel detail = new JPanel(new GridBagLayout());
            detail.setBackground(Color.WHITE);
            detail.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(6, 8, 6, 8);
            gc.fill   = GridBagConstraints.HORIZONTAL;
            gc.anchor = GridBagConstraints.WEST;

            Object[][] dRows = {
                {"Prescription ID:", presId},
                {"Patient:",         patient},
                {"Doctor:",          doctor},
                {"Appointment ID:",  aptId},
                {"Follow-up Date:",  fup},
                {"Diagnosis:",       diag},
                {"Medicines:",       meds.replace(",", "\n")}
            };
            for (int i = 0; i < dRows.length; i++) {
                gc.gridx = 0; gc.gridy = i; gc.weightx = 0;
                JLabel lbl = UIUtils.formLabel((String) dRows[i][0]);
                lbl.setPreferredSize(new Dimension(160, 28));
                detail.add(lbl, gc);
                gc.gridx = 1; gc.weightx = 1;
                // Medicines can be multi-line — use JTextArea
                if (i == dRows.length - 1) {
                    JTextArea ta = new JTextArea((String) dRows[i][1]);
                    ta.setFont(UIUtils.FONT_BODY);
                    ta.setEditable(false);
                    ta.setBackground(UIUtils.BG_LIGHT);
                    ta.setLineWrap(true); ta.setWrapStyleWord(true);
                    ta.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
                    detail.add(new JScrollPane(ta), gc);
                } else {
                    JLabel val = new JLabel((String) dRows[i][1]);
                    val.setFont(UIUtils.FONT_BODY);
                    detail.add(val, gc);
                }
            }
            UIUtils.showFormDialog(panel, detail, "Prescription Details", "Close", UIUtils.PRIMARY);
        });
        btnPanel.add(viewBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        refreshPrescriptionTable();
        return panel;
    }

    private void refreshPrescriptionTable() {
        DefaultTableModel model = (DefaultTableModel) prescriptionTable.getModel();
        model.setRowCount(0);
        List<Patient> patients = DataStore.loadPatients();
        List<Doctor>  doctors  = DataStore.loadDoctors();
        for (Prescription p : DataStore.loadPrescriptions()) {
            String pName = patients.stream().filter(pt -> pt.getId().equals(p.getPatientId()))
                .map(Patient::getName).findFirst().orElse(p.getPatientId());
            String dName = doctors.stream().filter(d -> d.getId().equals(p.getDoctorId()))
                .map(Doctor::getName).findFirst().orElse(p.getDoctorId());
            model.addRow(new Object[]{
                p.getId(), pName, dName, p.getAppointmentId(),
                p.getFollowUpDate().isEmpty() ? "\u2014" : p.getFollowUpDate(),
                p.getMedicines(), p.getDiagnosis()
            });
        }
    }

    // ─── ANALYTICS ────────────────────────────────────────────────────
    private JPanel createAnalyticsPanel() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(50, 80, 60, 80));

        JLabel title = new JLabel("Hospital Analytics Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 36));
        title.setForeground(UIUtils.PRIMARY);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 40, 0));
        main.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 3, 40, 40));
        grid.setBackground(Color.WHITE);

        Font vFont = new Font("Segoe UI", Font.BOLD, 42);
        Font lFont = new Font("Segoe UI", Font.PLAIN, 18);

        // BA-14: load doctors once
        List<Doctor> doctors = DataStore.loadDoctors();
        int    docCount  = doctors.size();
        int    patCount  = DataStore.loadPatients().size();
        int    aptCount  = DataStore.loadAppointments().size();
        int    billCount = DataStore.loadBills().size();
        double revenue   = DataStore.loadBills().stream().mapToDouble(Bill::getTotalAmount).sum();
        double avgRating = DataStore.loadFeedback().stream()
                             .mapToInt(Feedback::getRating).average().orElse(0.0);

        grid.add(makeAnalyticsCard("Total Doctors",      String.valueOf(docCount),   vFont, lFont, new Color(0,   123, 255)));
        grid.add(makeAnalyticsCard("Total Patients",     String.valueOf(patCount),   vFont, lFont, new Color(40,  167, 69)));
        grid.add(makeAnalyticsCard("Total Appointments", String.valueOf(aptCount),   vFont, lFont, new Color(255, 153, 0)));
        grid.add(makeAnalyticsCard("Bills Issued",       String.valueOf(billCount),  vFont, lFont, new Color(220, 53,  69)));
        grid.add(makeAnalyticsCard("Total Revenue",      "\u20B9" + String.format("%,.2f", revenue), UIUtils.symbolFont(42), lFont, new Color(111, 66, 193)));
        grid.add(makeAnalyticsCard("Avg. Rating",        String.format("%.1f \u2605", avgRating),    UIUtils.symbolFont(42), lFont, new Color(23,  162, 184)));

        main.add(grid, BorderLayout.CENTER);
        return main;
    }

    private JPanel makeAnalyticsCard(String title, String value, Font vf, Font lf, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIUtils.CARD_BORDER, 2),
            BorderFactory.createEmptyBorder(35, 35, 35, 35)
        ));
        JPanel bar = new JPanel(); bar.setBackground(accent); bar.setPreferredSize(new Dimension(0, 8));
        JLabel tl  = new JLabel(title, SwingConstants.CENTER); tl.setFont(lf); tl.setForeground(new Color(80,80,80));
        JLabel vl  = new JLabel(value, SwingConstants.CENTER); vl.setFont(vf); vl.setForeground(accent);
        card.add(bar, BorderLayout.NORTH);
        card.add(tl,  BorderLayout.CENTER);
        card.add(vl,  BorderLayout.SOUTH);
        return card;
    }

    // ─── Helpers ─────────────────────────────────────────────────────
    private javax.swing.event.DocumentListener docListener(Runnable r) {
        return new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { r.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { r.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
        };
    }
}
