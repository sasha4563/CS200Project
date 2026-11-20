/**
 * Builds reports: all of them for the weekly accounting procedure, or one
 * type on a manager's request.
 *
 * @author
 */
import java.util.ArrayList;
import java.util.List;

public class ReportController {

    private ChocAnDatabase db;
    private List<Report> reports;

    public ReportController(ChocAnDatabase db) {

        this.db = db;

        reports = new ArrayList<>();

        reports.add(new MemberReport(db));
        reports.add(new ProviderReport(db));
        reports.add(new SummaryReport());
        reports.add(new EFTReport(db));
    }

    // Runs all four weekly reports
    public void runWeeklyReports() {

        List<ServiceRecord> records = db.getServiceRecords();

        for (Report report : reports) {
            report.generate(records);
        }

        System.out.println("All weekly reports generated.");
    }

    public boolean makeReport(String type) {

        List<ServiceRecord> records = db.getServiceRecords();

        Report report;

        switch (type.toLowerCase()) {

            case "member":
                report = new MemberReport(db);
                break;

            case "provider":
                report = new ProviderReport(db);
                break;

            case "summary":
                report = new SummaryReport();
                break;

            case "eft":
                report = new EFTReport(db);
                break;

            default:
                System.out.println("Invalid report type.");
                return false;
        }

        report.generate(records);

        return true;
    }
}