import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulated link to the Acme Accounting System. Reads membership status
 * updates (active / suspended) from acmesimulation.txt and applies them to
 * member records once their scheduled time has passed.
 *
 * File format, one update per line (blank lines and lines starting with # are ignored):
 *   memberNumber,status,MM-DD-YYYY HH:MM:SS
 *   123456789,Suspended,11-20-2026 21:00:00
 *
 * A line is applied once, the first time a check happens at or after its time,
 * and then ",APPLIED" is added to it so it is never applied again. A line with
 * no time is applied at the next check.
 *
 * @author Sameeksha
 */
public class AcmeInterface {

    private static final DateTimeFormatter WITH_SECONDS =
            DateTimeFormatter.ofPattern("MM-dd-uuuu HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter NO_SECONDS =
            DateTimeFormatter.ofPattern("MM-dd-uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private String lastUpdate;              // when an update was last applied
    private MemberController memberCtrl;
    private String simulationFile;

    public AcmeInterface(MemberController memberCtrl) {
        this(memberCtrl, AppFolders.dataDir() + "/acmesimulation.txt");
    }

    public AcmeInterface(MemberController memberCtrl, String simulationFile) {
        this.memberCtrl = memberCtrl;
        this.simulationFile = simulationFile;
        this.lastUpdate = "never";
    }

    /**
     * Applies the lines of the file whose time has come (uses the current date/time).
     * @return the number of members updated, or -1 if the file could not be read
     */
    public int receiveUpdates(String file) {
        return process(file, false);
    }

    /** Applies every pending line of the file right now, whatever its scheduled time. */
    public int receiveUpdatesNow(String file) {
        return process(file, true);
    }

    /** Called each time the role menu shows: applies due lines of the simulation file. */
    public int checkSchedule() {
        return Math.max(0, process(simulationFile, false));
    }

    /** The file the schedule is read from. */
    public String getSimulationFile() {
        return simulationFile;
    }

    /** How many lines of the simulation file have not been applied yet. */
    public int countPending() {
        return pendingLines().size();
    }

    /** The earliest time of a pending line ("" if none has a valid time). */
    public String nextScheduled() {
        LocalDateTime next = null;
        for (String[] f : pendingLines()) {
            LocalDateTime t = (f.length > 2) ? parseTime(f[2]) : null;
            if (t != null && (next == null || t.isBefore(next))) {
                next = t;
            }
        }
        return next == null ? "" : next.format(ServiceRecord.DATE_TIME);
    }

    private List<String[]> pendingLines() {
        List<String[]> pending = new ArrayList<>();
        try {
            for (String raw : Files.readAllLines(Paths.get(simulationFile), StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] f = line.split(",");
                for (int i = 0; i < f.length; i++) {
                    f[i] = f[i].trim();
                }
                boolean done = f.length >= 4 && (f[3].equalsIgnoreCase("APPLIED") || f[3].equalsIgnoreCase("SKIPPED"));
                if (f.length >= 2 && !done) {
                    pending.add(f);
                }
            }
        } catch (IOException e) {
            // no file: nothing pending
        }
        return pending;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    private int process(String file, boolean ignoreTime) {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return -1;
        }

        LocalDateTime now = LocalDateTime.now();
        List<String> out = new ArrayList<>();
        int count = 0;
        boolean changed = false;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                out.add(raw);
                continue;
            }
            String[] f = line.split(",");
            for (int i = 0; i < f.length; i++) {
                f[i] = f[i].trim();
            }
            boolean done = f.length >= 4 && (f[3].equalsIgnoreCase("APPLIED") || f[3].equalsIgnoreCase("SKIPPED"));
            if (done || f.length < 2) {
                out.add(raw);
                continue;
            }

            String time = (f.length >= 3) ? f[2] : "";
            if (!time.isEmpty() && !ignoreTime) {
                LocalDateTime due = parseTime(time);
                if (due == null || due.isAfter(now)) {
                    out.add(raw);               // bad time, or not due yet
                    continue;
                }
            }

            boolean ok = memberCtrl.setStatus(f[0], f[1]);
            if (!ok && memberCtrl.getLastError().startsWith("Could not write")) {
                System.out.println("WARNING: " + memberCtrl.getLastError());
                out.add(raw);                   // try again at the next check
                continue;
            }
            if (ok) {
                count++;
                lastUpdate = now.format(ServiceRecord.DATE_TIME);
            }
            String stamp = time.isEmpty() ? now.format(ServiceRecord.DATE_TIME) : time;
            out.add(f[0] + "," + f[1] + "," + stamp + "," + (ok ? "APPLIED" : "SKIPPED"));
            changed = true;
        }

        if (changed) {
            try {
                Files.write(Paths.get(file), out, StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.out.println("Warning: could not update " + file);
            }
        }
        return count;
    }

    private static LocalDateTime parseTime(String s) {
        try {
            return LocalDateTime.parse(s, WITH_SECONDS);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(s, NO_SECONDS);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }
}
