/**
 * A ChocAn member, with a membership status of Active or Suspended.
 *
 * @author Florencia
 */
public class Member extends Person {

    private String status;   // "Active" / "Suspended"

    public Member(String[] info) {
        super(info);
        this.status = (info.length > 6 && info[6] != null) ? info[6] : "Active";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** Updates the common fields; the status changes only if info[6] is given. */
    @Override
    public void update(String[] info) {
        super.update(info);
        if (info.length > 6 && info[6] != null) {
            status = info[6];
        }
    }

    @Override
    public String toLine() {
        return baseLine() + "|" + status;
    }
}
