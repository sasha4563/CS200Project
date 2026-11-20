/**
 * Abstract base class for members and providers: name, nine-digit number,
 * and mailing address.
 *
 * info[] layout (shared by every class that takes a String[]):
 *   0 name | 1 number | 2 address | 3 city | 4 state | 5 zip | 6 status (member) or type (provider)
 *
 * @author Florencia
 */
public abstract class Person {

    protected String name, number, address, city, state, zip;

    protected Person(String[] info) {
        copyFields(info);
    }

    public String getName()    { return name; }
    public String getNumber()  { return number; }
    public String getAddress() { return address; }
    public String getCity()    { return city; }
    public String getState()   { return state; }
    public String getZip()     { return zip; }

    /** Overwrites the common fields from info[]. */
    public void update(String[] info) {
        copyFields(info);
    }

    private void copyFields(String[] info) {
        name = info[0];
        number = info[1];
        address = info[2];
        city = info[3];
        state = info[4];
        zip = info[5];
    }

    /**
     * Checks the first six fields against the ChocAn field limits.
     * '|' is not allowed because it is the separator in the data files.
     * @return null if everything is valid, otherwise a message saying which field is wrong
     */
    public static String validate(String[] info) {
        if (info == null || info.length < 6) {
            return "Some information is missing";
        }
        for (String f : info) {
            if (f == null) {
                return "Some information is missing";
            }
        }
        if (!info[0].matches("[^|]{1,25}"))  return "Name must be 1-25 characters (no '|')";
        if (!info[1].matches("\\d{9}"))       return "Number must be exactly 9 digits";
        if (!info[2].matches("[^|]{1,25}"))  return "Street address must be 1-25 characters (no '|')";
        if (!info[3].matches("[^|]{1,14}"))  return "City must be 1-14 characters (no '|')";
        if (!info[4].matches("[A-Za-z]{2}")) return "State must be exactly 2 letters";
        if (!info[5].matches("\\d{5}"))       return "ZIP code must be exactly 5 digits";
        return null;
    }

    public static boolean isValidInfo(String[] info) {
        return validate(info) == null;
    }

    /** The six common fields joined with '|' (used by the data files). */
    protected String baseLine() {
        return String.join("|", name, number, address, city, state, zip);
    }

    /** One line for the data file: the common fields plus status / type. */
    public abstract String toLine();
}
