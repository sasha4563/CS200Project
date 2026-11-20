/**
 * One Provider Directory entry: six-digit service code, service name
 * (max 20 characters), and fee.
 *
 * @author Namita
 */
public class Service {

    private String code;    // 6 digits
    private String name;    // max 20 chars
    private double fee;

    public Service(String code, String name, double fee) {
        this.code = code;
        this.name = name;
        this.fee = fee;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public double getFee()  { return fee; }
}
