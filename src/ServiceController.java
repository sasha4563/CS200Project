import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Billing and the Provider Directory: looks up services, creates service
 * records, and produces the directory file.
 *
 * @author Sameeksha
 */
public class ServiceController {

    private ChocAnDatabase db;
    private ProviderDirectory directory;

    public ServiceController(ChocAnDatabase db, ProviderDirectory directory) {
        this.db = db;
        this.directory = directory;
    }

    /** @return the service with that code, or null if the code does not exist */
    public Service getService(String code) {
        return directory.findService(code);
    }

    /**
     * Creates the service record, saves it, and returns the fee.
     * @param date MM-DD-YYYY (a real date, not in the future)
     * @throws IllegalArgumentException if anything about the bill is invalid
     */
    public double billService(String memberNum, String providerNum,
                              String date, String code, String comments) {
        Member member = db.getMember(memberNum);
        if (member == null) {
            throw new IllegalArgumentException("unknown member number");
        }
        if ("Suspended".equalsIgnoreCase(member.getStatus())) {
            throw new IllegalArgumentException("member is suspended");
        }
        if (db.getProvider(providerNum) == null) {
            throw new IllegalArgumentException("unknown provider number");
        }
        Service service = directory.findService(code);
        if (service == null) {
            throw new IllegalArgumentException("unknown service code");
        }
        try {
            if (LocalDate.parse(date, ServiceRecord.DATE).isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("date of service is in the future");
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("date must be MM-DD-YYYY");
        }
        if (comments == null) {
            comments = "";
        }
        if (comments.length() > 100) {
            throw new IllegalArgumentException("comments are longer than 100 characters");
        }

        ServiceRecord record = new ServiceRecord(memberNum, providerNum, date, service, comments, null);
        if (!db.saveRecord(record)) {
            throw new IllegalArgumentException("could not write records.txt (" + db.getLastWriteError() + ")");
        }
        return record.getFee();
    }

    /** Sorts the directory by service name and saves it to a file. */
    public boolean getDirectory() {
        directory.sortByName();
        return directory.saveToFile();
    }
}
