import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores and returns members, providers, service records, and staff IDs.
 * Contains no business rules.
 *
 * When created with loadFromFiles the data files and the program stay in sync
 * in both directions, in real time:
 *   - every change is written to the file immediately (the person's line is found
 *     by number in member.txt / provider.txt and replaced, removed, or added; billed
 *     services are appended to records.txt) and the file is read back to check
 *     that the change really is there;
 *   - if a file is changed by hand while the program is running, it is re-read
 *     the next time the data is used;
 *   - lines that are not valid are skipped with a WARNING that says why.
 *
 * Who wrote what: the storage and lookup methods are Samantha's. The data-file persistence
 * (loadFromFiles, line-level writes, read-back checks, real-time sync) is Sameeksha's, and
 * the weekly-update archive (clearWeekRecords) is Namita's.
 *
 * @author Samantha
 */
public class ChocAnDatabase {

    private List<Member> members;
    private List<Provider> providers;
    private List<ServiceRecord> records;
    private List<String> staffIds;      // stored as "id:ROLE"
    private String dataDir;             // null = keep everything in memory only

    private final Map<String, Long> seen = new HashMap<>();   // file name -> last-modified time we have loaded
    private String lastWriteError = "";

    public ChocAnDatabase(List<Member> members, List<Provider> providers,
                          List<ServiceRecord> records, List<String> staffIds) {
        this.members = members;
        this.providers = providers;
        this.records = records;
        this.staffIds = staffIds;
    }

    /** Loads everything from the data folder (creating sample data on the first run). */
    public static ChocAnDatabase loadFromFiles(String dir) {
        SampleData.ensureFiles(dir);
        ChocAnDatabase db = new ChocAnDatabase(new ArrayList<Member>(), new ArrayList<Provider>(),
                new ArrayList<ServiceRecord>(), new ArrayList<String>());
        db.dataDir = dir;
        db.reloadMembers();
        db.reloadProviders();
        db.reloadStaff();
        db.reloadRecords();
        return db;
    }

    // ---------- login ----------

    public boolean isValidProvider(String id) {
        return getProvider(id) != null;
    }

    /** role is "MANAGER" or "OPERATOR". */
    public boolean isValidStaff(String id, String role) {
        refresh();
        return id != null && role != null && staffIds.contains(id.trim() + ":" + role.toUpperCase());
    }

    // ---------- members and providers ----------

    public Member getMember(String memberNum) {
        refresh();
        for (Member m : members) {
            if (m.getNumber().equals(memberNum)) {
                return m;
            }
        }
        return null;
    }

    public Provider getProvider(String providerNum) {
        refresh();
        for (Provider p : providers) {
            if (p.getNumber().equals(providerNum)) {
                return p;
            }
        }
        return null;
    }

    /** All members (a copy), re-reading member.txt first if it changed. */
    public List<Member> getMembers() {
        refresh();
        return new ArrayList<>(members);
    }

    /** All providers (a copy), re-reading provider.txt first if it changed. */
    public List<Provider> getProviders() {
        refresh();
        return new ArrayList<>(providers);
    }

    /** Writes the new member's line to member.txt, then adds the member. False if the file could not be written. */
    public boolean addMember(Member m) {
        refresh();
        if (!writeLine("member.txt", m.getNumber(), m.toLine())) {
            return false;
        }
        members.add(m);
        return true;
    }

    /** Removes the member's line from member.txt, then removes the member. False if the file could not be written. */
    public boolean deleteMember(String memberNum) {
        refresh();
        if (!writeLine("member.txt", memberNum, null)) {
            return false;
        }
        members.removeIf(m -> m.getNumber().equals(memberNum));
        return true;
    }

    public boolean addProvider(Provider p) {
        refresh();
        if (!writeLine("provider.txt", p.getNumber(), p.toLine())) {
            return false;
        }
        providers.add(p);
        return true;
    }

    public boolean deleteProvider(String providerNum) {
        refresh();
        if (!writeLine("provider.txt", providerNum, null)) {
            return false;
        }
        providers.removeIf(p -> p.getNumber().equals(providerNum));
        return true;
    }

    /** Writes this member's line in member.txt (replaces the line with the same number, or adds it). */
    public boolean saveMember(Member m) {
        return writeLine("member.txt", m.getNumber(), m.toLine());
    }

    /** Writes this provider's line in provider.txt (replaces the line with the same number, or adds it). */
    public boolean saveProvider(Provider p) {
        return writeLine("provider.txt", p.getNumber(), p.toLine());
    }

    // ---------- service records ----------

    /** Appends the record to records.txt, then adds it. False if the file could not be written. */
    public boolean saveRecord(ServiceRecord r) {
        refresh();
        lastWriteError = "";
        if (dataDir != null) {
            Path path = Paths.get(dataDir, "records.txt");
            try {
                Files.write(path, (r.toLine() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                List<String> check = Files.readAllLines(path, StandardCharsets.UTF_8);
                if (check.isEmpty() || !check.get(check.size() - 1).trim().equals(r.toLine())) {
                    lastWriteError = path + " does not contain the new line after writing";
                    return false;
                }
                markSeen("records.txt");
            } catch (IOException e) {
                lastWriteError = path + " (" + e.getClass().getSimpleName() + ")";
                return false;
            }
        }
        records.add(r);
        return true;
    }

    /** Records received in the last 7 days, in the order they were received. */
    public List<ServiceRecord> getWeekRecords() {
        refresh();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        List<ServiceRecord> week = new ArrayList<>();
        for (ServiceRecord r : records) {
            if (!r.getReceivedDateTime().isBefore(cutoff)) {
                week.add(r);
            }
        }
        return week;
    }

    /**
     * Starts a new week: moves this week's service records (the ones getWeekRecords returns)
     * from records.txt to records_archive.txt. The archive is written first, so nothing is lost.
     * @return the number of records moved, or -1 if a file could not be written
     */
    public int clearWeekRecords() {
        refresh();
        lastWriteError = "";
        if (dataDir == null) {
            List<ServiceRecord> week = getWeekRecords();
            records.removeAll(week);
            return week.size();
        }
        Path path = Paths.get(dataDir, "records.txt");
        Path archive = Paths.get(dataDir, "records_archive.txt");
        try {
            Map<String, Service> services = loadServices(dataDir);
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
            List<String> keep = new ArrayList<>();
            List<String> moved = new ArrayList<>();
            for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String line = raw.trim();
                ServiceRecord r = (line.isEmpty() || line.startsWith("#")) ? null : ServiceRecord.fromLine(line, services);
                if (r != null && !r.getReceivedDateTime().isBefore(cutoff)) {
                    moved.add(line);
                } else {
                    keep.add(raw);
                }
            }
            if (moved.isEmpty()) {
                return 0;
            }
            List<String> block = new ArrayList<>();
            block.add("# archived " + LocalDateTime.now().format(ServiceRecord.DATE_TIME) + " (" + moved.size() + " services)");
            block.addAll(moved);
            Files.write(archive, block, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Files.write(path, keep, StandardCharsets.UTF_8);
            if (Files.readAllLines(path, StandardCharsets.UTF_8).size() != keep.size()) {
                lastWriteError = path + " does not show the change after writing";
                return -1;
            }
            reloadRecords();
            return moved.size();
        } catch (IOException e) {
            lastWriteError = path + " (" + e.getClass().getSimpleName() + ")";
            return -1;
        }
    }

    /** Members who received at least one service this week. */
    public List<Member> getMembersServed() {
        Set<String> nums = new LinkedHashSet<>();
        for (ServiceRecord r : getWeekRecords()) {
            nums.add(r.getMemberNum());
        }
        List<Member> served = new ArrayList<>();
        for (String n : nums) {
            Member m = getMember(n);
            if (m != null) {
                served.add(m);
            }
        }
        return served;
    }

    /** Providers who billed at least one service this week. */
    public List<Provider> getProvidersPaid() {
        Set<String> nums = new LinkedHashSet<>();
        for (ServiceRecord r : getWeekRecords()) {
            nums.add(r.getProviderNum());
        }
        List<Provider> paid = new ArrayList<>();
        for (String n : nums) {
            Provider p = getProvider(n);
            if (p != null) {
                paid.add(p);
            }
        }
        return paid;
    }

    // ---------- files: where they are and what went wrong ----------

    /** Full path of a data file, e.g. getFilePath("member.txt"). */
    public String getFilePath(String file) {
        return (dataDir == null) ? file : Paths.get(dataDir, file).toAbsolutePath().toString();
    }

    /**
     * Opens the data file again and reports what it holds for this number, e.g.
     * "line 9 of 9: Zed Young|321321321|...", or a message if no line has that number.
     * This reads the file itself, not the program's memory, so it shows what is really on disk.
     */
    public String readBack(String file, String number) {
        if (dataDir == null) {
            return "(no data file in use)";
        }
        try {
            List<String> all = Files.readAllLines(Paths.get(dataDir, file), StandardCharsets.UTF_8);
            for (int i = 0; i < all.size(); i++) {
                String[] f = all.get(i).split("\\|", -1);
                if (f.length > 1 && f[1].trim().equals(number)) {
                    return "line " + (i + 1) + " of " + all.size() + ": " + all.get(i).trim();
                }
            }
            return "no line for " + number + " is in the file now (" + all.size() + " lines in total)";
        } catch (IOException e) {
            return "could not read the file (" + e.getClass().getSimpleName() + ")";
        }
    }

    /** Every line of a data file, numbered, exactly as it is on disk right now. */
    public List<String> readFileNumbered(String file) {
        List<String> out = new ArrayList<>();
        if (dataDir == null) {
            return out;
        }
        try {
            List<String> all = Files.readAllLines(Paths.get(dataDir, file), StandardCharsets.UTF_8);
            for (int i = 0; i < all.size(); i++) {
                out.add(String.format("%3d  %s", i + 1, all.get(i)));
            }
        } catch (IOException e) {
            out.add("could not read the file (" + e.getClass().getSimpleName() + ")");
        }
        return out;
    }

    /** Why the last add / delete / save / saveRecord returned false. */
    public String getLastWriteError() {
        return lastWriteError;
    }

    // ---------- real-time sync with the files ----------

    /** Re-reads any data file that was changed on disk since we last read or wrote it. */
    private void refresh() {
        if (dataDir == null) {
            return;
        }
        if (changed("member.txt"))   { reloadMembers(); }
        if (changed("provider.txt")) { reloadProviders(); }
        if (changed("manager.txt") || changed("operator.txt")) { reloadStaff(); }
        if (changed("records.txt") || changed("services.txt")) { reloadRecords(); }
    }

    private long modified(String file) {
        try {
            return Files.getLastModifiedTime(Paths.get(dataDir, file)).toMillis();
        } catch (IOException e) {
            return -1;
        }
    }

    private boolean changed(String file) {
        Long last = seen.get(file);
        return last == null || last != modified(file);
    }

    private void markSeen(String file) {
        seen.put(file, modified(file));
    }

    private void reloadMembers() {
        List<Member> fresh = new ArrayList<>();
        Set<String> numbers = new HashSet<>();
        for (String line : readLines(dataDir, "member.txt")) {
            String[] f = fields(line);
            String error = Party.validate(f);
            if (error == null && (f.length < 7 || !f[6].matches("(?i)active|suspended"))) {
                error = "Status must be Active or Suspended";
            }
            if (error == null && !numbers.add(f[1])) {
                error = "Duplicate member number";
            }
            if (error != null) {
                warn("member.txt", error, line);
                continue;
            }
            f[4] = f[4].toUpperCase();
            f[6] = f[6].substring(0, 1).toUpperCase() + f[6].substring(1).toLowerCase();
            fresh.add(new Member(f));
        }
        members.clear();
        members.addAll(fresh);
        markSeen("member.txt");
    }

    private void reloadProviders() {
        List<Provider> fresh = new ArrayList<>();
        Set<String> numbers = new HashSet<>();
        for (String line : readLines(dataDir, "provider.txt")) {
            String[] f = fields(line);
            String error = Party.validate(f);
            if (error == null && (f.length < 7 || !f[6].matches("[^|]{1,25}"))) {
                error = "Provider type must be 1-25 characters";
            }
            if (error == null && !numbers.add(f[1])) {
                error = "Duplicate provider number";
            }
            if (error != null) {
                warn("provider.txt", error, line);
                continue;
            }
            f[4] = f[4].toUpperCase();
            fresh.add(new Provider(f));
        }
        providers.clear();
        providers.addAll(fresh);
        markSeen("provider.txt");
    }

    private void reloadStaff() {
        staffIds.clear();
        for (String id : readLines(dataDir, "manager.txt")) {
            if (id.matches("\\d{9}")) {
                staffIds.add(id + ":MANAGER");
            } else {
                warn("manager.txt", "Number must be exactly 9 digits", id);
            }
        }
        for (String id : readLines(dataDir, "operator.txt")) {
            if (id.matches("\\d{9}")) {
                staffIds.add(id + ":OPERATOR");
            } else {
                warn("operator.txt", "Number must be exactly 9 digits", id);
            }
        }
        markSeen("manager.txt");
        markSeen("operator.txt");
    }

    private void reloadRecords() {
        Map<String, Service> services = loadServices(dataDir);
        List<ServiceRecord> fresh = new ArrayList<>();
        for (String line : readLines(dataDir, "records.txt")) {
            ServiceRecord r = ServiceRecord.fromLine(line, services);
            if (r == null) {
                warn("records.txt", "Line is not in the record format", line);
            } else {
                fresh.add(r);
            }
        }
        records.clear();
        records.addAll(fresh);
        markSeen("records.txt");
        markSeen("services.txt");
    }

    private static String[] fields(String line) {
        String[] f = line.split("\\|", -1);
        for (int i = 0; i < f.length; i++) {
            f[i] = f[i].trim();
        }
        return f;
    }

    private static void warn(String file, String reason, String line) {
        System.out.println("WARNING: skipped a line in " + file + " (" + reason + "): " + line);
    }

    // ---------- writing ----------

    /**
     * Opens the data file, finds the line whose second field is this number, and
     * replaces it with newLine (or removes it when newLine is null). If there is no
     * such line, newLine is added at the end. Every other line is left as it is.
     * The file is then read back to check the change is really there.
     * @return true if the file now has the change; false (see getLastWriteError) otherwise
     */
    private boolean writeLine(String file, String number, String newLine) {
        lastWriteError = "";
        if (dataDir == null) {
            return true;
        }
        Path path = Paths.get(dataDir, file);
        try {
            List<String> old = Files.exists(path)
                    ? Files.readAllLines(path, StandardCharsets.UTF_8) : new ArrayList<String>();
            List<String> out = new ArrayList<>();
            boolean found = false;
            for (String line : old) {
                String[] f = line.split("\\|", -1);
                if (f.length > 1 && f[1].trim().equals(number)) {
                    if (newLine != null && !found) {
                        out.add(newLine);
                    }
                    found = true;
                } else {
                    out.add(line);
                }
            }
            if (!found && newLine != null) {
                out.add(newLine);
            }
            Files.write(path, out, StandardCharsets.UTF_8);

            // read it back: the change must really be in the file
            boolean present = false;
            boolean exact = false;
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String[] f = line.split("\\|", -1);
                if (f.length > 1 && f[1].trim().equals(number)) {
                    present = true;
                    exact = line.trim().equals(newLine);
                }
            }
            if ((newLine == null && present) || (newLine != null && !exact)) {
                lastWriteError = path + " does not show the change after writing";
                return false;
            }
            markSeen(file);
            return true;
        } catch (IOException e) {
            lastWriteError = path + " (" + e.getClass().getSimpleName() + ")";
            return false;
        }
    }

    /** Reads services.txt (code|name|fee) into a map in file order. */
    static Map<String, Service> loadServices(String dir) {
        Map<String, Service> map = new LinkedHashMap<>();
        for (String line : readLines(dir, "services.txt")) {
            String[] f = line.split("\\|");
            if (f.length == 3 && f[0].trim().matches("\\d{6}") && f[1].trim().length() <= 20) {
                try {
                    map.put(f[0].trim(), new Service(f[0].trim(), f[1].trim(), Double.parseDouble(f[2].trim())));
                } catch (NumberFormatException e) {
                    // skip a bad line
                }
            }
        }
        return map;
    }

    /** Non-empty lines of a data file, skipping lines that start with '#'. */
    static List<String> readLines(String dir, String file) {
        List<String> out = new ArrayList<>();
        Path p = Paths.get(dir, file);
        try {
            for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                line = line.trim();
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1).trim();     // byte-order mark added by some editors
                }
                if (!line.isEmpty() && !line.startsWith("#")) {
                    out.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("WARNING: could not read " + p + " (" + e.getMessage() + ")");
        }
        return out;
    }
}
