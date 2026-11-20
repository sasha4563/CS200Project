import java.util.Scanner;

/**
 * Abstract base class for the three terminals. Holds the logged-in user and
 * the shared login, display, and logout behavior.
 *
 * @author Sydney Renninger
 */
public abstract class Terminal {

    protected String userId;
    protected boolean loggedIn;
    protected LoginController login;
    protected Scanner input;            // keyboard input, shared with MainMenu

    public Terminal(ChocAnDatabase db, Scanner input) {
        this.login = new LoginController(db);
        this.input = input;
        this.userId = null;
        this.loggedIn = false;
    }

    /** Prints a message to the screen. */
    public void display(String message) {
        System.out.println(message);
    }

    /** Shows the menu for this terminal and loops until the user logs out. */
    public abstract void showMenu();

    /** Ends the session: loggedIn = false, userId = null. */
    public void logout() {
        loggedIn = false;
        userId = null;
        display("Logged out.");
    }

    /** Prints a label and returns the user's typed line (trimmed). */
    protected String prompt(String label) {
        System.out.print(label);
        if (!input.hasNextLine()) {          // input closed (e.g. Ctrl+D / Ctrl+Z)
            System.out.println();
            display("Input closed. Exiting.");
            System.exit(0);
        }
        return input.nextLine().trim();
    }

    /** Keeps prompting until the answer matches the regex; shows error otherwise. */
    protected String promptMatching(String label, String regex, String error) {
        while (true) {
            String answer = prompt(label);
            if (answer.matches(regex)) {
                return answer;
            }
            display(error);
        }
    }
}
