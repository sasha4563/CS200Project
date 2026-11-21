import java.util.Scanner;

/**
 * Manager terminal: request individual reports and run the main accounting
 * procedure on demand.
 *
 * @author Sydney Renninger
 */
public class ManagerTerminal extends Terminal {

    private String managerNum;
    private ReportController reports;
    private AcmeInterface acme;

    public ManagerTerminal(ChocAnDatabase db, Scanner input) {
        super(db, input);
        this.reports = new ReportController(db);
        this.acme = new AcmeInterface(new MemberController(db));
    }

    /** Verify Manager sequence diagram: check the number, then show the menu. */
    public boolean loginManager(String id) {
        boolean valid = login.checkLogin(id, "MANAGER");
        if (valid) {
            managerNum = id;
            userId = id;
            loggedIn = true;
            display("Validated");
            showMenu();
        } else {
            display("Invalid manager number");
        }
        return valid;
    }

    /** Request Report sequence diagram. type = "member", "provider", "summary", or "eft". */
    public void requestReport(String type) {
        boolean saved = reports.makeReport(type);
        if (saved) {
            display("Report saved");
        } else {
            display("Report could not be generated");
        }
    }

    /** Applies every pending line of the Acme file right now, ignoring its scheduled times. */
    public void receiveAcmeUpdates() {
        String file = prompt("Acme file (press Enter for the data folder's acmesimulation.txt): ");
        if (file.isEmpty()) {
            file = AppFolders.dataDir() + "/acmesimulation.txt";
        }
        int count = acme.receiveUpdatesNow(file);
        if (count < 0) {
            display("Could not read " + file);
        } else {
            display(count + " member record(s) updated from Acme.");
        }
    }

    /**
     * Moves this week's billed services to records_archive.txt so the next reports start clean.
     * The weekly update (Timer) does this by itself after its reports every Friday at 12 AM.
     */
    public void clearWeek() {
        String answer = prompt("Move this week's billed services to records_archive.txt? (Y/N): ");
        if (!answer.equalsIgnoreCase("Y")) {
            display("Cancelled. Nothing was cleared.");
            return;
        }
        int moved = reports.startNewWeek();
        if (moved < 0) {
            display("Could not clear this week's data.");
        }
    }

    @Override
    public void showMenu() {
        while (loggedIn) {
            display("");
            display("===== Manager Menu =====");
            display("1) Member reports");
            display("2) Provider reports");
            display("3) Accounts Payable Summary report");
            display("4) EFT data file");
            display("5) Run main accounting procedure (all reports)");
            display("6) Force apply the Acme changes right now");
            display("7) Clear this week's data (start a new week)");
            display("0) Log out");
            String choice = prompt("Choose an option: ");

            switch (choice) {
                case "1": requestReport("member"); break;
                case "2": requestReport("provider"); break;
                case "3": requestReport("summary"); break;
                case "4": requestReport("eft"); break;
                case "5":
                    // Same procedure the Timer runs at Friday midnight, started by hand.
                    reports.runWeeklyReports();
                    display("Main accounting procedure complete.");
                    break;
                case "6": receiveAcmeUpdates(); break;
                case "7": clearWeek(); break;
                case "0": logout(); break;
                default: display("Invalid option. Please choose 0-7.");
            }
        }
    }
}
