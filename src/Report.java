import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all reports. Each subclass builds its own content;
 * saving to a file is shared.
 *
 * @author Namita
 */
public abstract class Report {

    /** Parent folder for the manager reports (each kind has its own sub-folder). */
    public static final String OUTPUT_DIR = "reports";

    protected String reportDate;                       // MM-DD-YYYY
    protected String fileName;
    protected String folder = "";                    // sub-folder of OUTPUT_DIR, set by each report
    protected List<String> lines = new ArrayList<>();  // the report text, one entry per line

    protected Report() {
        this.reportDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
    }

    /** Builds the report text from the week's service records. */
    public abstract void generate(List<ServiceRecord> records);

    /** Writes the report to reports/<folder>/<fileName>. */
    public boolean saveToFile() {
        try {
            Files.createDirectories(Paths.get(AppFolders.reportsDir(), folder));
            Files.write(Paths.get(AppFolders.reportsDir(), folder, fileName), lines, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String getFileName()    { return fileName; }

    /** Where the file is saved, e.g. reports/members/Alice_Brown_11-20-2026.txt */
    public String getPath()        { return OUTPUT_DIR + "/" + folder + "/" + fileName; }
    public List<String> getLines() { return lines; }

    // ---------- helpers for the subclasses ----------

    /** "Alice Brown" -> "Alice_Brown" (safe to use in a file name). */
    protected static String safeName(String s) {
        return s.trim().replaceAll("[^A-Za-z0-9]+", "_");
    }

    /** Left-justifies s in a column that is n characters wide. */
    protected static String pad(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    protected static String money(double amount) {
        return String.format(java.util.Locale.US, "$%.2f", amount);
    }

    /** Provider name for a provider number, or a placeholder if the provider was deleted. */
    protected static String providerName(ChocAnDatabase db, String providerNum) {
        Provider p = db.getProvider(providerNum);
        return (p == null) ? "(unknown provider)" : p.getName();
    }

    /** Member name for a member number, or a placeholder if the member was deleted. */
    protected static String memberName(ChocAnDatabase db, String memberNum) {
        Member m = db.getMember(memberNum);
        return (m == null) ? "(unknown member)" : m.getName();
    }
}
