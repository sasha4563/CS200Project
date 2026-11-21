import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.function.BiConsumer;

/**
 * First screen: the user picks Provider, Operator, or Manager and types their
 * nine-digit number. The number is checked by LoginController against
 * provider.txt, operator.txt, or manager.txt.
 *
 * @author Sydney Renninger
 */
public class LoginPanel extends JPanel {

    private final LoginController login;
    private final ChocAnDatabase db;
    private final AcmeInterface acme;
    private final Ui.Host host;
    private final BiConsumer<String, String> onSuccess;   // (role, number)

    private final Ui.RoundButton[] roleButtons = new Ui.RoundButton[3];
    private final String[] roles = { "PROVIDER", "OPERATOR", "MANAGER" };
    private final String[] roleNames = { "Provider", "Operator", "Manager" };
    private final String[] roleHints = {
            "Hospital or clinic: validate members, bill ChocAn, get the Provider Directory",
            "Data center: add, update, and delete members and providers",
            "Reports, the accounting procedure, and Acme updates" };
    private int selected = 0;

    private final JLabel hint = Ui.muted(" ");
    private final JTextField number = Ui.field(16);
    private final JLabel message = Ui.label(" ");
    private final JButton signIn = Ui.primary("Sign in");
    private final JLabel acmeInfo = Ui.muted(" ");
    private final JLabel acmeMessage = Ui.label(" ");
    private final JButton forceAcme = Ui.danger("Force apply the Acme change right now");

    public LoginPanel(LoginController login, ChocAnDatabase db, AcmeInterface acme, Ui.Host host,
                      BiConsumer<String, String> onSuccess) {
        this.login = login;
        this.db = db;
        this.acme = acme;
        this.host = host;
        this.onSuccess = onSuccess;

        setLayout(new GridBagLayout());
        setOpaque(false);

        JPanel card = Ui.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(620, 660));

        JLabel logo = new JLabel(Ui.brandMark(64));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel heading = Ui.title("Welcome to ChocAn");
        heading.setFont(Ui.TITLE.deriveFont(26f));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = Ui.muted("Chocoholics Anonymous Data Processing System");
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel pick = Ui.bold("1. Choose your role");
        pick.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel roleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        roleRow.setOpaque(false);
        roleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        for (int i = 0; i < 3; i++) {
            final int index = i;
            roleButtons[i] = new Ui.RoundButton(roleNames[i], Ui.SOFT);
            roleButtons[i].setPreferredSize(new Dimension(150, 44));
            roleButtons[i].addActionListener(e -> select(index));
            roleRow.add(roleButtons[i]);
        }
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel enter = Ui.bold("2. Enter your nine-digit number");
        enter.setAlignmentX(Component.CENTER_ALIGNMENT);
        Ui.limit(number, 9, "[0-9]");
        number.setHorizontalAlignment(JTextField.CENTER);
        number.setFont(Ui.TITLE);
        number.setMaximumSize(new Dimension(300, 48));
        number.setAlignmentX(Component.CENTER_ALIGNMENT);
        number.addActionListener(e -> attempt());
        Ui.onChange(number, () -> message.setText(" "));

        signIn.setAlignmentX(Component.CENTER_ALIGNMENT);
        signIn.addActionListener(e -> attempt());
        message.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton samples = Ui.secondary("Show sample logins");
        samples.setAlignmentX(Component.CENTER_ALIGNMENT);
        samples.addActionListener(e -> showSamples());

        // ----- Acme link: shown before anyone logs in -----
        javax.swing.JSeparator line = new javax.swing.JSeparator();
        line.setMaximumSize(new Dimension(480, 2));
        line.setForeground(Ui.LINE);
        JLabel acmeTitle = Ui.bold("Acme Accounting link");
        acmeTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        acmeInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        forceAcme.setAlignmentX(Component.CENTER_ALIGNMENT);
        acmeMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
        forceAcme.addActionListener(e -> forceAcmeNow());

        card.add(logo);
        card.add(gap(10));
        card.add(heading);
        card.add(sub);
        card.add(gap(20));
        card.add(pick);
        card.add(gap(10));
        card.add(roleRow);
        card.add(gap(8));
        card.add(hint);
        card.add(gap(20));
        card.add(enter);
        card.add(gap(10));
        card.add(number);
        card.add(gap(16));
        card.add(signIn);
        card.add(gap(8));
        card.add(message);
        card.add(gap(6));
        card.add(samples);
        card.add(gap(16));
        card.add(line);
        card.add(gap(12));
        card.add(acmeTitle);
        card.add(gap(4));
        card.add(acmeInfo);
        card.add(gap(10));
        card.add(forceAcme);
        card.add(gap(6));
        card.add(acmeMessage);

        add(card, new GridBagConstraints());
        select(0);
        refreshAcme();
    }

    /** Applies every pending Acme change right now, whatever its scheduled time. */
    public void forceAcmeNow() {
        int n = acme.receiveUpdatesNow(acme.getSimulationFile());
        if (n < 0) {
            Ui.show(acmeMessage, "Could not read " + acme.getSimulationFile(), false);
            host.setStatus("Acme: could not read the simulation file.");
        } else {
            Ui.show(acmeMessage, n + " membership status change(s) applied.", true);
            host.setStatus("[Acme] " + n + " membership status change(s) force-applied.");
        }
        refreshAcme();
    }

    /** Updates the line that says how many Acme changes are waiting and when the next one is due. */
    public void refreshAcme() {
        int pending = acme.countPending();
        if (pending == 0) {
            acmeInfo.setText("No Acme changes are waiting.");
        } else {
            String next = acme.nextScheduled();
            acmeInfo.setText(pending + " change(s) waiting" + (next.isEmpty() ? "" : ", next at " + next)
                    + " (applied automatically)");
        }
    }

    public JButton getForceAcmeButton() {
        return forceAcme;
    }

    private static Component gap(int h) {
        return javax.swing.Box.createVerticalStrut(h);
    }

    /** Chooses a role (0 = provider, 1 = operator, 2 = manager). */
    public void select(int index) {
        selected = index;
        for (int i = 0; i < 3; i++) {
            roleButtons[i].setColor(i == index ? Ui.PRIMARY : Ui.SOFT);
        }
        hint.setText(roleHints[index]);
        message.setText(" ");
        number.requestFocusInWindow();
    }

    public void setNumber(String text) {
        number.setText(text);
    }

    /** Clears the number box (used after logging out). */
    public void reset() {
        number.setText("");
        message.setText(" ");
        acmeMessage.setText(" ");
        select(0);
        refreshAcme();
    }

    public JButton getSignInButton() {
        return signIn;
    }

    /** Checks the number for the chosen role; on success calls onSuccess(role, number). */
    public void attempt() {
        String id = number.getText().trim();
        String role = roles[selected];
        if (login.checkLogin(id, role)) {
            onSuccess.accept(role, id);
        } else {
            Ui.show(message, "Invalid " + roleNames[selected].toLowerCase() + " number", false);
            number.selectAll();
        }
    }

    private void showSamples() {
        StringBuilder sb = new StringBuilder();
        sb.append("Providers (provider.txt):\n");
        for (Provider p : db.getProviders()) {
            sb.append("   ").append(p.getNumber()).append("   ").append(p.getName()).append('\n');
        }
        sb.append("\nOperators (operator.txt):\n");
        for (String id : ChocAnDatabase.readLines(AppFolders.dataDir(), "operator.txt")) {
            sb.append("   ").append(id).append('\n');
        }
        sb.append("\nManagers (manager.txt):\n");
        for (String id : ChocAnDatabase.readLines(AppFolders.dataDir(), "manager.txt")) {
            sb.append("   ").append(id).append('\n');
        }
        JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this), sb.toString(),
                "Sample logins", JOptionPane.INFORMATION_MESSAGE);
    }
}
