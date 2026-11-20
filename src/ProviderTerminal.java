import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Scanner;

/**
 * Provider terminal: validate a member, bill ChocAn, and request the
 * Provider Directory.
 *
 * @author Sydney Renninger
 */
public class ProviderTerminal extends Terminal {

    private static final DateTimeFormatter SERVICE_DATE =
            DateTimeFormatter.ofPattern("MM-dd-uuuu").withResolverStyle(ResolverStyle.STRICT);

    private String providerNum;
    private ServiceController billing;
    private MemberController validator;

    public ProviderTerminal(ChocAnDatabase db, ProviderDirectory directory, Scanner input) {
        super(db, input);
        this.billing = new ServiceController(db, directory);
        this.validator = new MemberController(db);
    }

    /** Verify Provider sequence diagram: check the number, then show the menu. */
    public boolean loginProvider(String id) {
        boolean valid = login.checkLogin(id, "PROVIDER");
        if (valid) {
            providerNum = id;
            userId = id;
            loggedIn = true;
            display("Validated");
            showMenu();
        } else {
            display("Invalid provider number");
        }
        return valid;
    }

    /**
     * Validate Member sequence diagram.
     * @return "Validated", "Invalid number", or "Member suspended"
     */
    public String validateMember(String memberNum) {
        String result = validator.validateMember(memberNum);
        display(result);
        return result;
    }

    /** Bill ChocAn use case (p.9) and sequence diagram. */
    public void billChocAn(String memberNum) {
        // 1. The member must be validated before billing.
        if (!"Validated".equals(validateMember(memberNum))) {
            display("Billing cancelled.");
            return;
        }

        // 2. Date of service, MM-DD-YYYY.
        String date;
        while (true) {
            date = prompt("Date of service (MM-DD-YYYY): ");
            if (isValidServiceDate(date)) {
                break;
            }
            display("Invalid date. Use MM-DD-YYYY and a date that is not in the future.");
        }

        // 3. Service code -> show the service name -> provider confirms.
        Service service;
        while (true) {
            String code = prompt("Service code (6 digits, or 0 to cancel): ");
            if (code.equals("0")) {
                display("Billing cancelled.");
                return;
            }
            service = billing.getService(code);
            if (service == null) {
                display("Invalid service code");
                continue;
            }
            display("Service: " + service.getName());
            String answer = prompt("Is this the correct service? (Y/N): ");
            if (answer.equalsIgnoreCase("Y")) {
                break;
            }
        }

        // 4. Optional comments, up to 100 characters.
        String comments = promptMatching(
                "Comments (optional, up to 100 characters; press Enter to skip): ",
                ".{0,100}",
                "Comments must be 100 characters or less.");

        // 5. Create and save the service record, then show the fee.
        try {
            double fee = billing.billService(memberNum, providerNum, date,
                                             service.getCode(), comments);
            display("Service recorded.");
            display(String.format("Fee to be paid: $%.2f", fee));
        } catch (IllegalArgumentException e) {
            display("Could not record service: " + e.getMessage());
        }
    }

    /** Request Provider Directory sequence diagram. */
    public void requestDirectory() {
        boolean saved = billing.getDirectory();
        if (saved) {
            display("Directory saved");
        } else {
            display("Could not save directory");
        }
    }

    @Override
    public void showMenu() {
        while (loggedIn) {
            display("");
            display("===== Provider Menu =====");
            display("1) Validate member");
            display("2) Bill ChocAn for a service");
            display("3) Request Provider Directory");
            display("0) Log out");
            String choice = prompt("Choose an option: ");

            switch (choice) {
                case "1":
                    validateMember(prompt("Member number: "));
                    break;
                case "2":
                    billChocAn(prompt("Member number: "));
                    break;
                case "3":
                    requestDirectory();
                    break;
                case "0":
                    logout();
                    break;
                default:
                    display("Invalid option. Please choose 0-3.");
            }
        }
    }

    /** True if date is a real MM-DD-YYYY date that is not in the future. */
    private boolean isValidServiceDate(String date) {
        try {
            return !LocalDate.parse(date, SERVICE_DATE).isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
