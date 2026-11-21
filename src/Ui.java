import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Colors, fonts, and small builders shared by every screen of the ChocAn GUI, so
 * all of the panels look the same: a soft aqua background, white rounded cards
 * with gentle shadows, sky-blue buttons, and a sidebar for navigation.
 *
 * @author Sydney Renninger
 */
public final class Ui {

    private Ui() { }

    /** Implemented by the main window so a panel can show a message in the status bar. */
    public interface Host {
        void setStatus(String message);
    }

    /** Implemented by panels that show data from the files and can re-read it. */
    public interface Refreshable {
        void refresh();
    }

    /** Implemented by the role screens: their pages are listed in the sidebar. */
    public interface Paged {
        Pages getPages();
        String[] pageIcons();       // one icon name per page (see icon())
    }

    // ---------- colors ----------
    public static final Color NAVY = new Color(0x1F3A4D);       // titles
    public static final Color PRIMARY = new Color(0x2F95BE);    // main buttons, highlights
    public static final Color TEAL = new Color(0x2BB3A3);
    public static final Color ORANGE = new Color(0xF0A04B);
    public static final Color BG_TOP = new Color(0xEEF9FB);
    public static final Color BG_BOTTOM = new Color(0xCDE9F0);
    public static final Color SOFT = new Color(0xDCEEF4);       // quiet buttons
    public static final Color SELECT = new Color(0xD6EEF5);     // selected row / nav item
    public static final Color LINE = new Color(0xD3E5EC);
    public static final Color ROW_ALT = new Color(0xF3FAFC);
    public static final Color HEADER = new Color(0xE4F2F7);
    public static final Color TEXT = new Color(0x263B4A);
    public static final Color MUTED = new Color(0x778A99);
    public static final Color GREEN = new Color(0x2E9E6B);
    public static final Color RED = new Color(0xD9534F);

    // names used by older code
    public static final Color DARK = NAVY;
    public static final Color BROWN = PRIMARY;
    public static final Color ACCENT = new Color(0x5FB3D1);
    public static final Color GOLD = TEAL;
    public static final Color CREAM = BG_TOP;

    // ---------- fonts ----------
    public static final Font FONT = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font BOLD = FONT.deriveFont(Font.BOLD);
    public static final Font SMALL = FONT.deriveFont(12f);
    public static final Font BIG = FONT.deriveFont(Font.BOLD, 28f);
    public static final Font TITLE = FONT.deriveFont(Font.BOLD, 20f);
    public static final Font MONO = new Font("Monospaced", Font.PLAIN, 13);

    /** Call once, before any window is created. */
    public static void init() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // keep the default look and feel
        }
        UIManager.put("defaultFont", FONT);
        UIManager.put("control", BG_TOP);
        UIManager.put("text", TEXT);
        UIManager.put("ScrollBar.thumb", new Color(0x9CCFE0));
        UIManager.put("ScrollBar.thumbShadow", new Color(0x7FBCD2));
        UIManager.put("ScrollBar.thumbHighlight", new Color(0xB5DDEA));
        UIManager.put("ScrollBar.track", ROW_ALT);
        UIManager.put("ScrollBar.trackHighlight", LINE);
        UIManager.put("ScrollBar.width", 14);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.selectionBackground", SELECT);
        UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("CheckBox.font", FONT);
    }

    // =====================================================================
    // Buttons
    // =====================================================================

    public static JButton primary(String text)   { return new RoundButton(text, PRIMARY); }
    public static JButton secondary(String text) { return new RoundButton(text, SOFT); }
    public static JButton danger(String text)    { return new RoundButton(text, RED); }

    private static boolean isLight(Color c) {
        return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) > 170;
    }

    /** A flat, rounded button. White text on dark fills, blue text on light fills. */
    public static class RoundButton extends JButton {
        private Color color;
        private boolean hover;

        public RoundButton(String text, Color color) {
            super(text);
            this.color = color;
            setFont(BOLD);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        public void setColor(Color color) {
            this.color = color;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            if (isPreferredSizeSet()) {
                return super.getPreferredSize();
            }
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(getText()) + 36, fm.getHeight() + 18);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean light = isLight(color);
            Color fill;
            Color text;
            if (!isEnabled()) {
                fill = new Color(0xE3ECEF);
                text = new Color(0x9FB0BA);
            } else {
                text = light ? new Color(0x1F6F94) : Color.WHITE;
                if (getModel().isPressed()) {
                    fill = light ? shade(color, 0.86) : color.darker().darker();
                } else if (hover) {
                    fill = light ? shade(color, 0.93) : color.darker();
                } else {
                    fill = color;
                }
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.setFont(getFont());
            g2.setColor(text);
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    private static Color shade(Color c, double factor) {
        return new Color((int) (c.getRed() * factor), (int) (c.getGreen() * factor), (int) (c.getBlue() * factor));
    }

    /** One entry of the sidebar: an icon and a label; the selected one gets a blue pill. */
    public static class NavButton extends JButton {
        private final String kind;
        private boolean selected;
        private boolean hover;

        public NavButton(String text, String kind) {
            super(text);
            this.kind = kind;
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        public void setSelectedItem(boolean selected) {
            this.selected = selected;
            repaint();
        }

        public boolean isSelectedItem() {
            return selected;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(140, 44);       // narrow on purpose: it is stretched to the sidebar width
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (selected) {
                g2.setColor(SELECT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(PRIMARY);
                g2.fillRoundRect(0, 11, 4, getHeight() - 22, 4, 4);
            } else if (hover) {
                g2.setColor(ROW_ALT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            }
            Icon icon = icon(kind, selected ? PRIMARY : MUTED, 20);
            icon.paintIcon(this, g2, 16, (getHeight() - 20) / 2);
            g2.setFont(selected ? BOLD : FONT);
            g2.setColor(selected ? NAVY : TEXT);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), 48, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // =====================================================================
    // Cards, background, pages
    // =====================================================================

    /** A white rounded card with a soft shadow. */
    public static class RoundedCard extends JPanel {
        public static final int SHADOW = 7;
        public static final int RADIUS = 22;

        public RoundedCard() {
            setOpaque(false);
            setBorder(new EmptyBorder(SHADOW + 16, SHADOW + 20, SHADOW + 16, SHADOW + 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            for (int i = SHADOW; i > 0; i--) {                    // soft shadow, a little lower than the card
                g2.setColor(new Color(31, 99, 130, 5));
                int inset = SHADOW - i;
                g2.fillRoundRect(inset, inset + 2, w - 2 * inset, h - 2 * inset - 2, RADIUS + 2 * i, RADIUS + 2 * i);
            }
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(SHADOW, SHADOW - 1, w - 2 * SHADOW, h - 2 * SHADOW, RADIUS, RADIUS);
            g2.setColor(new Color(0xE4EFF3));
            g2.drawRoundRect(SHADOW, SHADOW - 1, w - 2 * SHADOW - 1, h - 2 * SHADOW - 1, RADIUS, RADIUS);
            g2.dispose();
        }
    }

    /** A white card with a soft shadow and padding. */
    public static JPanel card() {
        return new RoundedCard();
    }

    /** The soft aqua background behind everything. */
    public static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, BG_TOP, getWidth(), getHeight(), BG_BOTTOM));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 255, 255, 70));
            g2.fillOval(getWidth() - 360, -180, 520, 520);
            g2.fillOval(-200, getHeight() - 260, 460, 460);
            g2.dispose();
        }
    }

    /** Pages shown one at a time (the sidebar chooses which). Works like a tabbed pane without the tabs. */
    public static class Pages extends JPanel {
        private final CardLayout layout = new CardLayout();
        private final List<String> titles = new ArrayList<>();
        private final List<ChangeListener> listeners = new ArrayList<>();
        private int selected = 0;

        public Pages() {
            setLayout(layout);
            setOpaque(false);
        }

        public void addTab(String title, Component page) {
            add(page, "p" + titles.size());
            titles.add(title);
        }

        public void setSelectedIndex(int index) {
            selected = index;
            layout.show(this, "p" + index);
            for (ChangeListener l : new ArrayList<>(listeners)) {
                l.stateChanged(new ChangeEvent(this));
            }
        }

        public int getSelectedIndex() {
            return selected;
        }

        public int getTabCount() {
            return titles.size();
        }

        public String getTitleAt(int index) {
            return titles.get(index);
        }

        public void addChangeListener(ChangeListener l) {
            listeners.add(l);
        }
    }

    public static Pages pages() {
        return new Pages();
    }

    // =====================================================================
    // Icons (drawn with shapes, so they look the same everywhere)
    // =====================================================================

    /**
     * A small line icon. kind is one of: check, bill, book, user, users, hospital,
     * report, calendar, sync, logout, money, clock.
     */
    public static Icon icon(String kind, Color color, int size) {
        return new Icon() {
            @Override public int getIconWidth()  { return size; }
            @Override public int getIconHeight() { return size; }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                g2.scale(size / 20.0, size / 20.0);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                switch (kind) {
                    case "check":
                        g2.drawOval(2, 2, 16, 16);
                        g2.drawPolyline(new int[] { 6, 9, 14 }, new int[] { 10, 13, 7 }, 3);
                        break;
                    case "bill":
                        g2.drawRoundRect(4, 2, 12, 16, 3, 3);
                        g2.drawLine(7, 7, 13, 7);
                        g2.drawLine(7, 10, 13, 10);
                        g2.drawLine(7, 13, 11, 13);
                        break;
                    case "book":
                        g2.drawRoundRect(2, 4, 16, 12, 3, 3);
                        g2.drawLine(10, 4, 10, 16);
                        g2.drawLine(4, 8, 8, 8);
                        g2.drawLine(4, 11, 8, 11);
                        g2.drawLine(12, 8, 16, 8);
                        g2.drawLine(12, 11, 16, 11);
                        break;
                    case "user":
                        g2.drawOval(6, 2, 8, 8);
                        g2.drawArc(3, 11, 14, 12, 0, 180);
                        break;
                    case "users":
                        g2.drawOval(3, 4, 6, 6);
                        g2.drawArc(0, 12, 12, 10, 0, 180);
                        g2.drawOval(11, 5, 5, 5);
                        g2.drawArc(9, 12, 10, 9, 0, 180);
                        break;
                    case "hospital":
                        g2.drawRoundRect(3, 4, 14, 13, 3, 3);
                        g2.drawLine(10, 7, 10, 14);
                        g2.drawLine(6, 10, 14, 10);
                        break;
                    case "report":
                        g2.drawRoundRect(3, 3, 14, 14, 3, 3);
                        g2.fillRect(6, 10, 2, 4);
                        g2.fillRect(9, 7, 2, 7);
                        g2.fillRect(12, 9, 2, 5);
                        break;
                    case "calendar":
                        g2.drawRoundRect(3, 4, 14, 13, 3, 3);
                        g2.drawLine(3, 8, 17, 8);
                        g2.drawLine(7, 2, 7, 5);
                        g2.drawLine(13, 2, 13, 5);
                        break;
                    case "sync":
                        g2.drawArc(4, 4, 12, 12, 40, 260);
                        g2.fillPolygon(new int[] { 11, 17, 16 }, new int[] { 4, 5, 10 }, 3);
                        break;
                    case "logout":
                        g2.drawRoundRect(3, 3, 8, 14, 2, 2);
                        g2.drawLine(9, 10, 17, 10);
                        g2.drawPolyline(new int[] { 14, 17, 14 }, new int[] { 7, 10, 13 }, 3);
                        break;
                    case "money":
                        g2.drawRoundRect(2, 5, 16, 10, 3, 3);
                        g2.drawOval(7, 7, 6, 6);
                        break;
                    case "clock":
                        g2.drawOval(2, 2, 16, 16);
                        g2.drawLine(10, 6, 10, 10);
                        g2.drawLine(10, 10, 13, 12);
                        break;
                    default:
                        g2.drawOval(4, 4, 12, 12);
                }
                g2.dispose();
            }
        };
    }

    /** The ChocAn logo: a little chocolate bar. */
    public static Icon brandMark(int size) {
        return new Icon() {
            @Override public int getIconWidth()  { return size; }
            @Override public int getIconHeight() { return size; }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x58B7DA), size, size, PRIMARY));
                g2.fillRoundRect(0, 0, size, size, size / 3, size / 3);
                g2.setColor(new Color(255, 255, 255, 215));
                g2.setStroke(new BasicStroke(Math.max(1.5f, size / 22f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int m = size / 4;
                g2.drawRoundRect(m, m - 1, size - 2 * m, size - 2 * m + 2, size / 8, size / 8);
                g2.drawLine(size / 2, m - 1, size / 2, size - m + 1);
                g2.drawLine(m, size / 2, size - m, size / 2);
                g2.dispose();
            }
        };
    }

    /** A round avatar with a letter in it. */
    public static Icon avatar(String letter, int size) {
        return new Icon() {
            @Override public int getIconWidth()  { return size; }
            @Override public int getIconHeight() { return size; }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(x, y, new Color(0xBFE5F1), x + size, y + size, new Color(0x8CCBE3)));
                g2.fillOval(x, y, size, size);
                g2.setColor(NAVY);
                g2.setFont(BOLD.deriveFont(size * 0.45f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(letter, x + (size - fm.stringWidth(letter)) / 2, y + (size - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
    }

    // =====================================================================
    // Stat tiles (small number cards like the top row of a dashboard)
    // =====================================================================

    /** A small card: a colored icon, a label, and a big number (the value label is updated by the caller). */
    public static JPanel statTile(String iconKind, Color color, String label, JLabel value) {
        JPanel tile = card();
        tile.setBorder(new EmptyBorder(RoundedCard.SHADOW + 10, RoundedCard.SHADOW + 14,
                RoundedCard.SHADOW + 10, RoundedCard.SHADOW + 14));
        tile.setLayout(new java.awt.BorderLayout(12, 0));

        Color tint = new Color(color.getRed(), color.getGreen(), color.getBlue(), 38);
        JPanel box = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(tint);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        box.setOpaque(false);
        box.setPreferredSize(new Dimension(42, 42));
        box.add(new JLabel(icon(iconKind, color, 22)));

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 0));
        text.setOpaque(false);
        text.add(muted(label));
        value.setFont(TITLE);
        value.setForeground(NAVY);
        text.add(value);

        tile.add(box, java.awt.BorderLayout.WEST);
        tile.add(text, java.awt.BorderLayout.CENTER);
        return tile;
    }

    /** Tiles side by side, all the same width. */
    public static JPanel tileRow(JComponent... tiles) {
        JPanel row = new JPanel(new GridLayout(1, tiles.length, 4, 0));
        row.setOpaque(false);
        for (JComponent t : tiles) {
            row.add(t);
        }
        return row;
    }

    // =====================================================================
    // Text and fields
    // =====================================================================

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BOLD);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(TITLE);
        l.setForeground(NAVY);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(SMALL);
        l.setForeground(MUTED);
        return l;
    }

    /** Shows a message in green (ok) or red (not ok). */
    public static void show(JLabel label, String text, boolean ok) {
        label.setText(text);
        label.setForeground(ok ? GREEN : RED);
    }

    public static JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        f.setFont(FONT);
        f.setForeground(TEXT);
        f.setBorder(fieldBorder(LINE));
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { f.setBorder(fieldBorder(PRIMARY)); }
            @Override public void focusLost(FocusEvent e)   { f.setBorder(fieldBorder(LINE)); }
        });
        return f;
    }

    private static javax.swing.border.Border fieldBorder(Color line) {
        return BorderFactory.createCompoundBorder(new LineBorder(line, 1, true), new EmptyBorder(7, 10, 7, 10));
    }

    /**
     * Limits what can be typed into a field: at most max characters, and only
     * characters that match allowed (a one-character regex such as "[0-9]";
     * null means any character except '|').
     */
    public static void limit(JTextComponent c, int max, String allowed) {
        final String rule = (allowed == null) ? "[^|]" : allowed;
        ((AbstractDocument) c.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String s, AttributeSet a) throws BadLocationException {
                replace(fb, offset, 0, s, a);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String s, AttributeSet a) throws BadLocationException {
                if (s == null) {
                    super.replace(fb, offset, length, s, a);
                    return;
                }
                StringBuilder ok = new StringBuilder();
                for (char ch : s.toCharArray()) {
                    if (String.valueOf(ch).matches(rule)) {
                        ok.append(ch);
                    }
                }
                int room = max - (fb.getDocument().getLength() - length);
                if (room <= 0 && ok.length() > 0) {
                    return;
                }
                String text = ok.length() > room ? ok.substring(0, Math.max(room, 0)) : ok.toString();
                super.replace(fb, offset, length, text, a);
            }
        });
    }

    /** Runs the action whenever the text of the component changes. */
    public static void onChange(JTextComponent c, Runnable action) {
        c.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { action.run(); }
            @Override public void removeUpdate(DocumentEvent e)  { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        });
    }

    // =====================================================================
    // Layout helpers
    // =====================================================================

    /** Adds "label  component" as row y of a GridBagLayout form. */
    public static void row(JPanel form, int y, String label, JComponent component) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = y;
        g.gridx = 0;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.insets = new Insets(6, 0, 6, 14);
        form.add(bold(label), g);
        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 0, 6, 0);
        form.add(component, g);
    }

    /** A field followed by a button, side by side. */
    public static JPanel pair(JComponent first, JComponent second) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.add(first);
        p.add(Box.createHorizontalStrut(8));
        p.add(second);
        return p;
    }

    /** Components one under another, each full width but only as tall as it needs to be. */
    public static JPanel stack(int gap, JComponent... items) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTH;
        for (int i = 0; i < items.length; i++) {
            g.gridy = i;
            g.insets = new Insets(0, 0, i == items.length - 1 ? 0 : gap, 0);
            p.add(items[i], g);
        }
        g.gridy = items.length;
        g.weighty = 1;
        g.insets = new Insets(0, 0, 0, 0);
        p.add(Box.createGlue(), g);
        return p;
    }

    /** Keeps a component at its preferred size in the top-left corner of the space it is given. */
    public static JPanel topLeft(JComponent c) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.anchor = GridBagConstraints.NORTHWEST;
        g.weightx = 1;
        g.weighty = 1;
        p.add(c, g);
        return p;
    }

    public static JScrollPane scroll(Component c) {
        JScrollPane s = new JScrollPane(c);
        if (c instanceof JTable) {
            s.setColumnHeaderView(((JTable) c).getTableHeader());
        }
        s.setBorder(new LineBorder(LINE, 1));
        s.getViewport().setBackground(Color.WHITE);
        return s;
    }

    // =====================================================================
    // Tables
    // =====================================================================

    /** Styles a JTable: light header, striped rows, green/red for Active/Suspended. */
    public static void style(JTable t) {
        t.setFont(FONT);
        t.setRowHeight(30);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(SELECT);
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);

        JTableHeader header = t.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(100, 34));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean sel,
                                                           boolean focus, int row, int col) {
                super.getTableCellRendererComponent(table, value, false, false, row, col);
                setBackground(HEADER);
                setForeground(NAVY);
                setFont(BOLD);
                setOpaque(true);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(0, 10, 0, 10)));
                return this;
            }
        });

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean sel,
                                                           boolean focus, int row, int col) {
                super.getTableCellRendererComponent(table, value, sel, false, row, col);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setFont(FONT);
                setForeground(TEXT);
                setBackground(sel ? SELECT : (row % 2 == 0 ? Color.WHITE : ROW_ALT));
                if ("Suspended".equals(value)) {
                    setForeground(RED);
                    setFont(BOLD);
                } else if ("Active".equals(value)) {
                    setForeground(GREEN);
                    setFont(BOLD);
                }
                return this;
            }
        });
    }
}
