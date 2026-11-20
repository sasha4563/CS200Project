/**
 * One Provider Directory entry: six-digit service code, service name
 * (max 20 characters), and fee.
 *
 * @author
 */
public class Service {

    private String code;       // 6 digits
    private String name;       // max 20 characters
    private double fee;

    public Service(String code, String name, double fee) {

        if (code == null || !code.matches("\\d{6}")) {
            throw new IllegalArgumentException("Service code must be exactly 6 digits.");
        }

        if (name == null || name.length() > 20) {
            throw new IllegalArgumentException("Service name must be 20 characters or less.");
        }

        if (fee < 0) {
            throw new IllegalArgumentException("Fee cannot be negative.");
        }

        this.code = code;
        this.name = name;
        this.fee = fee;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public double getFee() {
        return fee;
    }
}