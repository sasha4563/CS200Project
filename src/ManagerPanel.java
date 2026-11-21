import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Manager screens: request each report (or run the whole accounting
 * procedure), see and clear this week's billed services, and manage the
 * Acme updates.
 *
 * @author Sydney Renninger
 */
public class ManagerPanel extends JPanel implements Ui.Refreshable, Ui.Paged {

    private final Ui.Host host;
    private final ChocAnDatabase db;
    private final ReportController reports;
    private final AcmeInterface acme;
    private final Timer scheduler;
    private final String acmeFile = AppFolders.dataDir() + "/acmesimulation.txt";
    private final Ui.Pages pages = Ui.pages();

    // reports page
    private final DefaultListModel<Report> reportList = new DefaultListModel<>();
    private final JList<Report> list = new JList<>(reportList);
    private final JTextArea preview = new JTextArea();
    private final JLabel reportMessage = Ui.label(" ");

    // this week page
    private final DefaultTableModel weekModel = new DefaultTableModel(
            new Object[] { "Received", "Service date", "Provider", "Member", "Code", "Service", "Fee" }, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JLabel weekServices = new JLabel("0");
    private final JLabel weekFees = new JLabel("$0.00");
    private final JLabel weekProviders = new JLabel("0");
    private final JLabel weekMembers = new JLabel("0");
    private final JLabel weekMessage = Ui.label(" ");
    private final JLabel weeklyInfo = Ui.muted(" ");

    // acme page
    private final DefaultTableModel acmeModel = new DefaultTableModel(
            new Object[] { "Member #", "Member", "New status", "Takes effect", "State" }, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JLabel acmeWaiting = new JLabel("0");
    private final JLabel acmeApplied = new JLabel("0");
    private final JLabel acmeNext = new JLabel("-");
    private final JLabel acmeMessage = Ui.label(" ");
    private final JLabel acmeLast = Ui.muted(" ");

    public ManagerPanel(Ui.Host host, ChocAnDatabase db, ReportController reports, AcmeInterface acme, Timer scheduler) {
        this.host = host;
        this.db = db;
        this.reports = reports;
        this.acme = acme;
        this.scheduler = scheduler;

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(0, 6, 0, 0));

        pages.addTab("Reports", reportsPage());
        pages.addTab("This Week", weekPage());
        pages.addTab("Acme Updates", acmePage());
        pages.addChangeListener(e -> refresh());
        add(pages, BorderLayout.CENTER);
        refresh();
    }

    @Override public Ui.Pages getPages() { return pages; }

    @Override public String[] pageIcons() { return new String[] { "report", "calendar", "sync" }; }

    @Override
    public void refresh() {
        loadWeek();
        loadAcme();
    }

    // =====================================================================
    // Reports
    // =====================================================================

    private JPanel reportsPage() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);

        JPanel left = Ui.card();
        left.setLayout(new BorderLayout(0, 14));
        left.setPreferredSize(new Dimension(340, 400));
        left.add(Ui.title("Request a report"), BorderLayout.NORTH);

        JButton member = Ui.primary("Member reports");
        JButton provider = Ui.primary("Provider reports");
        JButton summary = Ui.primary("Accounts Payable Summary");
        JButton eft = Ui.primary("EFT data file");
        JButton all = Ui.secondary("Run main accounting procedure");
        left.add(Ui.stack(10, member, provider, summary, eft, all), BorderLayout.CENTER);

        JPanel info = new JPanel(new BorderLayout(0, 6));
        info.setOpaque(false);
        info.add(Ui.muted("<html><body style='width:240px'>Files are saved in " + AppFolders.reportsDir()
                + ".<br>Reports use this week's data and do not clear it.</body></html>"), BorderLayout.NORTH);
        info.add(reportMessage, BorderLayout.SOUTH);
        left.add(info, BorderLayout.SOUTH);

        list.setFont(Ui.FONT);
        list.setCellRenderer((jl, value, index, selected, focus) -> {
            JLabel l = Ui.label(value.getPath());
            l.setOpaque(true);
            l.setBorder(new EmptyBorder(5, 10, 5, 10));
            l.setBackground(selected ? Ui.SELECT : (index % 2 == 0 ? java.awt.Color.WHITE : Ui.ROW_ALT));
            return l;
        });
        list.addListSelectionListener(e -> {
            Report r = list.getSelectedValue();
            preview.setText(r == null ? "" : String.join("\n", r.getLines()));
            preview.setCaretPosition(0);
        });
        preview.setEditable(false);
        preview.setFont(Ui.MONO);
        preview.setBorder(new EmptyBorder(8, 10, 8, 10));

        JPanel listBox = new JPanel(new BorderLayout(0, 8));
        listBox.setOpaque(false);
        listBox.add(Ui.bold("Files saved by the last request (click one to preview it)"), BorderLayout.NORTH);
        listBox.add(Ui.scroll(list), BorderLayout.CENTER);
        listBox.setPreferredSize(new Dimension(200, 190));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listBox, Ui.scroll(preview));
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerLocation(200);
        split.setResizeWeight(0.3);

        JPanel right = Ui.card();
        right.setLayout(new BorderLayout());
        right.add(split, BorderLayout.CENTER);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.CENTER);

        member.addActionListener(e -> request("member"));
        provider.addActionListener(e -> request("provider"));
        summary.addActionListener(e -> request("summary"));
        eft.addActionListener(e -> request("eft"));
        all.addActionListener(e -> request(null));
        return panel;
    }

    /** Builds one report type (or all of them when type is null) and shows the saved files. */
    void request(String type) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            boolean ok;
            if (type == null) {
                reports.runWeeklyReports();
                ok = !reports.getReports().isEmpty();
            } else {
                ok = reports.makeReport(type);
            }
            reportList.clear();
            for (Report r : reports.getReports()) {
                reportList.addElement(r);
            }
            if (!reportList.isEmpty()) {
                int pick = 0;
                for (int i = 0; i < reportList.size(); i++) {
                    if (reportList.get(i) instanceof SummaryReport) {
                        pick = i;
                    }
                }
                list.setSelectedIndex(pick);
            } else {
                preview.setText("");
            }
            String note = reports.getNote().isEmpty() ? "" : "  " + reports.getNote();
            if (ok) {
                Ui.show(reportMessage, "<html><body style='width:240px'>Report saved: " + reportList.size() + " file(s)." + note + "</body></html>", true);
                host.setStatus("Report saved (" + reportList.size() + " file(s)).");
            } else {
                String why = reports.getNote().isEmpty() ? "Report could not be generated." : reports.getNote();
                Ui.show(reportMessage, "<html><body style='width:240px'>" + why + "</body></html>", false);
                host.setStatus(why);
            }
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    // =====================================================================
    // This week's services
    // =====================================================================

    private JPanel weekPage() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);

        panel.add(Ui.tileRow(
                Ui.statTile("bill", Ui.PRIMARY, "Services this week", weekServices),
                Ui.statTile("money", Ui.GREEN, "Total fees", weekFees),
                Ui.statTile("hospital", Ui.TEAL, "Providers to be paid", weekProviders),
                Ui.statTile("users", Ui.ORANGE, "Members served", weekMembers)), BorderLayout.NORTH);

        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 12));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Ui.title("This week's billed services"), BorderLayout.NORTH);
        JPanel sub = new JPanel(new BorderLayout(0, 2));
        sub.setOpaque(false);
        sub.add(Ui.muted("This is the data every weekly report is built from (the last 7 days of records.txt)."), BorderLayout.NORTH);
        sub.add(weeklyInfo, BorderLayout.SOUTH);
        head.add(sub, BorderLayout.SOUTH);

        JTable table = new JTable(weekModel);
        Ui.style(table);
        int[] widths = { 160, 100, 200, 130, 70, 170, 80 };
        for (int c = 0; c < widths.length; c++) {
            table.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);
        }

        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setOpaque(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        JButton reload = Ui.secondary("Refresh");
        JButton clear = Ui.danger("Clear this week's data");
        reload.addActionListener(e -> loadWeek());
        clear.addActionListener(e -> clearWeek());
        buttons.add(clear);
        buttons.add(reload);
        south.add(buttons, BorderLayout.NORTH);
        south.add(weekMessage, BorderLayout.SOUTH);

        card.add(head, BorderLayout.NORTH);
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    /** Asks for confirmation, then starts a new week (see doClearWeek). */
    private void clearWeek() {
        int n = db.getWeekRecords().size();
        if (n == 0) {
            Ui.show(weekMessage, "There is no data to clear: no services were billed this week.", true);
            return;
        }
        int answer = javax.swing.JOptionPane.showConfirmDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                "Move this week's " + n + " billed service(s) to records_archive.txt?\n"
                        + "Reports will be empty until new services are billed.\n"
                        + "(The weekly update does this by itself every Friday at 12:00 AM, after its reports.)",
                "Clear this week's data", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
        if (answer == javax.swing.JOptionPane.YES_OPTION) {
            doClearWeek();
        }
    }

    /** Moves this week's records to the archive (after the manager has confirmed). */
    void doClearWeek() {
        int moved = reports.startNewWeek();
        if (moved < 0) {
            Ui.show(weekMessage, reports.getNote(), false);
        } else {
            Ui.show(weekMessage, moved + " service record(s) moved to records_archive.txt. This week starts fresh.", true);
            host.setStatus("This week's data cleared (" + moved + " record(s) archived).");
        }
        loadWeek();
    }

    private void loadWeek() {
        weekModel.setRowCount(0);
        double total = 0;
        Set<String> providers = new HashSet<>();
        Set<String> members = new HashSet<>();
        List<ServiceRecord> week = db.getWeekRecords();
        for (ServiceRecord r : week) {
            Provider p = db.getProvider(r.getProviderNum());
            Member m = db.getMember(r.getMemberNum());
            weekModel.addRow(new Object[] {
                    r.getDateReceived(), r.getServiceDate(),
                    p == null ? r.getProviderNum() + " (deleted)" : p.getName(),
                    m == null ? r.getMemberNum() + " (deleted)" : m.getName(),
                    r.getServiceCode(), r.getService().getName(),
                    String.format(Locale.US, "$%.2f", r.getFee()) });
            total += r.getFee();
            providers.add(r.getProviderNum());
            members.add(r.getMemberNum());
        }
        weekServices.setText(String.valueOf(week.size()));
        weekFees.setText(String.format(Locale.US, "$%.2f", total));
        weekProviders.setText(String.valueOf(providers.size()));
        weekMembers.setText(String.valueOf(members.size()));
        weeklyInfo.setText("Next weekly update: " + scheduler.getNextDue().format(ServiceRecord.DATE_TIME)
                + " (Friday 12:00 AM) - runs all reports, then clears this week's data.");
    }

    // =====================================================================
    // Acme updates
    // =====================================================================

    private JPanel acmePage() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);

        panel.add(Ui.tileRow(
                Ui.statTile("clock", Ui.ORANGE, "Changes waiting", acmeWaiting),
                Ui.statTile("check", Ui.GREEN, "Already applied", acmeApplied),
                Ui.statTile("calendar", Ui.PRIMARY, "Next change at", acmeNext)), BorderLayout.NORTH);

        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 12));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Ui.title("Acme Accounting updates"), BorderLayout.NORTH);
        head.add(Ui.muted("Read from " + acmeFile + ". Each line takes effect once its time has passed; the program checks every 10 seconds."),
                BorderLayout.SOUTH);

        JTable table = new JTable(acmeModel);
        Ui.style(table);
        int[] acmeWidths = { 110, 190, 120, 180, 150 };
        for (int c = 0; c < acmeWidths.length; c++) {
            table.getColumnModel().getColumn(c).setPreferredWidth(acmeWidths[c]);
        }

        JButton check = Ui.primary("Check schedule now");
        JButton apply = Ui.danger("Force apply Acme changes now");
        JButton reload = Ui.secondary("Refresh");
        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setOpaque(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(check);
        buttons.add(apply);
        buttons.add(reload);
        buttons.add(acmeMessage);
        south.add(buttons, BorderLayout.NORTH);
        south.add(acmeLast, BorderLayout.SOUTH);

        check.addActionListener(e -> {
            int n = acme.checkSchedule();
            Ui.show(acmeMessage, n + " membership status update(s) applied.", true);
            host.setStatus("[Acme] " + n + " update(s) applied.");
            loadAcme();
        });
        apply.addActionListener(e -> {
            int n = acme.receiveUpdatesNow(acmeFile);
            if (n < 0) {
                Ui.show(acmeMessage, "Could not read " + acmeFile, false);
            } else {
                Ui.show(acmeMessage, n + " membership status update(s) applied.", true);
                host.setStatus("[Acme] " + n + " update(s) applied (ignoring the times).");
            }
            loadAcme();
        });
        reload.addActionListener(e -> loadAcme());

        card.add(head, BorderLayout.NORTH);
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private void loadAcme() {
        acmeModel.setRowCount(0);
        int applied = 0;
        try {
            for (String raw : Files.readAllLines(Paths.get(acmeFile), StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] f = line.split(",");
                for (int i = 0; i < f.length; i++) {
                    f[i] = f[i].trim();
                }
                if (f.length < 2) {
                    continue;
                }
                Member m = db.getMember(f[0]);
                String time = f.length > 2 ? f[2] : "";
                String state = f.length > 3 ? f[3] : pendingState(time);
                if (state.equalsIgnoreCase("APPLIED")) {
                    applied++;
                }
                acmeModel.addRow(new Object[] { f[0], m == null ? "(not a member)" : m.getName(), f[1], time, state });
            }
        } catch (IOException e) {
            acmeModel.addRow(new Object[] { "", "Could not read " + acmeFile, "", "", "" });
        }
        acmeWaiting.setText(String.valueOf(acme.countPending()));
        acmeApplied.setText(String.valueOf(applied));
        String next = acme.nextScheduled();
        acmeNext.setText(next.isEmpty() ? "-" : next.substring(0, 5) + " " + next.substring(11, 16));
        acmeLast.setText("Last update applied: " + acme.getLastUpdate());
    }

    private static String pendingState(String time) {
        if (time.isEmpty()) {
            return "Pending";
        }
        try {
            LocalDateTime due = LocalDateTime.parse(time, ServiceRecord.DATE_TIME);
            return due.isAfter(LocalDateTime.now()) ? "Scheduled" : "Pending (due now)";
        } catch (DateTimeParseException e) {
            return "Pending";
        }
    }
}
