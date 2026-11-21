import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * The whole ChocAn GUI as one panel: the login screen, then a sidebar (navigation, who is
 * logged in, Log out) next to the Provider / Operator / Manager screen, a status bar, and
 * the background schedule. ChocAnGUI puts it in a window.
 *
 * The screens use the same controllers and data files as the command-line
 * version (MainMenu): LoginController checks the number against provider.txt /
 * operator.txt / manager.txt, then the Provider, Operator, or Manager screen
 * is shown.
 *
 * In the background a timer checks every 10 seconds whether the weekly update is
 * due (Friday midnight) and whether any Acme status change in acmesimulation.txt has
 * come due.
 *
 * @author Sydney Renninger
 */
public class AppPanel extends Ui.GradientPanel implements Ui.Host {

    private final ChocAnDatabase db;
    private final ProviderDirectory directory;
    private final LoginController login;
    private final MemberController memberCtrl;
    private final ProviderController providerCtrl;
    private final ServiceController billing;
    private final ReportController reportCtrl;
    private final AcmeInterface acme;
    private final Timer scheduler;                 // the ChocAn Timer (Friday midnight)

    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final LoginPanel loginPanel;
    private final JButton logout = Ui.secondary("Log out");
    private final JLabel status = Ui.muted(" ");
    private JPanel shell;                          // sidebar + screen, shown after logging in
    private Component roleScreen;                  // the Provider / Operator / Manager screen being shown

    private JButton wantedDefault;                 // the button Enter should press on this screen

    public AppPanel() {
        System.out.println("Data folder: " + AppFolders.dataDir());
        db = ChocAnDatabase.loadFromFiles(AppFolders.dataDir());
        directory = new ProviderDirectory();
        login = new LoginController(db);
        memberCtrl = new MemberController(db);
        providerCtrl = new ProviderController(db);
        billing = new ServiceController(db, directory);
        reportCtrl = new ReportController(db);
        reportCtrl.setQuiet(true);
        acme = new AcmeInterface(memberCtrl);
        scheduler = new Timer(reportCtrl);

        setLayout(new BorderLayout());

        loginPanel = new LoginPanel(login, db, acme, this, this::signedIn);
        body.setOpaque(false);
        body.add(loginPanel, "login");
        add(body, BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);

        logout.addActionListener(e -> showLogin());
        showLogin();

        // like the command-line menu, check the schedule right away and then every 10 seconds
        tick();
        new javax.swing.Timer(10_000, e -> tick()).start();
    }

    // ---------- window pieces ----------

    private JPanel footer() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(255, 255, 255, 150));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Ui.LINE);
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(7, 24, 7, 24));
        bar.add(Ui.muted("Data folder: " + AppFolders.dataDir()), BorderLayout.WEST);
        bar.add(status, BorderLayout.EAST);
        return bar;
    }

    /** The left card: logo, the pages of this role, who is logged in, and Log out. */
    private JPanel sidebar(String displayName, String roleLine, Ui.Paged paged) {
        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 18));
        card.setPreferredSize(new Dimension(246, 100));

        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        brand.add(new JLabel(Ui.brandMark(42)), BorderLayout.WEST);
        JPanel names = new JPanel(new GridLayout(2, 1));
        names.setOpaque(false);
        JLabel appName = Ui.title("ChocAn");
        appName.setFont(Ui.TITLE.deriveFont(22f));
        names.add(appName);
        names.add(Ui.muted("Data Processing"));
        brand.add(names, BorderLayout.CENTER);

        Ui.Pages pages = paged.getPages();
        String[] icons = paged.pageIcons();
        List<Ui.NavButton> items = new ArrayList<>();
        for (int i = 0; i < pages.getTabCount(); i++) {
            final int index = i;
            Ui.NavButton item = new Ui.NavButton(pages.getTitleAt(i), icons[i]);
            item.addActionListener(e -> pages.setSelectedIndex(index));
            items.add(item);
        }
        pages.addChangeListener(e -> highlight(items, pages.getSelectedIndex()));
        highlight(items, pages.getSelectedIndex());
        JPanel nav = new JPanel(new BorderLayout(0, 8));
        nav.setOpaque(false);
        nav.add(Ui.muted("MENU"), BorderLayout.NORTH);
        nav.add(Ui.stack(6, items.toArray(new JComponent[0])), BorderLayout.CENTER);

        JPanel user = new JPanel(new BorderLayout(10, 0));
        user.setOpaque(false);
        user.add(new JLabel(Ui.avatar(displayName.substring(0, 1).toUpperCase(), 40)), BorderLayout.WEST);
        JPanel who = new JPanel(new GridLayout(2, 1));
        who.setOpaque(false);
        who.add(Ui.bold(displayName));
        who.add(Ui.muted(roleLine));
        user.add(who, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        bottom.add(user, BorderLayout.NORTH);
        bottom.add(logout, BorderLayout.SOUTH);

        card.add(brand, BorderLayout.NORTH);
        card.add(nav, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private static void highlight(List<Ui.NavButton> items, int selected) {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setSelectedItem(i == selected);
        }
    }

    // ---------- screens ----------

    private void showLogin() {
        if (shell != null) {
            body.remove(shell);
            shell = null;
            roleScreen = null;
        }
        loginPanel.reset();
        cards.show(body, "login");
        wantedDefault = loginPanel.getSignInButton();
        applyDefault();
        setStatus("Choose a role and enter your number.");
    }

    /** Called by the login screen after the number was accepted for the role. */
    private void signedIn(String role, String id) {
        Component screen;
        String name;
        String roleLine;
        switch (role) {
            case "PROVIDER": {
                Provider p = db.getProvider(id);
                name = (p == null) ? id : p.getName();
                roleLine = "Provider  " + id;
                screen = new ProviderPanel(this, db, directory, billing, memberCtrl, id);
                break;
            }
            case "OPERATOR":
                name = "Operator";
                roleLine = "Number  " + id;
                screen = new OperatorPanel(this, db, memberCtrl, providerCtrl);
                break;
            default:
                name = "Manager";
                roleLine = "Number  " + id;
                screen = new ManagerPanel(this, db, reportCtrl, acme, scheduler);
                break;
        }
        roleScreen = screen;
        shell = new JPanel(new BorderLayout(4, 0));
        shell.setOpaque(false);
        shell.setBorder(new EmptyBorder(10, 10, 4, 14));
        shell.add(sidebar(name, roleLine, (Ui.Paged) screen), BorderLayout.WEST);
        shell.add(screen, BorderLayout.CENTER);
        body.add(shell, "shell");
        cards.show(body, "shell");
        wantedDefault = null;
        applyDefault();
        setStatus("Validated. Logged in as " + name + " (" + id + ").");
    }

    /** Makes Enter press the right button once the panel is inside a window. */
    private void applyDefault() {
        javax.swing.JRootPane root = SwingUtilities.getRootPane(this);
        if (root != null) {
            root.setDefaultButton(wantedDefault);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        applyDefault();
    }

    // ---------- Ui.Host ----------

    @Override
    public void setStatus(String message) {
        status.setText(message);
    }

    // ---------- background schedule ----------

    /** Weekly reports at Friday midnight, and Acme changes whose time has come. */
    private void tick() {
        boolean weekly = scheduler.trigger();
        int changed = acme.checkSchedule();
        if (weekly) {
            setStatus(scheduler.getLastSummary());
        }
        if (changed > 0) {
            setStatus("[Acme] " + changed + " membership status update(s) applied.");
        }
        loginPanel.refreshAcme();
        if ((weekly || changed > 0) && roleScreen instanceof Ui.Refreshable) {
            ((Ui.Refreshable) roleScreen).refresh();
        }
    }
}
