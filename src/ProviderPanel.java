import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Provider screens: validate a member, bill ChocAn for a service, and
 * request the Provider Directory.
 *
 * @author Sydney Renninger
 */
public class ProviderPanel extends JPanel implements Ui.Paged, Ui.Refreshable {

    private final Ui.Host host;
    private final ChocAnDatabase db;
    private final ProviderDirectory directory;
    private final ServiceController billing;
    private final MemberController validator;
    private final String providerNum;
    private final Ui.Pages pages = Ui.pages();
    private final JLabel tileMembers = new JLabel("0");
    private final JLabel tileActive = new JLabel("0");
    private final JLabel tileServices = new JLabel("0");

    // validate tab
    private final JTextField validateNumber = Ui.field(14);
    private final JLabel validateResult = new JLabel(" ");

    // bill tab
    private final JTextField billMember = Ui.field(14);
    private final JLabel memberMessage = Ui.label(" ");
    private final JTextField billDate = Ui.field(12);
    private final JTextField billCode = Ui.field(8);
    private final JLabel serviceMessage = Ui.label(" ");
    private final JCheckBox confirm = new JCheckBox("Yes, this is the correct service");
    private final JTextArea comments = new JTextArea(3, 26);
    private final JLabel counter = Ui.muted("0 / 100");
    private final JLabel billMessage = Ui.label(" ");
    private final JTextArea receipt = new JTextArea();

    // directory tab
    private final JLabel directoryMessage = Ui.label(" ");
    private DefaultTableModel directoryModel;

    public ProviderPanel(Ui.Host host, ChocAnDatabase db, ProviderDirectory directory,
                         ServiceController billing, MemberController validator, String providerNum) {
        this.host = host;
        this.db = db;
        this.directory = directory;
        this.billing = billing;
        this.validator = validator;
        this.providerNum = providerNum;

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(0, 6, 0, 0));

        pages.addTab("Validate Member", validatePage());
        pages.addTab("Bill ChocAn", billTab());
        pages.addTab("Provider Directory", directoryTab());
        add(pages, BorderLayout.CENTER);
        pages.addChangeListener(e -> refresh());
        refresh();
    }

    /** Re-reads the member counts shown on the first page. */
    @Override
    public void refresh() {
        int active = 0;
        List<Member> all = db.getMembers();
        for (Member m : all) {
            if ("Active".equalsIgnoreCase(m.getStatus())) {
                active++;
            }
        }
        tileMembers.setText(String.valueOf(all.size()));
        tileActive.setText(String.valueOf(active));
        tileServices.setText(String.valueOf(directory.getServices().size()));
    }

    /** First page: three number tiles, then the validate card. */
    private JPanel validatePage() {
        JPanel page = new JPanel(new BorderLayout(0, 6));
        page.setOpaque(false);
        page.add(Ui.tileRow(
                Ui.statTile("users", Ui.PRIMARY, "ChocAn members", tileMembers),
                Ui.statTile("check", Ui.GREEN, "Active members", tileActive),
                Ui.statTile("book", Ui.ORANGE, "Services offered", tileServices)), BorderLayout.NORTH);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(validateTab(), BorderLayout.NORTH);
        page.add(wrap, BorderLayout.CENTER);
        return page;
    }

    @Override public Ui.Pages getPages() { return pages; }

    @Override public String[] pageIcons() { return new String[] { "check", "bill", "book" }; }

    // =====================================================================
    // Validate member
    // =====================================================================

    private JPanel validateTab() {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 14));
        card.setPreferredSize(new Dimension(520, 260));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Ui.title("Validate a member"), BorderLayout.NORTH);
        top.add(Ui.muted("Enter the member number from the member's card."), BorderLayout.SOUTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        Ui.limit(validateNumber, 9, "[0-9]");
        JButton go = Ui.primary("Validate");
        Ui.row(form, 0, "Member number", Ui.pair(validateNumber, go));

        validateResult.setFont(Ui.BIG);
        validateResult.setBorder(new EmptyBorder(4, 0, 0, 0));

        Runnable run = () -> {
            String result = validator.validateMember(validateNumber.getText().trim());
            Ui.show(validateResult, result, "Validated".equals(result));
            host.setStatus("Member " + validateNumber.getText().trim() + ": " + result);
        };
        go.addActionListener(e -> run.run());
        validateNumber.addActionListener(e -> run.run());
        Ui.onChange(validateNumber, () -> validateResult.setText(" "));

        card.add(top, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(validateResult, BorderLayout.SOUTH);
        return card;
    }

    // =====================================================================
    // Bill ChocAn
    // =====================================================================

    private JPanel billTab() {
        JPanel left = Ui.card();
        left.setLayout(new BorderLayout(0, 10));
        left.setPreferredSize(new Dimension(560, 560));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Ui.title("Bill ChocAn for a service"), BorderLayout.NORTH);
        head.add(Ui.muted("Fill in the steps in order. The member must be validated first."), BorderLayout.SOUTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        Ui.limit(billMember, 9, "[0-9]");
        JButton validate = Ui.secondary("Validate");
        Ui.row(form, 0, "1. Member number", Ui.pair(billMember, validate));
        Ui.row(form, 1, "", memberMessage);

        Ui.limit(billDate, 10, "[0-9\\-]");
        JButton today = Ui.secondary("Today");
        Ui.row(form, 2, "2. Date of service", Ui.pair(billDate, today));
        Ui.row(form, 3, "", Ui.muted("Format MM-DD-YYYY (not in the future)"));

        Ui.limit(billCode, 6, "[0-9]");
        JButton lookup = Ui.secondary("Look up");
        Ui.row(form, 4, "3. Service code", Ui.pair(billCode, lookup));
        Ui.row(form, 5, "", serviceMessage);
        confirm.setOpaque(false);
        confirm.setFont(Ui.FONT);
        confirm.setEnabled(false);
        Ui.row(form, 6, "", confirm);

        comments.setFont(Ui.FONT);
        comments.setLineWrap(true);
        comments.setWrapStyleWord(true);
        Ui.limit(comments, 100, null);
        JScrollPane commentScroll = Ui.scroll(comments);
        commentScroll.setPreferredSize(new Dimension(300, 70));
        Ui.row(form, 7, "4. Comments", commentScroll);
        Ui.row(form, 8, "", counter);

        JButton record = Ui.primary("Record service");
        JButton clear = Ui.secondary("Clear form");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(record);
        buttons.add(clear);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(billMessage, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));      // form and buttons stay together at the top
        body.setOpaque(false);
        JPanel under = new JPanel(new BorderLayout());
        under.setOpaque(false);
        under.add(bottom, BorderLayout.NORTH);
        body.add(form, BorderLayout.NORTH);
        body.add(under, BorderLayout.CENTER);

        left.add(head, BorderLayout.NORTH);
        left.add(body, BorderLayout.CENTER);

        // ----- the provider's verification form -----
        JPanel right = Ui.card();
        right.setLayout(new BorderLayout(0, 10));
        right.add(Ui.title("Verification form"), BorderLayout.NORTH);
        receipt.setEditable(false);
        receipt.setFont(Ui.MONO);
        receipt.setOpaque(false);
        receipt.setBorder(new EmptyBorder(6, 0, 0, 0));
        receipt.setText("After a service is recorded, the form to keep for\nyour own weekly check appears here.");
        right.add(receipt, BorderLayout.CENTER);

        // ----- actions -----
        validate.addActionListener(e -> checkMember());
        billMember.addActionListener(e -> checkMember());
        Ui.onChange(billMember, () -> memberMessage.setText(" "));
        today.addActionListener(e -> billDate.setText(LocalDate.now().format(ServiceRecord.DATE)));
        lookup.addActionListener(e -> lookUpService());
        billCode.addActionListener(e -> lookUpService());
        Ui.onChange(billCode, () -> {
            serviceMessage.setText(" ");
            confirm.setSelected(false);
            confirm.setEnabled(false);
        });
        Ui.onChange(comments, () -> counter.setText(comments.getText().length() + " / 100"));
        record.addActionListener(e -> record());
        clear.addActionListener(e -> clearForm());

        JPanel both = new JPanel(new GridBagLayout());
        both.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.fill = GridBagConstraints.VERTICAL;
        g.weighty = 1;
        both.add(left, g);
        g.gridx = 1;
        g.insets = new java.awt.Insets(0, 16, 0, 0);
        g.fill = GridBagConstraints.BOTH;
        g.weightx = 1;
        both.add(right, g);
        return both;
    }

    private void checkMember() {
        String result = validator.validateMember(billMember.getText().trim());
        Ui.show(memberMessage, result, "Validated".equals(result));
    }

    private void lookUpService() {
        Service s = billing.getService(billCode.getText().trim());
        if (s == null) {
            Ui.show(serviceMessage, "Invalid service code", false);
            confirm.setSelected(false);
            confirm.setEnabled(false);
        } else {
            Ui.show(serviceMessage, "Service: " + s.getName(), true);
            confirm.setEnabled(true);
        }
    }

    private void fail(String text) {
        Ui.show(billMessage, text, false);
    }

    /** Checks every step, records the service, and shows the verification form. */
    private void record() {
        String memberNum = billMember.getText().trim();
        String result = validator.validateMember(memberNum);
        Ui.show(memberMessage, result, "Validated".equals(result));
        if (!"Validated".equals(result)) {
            fail("Billing cancelled: member check says \"" + result + "\".");
            return;
        }

        String dateText = billDate.getText().trim();
        try {
            if (LocalDate.parse(dateText, ServiceRecord.DATE).isAfter(LocalDate.now())) {
                fail("The date of service cannot be in the future.");
                return;
            }
        } catch (DateTimeParseException e) {
            fail("The date must be a real date in the form MM-DD-YYYY.");
            return;
        }

        Service service = billing.getService(billCode.getText().trim());
        if (service == null) {
            Ui.show(serviceMessage, "Invalid service code", false);
            fail("Enter a valid six-digit service code.");
            return;
        }
        if (!confirm.isSelected()) {
            fail("Look up the service and confirm it is the correct one.");
            return;
        }

        try {
            double fee = billing.billService(memberNum, providerNum, dateText,
                    service.getCode(), comments.getText().trim());
            Member m = db.getMember(memberNum);
            String now = LocalDateTime.now().format(ServiceRecord.DATE_TIME);
            receipt.setText(String.join("\n",
                    "Current date/time:  " + now,
                    "Date of service:    " + dateText,
                    "Member:             " + (m == null ? "" : m.getName()),
                    "Member number:      " + memberNum,
                    "Service code:       " + service.getCode(),
                    "Service:            " + service.getName(),
                    String.format(Locale.US, "Fee to be paid:     $%.2f", fee),
                    "",
                    "Keep this for your own weekly check."));
            Ui.show(billMessage, String.format(Locale.US, "Service recorded. Fee to be paid: $%.2f", fee), true);
            host.setStatus("Service recorded for member " + memberNum + " (saved to records.txt).");
            billMember.setText("");
            billCode.setText("");
            comments.setText("");
            memberMessage.setText(" ");
        } catch (IllegalArgumentException e) {
            fail("Could not record service: " + e.getMessage());
        }
    }

    private void clearForm() {
        billMember.setText("");
        billDate.setText("");
        billCode.setText("");
        comments.setText("");
        memberMessage.setText(" ");
        serviceMessage.setText(" ");
        billMessage.setText(" ");
        confirm.setSelected(false);
        confirm.setEnabled(false);
    }

    // =====================================================================
    // Provider directory
    // =====================================================================

    private JPanel directoryTab() {
        JPanel panel = Ui.card();
        panel.setLayout(new BorderLayout(0, 12));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Ui.title("Provider Directory"), BorderLayout.NORTH);
        head.add(Ui.muted("Every service ChocAn offers, in alphabetical order, with its code and fee."), BorderLayout.SOUTH);

        directoryModel = new DefaultTableModel(new Object[] { "Service name", "Service code", "Fee" }, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        fillDirectory();
        JTable table = new JTable(directoryModel);
        Ui.style(table);

        JButton save = Ui.primary("Save directory to a file");
        save.addActionListener(e -> {
            boolean saved = billing.getDirectory();
            fillDirectory();
            if (saved) {
                Ui.show(directoryMessage, "Directory saved in " + AppFolders.directoryDir(), true);
                host.setStatus("Provider Directory saved.");
            } else {
                Ui.show(directoryMessage, "Could not save the directory.", false);
            }
        });
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        south.setOpaque(false);
        south.add(save);
        south.add(directoryMessage);

        panel.add(head, BorderLayout.NORTH);
        panel.add(Ui.scroll(table), BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private void fillDirectory() {
        List<Service> list = new ArrayList<>(directory.getServices());
        list.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        directoryModel.setRowCount(0);
        for (Service s : list) {
            directoryModel.addRow(new Object[] { s.getName(), s.getCode(), String.format(Locale.US, "$%.2f", s.getFee()) });
        }
    }
}
