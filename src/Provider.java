/**
 * A ChocAn provider: a hospital or clinic that offers dietitian, internist, or
 * exercise-expert services. The name is the hospital / clinic name.
 *
 * @author Florencia
 */
public class Provider extends Party {

    private String type;   // dietitian, internist, exercise expert

    public Provider(String[] info) {
        super(info);
        this.type = (info.length > 6 && info[6] != null) ? info[6] : "";
    }

    public String getType() {
        return type;
    }

    /** Updates the common fields; the type changes only if info[6] is given. */
    @Override
    public void update(String[] info) {
        super.update(info);
        if (info.length > 6 && info[6] != null) {
            type = info[6];
        }
    }

    @Override
    public String toLine() {
        return baseLine() + "|" + type;
    }
}
