import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Operator screens: add, update, and delete members and providers. Every
 * change goes through MemberController / ProviderController and is written to
 * member.txt / provider.txt immediately; the line is then read back from the
 * file and shown at the bottom of the tab as proof.
 *
 * @author Sydney Renninger
 */
public class OperatorPanel extends JPanel implements Ui.Refreshable, Ui.Paged {

    /** What a tab needs from the controllers (one implementation for members, one for providers). */
    interface Backend {
        String kind();                        // "member" or "provider"
        String nameLabel();                   // "Name" or "Hospital / clinic name"
        String lastLabel();                   // "Status" or "Provider type"
        String[] lastChoices();               // the choices shown in the last field
        boolean lastEditable();               // true = the operator may type their own value
        List<String[]> rows();                // every record: name, number, address, city, state, zip, status/type
        boolean add(String[] info);
        boolean update(String number, String[] info);
        boolean delete(String number);
        String error();
        String savedLine(String number);
        String file();
    }

    private final Ui.Host host;
    private final PartyTab members;
    private final PartyTab providers;
    private final Ui.Pages pages = Ui.pages();

    public OperatorPanel(Ui.Host host, ChocAnDatabase db, MemberController memberCtrl, ProviderController providerCtrl) {
        this.host = host;

        Backend memberBackend = new Backend() {
            public String kind() { return "member"; }
            public String nameLabel() { return "Name"; }
            public String lastLabel() { return "Status"; }
            public String[] lastChoices() { return new String[] { "Active", "Suspended" }; }
            public boolean lastEditable() { return false; }
            public List<String[]> rows() {
                List<String[]> rows = new ArrayList<>();
                for (Member m : db.getMembers()) {
                    rows.add(new String[] { m.getName(), m.getNumber(), m.getAddress(), m.getCity(), m.getState(), m.getZip(), m.getStatus() });
                }
                return rows;
            }
            public boolean add(String[] info) { return memberCtrl.addMember(info); }
            public boolean update(String number, String[] info) { return memberCtrl.updateMember(number, info); }
            public boolean delete(String number) { return memberCtrl.deleteMember(number); }
            public String error() { return memberCtrl.getLastError(); }
            public String savedLine(String number) { return memberCtrl.getSavedLine(number); }
            public String file() { return memberCtrl.getDataFile(); }
        };

        Backend providerBackend = new Backend() {
            public String kind() { return "provider"; }
            public String nameLabel() { return "Hospital / clinic name"; }
            public String lastLabel() { return "Provider type"; }
            public String[] lastChoices() { return new String[] { "Dietitian", "Internist", "Exercise expert" }; }
            public boolean lastEditable() { return true; }
            public List<String[]> rows() {
                List<String[]> rows = new ArrayList<>();
                for (Provider p : db.getProviders()) {
                    rows.add(new String[] { p.getName(), p.getNumber(), p.getAddress(), p.getCity(), p.getState(), p.getZip(), p.getType() });
                }
                return rows;
            }
            public boolean add(String[] info) { return providerCtrl.addProvider(info); }
            public boolean update(String number, String[] info) { return providerCtrl.updateProvider(number, info); }
            public boolean delete(String number) { return providerCtrl.deleteProvider(number); }
            public String error() { return providerCtrl.getLastError(); }
            public String savedLine(String number) { return providerCtrl.getSavedLine(number); }
            public String file() { return providerCtrl.getDataFile(); }
        };

        members = new PartyTab(memberBackend);
        providers = new PartyTab(providerBackend);

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(0, 6, 0, 0));

        pages.addTab("Members", members);
        pages.addTab("Providers", providers);
        pages.addChangeListener(e -> refresh());
        add(pages, BorderLayout.CENTER);
    }

    @Override public Ui.Pages getPages() { return pages; }

    @Override public String[] pageIcons() { return new String[] { "users", "hospital" }; }

    /** Re-reads both tables from the files. */
    @Override
    public void refresh() {
        members.reload();
        providers.reload();
    }

    // ---- entry points that do not need a dialog (the buttons call these) ----

    PartyTab memberTab()   { return members; }
    PartyTab providerTab() { return providers; }

    // =====================================================================
    // One tab: table + search + Add / Edit / Delete + proof line
    // =====================================================================

    class PartyTab extends JPanel {
        private final Backend backend;
        private final DefaultTableModel model;
        private final JTable table;
        private final TableRowSorter<DefaultTableModel> sorter;
        private final JTextField search = Ui.field(14);
        private final JButton edit = Ui.secondary("Edit");
        private final JButton delete = Ui.danger("Delete");
        private final JLabel count = Ui.muted(" ");
        private final JTextArea proof = new JTextArea(3, 40);
        private final JLabel[] stat = { new JLabel("0"), new JLabel("0"), new JLabel("0"), new JLabel("0") };

        PartyTab(Backend backend) {
            this.backend = backend;
            setLayout(new BorderLayout(0, 6));
            setOpaque(false);

            String[] columns = { backend.nameLabel(), "Number", "Street address", "City", "State", "ZIP", backend.lastLabel() };
            model = new DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int row, int col) { return false; }
            };
            table = new JTable(model);
            Ui.style(table);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            int[] widths = { 175, 105, 165, 115, 55, 65, 105 };
            for (int c = 0; c < widths.length; c++) {
                table.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);
            }

            // ----- top: title, then a toolbar with the search box on the left and the buttons on the right -----
            JPanel top = new JPanel(new BorderLayout(0, 12));
            top.setOpaque(false);
            JPanel titles = new JPanel(new BorderLayout());
            titles.setOpaque(false);
            titles.add(Ui.title(capital(backend.kind()) + " records"), BorderLayout.NORTH);
            titles.add(count, BorderLayout.SOUTH);

            JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            searchBox.setOpaque(false);
            searchBox.add(Ui.label("Search:"));
            searchBox.add(search);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.setOpaque(false);
            JButton add = Ui.primary("Add " + backend.kind());
            JButton refresh = Ui.secondary("Refresh");
            edit.setToolTipText("Edit the selected row (or double-click it)");
            delete.setToolTipText("Delete the selected row");
            buttons.add(add);
            buttons.add(edit);
            buttons.add(delete);
            buttons.add(refresh);

            JPanel toolbar = new JPanel(new BorderLayout());
            toolbar.setOpaque(false);
            toolbar.add(searchBox, BorderLayout.WEST);
            toolbar.add(buttons, BorderLayout.EAST);
            top.add(titles, BorderLayout.NORTH);
            top.add(toolbar, BorderLayout.SOUTH);

            // ----- proof line at the bottom -----
            proof.setEditable(false);
            proof.setFont(Ui.MONO);
            proof.setLineWrap(true);
            proof.setWrapStyleWord(true);
            proof.setBackground(Ui.ROW_ALT);
            proof.setBorder(new EmptyBorder(8, 10, 8, 10));
            proof.setText("Every change is written to " + backend.file() + " immediately.");

            JPanel card = Ui.card();
            card.setLayout(new BorderLayout(0, 12));
            card.add(top, BorderLayout.NORTH);
            card.add(Ui.scroll(table), BorderLayout.CENTER);
            card.add(proof, BorderLayout.SOUTH);

            JPanel tiles;
            if (backend.kind().equals("member")) {
                tiles = Ui.tileRow(
                        Ui.statTile("users", Ui.PRIMARY, "Total members", stat[0]),
                        Ui.statTile("check", Ui.GREEN, "Active", stat[1]),
                        Ui.statTile("clock", Ui.RED, "Suspended", stat[2]));
            } else {
                tiles = Ui.tileRow(
                        Ui.statTile("hospital", Ui.PRIMARY, "Total providers", stat[0]),
                        Ui.statTile("user", Ui.TEAL, "Dietitians", stat[1]),
                        Ui.statTile("user", Ui.ORANGE, "Internists", stat[2]),
                        Ui.statTile("user", Ui.GREEN, "Exercise experts", stat[3]));
            }
            add(tiles, BorderLayout.NORTH);
            add(card, BorderLayout.CENTER);

            // ----- actions -----
            add.addActionListener(e -> openForm(null));
            edit.addActionListener(e -> editSelected());
            delete.addActionListener(e -> deleteSelected());
            refresh.addActionListener(e -> reload());
            Ui.onChange(search, this::applyFilter);
            table.getSelectionModel().addListSelectionListener(e -> updateButtons());
            table.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        editSelected();
                    }
                }
            });
            reload();
        }

        /** Fills the table from the file data (keeps the selected number selected). */
        void reload() {
            String keep = selectedNumber();
            model.setRowCount(0);
            for (String[] row : backend.rows()) {
                model.addRow(row);
            }
            count.setText(model.getRowCount() + " " + backend.kind() + "s in " + backend.file());
            updateStats();
            if (keep != null) {
                select(keep);
            }
            updateButtons();
        }

        private void updateStats() {
            int total = model.getRowCount();
            int a = 0;
            int b = 0;
            int c = 0;
            for (int i = 0; i < total; i++) {
                String last = String.valueOf(model.getValueAt(i, 6)).toLowerCase();
                if (backend.kind().equals("member")) {
                    if (last.equals("active")) { a++; } else if (last.equals("suspended")) { b++; }
                } else {
                    if (last.contains("diet")) { a++; } else if (last.contains("intern")) { b++; } else if (last.contains("exercise")) { c++; }
                }
            }
            stat[0].setText(String.valueOf(total));
            stat[1].setText(String.valueOf(a));
            stat[2].setText(String.valueOf(b));
            stat[3].setText(String.valueOf(c));
        }

        private void applyFilter() {
            String text = search.getText().trim();
            sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }

        private void updateButtons() {
            boolean any = table.getSelectedRow() >= 0;
            edit.setEnabled(any);
            delete.setEnabled(any);
        }

        String selectedNumber() {
            int row = table.getSelectedRow();
            return row < 0 ? null : String.valueOf(model.getValueAt(table.convertRowIndexToModel(row), 1));
        }

        /** Selects the row with this number, if it is in the table. */
        void select(String number) {
            for (int i = 0; i < model.getRowCount(); i++) {
                if (number.equals(model.getValueAt(i, 1))) {
                    int view = table.convertRowIndexToView(i);
                    if (view >= 0) {
                        table.setRowSelectionInterval(view, view);
                        table.scrollRectToVisible(table.getCellRect(view, 0, true));
                    }
                    return;
                }
            }
        }

        private String[] selectedRow() {
            int row = table.getSelectedRow();
            if (row < 0) {
                return null;
            }
            int m = table.convertRowIndexToModel(row);
            String[] values = new String[model.getColumnCount()];
            for (int c = 0; c < values.length; c++) {
                values[c] = String.valueOf(model.getValueAt(m, c));
            }
            return values;
        }

        private void editSelected() {
            String[] row = selectedRow();
            if (row != null) {
                openForm(row);
            }
        }

        private void deleteSelected() {
            String[] row = selectedRow();
            if (row == null) {
                return;
            }
            int answer = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this),
                    "Delete " + backend.kind() + " " + row[1] + " (" + row[0] + ")?\nTheir line is removed from "
                            + backend.file() + ".",
                    "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                doDelete(row[1]);
            }
        }

        /** Deletes the record (after the operator has confirmed) and shows the proof. */
        void doDelete(String number) {
            if (backend.delete(number)) {
                String message = capital(backend.kind()) + " deleted. Removed from " + backend.file();
                showProof(message, backend.savedLine(number));
            } else {
                showError("Not deleted: " + backend.error());
            }
            reload();
        }

        /** Opens the Add (existing == null) or Edit dialog. */
        private void openForm(String[] existing) {
            Window owner = SwingUtilities.getWindowAncestor(this);
            JDialog dialog = new JDialog(owner, (existing == null ? "Add " : "Edit ") + backend.kind(),
                    JDialog.ModalityType.APPLICATION_MODAL);
            JButton[] save = new JButton[1];
            dialog.setContentPane(formContent(existing, dialog::dispose, save));
            dialog.getRootPane().setDefaultButton(save[0]);
            dialog.pack();
            dialog.setLocationRelativeTo(owner);
            dialog.setVisible(true);
        }

        /**
         * The inside of the Add / Edit dialog: title, form, and Save / Cancel buttons.
         * close is run after a successful Save or after Cancel; the Save button is returned in saveOut[0].
         */
        JPanel formContent(String[] existing, Runnable close, JButton[] saveOut) {
            PartyForm form = newForm(existing);
            JPanel content = new JPanel(new BorderLayout(0, 12));
            content.setBackground(java.awt.Color.WHITE);
            content.setBorder(new EmptyBorder(18, 22, 18, 22));
            content.add(Ui.title((existing == null ? "Add a new " : "Edit this ") + backend.kind()), BorderLayout.NORTH);
            content.add(form, BorderLayout.CENTER);

            JButton save = Ui.primary("Save");
            JButton cancel = Ui.secondary("Cancel");
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.setOpaque(false);
            buttons.add(cancel);
            buttons.add(save);
            content.add(buttons, BorderLayout.SOUTH);

            save.addActionListener(e -> {
                if (form.submit()) {
                    close.run();
                }
            });
            cancel.addActionListener(e -> close.run());
            saveOut[0] = save;
            return content;
        }

        /** Builds the form for Add (existing == null) or Edit. */
        PartyForm newForm(String[] existing) {
            return new PartyForm(this, backend, existing);
        }

        void showProof(String message, String line) {
            proof.setText(message + "\nIn the file now -> " + line);
            host.setStatus(message);
        }

        void showError(String message) {
            proof.setText(message);
            host.setStatus(message);
        }

        JTable table() { return table; }
    }

    // =====================================================================
    // The Add / Edit form
    // =====================================================================

    class PartyForm extends JPanel {
        private final PartyTab tab;
        private final Backend backend;
        private final String[] existing;

        final JTextField name = Ui.field(24);
        final JTextField number = Ui.field(24);
        final JTextField address = Ui.field(24);
        final JTextField city = Ui.field(24);
        final JTextField state = Ui.field(24);
        final JTextField zip = Ui.field(24);
        final JComboBox<String> last;
        private final JLabel error = Ui.label(" ");

        PartyForm(PartyTab tab, Backend backend, String[] existing) {
            this.tab = tab;
            this.backend = backend;
            this.existing = existing;
            setOpaque(false);
            setLayout(new GridBagLayout());

            last = new JComboBox<>(backend.lastChoices());
            last.setEditable(backend.lastEditable());
            last.setFont(Ui.FONT);

            Ui.limit(name, 25, null);
            Ui.limit(number, 9, "[0-9]");
            Ui.limit(address, 25, null);
            Ui.limit(city, 14, null);
            Ui.limit(state, 2, "[A-Za-z]");
            Ui.limit(zip, 5, "[0-9]");
            if (backend.lastEditable()) {
                Ui.limit((JTextField) last.getEditor().getEditorComponent(), 25, null);
            }

            Ui.row(this, 0, backend.nameLabel() + " (25)", name);
            Ui.row(this, 1, "Number (9 digits)", number);
            Ui.row(this, 2, "Street address (25)", address);
            Ui.row(this, 3, "City (14)", city);
            Ui.row(this, 4, "State (2 letters)", state);
            Ui.row(this, 5, "ZIP code (5 digits)", zip);
            Ui.row(this, 6, backend.lastLabel(), last);
            Ui.row(this, 7, "", error);

            if (existing != null) {
                name.setText(existing[0]);
                number.setText(existing[1]);
                number.setEditable(false);
                number.setBackground(Ui.ROW_ALT);
                number.setToolTipText("The number cannot be changed");
                address.setText(existing[2]);
                city.setText(existing[3]);
                state.setText(existing[4]);
                zip.setText(existing[5]);
                last.setSelectedItem(existing[6]);
            }
        }

        /** The text of the last field (status / type). */
        String lastValue() {
            Object v = last.getSelectedItem();
            return v == null ? "" : v.toString().trim();
        }

        String[] info() {
            return new String[] { name.getText().trim(), number.getText().trim(), address.getText().trim(),
                    city.getText().trim(), state.getText().trim().toUpperCase(), zip.getText().trim(), lastValue() };
        }

        /** Saves through the controller. On failure the reason is shown in red and false is returned. */
        boolean submit() {
            String[] info = info();
            boolean adding = (existing == null);
            boolean ok = adding ? backend.add(info) : backend.update(existing[1], info);
            if (!ok) {
                Ui.show(error, backend.error(), false);
                return false;
            }
            String verb = adding ? " added. Saved in " : " updated. Saved in ";
            tab.reload();
            tab.select(info[1]);
            tab.showProof(capital(backend.kind()) + verb + backend.file(), backend.savedLine(info[1]));
            return true;
        }

        JLabel errorLabel() { return error; }
    }

    private static String capital(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
