import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * One billed service: date/time received, date of service, provider number,
 * member number, service code, comments, and fee.
 *
 * @author Namita
 */
public class ServiceRecord {

    /** MM-DD-YYYY */
    public static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("MM-dd-uuuu").withResolverStyle(ResolverStyle.STRICT);
    /** MM-DD-YYYY HH:MM:SS */
    public static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("MM-dd-uuuu HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

    private String dateReceived, serviceDate, providerNum, memberNum, serviceCode, comments;
    private double fee;
    private Service service;

    /** dateReceived may be null/empty, meaning "now". The fee is copied from the service. */
    public ServiceRecord(String memberNum, String providerNum, String date,
                         Service s, String comments, String dateReceived) {
        this.memberNum = memberNum;
        this.providerNum = providerNum;
        this.serviceDate = date;
        this.service = s;
        this.serviceCode = s.getCode();
        this.fee = s.getFee();
        this.comments = (comments == null) ? "" : comments;
        this.dateReceived = (dateReceived == null || dateReceived.isEmpty())
                ? LocalDateTime.now().format(DATE_TIME) : dateReceived;
    }

    public String getDateReceived() { return dateReceived; }
    public String getServiceDate()  { return serviceDate; }
    public String getProviderNum()  { return providerNum; }
    public String getMemberNum()    { return memberNum; }
    public String getServiceCode()  { return serviceCode; }
    public String getComments()     { return comments; }
    public double getFee()          { return fee; }
    public Service getService()     { return service; }

    /** Date of service as a LocalDate (LocalDate.MIN if the text is bad). */
    public LocalDate getServiceLocalDate() {
        try {
            return LocalDate.parse(serviceDate, DATE);
        } catch (DateTimeParseException e) {
            return LocalDate.MIN;
        }
    }

    /** Date/time received as a LocalDateTime (LocalDateTime.MIN if the text is bad). */
    public LocalDateTime getReceivedDateTime() {
        try {
            return LocalDateTime.parse(dateReceived, DATE_TIME);
        } catch (DateTimeParseException e) {
            return LocalDateTime.MIN;
        }
    }

    /**
     * One line for records.txt, exactly the fields in the project description:
     * current date/time | date of service | provider number | member number | service code | comments
     * (the service name and fee are looked up from the Provider Directory).
     */
    public String toLine() {
        String safeComments = comments.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
        return String.join("|", dateReceived, serviceDate, providerNum, memberNum, serviceCode, safeComments);
    }

    /**
     * Rebuilds a record from a records.txt line, looking the service up by code.
     * Returns null if the line is malformed.
     */
    public static ServiceRecord fromLine(String line, java.util.Map<String, Service> services) {
        String[] f = line.split("\\|", 6);
        if (f.length < 5) {
            return null;
        }
        Service s = services.get(f[4]);
        if (s == null) {
            s = new Service(f[4], "(unknown service)", 0.0);
        }
        return new ServiceRecord(f[3], f[2], f[1], s, f.length > 5 ? f[5] : "", f[0]);
    }
}
