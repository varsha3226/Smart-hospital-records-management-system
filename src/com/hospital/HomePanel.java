package com.hospital;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * HomePanel – public-facing landing screen.
 *
 * Faults fixed
 * ─────────────
 * F13. loadTestimonials() is called every time refreshStats() runs so
 *      testimonials list stays up-to-date during the session.
 * F14. createStandardButton() removed — was dead code that duplicated
 *      UIUtils.makeBtn() with conflicting opacity settings.
 * F15. Departments shown in statsLabel now read dynamically from DataStore
 *      instead of being a hardcoded string.
 */
public class HomePanel extends JPanel {

    private final MainWindow mainWindow;
    private JLabel statsLabel;
    private JLabel testimonialLabel;
    private int testimonialIndex = 0;
    private List<String> testimonials;
    private JLabel carouselLabel;
    private int carouselIndex = 0;

    private static final String[] CAROUSEL_TEXTS = {
        "24/7 Emergency Care",
        "Expert Doctors Across Departments",
        "Advanced Operation Theatre",
        "Patient First Approach"
    };

    public HomePanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout());
        setBackground(UIUtils.BG_LIGHT);

        // Top bar
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UIUtils.PRIMARY);
        top.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("Vasavi Multi-Speciality Hospital", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(UIUtils.ACCENT);

        JButton loginBtn = UIUtils.makeBtn("Login", UIUtils.ACCENT);
        loginBtn.setPreferredSize(new Dimension(120, 44));
        loginBtn.addActionListener(e -> mainWindow.showLogin());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setOpaque(false);
        right.add(loginBtn);
        top.add(title, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // Center content
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        JPanel welcomePanel = new JPanel();
        welcomePanel.setLayout(new BoxLayout(welcomePanel, BoxLayout.Y_AXIS));
        welcomePanel.setOpaque(false);

        JLabel welcome = new JLabel(
            "<html><h1 style='color:#0a3d62; font-size:42px;'>We Make Quality Healthcare</h1>"
            + "<p style='color:#3eadda; font-size:24px;'>Same-Day Emergency Appointments!</p></html>");
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcomePanel.add(welcome);
        welcomePanel.add(Box.createVerticalStrut(30));

        // Use UIUtils.makeBtn for consistency (F14: removed duplicate createStandardButton)
        JButton appointBtn = UIUtils.makeBtn("MAKE AN APPOINTMENT", UIUtils.ACCENT);
        appointBtn.setPreferredSize(new Dimension(300, 60));
        appointBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        appointBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        appointBtn.addActionListener(e -> mainWindow.showLogin());
        welcomePanel.add(appointBtn);

        centerWrapper.add(welcomePanel, BorderLayout.NORTH);

        statsLabel = new JLabel();
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        statsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statsLabel.setForeground(UIUtils.PRIMARY);
        centerWrapper.add(statsLabel, BorderLayout.CENTER);

        carouselLabel = new JLabel(CAROUSEL_TEXTS[0], SwingConstants.CENTER);
        carouselLabel.setFont(new Font("Segoe UI", Font.ITALIC, 22));
        carouselLabel.setForeground(new Color(0, 123, 255));
        centerWrapper.add(carouselLabel, BorderLayout.SOUTH);

        add(centerWrapper, BorderLayout.CENTER);

        testimonialLabel = new JLabel();
        testimonialLabel.setFont(new Font("Segoe UI", Font.ITALIC, 20));
        testimonialLabel.setHorizontalAlignment(SwingConstants.CENTER);
        testimonialLabel.setForeground(new Color(80, 80, 80));
        testimonialLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 40, 0));
        add(testimonialLabel, BorderLayout.SOUTH);

        refreshStats();      // loads stats AND testimonials on first show
        startAnimations();
    }

    // F13: called by MainWindow on every showHome() so data stays fresh
    public void refreshStats() {
        int doctorCount  = DataStore.loadDoctors().size();
        int patientCount = DataStore.loadPatients().size();

        // F15: dynamic department list from DataStore
        java.util.Set<String> deptSet = new java.util.LinkedHashSet<>();
        DataStore.loadDoctors().stream().filter(Doctor::isActive)
            .map(Doctor::getDepartment).forEach(deptSet::add);
        String deptStr = deptSet.isEmpty() ? "N/A" : String.join(", ", deptSet);

        int todayApts = 0;
        String today = UIUtils.nowDate();
        for (Appointment a : DataStore.loadAppointments())
            if (today.equals(a.getDate())) todayApts++;

        statsLabel.setText(
            "<html><center>"
            + "Total Doctors: <b>" + doctorCount + "</b>&nbsp;&nbsp;&nbsp;"
            + "Total Patients: <b>" + patientCount + "</b>&nbsp;&nbsp;&nbsp;"
            + "Appointments Today: <b>" + todayApts + "</b><br>"
            + "Departments: " + deptStr
            + "</center></html>"
        );

        // F13: reload testimonials so new feedback appears without restart
        loadTestimonials();
    }

    private void loadTestimonials() {
        testimonials = new java.util.ArrayList<>();
        testimonials.add("\"Excellent care and very friendly staff.\" \u2014 Anil");
        testimonials.add("\"Clean environment and quick diagnosis.\" \u2014 Priya");
        for (Feedback f : DataStore.loadFeedback()) {
            if (f.getComments() != null && !f.getComments().isBlank()
                    && !f.getComments().equals("No comments provided"))
                testimonials.add("\"" + f.getComments() + "\" \u2014 Patient " + f.getPatientId());
        }
        // Reset index so it doesn't go out of bounds if list shrank
        testimonialIndex = 0;
    }

    private void startAnimations() {
        new javax.swing.Timer(3000, e -> {
            carouselLabel.setText(CAROUSEL_TEXTS[carouselIndex]);
            carouselIndex = (carouselIndex + 1) % CAROUSEL_TEXTS.length;
        }).start();

        new javax.swing.Timer(5000, e -> {
            if (testimonials != null && !testimonials.isEmpty()) {
                testimonialLabel.setText("<html>" + testimonials.get(testimonialIndex) + "</html>");
                testimonialIndex = (testimonialIndex + 1) % testimonials.size();
            }
        }).start();
    }
}
