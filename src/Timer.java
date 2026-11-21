import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

/**
 * System scheduler: the weekly update at midnight on Friday.
 *
 * The weekly update is:
 *   1. the main accounting procedure (all reports for the week), then
 *   2. a new week starts: the week's service records are moved to records_archive.txt
 *      so the next week's reports start clean.
 *
 * The time of the last update is saved in weekly_update.txt, so the update is never
 * run twice for the same Friday, and if the program was not running at Friday midnight
 * the update is run (once) the next time trigger() is called. The very first time
 * (no weekly_update.txt yet) the file is just created; nothing runs.
 *
 * MainMenu / AppPanel call trigger() regularly.
 *
 * @author Namita
 */
public class Timer {

    private String runDay = "Friday";
    private String runTime = "12:00 AM";
    private ReportController reports;
    private Clock clock;
    private Path stateFile;
    private LocalDateTime lastRun;         // when the weekly update last ran
    private String lastSummary = "";

    public Timer(ReportController reports) {
        this(reports, Clock.systemDefaultZone(), Paths.get(AppFolders.dataDir(), "weekly_update.txt"));
    }

    /** The clock and the state file can be replaced (used by the tests). */
    public Timer(ReportController reports, Clock clock, Path stateFile) {
        this.reports = reports;
        this.clock = clock;
        this.stateFile = stateFile;
        this.lastRun = loadState();
        if (lastRun == null) {
            lastRun = now();
            saveState();
        }
    }

    /**
     * Runs the weekly update if it is due and has not run for this Friday yet.
     * @return true if the weekly update ran
     */
    public boolean trigger() {
        LocalDateTime now = now();
        if (!lastRun.isBefore(mostRecentRunTime(now))) {
            return false;
        }
        System.out.println();
        System.out.println(runDay + " " + runTime + " has passed: running the weekly update.");
        reports.runWeeklyReports();
        int archived = reports.startNewWeek();
        lastRun = now;
        saveState();
        lastSummary = "Weekly update ran " + now.format(ServiceRecord.DATE_TIME) + ": "
                + reports.getReports().size() + " report file(s) written, "
                + Math.max(archived, 0) + " service record(s) archived (a new week starts).";
        System.out.println(lastSummary);
        return true;
    }

    /** When the weekly update last ran. */
    public LocalDateTime getLastRun() {
        return lastRun;
    }

    /** The next Friday 12:00 AM after now. */
    public LocalDateTime getNextDue() {
        return mostRecentRunTime(now()).plusDays(7);
    }

    /** One line about the last weekly update that ran during this session ("" if none). */
    public String getLastSummary() {
        return lastSummary;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** The most recent Friday 12:00 AM at or before the given time. */
    private static LocalDateTime mostRecentRunTime(LocalDateTime now) {
        LocalDate day = now.toLocalDate();
        while (day.getDayOfWeek() != DayOfWeek.FRIDAY) {
            day = day.minusDays(1);
        }
        return day.atStartOfDay();
    }

    private LocalDateTime loadState() {
        try {
            for (String line : Files.readAllLines(stateFile, StandardCharsets.UTF_8)) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    return LocalDateTime.parse(line, ServiceRecord.DATE_TIME);
                }
            }
        } catch (IOException | DateTimeParseException e) {
            // no usable state yet
        }
        return null;
    }

    private void saveState() {
        try {
            Files.write(stateFile, Arrays.asList(
                    "# when the weekly update (reports + new week) last ran; written by the program",
                    lastRun.format(ServiceRecord.DATE_TIME)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("WARNING: could not write " + stateFile + " (" + e.getClass().getSimpleName() + ")");
        }
    }
}
