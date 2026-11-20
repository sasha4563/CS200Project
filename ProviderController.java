/**
 * All rules about providers: add, update, and delete provider records.
 * When an operation returns false, getLastError() says why.
 *
 * @author Sameeksha
 */
public class ProviderController {

    private ChocAnDatabase db;
    private String lastError = "";

    public ProviderController(ChocAnDatabase db) {
        this.db = db;
    }

    public boolean providerExists(String providerNum) {
        return db.getProvider(providerNum) != null;
    }

    /** Full path of provider.txt (shown to the operator after a change). */
    public String getDataFile() {
        return db.getFilePath("provider.txt");
    }

    /** What provider.txt holds for this number right now (read back from the file). */
    public String getSavedLine(String providerNum) {
        return db.readBack("provider.txt", providerNum);
    }

    /** All lines of provider.txt, numbered. */
    public java.util.List<String> getFileContents() {
        return db.readFileNumbered("provider.txt");
    }

    /** The provider with this number, or null. */
    public Provider getProvider(String providerNum) {
        return db.getProvider(providerNum);
    }

    /** The reason the last add / update / delete returned false. */
    public String getLastError() {
        return lastError;
    }

    /** info[] = name, number, address, city, state, zip, type. */
    public boolean addProvider(String[] info) {
        String error = Person.validate(info);
        if (error != null) {
            return fail(error);
        }
        if (info.length < 7 || !validType(info[6])) {
            return fail("Provider type must be 1-25 characters (no '|')");
        }
        if (providerExists(info[1])) {
            return fail("A provider with number " + info[1] + " already exists");
        }
        if (!db.addProvider(new Provider(clean(info)))) {
            return fail("Could not write provider.txt: " + db.getLastWriteError());
        }
        return succeed();
    }

    /** info[] has 6 fields (type unchanged) or 7 (type changed). The provider number never changes. */
    public boolean updateProvider(String providerNum, String[] info) {
        Provider p = db.getProvider(providerNum);
        if (p == null) {
            return fail("Provider " + providerNum + " does not exist");
        }
        if (info == null || info.length < 6) {
            return fail("Some information is missing");
        }
        String[] copy = info.clone();
        copy[1] = providerNum;
        String error = Person.validate(copy);
        if (error != null) {
            return fail(error);
        }
        if (copy.length > 6 && !validType(copy[6])) {
            return fail("Provider type must be 1-25 characters (no '|')");
        }
        String[] before = { p.getName(), p.getNumber(), p.getAddress(), p.getCity(), p.getState(), p.getZip(), p.getType() };
        p.update(clean(copy));
        if (!db.saveProvider(p)) {
            p.update(before);                       // put it back: the file was not changed
            return fail("Could not write provider.txt: " + db.getLastWriteError());
        }
        return succeed();
    }

    public boolean deleteProvider(String providerNum) {
        if (!providerExists(providerNum)) {
            return fail("Provider " + providerNum + " does not exist");
        }
        if (!db.deleteProvider(providerNum)) {
            return fail("Could not write provider.txt: " + db.getLastWriteError());
        }
        return succeed();
    }

    // ---------- helpers ----------

    private boolean fail(String message) {
        lastError = message;
        return false;
    }

    private boolean succeed() {
        lastError = "";
        return true;
    }

    private static boolean validType(String t) {
        return t != null && t.matches("[^|]{1,25}");
    }

    /** Copy of info[] with the state upper-cased. */
    private static String[] clean(String[] info) {
        String[] c = info.clone();
        c[4] = c[4].toUpperCase();
        return c;
    }
}
