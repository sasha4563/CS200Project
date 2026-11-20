import java.util.Scanner;

/**
 * Operator terminal: add, update, and delete member and provider records.
 *
 * info[] passed to the controllers uses this layout:
 *   0 name | 1 number | 2 address | 3 city | 4 state | 5 zip | 6 status (member) or type (provider)
 *
 * @author Sydney Renninger
 */
public class OperatorTerminal extends Terminal {

    private String operatorNum;
    private MemberController memberCtrl;
    private ProviderController providerCtrl;

    public OperatorTerminal(ChocAnDatabase db, Scanner input) {
        super(db, input);
        this.memberCtrl = new MemberController(db);
        this.providerCtrl = new ProviderController(db);
    }

    /** Verify Operator sequence diagram: check the number, then show the menu. */
    public boolean loginOperator(String id) {
        boolean valid = login.checkLogin(id, "OPERATOR");
        if (valid) {
            operatorNum = id;
            userId = id;
            loggedIn = true;
            display("Validated");
            showMenu();
        } else {
            display("Invalid operator number");
        }
        return valid;
    }

    /** Manage Member Records sequence diagram. action = "add", "update", or "delete". */
    public void manageMember(String action, String memberNum) {
        boolean ok;
        switch (action.toLowerCase()) {
            case "add":
                String status = promptMatching("Status (Active/Suspended): ",
                        "(?i)active|suspended", "Enter Active or Suspended.");
                ok = memberCtrl.addMember(readInfo(memberNum, capitalize(status)));
                break;
            case "update":
                ok = memberCtrl.updateMember(memberNum, readInfo(memberNum, null));
                break;
            case "delete":
                ok = confirmDelete("member", memberNum) && memberCtrl.deleteMember(memberNum);
                break;
            default:
                display("Unknown action: " + action);
                return;
        }
        showResult(ok);
    }

    /** Manage Provider Records sequence diagram. action = "add", "update", or "delete". */
    public void manageProvider(String action, String providerNum) {
        boolean ok;
        switch (action.toLowerCase()) {
            case "add":
                String type = promptMatching("Provider type (e.g. Dietitian): ",
                        ".{1,25}", "Type is required (25 characters max).");
                ok = providerCtrl.addProvider(readInfo(providerNum, type));
                break;
            case "update":
                ok = providerCtrl.updateProvider(providerNum, readInfo(providerNum, null));
                break;
            case "delete":
                ok = confirmDelete("provider", providerNum) && providerCtrl.deleteProvider(providerNum);
                break;
            default:
                display("Unknown action: " + action);
                return;
        }
        showResult(ok);
    }

    @Override
    public void showMenu() {
        while (loggedIn) {
            display("");
            display("===== Operator Menu =====");
            display("1) Add member");
            display("2) Update member");
            display("3) Delete member");
            display("4) Add provider");
            display("5) Update provider");
            display("6) Delete provider");
            display("0) Log out");
            String choice = prompt("Choose an option: ");

            switch (choice) {
                case "1": manageMember("add", askNumber("New member number")); break;
                case "2": manageMember("update", askNumber("Member number")); break;
                case "3": manageMember("delete", askNumber("Member number")); break;
                case "4": manageProvider("add", askNumber("New provider number")); break;
                case "5": manageProvider("update", askNumber("Provider number")); break;
                case "6": manageProvider("delete", askNumber("Provider number")); break;
                case "0": logout(); break;
                default: display("Invalid option. Please choose 0-6.");
            }
        }
    }

    // ---------- helpers ----------

    /** Asks for a nine-digit number until one is entered. */
    private String askNumber(String label) {
        return promptMatching(label + " (9 digits): ", "\\d{9}", "Number must be exactly 9 digits.");
    }

    /**
     * Prompts for name and address fields (ChocAn field limits) and builds info[].
     * extra = status (member) or type (provider); null when updating.
     */
    private String[] readInfo(String number, String extra) {
        String name    = promptMatching("Name: ", ".{1,25}", "Name is required (25 characters max).");
        String address = promptMatching("Street address: ", ".{1,25}", "Address is required (25 characters max).");
        String city    = promptMatching("City: ", ".{1,14}", "City is required (14 characters max).");
        String state   = promptMatching("State (2 letters): ", "[A-Za-z]{2}", "State must be 2 letters.").toUpperCase();
        String zip     = promptMatching("ZIP code (5 digits): ", "\\d{5}", "ZIP code must be 5 digits.");

        if (extra == null) {
            return new String[] { name, number, address, city, state, zip };
        }
        return new String[] { name, number, address, city, state, zip, extra };
    }

    /** Asks the operator to confirm a delete. */
    private boolean confirmDelete(String kind, String number) {
        String answer = prompt("Delete " + kind + " " + number + "? (Y/N): ");
        if (!answer.equalsIgnoreCase("Y")) {
            display("Delete cancelled.");
            return false;
        }
        return true;
    }

    /** Confirmation message from the sequence diagrams. */
    private void showResult(boolean ok) {
        if (ok) {
            display("Record saved");
        } else {
            display("Record not found or data invalid");
        }
    }

    /** "active" -> "Active" */
    private static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
