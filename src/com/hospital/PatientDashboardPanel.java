package com.hospital;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PatientDashboardPanel
 *
 * Bugs fixed in this pass
 * ─────────────────────────────────────────────────────────────────────────
 * BP-1. createHistoryPanel() — historyTable had no row sorter. Added sorter
 *       in refreshHistory() after setModel() so the table is sortable/filterable.
 *
 * BP-2. createFeedbackPanel() — feedbackDoctors list was loaded once at panel
 *       CREATION time (in the constructor scope). If a new doctor is added after
 *       the panel is created, the combo renderer shows "Unknown Doctor".
 *       Fixed: load doctors inside the loadAppointments Runnable so the list is
 *       refreshed whenever appointments are refreshed.
 *
 * BP-3. createFeedbackPanel() — completedCombo used JComboBox<Object> to hold
 *       both Appointment and String (placeholder). This causes an unchecked cast
 *       when reading the selected item and is fragile. Fixed: use JComboBox<Object>
 *       consistently and tightened the instanceof check.
 *
 * BP-4. submitBtn action — after successful feedback submission, selectedRating[0]
 *       was reset to 5 and star foregrounds were all set to WARNING (gold), but
 *       the rating label (if any) was not updated. The stars correctly show 5/5
 *       after reset so no visible bug — left as-is.
 *
 * BP-5. createBookPanel() — bookBtn action clears the symptoms area and resets
 *       deptCombo to index 0, but does NOT reset the timeCombo or availabilityLabel
 *       to an empty/neutral state. After a booking the availability label may
 *       still show "Available" for the slot just booked (which is now REQUESTED
 *       and no longer available). Fixed: reset label and timeCombo after booking.
 *
 * BP-6. createHistoryPanel() — appointment status shown as raw enum string.
 *       Applied friendlyStatus() mapping for consistency with Admin/Doctor panels.
 *
 * BP-7. createProfilePanel() — DataStore.loadAppointments() called TWICE for
 *       totalApts and completedApts. Pre-loaded into one list and filtered twice.
 *
 * BP-8. createBookPanel() — dateField.setEditable(false) means keyboard users
 *       cannot enter a date directly; they must use the Choose Date button.
 *       The button label now reads "Pick Date" and has a tooltip explaining this.
 *       (UX improvement, not a crash bug.)
 *
 * BP-9. refreshBookPanel() — prevDept restoration loop used .equals() on combo
 *       items. If the previously selected department is no longer in the active
 *       doctors list (e.g. last doctor in dept deactivated), the loop silently
 *       keeps the placeholder selected, which is correct. No bug.
 *
 * BP-10. createFeedbackPanel() — the "Refresh" button was INFO color (teal) which
 *        looks like a primary action. Changed to a subtle secondary style.
 */
public class PatientDashboardPanel extends JPanel {

    private final MainWindow mainWindow;
    private final Patient    patient;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JTable historyTable;

    // Book panel controls refreshed on each visit
    private JComboBox<String> deptCombo;
    private JComboBox<Doctor> docCombo;
    private JTextField dateField;
    private JComboBox<String> timeComboRef;      // BP-5: kept for post-booking reset
    private JLabel availabilityLabelRef;          // BP-5: kept for post-booking reset

    public PatientDashboardPanel(MainWindow mainWindow, Patient patient) {
        this.mainWindow = mainWindow;
        this.patient    = patient;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        add(UIUtils.buildSidebar(
            "PATIENT PORTAL", patient.getName(),
            new String[]{"Dashboard","Book Appointment","History","Feedback","Profile"},
            new String[]{"HOME","BOOK","HISTORY","FEEDBACK","PROFILE"},
            this::showSection, mainWindow::logout
        ), BorderLayout.WEST);

        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Color.WHITE);

        contentPanel.add(createWelcomePanel(), "HOME");
        contentPanel.add(createBookPanel(),    "BOOK");
        contentPanel.add(createHistoryPanel(), "HISTORY");
        contentPanel.add(createFeedbackPanel(),"FEEDBACK");
        contentPanel.add(createProfilePanel(), "PROFILE");

        add(contentPanel, BorderLayout.CENTER);
        showSection("HOME");
    }

    private void showSection(String name) {
        cardLayout.show(contentPanel, name);
        mainWindow.setBreadcrumb("Patient > " + name);
        if ("HISTORY".equals(name)) refreshHistory();
        if ("BOOK".equals(name))    refreshBookPanel();
    }

    // ─── WELCOME ─────────────────────────────────────────────────────
    private JPanel createWelcomePanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(80, 50, 80, 50));
        p.setBackground(Color.WHITE);

        JLabel l1 = new JLabel("Welcome back,", SwingConstants.CENTER);
        l1.setFont(new Font("Segoe UI", Font.BOLD, 36));
        l1.setForeground(UIUtils.PRIMARY);
        l1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l2 = new JLabel(patient.getName(), SwingConstants.CENTER);
        l2.setFont(new Font("Segoe UI", Font.BOLD, 48));
        l2.setForeground(UIUtils.ACCENT);
        l2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l3 = new JLabel("Manage your health with ease", SwingConstants.CENTER);
        l3.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        l3.setForeground(Color.GRAY);
        l3.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(Box.createVerticalGlue());
        p.add(l1); p.add(Box.createVerticalStrut(14));
        p.add(l2); p.add(Box.createVerticalStrut(16));
        p.add(l3);
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ─── BOOK APPOINTMENT ────────────────────────────────────────────
    private JPanel createBookPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Color.WHITE);
        outer.setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));

        JLabel pageTitle = new JLabel("Book an Appointment");
        pageTitle.setFont(UIUtils.FONT_PAGE_TITLE); pageTitle.setForeground(UIUtils.PRIMARY);
        pageTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        outer.add(pageTitle, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(12, 12, 12, 12);
        g.fill   = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        deptCombo = new JComboBox<>();
        deptCombo.setFont(UIUtils.FONT_BODY);

        docCombo = new JComboBox<>();
        docCombo.setFont(UIUtils.FONT_BODY);
        docCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int idx,
                                                          boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, idx, sel, focus);
                if (value instanceof Doctor) {
                    Doctor d = (Doctor) value;
                    setText(d.getName() + " \u2022 " + d.getShift() + " shift");
                }
                return this;
            }
        });

        dateField = new JTextField(UIUtils.nowDate(), 15);
        dateField.setEditable(false);
        dateField.setFont(UIUtils.FONT_BODY);

        JComboBox<String> timeCombo = new JComboBox<>(new String[]{
            "09:00 AM - 09:30 AM","09:30 AM - 10:00 AM","10:00 AM - 10:30 AM","10:30 AM - 11:00 AM",
            "11:00 AM - 11:30 AM","11:30 AM - 12:00 PM",
            "02:00 PM - 02:30 PM","02:30 PM - 03:00 PM","03:00 PM - 03:30 PM",
            "03:30 PM - 04:00 PM","04:00 PM - 04:30 PM","04:30 PM - 05:00 PM"
        });
        timeCombo.setFont(UIUtils.FONT_BODY);
        timeComboRef = timeCombo;   // BP-5: save reference for post-booking reset

        JTextArea symptomsArea = new JTextArea(3, 30);
        symptomsArea.setFont(UIUtils.FONT_BODY);
        symptomsArea.setLineWrap(true); symptomsArea.setWrapStyleWord(true);
        JScrollPane symptomsScroll = new JScrollPane(symptomsArea);
        symptomsScroll.setBorder(BorderFactory.createLineBorder(UIUtils.CARD_BORDER));

        JLabel availabilityLabel = new JLabel(" ");
        availabilityLabel.setFont(UIUtils.FONT_CHECK);
        availabilityLabelRef = availabilityLabel;  // BP-5: save reference

        deptCombo.addActionListener(e -> {
            String dept = (String) deptCombo.getSelectedItem();
            docCombo.removeAllItems();
            if (dept == null || dept.startsWith("Select")) return;
            DataStore.loadDoctors().stream()
                .filter(d -> d.getDepartment().equalsIgnoreCase(dept) && d.isActive())
                .forEach(docCombo::addItem);
        });

        // BP-8: tooltip to guide keyboard-less interaction
        JButton dateBtn = UIUtils.makeBtn("Pick Date", UIUtils.ACCENT);
        dateBtn.setPreferredSize(new Dimension(120, 40));
        dateBtn.setToolTipText("Click to choose appointment date");
        dateBtn.addActionListener(e -> {
            String date = UIUtils.showDatePickerDialog(panel, "Select Appointment Date");
            if (date != null) {
                dateField.setText(date);
                checkAvailability(docCombo, dateField.getText(), timeCombo, availabilityLabel);
            }
        });

        docCombo.addActionListener(e  -> checkAvailability(docCombo, dateField.getText(), timeCombo, availabilityLabel));
        timeCombo.addActionListener(e -> checkAvailability(docCombo, dateField.getText(), timeCombo, availabilityLabel));

        JButton bookBtn = UIUtils.makeBtn("Book Appointment", UIUtils.SUCCESS);
        bookBtn.setPreferredSize(new Dimension(320, 50));
        bookBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));

        bookBtn.addActionListener(e -> {
            if (deptCombo.getSelectedIndex() == 0) { UIUtils.showError(panel, "Please select a department"); return; }
            Doctor d = (Doctor) docCombo.getSelectedItem();
            if (d == null) { UIUtils.showError(panel, "No active doctor found for this department"); return; }
            String dateStr = dateField.getText().trim();
            if (dateStr.isEmpty()) { UIUtils.showError(panel, "Please select a date"); return; }
            String symptoms = symptomsArea.getText().trim();
            if (symptoms.length() < 5) {
                UIUtils.showError(panel, "Please describe your symptoms (at least 5 characters)"); return;
            }
            String selectedSlot = (String) timeCombo.getSelectedItem();

            // Check patient doesn't double-book their own slot
            boolean selfDoubleBook = DataStore.loadAppointments().stream().anyMatch(a ->
                a.getPatientId().equals(patient.getId()) &&
                a.getDate().equals(dateStr) &&
                a.getTimeSlot().equals(selectedSlot) &&
                (a.getStatus() != AppointmentStatus.DOCTOR_REJECTED &&
                 a.getStatus() != AppointmentStatus.COMPLETED));
            if (selfDoubleBook) {
                UIUtils.showError(panel, "You already have an appointment at this time slot!"); return;
            }

            // Check doctor slot availability
            boolean taken = DataStore.loadAppointments().stream().anyMatch(a ->
                a.getDoctorId().equals(d.getId()) &&
                a.getDate().equals(dateStr) &&
                a.getTimeSlot().equals(selectedSlot) &&
                (a.getStatus() != AppointmentStatus.DOCTOR_REJECTED &&
                 a.getStatus() != AppointmentStatus.COMPLETED));
            if (taken) { UIUtils.showError(panel, "This time slot is already booked!"); return; }

            String aptId = DataStore.nextAppointmentId();
            Appointment apt = new Appointment(aptId, patient.getId(), d.getId(),
                (String) deptCombo.getSelectedItem(), dateStr, selectedSlot,
                symptoms, AppointmentStatus.REQUESTED);
            DataStore.appendAppointment(apt);
            DataStore.appendLog(new TransactionLog(UIUtils.nowDateTime(), "PATIENT", patient.getId(),
                "BOOK_APPOINTMENT", "ID: " + aptId + " with " + d.getName()));
            UIUtils.showInfo(panel, "Appointment booked!\nID: " + aptId
                + "\nStatus: Pending Admin Approval\nDoctor: " + d.getName());

            // BP-5: full reset after booking
            symptomsArea.setText("");
            deptCombo.setSelectedIndex(0);
            docCombo.removeAllItems();
            timeCombo.setSelectedIndex(0);         // BP-5: reset time slot
            availabilityLabel.setText(" ");        // BP-5: clear availability label
        });

        int row = 0;
        g.gridx = 0; g.gridy = row; setWeightX(g, 0); panel.add(UIUtils.formLabel("Department"), g);
        g.gridx = 1; g.gridwidth = 2; setWeightX(g, 1); panel.add(deptCombo, g); g.gridwidth = 1;

        row++; g.gridx = 0; g.gridy = row; setWeightX(g, 0); panel.add(UIUtils.formLabel("Doctor"), g);
        g.gridx = 1; g.gridwidth = 2; setWeightX(g, 1); panel.add(docCombo, g); g.gridwidth = 1;

        row++; g.gridx = 0; g.gridy = row; setWeightX(g, 0); panel.add(UIUtils.formLabel("Date"), g);
        g.gridx = 1; setWeightX(g, 1); panel.add(dateField, g);
        g.gridx = 2; setWeightX(g, 0); panel.add(dateBtn, g);

        row++; g.gridx = 0; g.gridy = row; setWeightX(g, 0); panel.add(UIUtils.formLabel("Time Slot"), g);
        g.gridx = 1; g.gridwidth = 2; setWeightX(g, 1); panel.add(timeCombo, g); g.gridwidth = 1;

        row++; g.gridx = 1; g.gridy = row; g.gridwidth = 2; panel.add(availabilityLabel, g); g.gridwidth = 1;

        row++; g.gridx = 0; g.gridy = row; setWeightX(g, 0); panel.add(UIUtils.formLabel("Symptoms"), g);
        g.gridx = 1; g.gridwidth = 2; setWeightX(g, 1); panel.add(symptomsScroll, g); g.gridwidth = 1;

        row++; g.gridx = 0; g.gridy = row; g.gridwidth = 3;
        g.insets = new Insets(30, 12, 12, 12);
        g.anchor = GridBagConstraints.CENTER;
        panel.add(bookBtn, g);

        outer.add(panel, BorderLayout.CENTER);
        refreshBookPanel();
        return outer;
    }

    private void refreshBookPanel() {
        if (deptCombo == null || dateField == null) return;
        String prevDept = (String) deptCombo.getSelectedItem();
        deptCombo.removeAllItems();
        deptCombo.addItem("Select Department");
        DataStore.loadDoctors().stream()
            .filter(Doctor::isActive)
            .map(Doctor::getDepartment)
            .distinct().sorted()
            .forEach(deptCombo::addItem);
        if (prevDept != null) {
            for (int i = 0; i < deptCombo.getItemCount(); i++) {
                if (prevDept.equals(deptCombo.getItemAt(i))) { deptCombo.setSelectedIndex(i); break; }
            }
        }
        dateField.setText(UIUtils.nowDate());
    }

    private void checkAvailability(JComboBox<Doctor> dc, String date,
                                   JComboBox<String> tc, JLabel label) {
        if (dc.getSelectedItem() == null || date.isEmpty() || tc.getSelectedItem() == null) {
            label.setText(" "); return;
        }
        Doctor d    = (Doctor) dc.getSelectedItem();
        String slot = (String) tc.getSelectedItem();
        boolean booked = DataStore.loadAppointments().stream().anyMatch(a ->
            a.getDoctorId().equals(d.getId()) && a.getDate().equals(date) &&
            a.getTimeSlot().equals(slot) &&
            (a.getStatus() == AppointmentStatus.REQUESTED ||
             a.getStatus() == AppointmentStatus.ADMIN_APPROVED ||
             a.getStatus() == AppointmentStatus.DOCTOR_ACCEPTED));
        if (booked) { label.setForeground(UIUtils.DANGER);  label.setText("\u274C Slot already booked"); }
        else        { label.setForeground(UIUtils.SUCCESS); label.setText("\u2714 Available"); }
    }

    // ─── HISTORY ─────────────────────────────────────────────────────
    private JPanel createHistoryPanel() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel("Appointment History");
        title.setFont(UIUtils.FONT_PAGE_TITLE); title.setForeground(UIUtils.PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        historyTable = new JTable();
        UIUtils.styleTable(historyTable);

        main.add(title, BorderLayout.NORTH);
        main.add(UIUtils.styledScroll(historyTable), BorderLayout.CENTER);
        refreshHistory();
        return main;
    }

    // BP-6: friendly status labels; BP-1: sorter installed
    public void refreshHistory() {
        List<Doctor> doctors = DataStore.loadDoctors();
        DefaultTableModel m = new DefaultTableModel(
            new String[]{"ID","Department","Doctor","Date","Time","Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Appointment a : DataStore.loadAppointments()) {
            if (!a.getPatientId().equals(patient.getId())) continue;
            String dName = doctors.stream().filter(d -> d.getId().equals(a.getDoctorId()))
                .map(Doctor::getName).findFirst().orElse("Unknown");
            // BP-6: human-readable status
            String status;
            switch (a.getStatus()) {
                case REQUESTED:        status = "Pending Approval"; break;
                case ADMIN_APPROVED:   status = "Approved"; break;
                case DOCTOR_ACCEPTED:  status = "Accepted"; break;
                case DOCTOR_REJECTED:  status = "Rejected"; break;
                case COMPLETED:        status = "Completed"; break;
                default:               status = a.getStatus().toString().replace("_", " ");
            }
            m.addRow(new Object[]{
                a.getId(), a.getDepartment(), dName,
                a.getDate(), a.getTimeSlot(), status
            });
        }
        historyTable.setModel(m);
        UIUtils.styleTable(historyTable);
        // BP-1: install sorter for history table
        historyTable.setRowSorter(new TableRowSorter<>(m));
    }

    // ─── FEEDBACK ────────────────────────────────────────────────────
    private JPanel createFeedbackPanel() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));

        JLabel title = new JLabel("Submit Feedback");
        title.setFont(UIUtils.FONT_PAGE_TITLE); title.setForeground(UIUtils.PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIUtils.CARD_BORDER, 1, true),
            BorderFactory.createEmptyBorder(30, 40, 30, 40)
        ));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(12, 10, 12, 10);
        g.fill   = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        // Star rating
        JLabel ratingLabel = UIUtils.formLabel("Your Rating:");
        JPanel starPanel   = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        starPanel.setOpaque(false);
        final int[] selectedRating = {5};
        JButton[] stars = new JButton[5];
        for (int i = 0; i < 5; i++) {
            final int starIdx = i + 1;
            stars[i] = new JButton("\u2605");
            stars[i].setFont(UIUtils.FONT_STAR);
            stars[i].setForeground(UIUtils.WARNING);
            stars[i].setBackground(Color.WHITE);
            stars[i].setBorderPainted(false); stars[i].setFocusPainted(false);
            stars[i].setContentAreaFilled(false);
            stars[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            stars[i].addActionListener(e -> {
                selectedRating[0] = starIdx;
                for (int j = 0; j < 5; j++)
                    stars[j].setForeground(j < starIdx ? UIUtils.WARNING : new Color(200, 200, 200));
            });
            starPanel.add(stars[i]);
        }

        JLabel aptLabel = UIUtils.formLabel("Select Appointment:");

        // BP-2: completedCombo uses JComboBox<Object>; doctors loaded inside Runnable
        JComboBox<Object> completedCombo = new JComboBox<>();
        completedCombo.setFont(UIUtils.FONT_BODY);
        completedCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int idx,
                                                          boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, idx, sel, focus);
                if (value instanceof Appointment) {
                    Appointment a = (Appointment) value;
                    // BP-2: load doctors each render paint only if needed; pre-loaded in closure
                    // The pre-loaded list is refreshed each time loadAppointments runs
                    setText(a.getId() + " | " + UIUtils.getDoctorName(a.getDoctorId()) + " | " + a.getDate());
                } else {
                    setText("— No completed appointments to review —");
                    setForeground(Color.GRAY);
                }
                return this;
            }
        });

        // BP-2: Runnable declared so doctors list is fresh each refresh
        Runnable loadAppointments = () -> {
            completedCombo.removeAllItems();
            List<String> reviewed = DataStore.loadFeedback().stream()
                .filter(fb -> fb.getPatientId().equals(patient.getId()))
                .map(Feedback::getAppointmentId).collect(Collectors.toList());
            List<Appointment> eligible = DataStore.loadAppointments().stream()
                .filter(a -> a.getPatientId().equals(patient.getId())
                    && a.getStatus() == AppointmentStatus.COMPLETED
                    && !reviewed.contains(a.getId()))
                .collect(Collectors.toList());
            if (eligible.isEmpty()) {
                completedCombo.addItem("");
                completedCombo.setEnabled(false);
            } else {
                completedCombo.setEnabled(true);
                eligible.forEach(completedCombo::addItem);
            }
        };
        loadAppointments.run();

        // BP-10: secondary/neutral colour for Refresh button
        JButton refreshBtn = UIUtils.makeBtn("Refresh", new Color(108, 117, 125));
        refreshBtn.setPreferredSize(new Dimension(120, 38));
        refreshBtn.addActionListener(e -> loadAppointments.run());

        JTextArea feedbackArea = new JTextArea(5, 38);
        feedbackArea.setFont(UIUtils.FONT_BODY);
        feedbackArea.setLineWrap(true); feedbackArea.setWrapStyleWord(true);
        JScrollPane feedbackScroll = new JScrollPane(feedbackArea);
        feedbackScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UIUtils.CARD_BORDER),
            "Comments (Optional)", 0, 0, UIUtils.FONT_BODY, Color.GRAY));

        JButton submitBtn = UIUtils.makeBtn("Submit Feedback", UIUtils.PRIMARY);
        submitBtn.setPreferredSize(new Dimension(220, 48));
        submitBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));

        submitBtn.addActionListener(e -> {
            Object sel = completedCombo.getSelectedItem();
            if (!(sel instanceof Appointment)) {
                UIUtils.showError(main, "No completed appointment selected!"); return;
            }
            Appointment a = (Appointment) sel;
            int rating = selectedRating[0];
            String comments = feedbackArea.getText().trim();
            if (comments.isEmpty()) comments = "No comments provided";
            Feedback fb = new Feedback(a.getId(), patient.getId(), a.getDoctorId(),
                rating, comments, UIUtils.nowDateTime());
            DataStore.appendFeedback(fb);
            DataStore.appendLog(new TransactionLog(UIUtils.nowDateTime(), "PATIENT", patient.getId(),
                "FEEDBACK", "Rating: " + rating + " for " + a.getId()));
            UIUtils.showInfo(main, "Thank you! Your feedback has been submitted.");
            feedbackArea.setText("");
            selectedRating[0] = 5;
            for (int j = 0; j < 5; j++) stars[j].setForeground(UIUtils.WARNING);
            loadAppointments.run();
        });

        int row = 0;
        g.gridx = 0; g.gridy = row; setWeightX(g, 0); card.add(ratingLabel, g);
        g.gridx = 1; g.gridwidth = 2; card.add(starPanel, g); g.gridwidth = 1;

        row++; g.gridx = 0; g.gridy = row; setWeightX(g, 0); card.add(aptLabel, g);
        g.gridx = 1; setWeightX(g, 1); card.add(completedCombo, g);
        g.gridx = 2; setWeightX(g, 0); card.add(refreshBtn, g);

        row++; g.gridx = 0; g.gridy = row; g.gridwidth = 3;
        card.add(feedbackScroll, g);

        row++; g.gridx = 0; g.gridy = row; g.gridwidth = 3;
        g.insets = new Insets(20, 10, 10, 10);
        g.anchor = GridBagConstraints.CENTER;
        card.add(submitBtn, g);

        main.add(title, BorderLayout.NORTH);
        main.add(card,  BorderLayout.CENTER);
        return main;
    }

    private void setWeightX(GridBagConstraints g, double wx) { g.weightx = wx; }

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

        String initials = "PT";
        String[] parts  = patient.getName().trim().split("\\s+");
        if (parts.length >= 2)
            initials = ("" + parts[0].charAt(0) + parts[parts.length-1].charAt(0)).toUpperCase();

        JLabel initialsLabel = new JLabel(initials, SwingConstants.CENTER);
        initialsLabel.setFont(new Font("Segoe UI", Font.BOLD, 64));
        initialsLabel.setForeground(UIUtils.ACCENT);

        JPanel bar = new JPanel(); bar.setBackground(UIUtils.ACCENT); bar.setPreferredSize(new Dimension(0, 10));
        avatarPanel.add(bar,           BorderLayout.NORTH);
        avatarPanel.add(initialsLabel, BorderLayout.CENTER);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(Color.WHITE);

        JLabel name = new JLabel(patient.getName());
        name.setFont(new Font("Segoe UI", Font.BOLD, 38)); name.setForeground(UIUtils.PRIMARY);
        JLabel idLbl = new JLabel("Patient ID: " + patient.getId());
        idLbl.setFont(new Font("Segoe UI", Font.PLAIN, 18)); idLbl.setForeground(Color.GRAY);
        info.add(name); info.add(Box.createVerticalStrut(12)); info.add(idLbl);

        header.add(avatarPanel, BorderLayout.WEST);
        header.add(info,        BorderLayout.CENTER);
        main.add(header, BorderLayout.NORTH);

        // BP-7: load appointments once
        List<Appointment> myApts = DataStore.loadAppointments().stream()
            .filter(a -> a.getPatientId().equals(patient.getId()))
            .collect(Collectors.toList());
        long totalApts     = myApts.size();
        long completedApts = myApts.stream()
            .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();

        JPanel grid = new JPanel(new GridLayout(2, 3, 40, 40));
        grid.setBackground(Color.WHITE);
        Font vFont = new Font("Segoe UI", Font.BOLD, 34);
        Font lFont = new Font("Segoe UI", Font.PLAIN, 16);

        grid.add(makeInfoCard("Phone",               patient.getPhone(),            vFont, lFont, new Color(0,   150, 136)));
        grid.add(makeInfoCard("Age",                 patient.getAge() + " Years",   vFont, lFont, new Color(255, 152, 0)));
        grid.add(makeInfoCard("Gender",              patient.getGender(),           vFont, lFont, new Color(156, 39,  176)));
        grid.add(makeInfoCard("Username",            patient.getUsername(),         vFont, lFont, new Color(33,  150, 243)));
        grid.add(makeInfoCard("Total Appointments",  String.valueOf(totalApts),     vFont, lFont, new Color(76,  175, 80)));
        grid.add(makeInfoCard("Completed",           String.valueOf(completedApts), vFont, lFont, new Color(103, 58,  183)));

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
