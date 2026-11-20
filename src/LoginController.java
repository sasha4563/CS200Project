/**
 * Checks whether a provider, operator, or manager number is valid for the
 * requested role. Used by all three terminals (see the Verify Provider /
 * Operator / Manager sequence diagrams).
 *
 * @author Sydney Renninger
 */
public class LoginController {

    private ChocAnDatabase db;

    public LoginController(ChocAnDatabase db) {
        this.db = db;
    }

    /**
     * @param id   the nine-digit number the user typed
     * @param role "PROVIDER", "OPERATOR", or "MANAGER" (any case)
     * @return true if the number is valid for that role
     */
    public boolean checkLogin(String id, String role) {
        if (id == null || role == null || !id.matches("\\d{9}")) {
            return false;                    // every ChocAn number is exactly 9 digits
        }

        switch (role.toUpperCase()) {
            case "PROVIDER":
                return db.isValidProvider(id);
            case "OPERATOR":
            case "MANAGER":
                return db.isValidStaff(id, role.toUpperCase());
            default:
                return false;
        }
    }
}
