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
    private Timer timer;                // system scheduler: checked each time the role menu shows
    private Scanner input;

    public MainMenu(ChocAnDatabase db, ProviderDirectory directory, Scanner input) {
        this.input = input;
        this.terminals = new ArrayList<>();
        terminals.add(new ProviderTerminal(db, directory, input));
        terminals.add(new OperatorTerminal(db, input));
        terminals.add(new ManagerTerminal(db, input));
        this.timer = new Timer(new ReportController(db));
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
     * Builds the database with sample data so every feature can be demonstrated.
     * Number ranges: providers 000000000-000099999, managers 000100000-000199999,
     * operators 000200000-000299999. Members can use any other 9-digit number.
     */
    private static ChocAnDatabase createDatabase() {
        List<Member> members = new ArrayList<>();
        members.add(new Member(new String[] {"Alice Brown", "123456789", "101 Main St", "Tuscaloosa", "AL", "35401", "Active"}));
        members.add(new Member(new String[] {"Ben Carter", "987654321", "22 Elm Ave", "Northport", "AL", "35473", "Suspended"}));
        members.add(new Member(new String[] {"Cara Diaz", "555123456", "9 Oak Ct", "Birmingham", "AL", "35203", "Active"}));

        List<Provider> providers = new ArrayList<>();
        providers.add(new Provider(new String[] {"Dana Evans", "000012345", "500 Health Way", "Tuscaloosa", "AL", "35401", "Dietitian"}));
        providers.add(new Provider(new String[] {"Eli Ford", "000054321", "77 Clinic Rd", "Tuscaloosa", "AL", "35404", "Internist"}));
        providers.add(new Provider(new String[] {"Fay Green", "000099999", "3 Gym Ln", "Northport", "AL", "35476", "Exercise expert"}));

        // Format must match ChocAnDatabase.isValidStaff (currently id + ":" + role).
        List<String> staffIds = new ArrayList<>();
        staffIds.add("000150000:MANAGER");
        staffIds.add("000250000:OPERATOR");

        return new ChocAnDatabase(members, providers, new ArrayList<ServiceRecord>(), staffIds);
    }
}
