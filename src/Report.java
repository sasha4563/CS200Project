/**
 * Abstract base class for all reports. Each subclass builds its own content;
 * saving to a file is shared.
 *
 * @author
 */
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public abstract class Report {

    protected String reportDate;
    protected String fileName;

    protected static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MM-dd-yyyy");

    protected static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");

    public Report() {
        reportDate = LocalDate.now().format(DATE_FORMAT);
    }

    public abstract void generate(List<ServiceRecord> records);

    protected String cleanFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    protected boolean writeFile(String content) {

        try (FileWriter writer = new FileWriter(fileName)) {

            writer.write(content);
            return true;

        } catch (IOException e) {

            System.out.println("Error saving report: " + e.getMessage());
            return false;
        }
    }

    public boolean saveToFile() {
        return writeFile("Report Date: " + reportDate + "\n");
    }
}