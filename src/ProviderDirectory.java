import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The list of services ChocAn offers, sorted alphabetically by service name
 * and saved to a file on request.
 *
 * Services are read from services.txt (code|name|fee) in the data folder.
 *
 * @author Sameeksha
 */
public class ProviderDirectory {

    /** The directory file is saved in this folder (not in the reports folder). */
    public static final String OUTPUT_DIR = "ProviderDirectory";

    private List<Service> services = new ArrayList<>();
    private String dataDir;
    private long loadedAt = -1;                 // services.txt last-modified time that was loaded

    public ProviderDirectory() {
        this(AppFolders.dataDir());
    }

    public ProviderDirectory(String dataDir) {
        this.dataDir = dataDir;
        SampleData.ensureFiles(dataDir);
        reload();
    }

    /** Re-reads services.txt if it was changed while the program is running. */
    private void reloadIfChanged() {
        try {
            if (Files.getLastModifiedTime(Paths.get(dataDir, "services.txt")).toMillis() != loadedAt) {
                reload();
            }
        } catch (IOException e) {
            // keep the list we have
        }
    }

    private void reload() {
        services.clear();
        services.addAll(ChocAnDatabase.loadServices(dataDir).values());
        try {
            loadedAt = Files.getLastModifiedTime(Paths.get(dataDir, "services.txt")).toMillis();
        } catch (IOException e) {
            loadedAt = -1;
        }
    }

    /** @return the service with that six-digit code, or null if there is none */
    public Service findService(String code) {
        reloadIfChanged();
        for (Service s : services) {
            if (s.getCode().equals(code)) {
                return s;
            }
        }
        return null;
    }

    public void sortByName() {
        services.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
    }

    public List<Service> getServices() {
        reloadIfChanged();
        return services;
    }

    /** Writes ProviderDirectory/ProviderDirectory_MM-DD-YYYY.txt (service name, code, fee). */
    public boolean saveToFile() {
        reloadIfChanged();
        List<String> lines = new ArrayList<>();
        lines.add("PROVIDER DIRECTORY");
        lines.add("");
        lines.add(String.format("%-20s  %-6s  %s", "Service Name", "Code", "Fee"));
        for (Service s : services) {
            lines.add(String.format(Locale.US, "%-20s  %-6s  $%.2f", s.getName(), s.getCode(), s.getFee()));
        }
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
        try {
            Files.createDirectories(Paths.get(AppFolders.directoryDir()));
            Files.write(Paths.get(AppFolders.directoryDir(), "ProviderDirectory_" + date + ".txt"),
                    lines, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
