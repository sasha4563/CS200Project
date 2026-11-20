/**
 * All rules about members: validate a member number, add, update, delete,
 * and change membership status. When an operation returns false,
 * getLastError() says why.
 *
 * @author Sameeksha
 */
public class MemberController {

    private ChocAnDatabase db;
    private String lastError = "";

    public MemberController(ChocAnDatabase db) {
        this.db = db;
    }

    /** @return "Validated", "Invalid number", or "Member suspended" */
    public String validateMember(String memberNum) {
        if (memberNum == null || !memberNum.matches("\\d{9}")) {
            return "Invalid number";
        }
        Member m = db.getMember(memberNum);
        if (m == null) {
            return "Invalid number";
        }
        if ("Suspended".equalsIgnoreCase(m.getStatus())) {
            return "Member suspended";
        }
        return "Validated";
    }

    public boolean memberExists(String memberNum) {
        return db.getMember(memberNum) != null;
    }

    /** Full path of member.txt (shown to the operator after a change). */
    public String getDataFile() {
        return db.getFilePath("member.txt");
    }

    /** What member.txt holds for this number right now (read back from the file). */
    public String getSavedLine(String memberNum) {
        return db.readBack("member.txt", memberNum);
    }

    /** All lines of member.txt, numbered. */
    public java.util.List<String> getFileContents() {
        return db.readFileNumbered("member.txt");
    }

    /** The member with this number, or null. */
    public Member getMember(String memberNum) {
        return db.getMember(memberNum);
    }

    /** The reason the last add / update / delete / setStatus returned false. */
    public String getLastError() {
        return lastError;
    }

    /** info[] = name, number, address, city, state, zip, status. */
    public boolean addMember(String[] info) {
        String error = Person.validate(info);
        if (error != null) {
            return fail(error);
        }
        if (info.length < 7 || !validStatus(info[6])) {
            return fail("Status must be Active or Suspended");
        }
        if (memberExists(info[1])) {
            return fail("A member with number " + info[1] + " already exists");
        }
        if (!db.addMember(new Member(clean(info)))) {
            return fail("Could not write member.txt: " + db.getLastWriteError());
        }
        return succeed();
    }

    /** info[] has 6 fields (status unchanged) or 7 (status changed). The member number never changes. */
    public boolean updateMember(String memberNum, String[] info) {
        Member m = db.getMember(memberNum);
        if (m == null) {
            return fail("Member " + memberNum + " does not exist");
        }
        if (info == null || info.length < 6) {
            return fail("Some information is missing");
        }
        String[] copy = info.clone();
        copy[1] = memberNum;
        String error = Person.validate(copy);
        if (error != null) {
            return fail(error);
        }
        if (copy.length > 6 && !validStatus(copy[6])) {
            return fail("Status must be Active or Suspended");
        }
        String[] before = { m.getName(), m.getNumber(), m.getAddress(), m.getCity(), m.getState(), m.getZip(), m.getStatus() };
        m.update(clean(copy));
        if (!db.saveMember(m)) {
            m.update(before);                       // put it back: the file was not changed
            return fail("Could not write member.txt: " + db.getLastWriteError());
        }
        return succeed();
    }

    public boolean deleteMember(String memberNum) {
        if (!memberExists(memberNum)) {
            return fail("Member " + memberNum + " does not exist");
        }
        if (!db.deleteMember(memberNum)) {
            return fail("Could not write member.txt: " + db.getLastWriteError());
        }
        return succeed();
    }

    /** status is "Active" or "Suspended" (any case). Used by the Acme interface. */
    public boolean setStatus(String memberNum, String status) {
        Member m = db.getMember(memberNum);
        if (m == null) {
            return fail("Member " + memberNum + " does not exist");
        }
        if (!validStatus(status)) {
            return fail("Status must be Active or Suspended");
        }
        String before = m.getStatus();
        m.setStatus(capitalize(status));
        if (!db.saveMember(m)) {
            m.setStatus(before);
            return fail("Could not write member.txt: " + db.getLastWriteError());
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

    private static boolean validStatus(String s) {
        return s != null && s.matches("(?i)active|suspended");
    }

    private static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    /** Copy of info[] with the state upper-cased and the status capitalized. */
    private static String[] clean(String[] info) {
        String[] c = info.clone();
        c[4] = c[4].toUpperCase();
        if (c.length > 6) {
            c[6] = capitalize(c[6]);
        }
        return c;
    }
}
