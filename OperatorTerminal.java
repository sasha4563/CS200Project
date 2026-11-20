import java.util.Scanner;

/**
 * Operator terminal: add, update, and delete member and provider records.
 *
 * info[] passed to the controllers uses this layout:
 *   0 name | 1 number | 2 address | 3 city | 4 state | 5 zip | 6 status (member) or type (provider)
 *
 * Rules shown to the operator:
 *   add    - refused straight away if the number already exists
 *   update - asks which part to change (name, address, ...) and saves it right away;
 *            if the number does not exist, says so and creates a new record instead
 *   delete - says so if the number does not exist
 *   any failure says which piece of data was wrong
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
        switch (action.toLowerCase()) {
            case "add":
                if (memberCtrl.memberExists(memberNum)) {
                    display("Member " + memberNum + " already exists. Nothing was added (use Update to change them).");
                    return;
                }
                addNewMember(memberNum);
                break;
            case "update":
                if (!memberCtrl.memberExists(memberNum)) {
                    display("Member " + memberNum + " does not exist. Okay, going to create a new member with this number.");
                    addNewMember(memberNum);
                    return;
                }
                updateMemberFields(memberNum);
                break;
            case "delete":
                if (!memberCtrl.memberExists(memberNum)) {
                    display("Member " + memberNum + " does not exist. Nothing was deleted.");
                    return;
                }
                if (confirmDelete("member", memberNum)) {
                    showResult(memberCtrl.deleteMember(memberNum), "Member deleted. Removed from " + memberCtrl.getDataFile(),
                            memberCtrl.getLastError(), memberCtrl.getSavedLine(memberNum));
                }
                break;
            default:
                display("Unknown action: " + action);
        }
    }

    /** Manage Provider Records sequence diagram. action = "add", "update", or "delete". */
    public void manageProvider(String action, String providerNum) {
        switch (action.toLowerCase()) {
            case "add":
                if (providerCtrl.providerExists(providerNum)) {
                    display("Provider " + providerNum + " already exists. Nothing was added (use Update to change them).");
                    return;
                }
                addNewProvider(providerNum);
                break;
            case "update":
                if (!providerCtrl.providerExists(providerNum)) {
                    display("Provider " + providerNum + " does not exist. Okay, going to create a new provider with this number.");
                    addNewProvider(providerNum);
                    return;
                }
                updateProviderFields(providerNum);
                break;
            case "delete":
                if (!providerCtrl.providerExists(providerNum)) {
                    display("Provider " + providerNum + " does not exist. Nothing was deleted.");
                    return;
                }
                if (confirmDelete("provider", providerNum)) {
                    showResult(providerCtrl.deleteProvider(providerNum), "Provider deleted. Removed from " + providerCtrl.getDataFile(),
                            providerCtrl.getLastError(), providerCtrl.getSavedLine(providerNum));
                }
                break;
            default:
                display("Unknown action: " + action);
        }
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
            display("7) View member.txt (the file this program is using)");
            display("8) View provider.txt (the file this program is using)");
            display("0) Log out");
            String choice = prompt("Choose an option: ");

            switch (choice) {
                case "1": manageMember("add", askNumber("New member number")); break;
                case "2": manageMember("update", askNumber("Member number")); break;
                case "3": manageMember("delete", askNumber("Member number")); break;
                case "4": manageProvider("add", askNumber("New provider number")); break;
                case "5": manageProvider("update", askNumber("Provider number")); break;
                case "6": manageProvider("delete", askNumber("Provider number")); break;
                case "7": showFile(memberCtrl.getDataFile(), memberCtrl.getFileContents()); break;
                case "8": showFile(providerCtrl.getDataFile(), providerCtrl.getFileContents()); break;
                case "0": logout(); break;
                default: display("Invalid option. Please choose 0-8.");
            }
        }
    }

    // ---------- helpers ----------

    /**
     * Update Member: shows the current information and asks which part to change.
     * Each change is validated and saved to member.txt right away. The number cannot change.
     */
    private void updateMemberFields(String num) {
        while (true) {
            Member m = memberCtrl.getMember(num);
            display("");
            display("Member " + num + " - current information:");
            display("  1) Name:           " + m.getName());
            display("  2) Street address: " + m.getAddress());
            display("  3) City:           " + m.getCity());
            display("  4) State:          " + m.getState());
            display("  5) ZIP code:       " + m.getZip());
            display("  6) Status:         " + m.getStatus());
            display("  0) Done");
            String choice = prompt("Which part do you want to update? ");
            if (choice.equals("0")) {
                return;
            }
            if (!choice.matches("[1-6]")) {
                display("Please choose 0-6.");
                continue;
            }
            String[] info = { m.getName(), num, m.getAddress(), m.getCity(), m.getState(), m.getZip() };
            if (choice.equals("6")) {
                String status = promptMatching("New status (Active/Suspended): ",
                        "(?i)active|suspended", "Enter Active or Suspended.");
                info = new String[] { info[0], num, info[2], info[3], info[4], info[5], capitalize(status) };
            } else {
                int slot = Integer.parseInt(choice) - 1;      // 1->name(0), 2->address(2), ...
                int index = (slot == 0) ? 0 : slot + 1;
                info[index] = readField(choice);
            }
            showResult(memberCtrl.updateMember(num, info),
                    "Member updated. Saved in " + memberCtrl.getDataFile(), memberCtrl.getLastError(),
                    memberCtrl.getSavedLine(num));
        }
    }

    /**
     * Update Provider: shows the current information and asks which part to change.
     * Each change is validated and saved to provider.txt right away. The number cannot change.
     */
    private void updateProviderFields(String num) {
        while (true) {
            Provider p = providerCtrl.getProvider(num);
            display("");
            display("Provider " + num + " - current information:");
            display("  1) Hospital / clinic name: " + p.getName());
            display("  2) Street address:         " + p.getAddress());
            display("  3) City:                   " + p.getCity());
            display("  4) State:                  " + p.getState());
            display("  5) ZIP code:               " + p.getZip());
            display("  6) Provider type:          " + p.getType());
            display("  0) Done");
            String choice = prompt("Which part do you want to update? ");
            if (choice.equals("0")) {
                return;
            }
            if (!choice.matches("[1-6]")) {
                display("Please choose 0-6.");
                continue;
            }
            String[] info = { p.getName(), num, p.getAddress(), p.getCity(), p.getState(), p.getZip() };
            if (choice.equals("6")) {
                String type = promptMatching("New provider type (e.g. Dietitian): ",
                        "[^|]{1,25}", "Provider type is required (25 characters max).");
                info = new String[] { info[0], num, info[2], info[3], info[4], info[5], type };
            } else {
                int slot = Integer.parseInt(choice) - 1;
                int index = (slot == 0) ? 0 : slot + 1;
                info[index] = readField(choice);
            }
            showResult(providerCtrl.updateProvider(num, info),
                    "Provider updated. Saved in " + providerCtrl.getDataFile(), providerCtrl.getLastError(),
                    providerCtrl.getSavedLine(num));
        }
    }

    /** Asks for the new value of one field: 1 name, 2 address, 3 city, 4 state, 5 ZIP. */
    private String readField(String choice) {
        switch (choice) {
            case "1": return promptMatching("New name (25 characters max): ", "[^|]{1,25}", "Name is required (1-25 characters, no '|').");
            case "2": return promptMatching("New street address (25 characters max): ", "[^|]{1,25}", "Address is required (1-25 characters, no '|').");
            case "3": return promptMatching("New city (14 characters max): ", "[^|]{1,14}", "City is required (1-14 characters, no '|').");
            case "4": return promptMatching("New state (2 letters): ", "[A-Za-z]{2}", "State must be exactly 2 letters.").toUpperCase();
            default:  return promptMatching("New ZIP code (5 digits): ", "\\d{5}", "ZIP code must be exactly 5 digits.");
        }
    }

    /** Asks for a new member's details and adds them. */
    private void addNewMember(String memberNum) {
        String status = promptMatching("Status (Active/Suspended): ",
                "(?i)active|suspended", "Enter Active or Suspended.");
        showResult(memberCtrl.addMember(readInfo("Name", memberNum, capitalize(status))),
                "Member added. Saved in " + memberCtrl.getDataFile(), memberCtrl.getLastError(),
                memberCtrl.getSavedLine(memberNum));
    }

    /** Asks for a new provider's details and adds them. */
    private void addNewProvider(String providerNum) {
        String type = promptMatching("Provider type (e.g. Dietitian): ",
                "[^|]{1,25}", "Provider type is required (25 characters max).");
        showResult(providerCtrl.addProvider(readInfo("Hospital / clinic name", providerNum, type)),
                "Provider added. Saved in " + providerCtrl.getDataFile(), providerCtrl.getLastError(),
                providerCtrl.getSavedLine(providerNum));
    }

    /** Prints a data file's full path and its lines exactly as they are on disk right now. */
    private void showFile(String path, java.util.List<String> lines) {
        display("");
        display("File: " + path);
        for (String line : lines) {
            display(line);
        }
    }

    /** Asks for a nine-digit number until one is entered. */
    private String askNumber(String label) {
        return promptMatching(label + " (9 digits): ", "\\d{9}", "Number must be exactly 9 digits.");
    }

    /**
     * Prompts for name and address fields (ChocAn field limits) and builds info[].
     * extra = status (member) or type (provider); null when updating.
     */
    private String[] readInfo(String nameLabel, String number, String extra) {
        String name    = promptMatching(nameLabel + " (25 characters max): ", "[^|]{1,25}", "Name is required (1-25 characters, no '|').");
        String address = promptMatching("Street address (25 characters max): ", "[^|]{1,25}", "Address is required (1-25 characters, no '|').");
        String city    = promptMatching("City (14 characters max): ", "[^|]{1,14}", "City is required (1-14 characters, no '|').");
        String state   = promptMatching("State (2 letters): ", "[A-Za-z]{2}", "State must be exactly 2 letters.").toUpperCase();
        String zip     = promptMatching("ZIP code (5 digits): ", "\\d{5}", "ZIP code must be exactly 5 digits.");

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

    /** Confirmation (or the reason it failed). */
    private void showResult(boolean ok, String successMessage, String error, String proof) {
        if (ok) {
            display(successMessage);
            display("  In the file now -> " + proof);
        } else {
            display("Not saved: " + error);
        }
    }

    /** "active" -> "Active" */
    private static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
