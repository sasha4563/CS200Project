import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Program entry point. Shows the role menu and hands control to the
 * Provider, Operator, or Manager terminal.
 *
 * @author Sydney Renninger
 */
public class MainMenu {

    private String currentRole;
    private List<Terminal> terminals;   // 3 terminals (composition): provider, operator, manager
    private AcmeInterface acme;        // simulated Acme link: applies scheduled status changes
    private Timer timer;                // system scheduler: checked each time the role menu shows
    private Scanner input;

    public MainMenu(ChocAnDatabase db, ProviderDirectory directory, Scanner input) {
        this.input = input;
        this.terminals = new ArrayList<>();
        terminals.add(new ProviderTerminal(db, directory, input));
        terminals.add(new OperatorTerminal(db, input));
        terminals.add(new ManagerTerminal(db, input));
        this.timer = new Timer(new ReportController(db));
        this.acme = new AcmeInterface(new MemberController(db));
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        MainMenu menu = new MainMenu(createDatabase(), new ProviderDirectory(), input);
        menu.start();
    }

    /** Shows the role menu and loops until the user chooses Exit. */
    public void start() {
        while (true) {
            timer.trigger();                 // runs the weekly reports if it is Friday midnight
            int changed = acme.checkSchedule();   // applies Acme status changes that are now due
            if (changed > 0) {
                System.out.println("[Acme] " + changed + " membership status update(s) applied.");
            }

            System.out.println();
            System.out.println("===== ChocAn Data Processing System =====");
            System.out.println("1) Provider");
            System.out.println("2) Operator");
            System.out.println("3) Manager");
            System.out.println("0) Exit");
            String choice = readLine("Choose your role: ");

            switch (choice) {
                case "1": {
                    currentRole = "provider";
                    ProviderTerminal pt = (ProviderTerminal) chooseRole(currentRole);
                    while (!pt.loginProvider(readLine("Enter provider number: ")) && tryAgain()) { }
                    break;
                }
                case "2": {
                    currentRole = "operator";
                    OperatorTerminal ot = (OperatorTerminal) chooseRole(currentRole);
                    while (!ot.loginOperator(readLine("Enter operator number: ")) && tryAgain()) { }
                    break;
                }
                case "3": {
                    currentRole = "manager";
                    ManagerTerminal mt = (ManagerTerminal) chooseRole(currentRole);
                    while (!mt.loginManager(readLine("Enter manager number: ")) && tryAgain()) { }
                    break;
                }
                case "0":
                    System.out.println("Goodbye.");
                    return;
                default:
                    System.out.println("Invalid option. Please choose 0-3.");
            }
            currentRole = null;
        }
    }

    /** Returns the Provider, Operator, or Manager terminal; null for an unknown role. */
    public Terminal chooseRole(String role) {
        switch (role.toLowerCase()) {
            case "provider": return terminals.get(0);
            case "operator": return terminals.get(1);
            case "manager":  return terminals.get(2);
            default:         return null;
        }
    }

    // ---------- helpers ----------

    /** "Try again?" step from the Verify Provider / Operator / Manager activity diagrams. */
    private boolean tryAgain() {
        return readLine("Try again? (Y/N): ").equalsIgnoreCase("Y");
    }

    private String readLine(String label) {
        System.out.print(label);
        if (!input.hasNextLine()) {
            System.out.println();
            System.exit(0);
        }
        return input.nextLine().trim();
    }

    /**
     * Loads members, providers, services, and the provider / operator / manager
     * login numbers from the data folder (provider.txt, operator.txt, manager.txt,
     * member.txt, ...). On the first run the folder is created with sample data.
     */
    private static ChocAnDatabase createDatabase() {
        System.out.println("Data folder: " + AppFolders.dataDir());
        System.out.println("(member.txt, provider.txt, ... are read from and written to this folder - open THIS folder to see changes)");
        return ChocAnDatabase.loadFromFiles(AppFolders.dataDir());
    }
}
