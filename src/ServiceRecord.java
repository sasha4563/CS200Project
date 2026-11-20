/**
 * One billed service: date/time received, date of service, provider number,
 * member number, service code, comments, and fee.
 *
 * @author
 */
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServiceRecord {

    private LocalDateTime dateReceived;
    private LocalDate serviceDate;

    private String providerNum;
    private String memberNum;
    private String serviceCode;
    private String comments;

    private double fee;
    private Service service;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    public ServiceRecord(String memberNum, String providerNum, String date, Service s, String comments) {

        if (memberNum == null || !memberNum.matches("\\d{9}")) {
            throw new IllegalArgumentException("Member number must be 9 digits.");
        }

        if (providerNum == null || !providerNum.matches("\\d{9}")) {
            throw new IllegalArgumentException("Provider number must be 9 digits.");
        }

        if (s == null) {
            throw new IllegalArgumentException("Service cannot be null.");
        }

        if (comments != null && comments.length() > 100) {
            throw new IllegalArgumentException("Comments cannot exceed 100 characters.");
        }

        this.memberNum = memberNum;
        this.providerNum = providerNum;

        // converts string into LocalDate
        this.serviceDate = LocalDate.parse(date, DATE_FORMAT);

        // records date and time received
        this.dateReceived = LocalDateTime.now();

        this.service = s;
        this.serviceCode = s.getCode();
        this.fee = s.getFee();
        this.comments = comments;
    }

    public LocalDateTime getDateReceived() {
        return dateReceived;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public String getProviderNum() {
        return providerNum;
    }

    public String getMemberNum() {
        return memberNum;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public String getComments() {
        return comments;
    }

    public Service getService() {
        return service;
    }

    public double getFee() {
        return fee;
    }
}