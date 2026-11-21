import java.util.ArrayList;
import java.util.List;

/**
 * Builds reports: all of them for the weekly accounting procedure, or one
 * type on a manager's request. Each report is written to its own file, in a
 * sub-folder for its kind: reports/members, reports/providers, reports/summary, reports/eft.
 *
 * @author Namita
 */
public class ReportController {

    private ChocAnDatabase db;
    private List<Report> reports = new ArrayList<>();
    private boolean quiet = false;       // true = do not print to the console (the GUI shows the results itself)
    private String note = "";            // extra information about the last run, e.g. skipped services

    /** When quiet, nothing is printed to the console. */
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    /** The reports built by the last runWeeklyReports / makeReport call. */
    public List<Report> getReports() {
        return new ArrayList<>(reports);
    }

    /** A message about the last run (for example "No services have been billed this week."), or "". */
    public String getNote() {
        return note;
    }

    public ReportController(ChocAnDatabase db) {
        this.db = db;
    }

    /** Main accounting procedure: member reports, provider reports, summary, and EFT data. */
    public void runWeeklyReports() {
        reports.clear();
        note = "";
        for (Member m : db.getMembersServed()) {
            reports.add(new MemberReport(m, db));
        }
        for (Provider p : db.getProvidersPaid()) {
            reports.add(new ProviderReport(p, db));
        }
        reports.add(new SummaryReport(db));
        reports.add(new EFTReport(db));
        generateAndSave();
    }

    /**
     * Starts a new week: this week's service records are moved to records_archive.txt, so the
     * next reports start clean. The weekly update (Timer) does this right after its reports; a
     * manager can also do it by hand at any time.
     * @return the number of service records moved, or -1 if the files could not be written
     */
    public int startNewWeek() {
        int moved = db.clearWeekRecords();
        note = (moved < 0) ? "Could not clear this week's data: " + db.getLastWriteError()
                : moved + " service record(s) moved to records_archive.txt. This week starts fresh.";
        if (!quiet) {
            System.out.println("  " + note);
        }
        return moved;
    }

    /**
     * One report type on a manager's request.
     * @param type "member", "provider", "summary", or "eft"
     * @return true if the report(s) were saved
     */
    public boolean makeReport(String type) {
        reports.clear();
        note = "";
        switch (type.toLowerCase()) {
            case "member":
                for (Member m : db.getMembersServed()) {
                    reports.add(new MemberReport(m, db));
                }
                break;
            case "provider":
                for (Provider p : db.getProvidersPaid()) {
                    reports.add(new ProviderReport(p, db));
                }
                break;
            case "summary":
                reports.add(new SummaryReport(db));
                break;
            case "eft":
                reports.add(new EFTReport(db));
                break;
            default:
                return false;
        }
        if (reports.isEmpty()) {
            note = "No services have been billed this week.";
            if (!quiet) {
                System.out.println(note);
            }
            return false;
        }
        return generateAndSave();
    }

    /**
     * Generates every report in the list, saves it, and shows the summary on screen.
     * The Summary and EFT reports (money) only cover providers that are still in the
     * system; a note is printed if billed services from a deleted provider are skipped.
     */
    private boolean generateAndSave() {
        List<ServiceRecord> week = db.getWeekRecords();
        List<ServiceRecord> payable = new ArrayList<>();
        for (ServiceRecord r : week) {
            if (db.getProvider(r.getProviderNum()) != null) {
                payable.add(r);
            }
        }
        if (payable.size() < week.size()) {
            note = (week.size() - payable.size())
                    + " billed service(s) from provider(s) no longer in the system are not in the Summary / EFT.";
            if (!quiet) {
                System.out.println("  Note: " + note);
            }
        }

        boolean allSaved = true;
        for (Report r : reports) {
            boolean money = (r instanceof SummaryReport) || (r instanceof EFTReport);
            r.generate(money ? payable : week);
            boolean saved = r.saveToFile();
            allSaved &= saved;
            if (!quiet) {
                System.out.println((saved ? "  Saved " : "  FAILED to save ") + r.getPath());
            }
            if (!quiet && r instanceof SummaryReport) {
                System.out.println();
                for (String line : r.getLines()) {
                    System.out.println("  " + line);
                }
                System.out.println();
            }
        }
        return allSaved;
    }
}
