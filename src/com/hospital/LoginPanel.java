package com.hospital;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * LoginPanel – tabbed login for Admin, Doctor, Patient.
 *
 * Faults fixed
 * ─────────────
 * F9.  Admin credentials moved to a named constant block (would ideally be in
 *      a config file; at minimum they are not scattered inline).
 * F11. RoundedPanel extracted as a private static inner class instead of being
 *      redefined inside a method on every call.
 * F12. "Invalid Admin  Credentials" double-space typo fixed.
 * F10. Login comparison uses equalsIgnoreCase for username (case-insensitive),
 *      plain-text password comparison documented clearly.
 */
public class LoginPanel extends JPanel {

    // F9: credentials in one place (move to config file in production)
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private final MainWindow mainWindow;

    private final JTextField    adminUserField  = new JTextField(22);
    private final JPasswordField adminPassField = new JPasswordField(22);

    private final JTextField    doctorUserField  = new JTextField(22);
    private final JPasswordField doctorPassField = new JPasswordField(22);

    private final JTextField    patientUserField  = new JTextField(22);
    private final JPasswordField patientPassField = new JPasswordField(22);

    public LoginPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout());
        setBackground(UIUtils.BG_LIGHT);
        // No outer border — top bar spans full width like HomePanel

        // Top bar — identical structure and padding to HomePanel
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UIUtils.PRIMARY);
        topBar.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));  // matches HomePanel exactly

        JLabel titleLabel = new JLabel("Welcome to SHRMS", SwingConstants.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));  // same size as HomePanel title
        titleLabel.setForeground(UIUtils.ACCENT);                 // same accent colour as HomePanel

        JButton backButton = UIUtils.makeBtn("Back to Home", UIUtils.ACCENT);
        backButton.setPreferredSize(new Dimension(160, 44));
        backButton.addActionListener(e -> mainWindow.showHome());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(backButton);

        topBar.add(titleLabel, BorderLayout.WEST);
        topBar.add(right,      BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Tabs area with consistent padding
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UIUtils.BG_LIGHT);
        content.setBorder(BorderFactory.createEmptyBorder(30, 60, 60, 60));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tabs.setBackground(Color.WHITE);

        tabs.addTab("  Admin  ",   createAdminLoginPanel());
        tabs.addTab("  Doctor  ",  createDoctorLoginPanel());
        tabs.addTab("  Patient  ", createPatientLoginPanel());

        content.add(tabs, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private JPanel createAdminLoginPanel() {
        return buildLoginForm("Admin Login", adminUserField, adminPassField,
            "Login as Admin", e -> {
                String user = adminUserField.getText().trim();
                String pass = new String(adminPassField.getPassword());
                // F9/F12: single-place credential check, fixed typo in error msg
                if (ADMIN_USERNAME.equalsIgnoreCase(user) && ADMIN_PASSWORD.equals(pass)) {
                    mainWindow.loginAsAdmin();
                } else {
                    UIUtils.showError(mainWindow, "Invalid admin credentials.");
                }
            });
    }

    private JPanel createDoctorLoginPanel() {
        return buildLoginForm("Doctor Login", doctorUserField, doctorPassField,
            "Login as Doctor", e -> {
                String u = doctorUserField.getText().trim();
                String p = new String(doctorPassField.getPassword());
                // F10: username match is case-insensitive; password plain-text (documented)
                Doctor doc = DataStore.loadDoctors().stream()
                    .filter(d -> d.getUsername().equalsIgnoreCase(u) && d.getPassword().equals(p))
                    .findFirst().orElse(null);
                if (doc != null && doc.isActive()) {
                    mainWindow.loginAsDoctor(doc);
                } else if (doc != null) {
                    UIUtils.showError(mainWindow, "Your account is inactive. Contact admin.");
                } else {
                    UIUtils.showError(mainWindow, "Invalid doctor credentials.");
                }
            });
    }

    private JPanel createPatientLoginPanel() {
        return buildLoginForm("Patient Login", patientUserField, patientPassField,
            "Login as Patient", e -> {
                String u = patientUserField.getText().trim();
                String p = new String(patientPassField.getPassword());
                Patient pat = DataStore.loadPatients().stream()
                    .filter(pt -> pt.getUsername().equalsIgnoreCase(u) && pt.getPassword().equals(p))
                    .findFirst().orElse(null);
                if (pat != null) {
                    mainWindow.loginAsPatient(pat);
                } else {
                    UIUtils.showError(mainWindow, "Invalid patient credentials.");
                }
            });
    }

    /**
     * F11: builds the login card. RoundedPanel is a static inner class —
     * not redefined on every call.
     */
    private JPanel buildLoginForm(String title, JTextField userField,
                                   JPasswordField passField, String btnText,
                                   ActionListener action) {
        RoundedPanel card = new RoundedPanel(40, Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(50, 80, 60, 80));

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLbl.setForeground(UIUtils.PRIMARY);
        card.add(titleLbl, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(20, 10, 20, 10);
        g.fill   = GridBagConstraints.HORIZONTAL;

        JLabel uLbl = new JLabel("Username");
        uLbl.setFont(UIUtils.FONT_LABEL);
        JLabel pLbl = new JLabel("Password");
        pLbl.setFont(UIUtils.FONT_LABEL);

        g.gridx = 0; g.gridy = 0; form.add(uLbl, g);
        g.gridx = 1;              form.add(userField, g);
        g.gridx = 0; g.gridy = 1; form.add(pLbl, g);
        g.gridx = 1;              form.add(passField, g);

        JButton btn = UIUtils.makeBtn(btnText, UIUtils.PRIMARY);
        btn.setPreferredSize(new Dimension(300, 55));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btn.addActionListener(action);
        // Allow pressing Enter in the password field to trigger login
        passField.addActionListener(action);

        g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
        g.insets = new Insets(40, 0, 0, 0);
        form.add(btn, g);

        card.add(form, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    /** F11: static inner class — defined once, not per-call. */
    private static class RoundedPanel extends JPanel {
        private final int   radius;
        private final Color bgColor;
        RoundedPanel(int radius, Color bgColor) {
            this.radius  = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
        }
    }

    public void clearAllFields() {
        adminUserField.setText("");   adminPassField.setText("");
        doctorUserField.setText("");  doctorPassField.setText("");
        patientUserField.setText(""); patientPassField.setText("");
    }
}
