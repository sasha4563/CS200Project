/**
 * Stores and returns members, providers, service records, and staff IDs.
 * Contains no business rules.
 *
 * @author
 */
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
    

    public boolean isValidProvider(String id);
    public boolean isValidStaff(String id, String role);
    public Member getMember(String memberNum);    // null if not found
    public Provider getProvider(String providerNum);
    public void addMember(Member m);
    public void deleteMember(String memberNum);
    public void addProvider(Provider p);
    public void deleteProvider(String providerNum);
    public void saveRecord(ServiceRecord r);
    public List<ServiceRecord> getWeekRecords();
    public List<Member> getMembersServed();
    public List<Provider> getProvidersPaid();

}
