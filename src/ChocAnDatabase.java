/**
 * Stores and returns members, providers, service records, and staff IDs.
 * Contains no business rules.
 *
 * @author
 */
import java.time.LocalDate;
import java.util.List;

public class ChocAnDatabase {


    private List<Member> members;
    private List<Provider> providers;
    private List<ServiceRecord> records;
    private List<String> staffIds;  // stored as "Role:id"

    public ChocAnDatabase(List<Member> members, List<Provider> providers, List<ServiceRecord> records, List<String> staffIds) {
        this.members = members;
        this.providers = providers;
        this.records = records;
        this.staffIds = staffIds;
    }
    

    public boolean isValidProvider(String id) {
        return providers.contains(id);
    }
    public boolean isValidStaff(String id, String role) {
        String key = id + ":" + role;
        return staffIds.contains(key);

    }
    public Member getMember(String memberNum) { //returns null if not found
        for (member m : members) {
            if (m.getNumber() == memberNum) {
                return m;
            }
        }
        return null;
    }
    public Provider getProvider(String providerNum) {
        for (provider p : providers) {
            if (p.getNumber() == providerNum) {
                return p;
            }
        }
        return null;
    }

    public void addMember(Member m) {
        members.add(m);
    }
    public void deleteMember(String memberNum) {
        for (member m : members) {
            if (m.getNumber() == memberNum) {
                members.remove(m);
            }
        }
    }
    public void addProvider(Provider p) {
        providers.add(p);

    }
    public void deleteProvider(String providerNum) {
        for (provider p : providers) {
            if (p.getNumber == providerNum) {
                providers.remove(p);
            }
        }
    }
    public void saveRecord(ServiceRecord r) {
        records.add(r);
    }
    public List<ServiceRecord> getWeekRecords() { //returns records saved during the last week
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.minusDays(7);
        List<ServiceRecord> weekRecords;
        for (record r : records) {
            if (!r.daterecieved.isBefore(cutoff) && !r.daterecieved.isAfter(today)) {
                weekRecords.add(r);
            }
        }
        return weekRecords;
    }
    public List<Member> getMembersServed() {
        return members;
    }
    public List<Provider> getProvidersPaid() {
        return providers;
    }

}
