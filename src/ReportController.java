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

    public ReportController(ChocAnDatabase db) {
        this.db = db;
    }

    /** Main accounting procedure: member reports, provider reports, summary, and EFT data. */
    public void runWeeklyReports() {
        reports.clear();
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
     * One report type on a manager's request.
     * @param type "member", "provider", "summary", or "eft"
     * @return true if the report(s) were saved
     */
    public boolean makeReport(String type) {
        reports.clear();
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
            System.out.println("No services have been billed this week.");
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
            System.out.println("  Note: " + (week.size() - payable.size())
                    + " billed service(s) from provider(s) no longer in the system are not in the Summary / EFT.");
        }

        boolean allSaved = true;
        for (Report r : reports) {
            boolean money = (r instanceof SummaryReport) || (r instanceof EFTReport);
            r.generate(money ? payable : week);
            boolean saved = r.saveToFile();
            allSaved &= saved;
            System.out.println((saved ? "  Saved " : "  FAILED to save ") + r.getPath());
            if (r instanceof SummaryReport) {
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
