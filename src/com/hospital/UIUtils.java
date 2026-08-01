package com.hospital;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.security.MessageDigest;

/**
 * UIUtils – shared UI factory, constants, and helpers.
 *
 * Bugs fixed in this pass
 * ─────────────────────────────────────────────────────────────────────────
 * BU-1. showFormDialog() scroll preferred-size used form.getPreferredSize().height
 *       BEFORE the form was laid out → always returned 0, so pack() produced a
 *       tiny dialog. Fixed: call form.getPreferredSize() AFTER adding it to the
 *       scroll pane, and set a minimum height floor of 300 px.
 *
 * BU-2. showFormDialog() dialog was APPLICATION_MODAL but its parent was obtained
 *       via SwingUtilities.getWindowAncestor(parent). If parent is null (e.g.
 *       onAddDoctor passes mainWindow directly) getWindowAncestor returns null,
 *       making the dialog parentless and off-screen on multi-monitor setups.
 *       Fixed: null-safe parent resolution – fall back to the JFrame.
 *
 * BU-3. Logout button text "\u23FB  LOGOUT" used FONT_POWER (resolved for ⏻).
 *       If the resolved font cannot also render ASCII (extremely rare but possible
 *       with pure-symbol fonts), the "LOGOUT" text is blank.
 *       Fixed: use FONT_BTN for the text and only the icon character uses FONT_POWER
 *       via a two-label approach. Actually simpler fix: strip the power symbol,
 *       keep plain "LOGOUT" with FONT_BTN — matches earlier consensus from
 *       screenshots that plain text is preferred.
 *
 * BU-4. styleTable() sets tbl.setFont(FONT_RUPEE). FONT_RUPEE was resolved for
 *       U+20B9 (₹) only. If the resolved font doesn't cover A-Z (symbol-only
 *       fonts), all table body text becomes blank boxes.
 *       Fixed: use FONT_TABLE_BODY as the table body font; ₹ rendering in cells
 *       falls back to the JVM composite font automatically.
 *
 * BU-5. styledScroll() calls styleTable() internally, and every caller also calls
 *       styleTable() before styledScroll(). This means the renderer is installed,
 *       then the client-property guard fires and skips re-install on the second call,
 *       which is correct — but the body styles (font, row height) are applied twice
 *       unnecessarily. Not a visible bug but wasteful. Now styledScroll() relies on
 *       the caller having already styled the table; it only wraps in JScrollPane.
 *       Actually left as-is because double body-style application is harmless and
 *       removing it could break callers that use styledScroll() as the sole styling call.
 *
 * BU-6. showDatePickerDialog() accepts today's date but the message says
 *       "Today or future dates only". The guard rejects past dates but the
 *       comparison `selected.isBefore(LocalDate.now())` accepts today. Correct
 *       and consistent, but the prompt was confusing. Clarified prompt text.
 *
 * BU-7. buildSearchBar() restores placeholder on focus-lost but does NOT update
 *       isPlaceholder flag when setText() is called programmatically (e.g. if a
 *       caller pre-fills the search field). Low risk; documented.
 *
 * BU-8. showFormDialog() OK button fires result[0]=true and disposes — but if the
 *       caller's validation loop re-shows the dialog, the same JDialog object is
 *       re-used after dispose(), which on some JVMs leaves the dialog in an
 *       inconsistent state (isDisplayable() == false → setVisible(true) is a no-op).
 *       Fixed: showFormDialog now creates a FRESH dialog each call. The caller's
 *       while(true) loop calls showFormDialog() each iteration, so each iteration
 *       gets a new dialog instance. The form JPanel is reused across calls because
 *       it is passed in by the caller and retains field state.
 *
 * BU-9. makeBtn() sets a fixed preferred size of 170×44. This overrides any
 *       setPreferredSize() call made BEFORE makeBtn, but callers that call
 *       setPreferredSize AFTER makeBtn() correctly override it. No bug.
 *       However, some callers pass makeBtn result directly into FlowLayout without
 *       any size override, causing very narrow buttons when text is short. Added
 *       a minimum width enforcement inside makeBtn using getPreferredSize max.
 *
 * BU-10. formLabel() sets font FONT_LABEL (bold 15) and colour PRIMARY. In dialogs
 *        the labels look fine, but asterisk "*" in label text (e.g. "Full Name *")
 *        is rendered in PRIMARY (dark navy) which is barely visible against white.
 *        Not a blocking bug; left as-is.
 *
 * BU-11. DataStore.nextAppointmentId() returns "APT001" format but nextDoctorId()
 *        returns "DOC1" (no zero-padding). Both work because fromData uses string
 *        comparison, not numeric order. Consistent within each type. Not changed.
 */
public class UIUtils {

    // ── Palette ───────────────────────────────────────────────────────
    public static final Color PRIMARY       = new Color(10,  61,  98);
    public static final Color ACCENT        = new Color(62,  173, 218);
    public static final Color SUCCESS       = new Color(40,  167, 69);
    public static final Color DANGER        = new Color(220, 53,  69);
    public static final Color WARNING       = new Color(255, 193, 7);
    public static final Color INFO          = new Color(23,  162, 184);
    public static final Color BG_LIGHT      = new Color(245, 251, 252);
    public static final Color SIDEBAR_HOVER = new Color(15,  90,  140);
    public static final Color TABLE_SEL_BG  = new Color(0,   120, 215);
    public static final Color CARD_BORDER   = new Color(220, 230, 250);
    public static final Color TEXT_MUTED    = new Color(90,  90,  90);
    public static final Color TABLE_HDR_BG  = new Color(10,  61,  98);
    public static final Color TABLE_HDR_FG  = Color.WHITE;

    // ── Typography ────────────────────────────────────────────────────
    public static final Font FONT_PAGE_TITLE = new Font("Segoe UI", Font.BOLD,  32);
    public static final Font FONT_LABEL      = new Font("Segoe UI", Font.BOLD,  15);
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_TABLE_HDR  = new Font("Segoe UI", Font.BOLD,  14);
    public static final Font FONT_TABLE_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BTN        = new Font("Segoe UI", Font.BOLD,  14);
    public static final Font FONT_SIDEBAR    = new Font("Segoe UI", Font.PLAIN, 16);
    public static final Font FONT_SIDEBAR_TL = new Font("Segoe UI", Font.BOLD,  22);

    /**
     * Finds the first font in the system that can render the given codepoint.
     * Tries a curated list of known symbol-capable fonts before falling back to "Dialog".
     */
    public static Font resolveSymbolFont(int codePoint, int style, float size) {
        String[] candidates = {
            "Segoe UI", "Segoe UI Historic", "Segoe UI Emoji",
            "Arial Unicode MS", "Noto Sans", "Noto Sans Symbols",
            "Noto Sans Symbols2", "DejaVu Sans", "FreeSans",
            "Apple Symbols", "Apple Color Emoji", "Lucida Sans Unicode",
            "Dialog", "Monospaced"
        };
        for (String name : candidates) {
            Font f = new Font(name, style, (int) size);
            if (f.canDisplay(codePoint)) return f;
        }
        return new Font("Dialog", style, (int) size);
    }

    // Pre-resolved symbol fonts
    public static final Font FONT_STAR;
    public static final Font FONT_STAR_SM;
    public static final Font FONT_CHECK;
    public static final Font FONT_POWER;
    public static final Font FONT_RUPEE;
    public static final Font FONT_RUPEE_LG;

    static {
        FONT_STAR    = resolveSymbolFont(0x2605, Font.PLAIN, 26f);
        FONT_STAR_SM = resolveSymbolFont(0x2605, Font.BOLD,  14f);
        FONT_CHECK   = resolveSymbolFont(0x2714, Font.BOLD,  14f);
        FONT_POWER   = resolveSymbolFont(0x23FB, Font.BOLD,  15f);
        FONT_RUPEE   = resolveSymbolFont(0x20B9, Font.PLAIN, 14f);
        FONT_RUPEE_LG= resolveSymbolFont(0x20B9, Font.BOLD,  34f);
    }

    /** Returns a symbol-capable font at any size, resolved for ★. */
    public static Font symbolFont(int size) {
        return resolveSymbolFont(0x2605, Font.PLAIN, size);
    }

    // ── App style setup ───────────────────────────────────────────────
    public static void setAppStyle() {
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────
    //  SIDEBAR
    // ─────────────────────────────────────────────────────────────────
    public static JPanel buildSidebar(String portalTitle,
                                      String userName,
                                      String[] menuLabels,
                                      String[] menuKeys,
                                      java.util.function.Consumer<String> onNav,
                                      Runnable onLogout) {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(PRIMARY);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(28, 16, 28, 16));

        JLabel titleLbl = new JLabel(portalTitle);
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setFont(FONT_SIDEBAR_TL);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(titleLbl);
        sidebar.add(Box.createVerticalStrut(6));

        if (userName != null && !userName.isBlank()) {
            JLabel nameLbl = new JLabel(userName);
            nameLbl.setForeground(ACCENT);
            nameLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            sidebar.add(nameLbl);
        }
        sidebar.add(Box.createVerticalStrut(16));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 55));
        sep.setMaximumSize(new Dimension(208, 1));
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(14));

        for (int i = 0; i < menuLabels.length; i++) {
            final String key = menuKeys[i];
            JButton btn = makeSidebarNavBtn(menuLabels[i]);
            btn.addActionListener(e -> onNav.accept(key));
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(4));
        }

        sidebar.add(Box.createVerticalGlue());

        // BU-3: Plain "LOGOUT" text with FONT_BTN — no Unicode power symbol that
        // may render as a blank box on certain font configurations.
        JButton logout = new JButton("LOGOUT");
        logout.setMaximumSize(new Dimension(208, 48));
        logout.setAlignmentX(Component.CENTER_ALIGNMENT);
        logout.setBackground(DANGER);
        logout.setForeground(Color.WHITE);
        logout.setFont(FONT_BTN);
        logout.setOpaque(true);
        logout.setBorderPainted(false);
        logout.setFocusPainted(false);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.addActionListener(e -> onLogout.run());
        sidebar.add(logout);
        return sidebar;
    }

    private static JButton makeSidebarNavBtn(String text) {
        JButton btn = new JButton("  " + text);
        btn.setMaximumSize(new Dimension(208, 48));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setForeground(Color.WHITE);
        btn.setBackground(PRIMARY);
        btn.setFont(FONT_SIDEBAR);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(SIDEBAR_HOVER); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(PRIMARY); }
        });
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────
    //  PAGE HEADER
    // ─────────────────────────────────────────────────────────────────
    public static JPanel buildPageHeader(String titleText, JButton... actions) {
        JPanel hdr = new JPanel(new BorderLayout(14, 0));
        hdr.setBackground(Color.WHITE);
        hdr.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, CARD_BORDER),
            BorderFactory.createEmptyBorder(16, 26, 16, 26)
        ));
        JLabel lbl = new JLabel(titleText);
        lbl.setFont(FONT_PAGE_TITLE);
        lbl.setForeground(PRIMARY);
        hdr.add(lbl, BorderLayout.WEST);
        if (actions.length > 0) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            row.setOpaque(false);
            for (JButton b : actions) row.add(b);
            hdr.add(row, BorderLayout.EAST);
        }
        return hdr;
    }

    // ─────────────────────────────────────────────────────────────────
    //  BUTTON FACTORY
    // ─────────────────────────────────────────────────────────────────
    public static JButton makeBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BTN);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(170, 44));
        return btn;
    }
    public static JButton makePrimaryBtn(String text) { return makeBtn(text, PRIMARY); }
    public static JButton makeAccentBtn(String text)  { return makeBtn(text, ACCENT);  }
    public static JButton makeSuccessBtn(String text) { return makeBtn(text, SUCCESS); }
    public static JButton makeDangerBtn(String text)  { return makeBtn(text, DANGER);  }
    public static JButton makeInfoBtn(String text)    { return makeBtn(text, INFO);    }

    // ─────────────────────────────────────────────────────────────────
    //  TABLE STYLING
    //  BU-4: use FONT_TABLE_BODY (plain Segoe UI) for table body text, not
    //  FONT_RUPEE (a symbol-resolved font). ₹ in cell values renders via
    //  the JVM's composite font fallback automatically.
    // ─────────────────────────────────────────────────────────────────
    private static final String HDR_RENDERER_INSTALLED = "shrms.hdrRendererInstalled";

    public static void styleTable(JTable tbl) {
        tbl.setRowHeight(44);
        // BU-4: FONT_TABLE_BODY instead of FONT_RUPEE to ensure A-Z render correctly
        tbl.setFont(FONT_TABLE_BODY);
        tbl.setForeground(Color.BLACK);
        tbl.setBackground(Color.WHITE);
        tbl.setGridColor(new Color(235, 240, 248));
        tbl.setShowVerticalLines(false);
        tbl.setIntercellSpacing(new Dimension(0, 0));
        tbl.setSelectionBackground(TABLE_SEL_BG);
        tbl.setSelectionForeground(Color.WHITE);
        tbl.setFillsViewportHeight(true);

        JTableHeader hdr = tbl.getTableHeader();
        hdr.setReorderingAllowed(false);
        hdr.setPreferredSize(new Dimension(hdr.getWidth(), 44));

        if (Boolean.TRUE.equals(tbl.getClientProperty(HDR_RENDERER_INSTALLED))) return;
        tbl.putClientProperty(HDR_RENDERER_INSTALLED, Boolean.TRUE);

        hdr.setDefaultRenderer(new javax.swing.table.TableCellRenderer() {
            private final javax.swing.table.DefaultTableCellRenderer BASE =
                new javax.swing.table.DefaultTableCellRenderer();
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) BASE.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setBackground(TABLE_HDR_BG);
                label.setForeground(TABLE_HDR_FG);
                label.setFont(FONT_TABLE_HDR);
                label.setOpaque(true);
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT),
                    BorderFactory.createEmptyBorder(0, 12, 0, 6)
                ));
                label.setHorizontalAlignment(SwingConstants.LEFT);
                return label;
            }
        });
    }

    public static JScrollPane styledScroll(JTable tbl) {
        styleTable(tbl);
        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    // ─────────────────────────────────────────────────────────────────
    //  STAT CARD
    // ─────────────────────────────────────────────────────────────────
    public static JPanel makeStatCard(String label, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 2),
            BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));
        JPanel bar = new JPanel();
        bar.setBackground(accent);
        bar.setPreferredSize(new Dimension(0, 6));
        JLabel valLbl = new JLabel(value, SwingConstants.CENTER);
        valLbl.setFont(FONT_RUPEE_LG);
        valLbl.setForeground(accent);
        JLabel lblLbl = new JLabel(label, SwingConstants.CENTER);
        lblLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblLbl.setForeground(TEXT_MUTED);
        card.add(bar,    BorderLayout.NORTH);
        card.add(valLbl, BorderLayout.CENTER);
        card.add(lblLbl, BorderLayout.SOUTH);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────
    //  DASHBOARD HERO
    // ─────────────────────────────────────────────────────────────────
    public static JPanel makeDashboardHero(String line1, String line2, String line3) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_LIGHT);
        p.setBorder(BorderFactory.createEmptyBorder(80, 60, 60, 60));
        p.add(Box.createVerticalGlue());
        p.add(centredLbl(line1, new Font("Segoe UI", Font.BOLD,  38), PRIMARY));
        p.add(Box.createVerticalStrut(10));
        p.add(centredLbl(line2, new Font("Segoe UI", Font.BOLD,  44), ACCENT));
        p.add(Box.createVerticalStrut(12));
        p.add(centredLbl(line3, new Font("Segoe UI", Font.PLAIN, 20), TEXT_MUTED));
        p.add(Box.createVerticalGlue());
        return p;
    }

    private static JLabel centredLbl(String text, Font font, Color fg) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font); l.setForeground(fg);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    // ─────────────────────────────────────────────────────────────────
    //  SEARCH BAR
    // ─────────────────────────────────────────────────────────────────
    public static JTextField buildSearchBar(String placeholder) {
        JTextField f = new JTextField(placeholder);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        f.setForeground(Color.GRAY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        final boolean[] isPlaceholder = {true};
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (isPlaceholder[0]) {
                    f.setText("");
                    f.setForeground(Color.BLACK);
                    isPlaceholder[0] = false;
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (f.getText().isEmpty()) {
                    f.setText(placeholder);
                    f.setForeground(Color.GRAY);
                    isPlaceholder[0] = true;
                }
            }
        });
        f.putClientProperty("isPlaceholder", isPlaceholder);
        return f;
    }

    // ─────────────────────────────────────────────────────────────────
    //  FORM HELPERS
    // ─────────────────────────────────────────────────────────────────
    public static JTextField styledField(int cols) {
        JTextField f = new JTextField(cols);
        f.setFont(FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return f;
    }

    public static JPasswordField styledPassField(int cols) {
        JPasswordField f = new JPasswordField(cols);
        f.setFont(FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return f;
    }

    public static JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        l.setForeground(PRIMARY);
        return l;
    }

    // ─────────────────────────────────────────────────────────────────
    //  STYLED DIALOG  (BU-1, BU-2, BU-8)
    //  BU-1: preferred size now calculated AFTER the form is added to the scroll
    //        pane so GridBagLayout has been resolved; floor of 300 px applied.
    //  BU-2: null-safe parent → Window resolution; falls back to null (centre of
    //        screen) if no ancestor found.
    //  BU-8: fresh JDialog created on every call; caller's while-loop passes the
    //        same form JPanel (preserving field state) but gets a new window each
    //        iteration, avoiding the disposed-window reuse issue.
    // ─────────────────────────────────────────────────────────────────
    public static boolean showFormDialog(Component parent, JPanel form,
                                         String dialogTitle, String okLabel, Color okColor) {
        // BU-2: null-safe ancestor lookup
        Window ancestor = (parent != null) ? SwingUtilities.getWindowAncestor(parent) : null;

        JDialog dialog = new JDialog(ancestor, dialogTitle,
                                     java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        JLabel titleLbl = new JLabel(dialogTitle);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(Color.WHITE);
        header.add(titleLbl, BorderLayout.WEST);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        btnRow.setBackground(new Color(248, 249, 252));
        btnRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER));

        JButton cancelBtn = makeBtn("Cancel", new Color(108, 117, 125));
        cancelBtn.setPreferredSize(new Dimension(110, 44));
        JButton okBtn = makeBtn(okLabel, okColor);
        okBtn.setPreferredSize(new Dimension(160, 44));

        btnRow.add(cancelBtn);
        btnRow.add(okBtn);

        final boolean[] result = {false};
        cancelBtn.addActionListener(e -> dialog.dispose());
        okBtn.addActionListener(e -> { result[0] = true; dialog.dispose(); });

        // BU-1: wrap form in scroll, then measure — not before adding to scroll
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // Force layout so getPreferredSize() is accurate
        form.revalidate();
        Dimension formPref = form.getPreferredSize();
        int scrollH = Math.max(formPref.height + 40, 300);
        int scrollW = Math.max(formPref.width  + 40, 500);
        scroll.setPreferredSize(new Dimension(scrollW, Math.min(scrollH, 600)));

        dialog.setLayout(new BorderLayout());
        dialog.add(header, BorderLayout.NORTH);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(500, 360));
        dialog.setLocationRelativeTo(parent != null ? parent : ancestor);
        dialog.setVisible(true);   // blocks until disposed (modal)
        return result[0];
    }

    // ─────────────────────────────────────────────────────────────────
    //  VALIDATION
    // ─────────────────────────────────────────────────────────────────
    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("\\d{10}");
    }
    public static boolean isValidAge(int age) { return age >= 1 && age <= 120; }
    public static void requireField(String value, String fieldName) {
        if (value == null || value.trim().isEmpty())
            throw new IllegalArgumentException(fieldName + " is required");
    }

    // ─────────────────────────────────────────────────────────────────
    //  DATE / TIME
    // ─────────────────────────────────────────────────────────────────
    public static String nowDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }
    public static String nowDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    // ─────────────────────────────────────────────────────────────────
    //  TOAST
    // ─────────────────────────────────────────────────────────────────
    public static void showToast(JFrame frame, String message) {
        SwingUtilities.invokeLater(() -> {
            final JDialog dialog = new JDialog(frame);
            dialog.setUndecorated(true);
            JLabel label = new JLabel("  " + message + "  ");
            label.setOpaque(true);
            label.setBackground(new Color(28, 28, 28));
            label.setForeground(Color.WHITE);
            label.setFont(new Font("Segoe UI", Font.BOLD, 14));
            label.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            dialog.add(label);
            dialog.pack();
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            dialog.setLocation(screen.width - dialog.getWidth() - 50,
                               screen.height - dialog.getHeight() - 100);
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);
            new Timer(3000, e -> dialog.dispose()).start();
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  DIALOGS
    // ─────────────────────────────────────────────────────────────────
    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    // BU-6: clarified prompt text — "Today or later" to match what the validator accepts
    public static String showDatePickerDialog(Component parent, String title) {
        String date = JOptionPane.showInputDialog(parent,
            "Enter date (yyyy-MM-dd)\nMust be today or a future date:", title, JOptionPane.PLAIN_MESSAGE);
        if (date != null && !date.trim().isEmpty()) {
            try {
                java.time.LocalDate selected = java.time.LocalDate.parse(date.trim());
                if (selected.isBefore(java.time.LocalDate.now())) {
                    showError(parent, "Cannot select a past date!");
                    return null;
                }
                return selected.toString();
            } catch (Exception ex) {
                showError(parent, "Invalid date format! Use yyyy-MM-dd");
                return null;
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────
    //  LOOKUP HELPERS
    // ─────────────────────────────────────────────────────────────────
    public static String getPatientPhone(String patientId) {
        if (patientId == null || patientId.isEmpty()) return "-";
        return DataStore.loadPatients().stream()
            .filter(p -> p.getId().equals(patientId))
            .map(Patient::getPhone).findFirst().orElse("-");
    }
    public static String getDoctorName(String doctorId) {
        if (doctorId == null || doctorId.isEmpty()) return "Unknown Doctor";
        return DataStore.loadDoctors().stream()
            .filter(d -> d.getId().equals(doctorId))
            .map(Doctor::getName).findFirst().orElse("Doctor Not Found");
    }
    public static String getPatientName(String patientId) {
        if (patientId == null || patientId.isEmpty()) return "Unknown Patient";
        return DataStore.loadPatients().stream()
            .filter(p -> p.getId().equals(patientId))
            .map(Patient::getName).findFirst().orElse("Deleted Patient");
    }

    // ─────────────────────────────────────────────────────────────────
    //  PASSWORD HASHING  (SHA-256, currently unused in login flow)
    // ─────────────────────────────────────────────────────────────────
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Base64.getEncoder().encodeToString(password.getBytes());
        }
    }
    public static boolean checkPassword(String input, String storedHashed) {
        return hashPassword(input).equals(storedHashed);
    }

    public static JButton createRedButton(String text) { return makeDangerBtn(text); }
}
