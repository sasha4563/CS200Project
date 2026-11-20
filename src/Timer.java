/**
 * System scheduler. Triggers the weekly accounting procedure at midnight
 * on Friday.
 *
 * @author
 */
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Timer {

    private String runDay = "Friday";
    private String runTime = "12:00 AM";

    private ReportController reports;

    public Timer(ReportController reports) {
        this.reports = reports;
    }

    public void trigger() {

        if (isFridayMidnight()) {
            reports.runWeeklyReports();
        }
    }

    private boolean isFridayMidnight() {

        LocalDateTime now = LocalDateTime.now();

        DayOfWeek currentDay = now.getDayOfWeek();

        LocalTime currentTime = now.toLocalTime();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

        LocalTime scheduledTime = LocalTime.parse(runTime, timeFormatter);

        boolean correctDay = currentDay.toString().equalsIgnoreCase(runDay);

        boolean correctTime = currentTime.getHour() == scheduledTime.getHour() && currentTime.getMinute() == scheduledTime.getMinute();

        return correctDay && correctTime;
    }
}
