import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * System scheduler. Triggers the weekly accounting procedure at midnight
 * on Friday. MainMenu calls trigger() each time the role menu is shown.
 *
 * @author Namita
 */
public class Timer {

    private String runDay = "Friday";
    private String runTime = "12:00 AM";
    private ReportController reports;
    private LocalDate lastRun;            // so the procedure runs only once per Friday

    public Timer(ReportController reports) {
        this.reports = reports;
    }

    /** If it is Friday midnight (the 12 AM hour) and it has not run today, run the weekly reports. */
    public void trigger() {
        if (isFridayMidnight() && !LocalDate.now().equals(lastRun)) {
            System.out.println();
            System.out.println("It is " + runDay + " " + runTime + ": running the main accounting procedure.");
            reports.runWeeklyReports();
            lastRun = LocalDate.now();
        }
    }

    private boolean isFridayMidnight() {
        LocalDateTime now = LocalDateTime.now();
        return now.getDayOfWeek() == DayOfWeek.FRIDAY && now.getHour() == 0;
    }
}
