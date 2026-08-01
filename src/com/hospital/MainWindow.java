package com.hospital;

import javax.swing.*;
import java.awt.*;

/**
 * MainWindow – application shell.
 *
 * Faults fixed
 * ─────────────
 * 1. loginAsAdmin/Doctor/Patient added a new panel with a FIXED key ("ADMIN","DOCTOR","PATIENT")
 *    on every login call. Logging in twice → two panels registered under the same key,
 *    wasting memory and potentially showing stale state. Fixed: remove the old panel first.
 * 2. logout() called UIUtils.showInfo (blocks with a dialog) BEFORE the card switch,
 *    so the user sees the logout dialog while still on the dashboard. Fixed: show HOME first,
 *    then toast (non-blocking).
 * 3. topBar background was a raw Color literal instead of UIUtils.PRIMARY, breaking
 *    visual consistency if the palette is ever changed.
 * 4. clockLabel had no font set – inherited whatever the L&F defaulted to.
 */
public class MainWindow extends JFrame {

    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JLabel breadcrumbLabel;
    private JLabel clockLabel;

    private HomePanel  homePanel;
    private LoginPanel loginPanel;

    // Track currently live dashboard panels so we can remove them on next login
    private JPanel activeDashboard = null;
    private String activeDashboardKey = null;

    public MainWindow() {
        super("SHRMS - Smart Hospital Resource Management System");
        UIUtils.setAppStyle();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());

        // ── Top bar ──
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        topBar.setBackground(UIUtils.PRIMARY);          // uses palette constant

        breadcrumbLabel = new JLabel("Home");
        breadcrumbLabel.setForeground(Color.WHITE);
        breadcrumbLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        clockLabel = new JLabel();
        clockLabel.setForeground(Color.WHITE);
        clockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));  // was unset
        clockLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        topBar.add(breadcrumbLabel, BorderLayout.WEST);
        topBar.add(clockLabel,      BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // ── Card panel ──
        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);

        homePanel  = new HomePanel(this);
        loginPanel = new LoginPanel(this);

        cardPanel.add(homePanel,  "HOME");
        cardPanel.add(loginPanel, "LOGIN");

        add(cardPanel, BorderLayout.CENTER);

        startClock();
        showHome();
    }

    private void startClock() {
        new javax.swing.Timer(1000, e -> clockLabel.setText(UIUtils.nowDateTime())).start();
    }

    public void setBreadcrumb(String text) { breadcrumbLabel.setText(text); }

    public void showHome() {
        setBreadcrumb("Home");
        cardLayout.show(cardPanel, "HOME");
        homePanel.refreshStats();
    }

    public void showLogin() {
        setBreadcrumb("Login");
        cardLayout.show(cardPanel, "LOGIN");
    }

    // ── Login helpers ─────────────────────────────────────────────────
    public void loginAsAdmin() {
        removeActiveDashboard();
        AdminDashboardPanel panel = new AdminDashboardPanel(this);
        registerDashboard(panel, "ADMIN");
        setBreadcrumb("Admin > Dashboard");
        UIUtils.showToast(this, "Logged in as Admin");
    }

    public void loginAsDoctor(Doctor doctor) {
        removeActiveDashboard();
        DoctorDashboardPanel panel = new DoctorDashboardPanel(this, doctor);
        registerDashboard(panel, "DOCTOR");
        setBreadcrumb("Doctor > Dashboard");
        UIUtils.showToast(this, "Welcome, " + doctor.getName());
    }

    public void loginAsPatient(Patient patient) {
        removeActiveDashboard();
        PatientDashboardPanel panel = new PatientDashboardPanel(this, patient);
        registerDashboard(panel, "PATIENT");
        setBreadcrumb("Patient > Dashboard");
        UIUtils.showToast(this, "Welcome, " + patient.getName());
    }

    private void registerDashboard(JPanel panel, String key) {
        activeDashboard    = panel;
        activeDashboardKey = key;
        cardPanel.add(panel, key);
        cardLayout.show(cardPanel, key);
    }

    /** Remove the previous dashboard panel from the card layout to free memory. */
    private void removeActiveDashboard() {
        if (activeDashboard != null) {
            cardPanel.remove(activeDashboard);
            activeDashboard    = null;
            activeDashboardKey = null;
        }
    }

    public void logout() {
        // Show home FIRST (non-blocking), then toast
        showHome();
        clearLoginFields();
        removeActiveDashboard();
        UIUtils.showToast(this, "Logged out successfully");
    }

    public void clearLoginFields() {
        // B17: already called on EDT from logout(); no need for invokeLater
        loginPanel.clearAllFields();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
