/**
 * Stores and returns members, providers, service records, and staff IDs.
 * Contains no business rules.
 *
 * @author
 */
import java.time.LocalDate;
import java.nio.file.*;
import java.security.Provider.Service;
import java.io.*;
import java.lang.reflect.Member;
import java.util.*;

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
        this.loadDatabase("src/providers.txt", "Provider");
        this.loadDatabase("src/members.txt", "Member");
        this.loadDatabase("src/records.txt", "ServiceRecord");
        this.loadDatabase("src/staffIds.txt", "String");

    }

    private void loadDatabase(String filename, String type) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            if (type.equals("Provider")) {
                while ((line = br.readLine()) != null) {
                    String[] attributes = line.split("\\|");

                    Provider p = new Provider(attributes[0], Integer.parseInt(attributes[1]), attributes[2]); //!!Need to modify to reflect constructor
                    providers.add(p);
                }
            }
            else if (type.equals("Member")) {
                while ((line = br.readLine()) != null) {
                    String[] attributes = line.split("\\|");

                    Member m = new Member(attributes[0], Integer.parseInt(attributes[1]), attributes[2]); //!!Need to modify to reflect constructor
                    members.add(m);
                }
            }
            else if (type.equals("ServiceRecord")) {
                while ((line = br.readLine()) != null) {
                    String[] attributes = line.split("\\|");
                    // constructor ServiceRecord(String memberNum, String providerNum, String date, Service s, String comments)
                    //constructor Service(String code, String name, double fee)
                    //servicerecord stored in txt file as information for service first, then memberNum, providerNum, date, comments, daterecieved)
                    Service s = new Service(attributes[0], attributes[1], Double.parseDouble(attributes[2]));
                    ServiceRecord sr = new ServiceRecord(attributes[3], attributes[4], attributes[5], s, attributes[6], attributes[7]);
                    records.add(sr);
                }
            }
            else if (type.equals("String")) {
                while ((line = br.readLine()) != null) {
                    String[] attributes = line.split("\\|");

                    String si = attributes[0];
                    staffIds.add(si);
                }
            }
        }
        catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }

    }

    private void writeDatabase(String filename) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            if (filename.equals("src/providers.txt")) {
                for (Provider p : providers) {
                    //writer.write(attributes)
                    // write attributes with "|" between them for example writer.write(o.a + "|" + o.b +"|" + o.c);
                    writer.newLine();
                }
            }
            else if (filename.equals("src/members.txt")) {
                for (Member m : members) {
                    //writer.write(attributes)
                    // write attributes with "|" between them for example writer.write(o.a + "|" + o.b +"|" + o.c);
                    writer.newLine();
                }
            }
            else if (filename.equals("src/records.txt")) {
                for (ServiceRecord r : records) {
                    //constructor Service(String code, String name, double fee)
                    // then memberNum, providerNum, date, comments, daterecieved 
                    ServiceRecord sr = new ServiceRecord(attributes[3], attributes[4], attributes[5], s, attributes[6]);
                    Service s = r.getService();
                    writer.write(s.getCode() + "|" + s.getName() + "|" + Double.toString(s.getFee()) + "|");
                    writer.write(sr.getMemberNum() + "|" + sr.getProviderNum() + "|" + st.getServiceDate().toString() + "|" + sr.getComments() + "|" + sr.getDateReceived().toString());
                    writer.newLine();
                }
            }
            else if (filename.equals("src/staffIds.txt")) {
                for (String si : staffIds) {
                    writer.write(si);
                    writer.newLine();
                }
            }
            else {
                System.out.println("Invalid file name.");
            }

            writer.close();
        }
        catch (IOException e) {
            System.out.println("Error");
        }
    }

    public void saveDatabase() {
        this.writeDatabase("src/providers.txt");
        this.writeDatabase("src/members.txt");
        this.writeDatabase("src/records.txt");
        this.writeDatabase("src/staffIds.txt");
    }
    

    public boolean isValidProvider(String id) {
        for (Provider p : providers) {
            if (p.getNumber() == Integer.parseInt(id)) {
                return true;
            }
        }
        return false;
    }
    public boolean isValidStaff(String id, String role) {
        String key = role + ":" + id;
        return staffIds.contains(key);

    }
    public Member getMember(String memberNum) { //returns null if not found
        for (Member m : members) {
            if (m.getNumber().equals(memberNum)) {
                return m;
            }
        }
        return null;
    }
    public Provider getProvider(String providerNum) {
        for (Provider p : providers) {
            if (p.getNumber().equals(providerNum)) {
                return p;
            }
        }
        return null;
    }

    public void addMember(Member m) {
        members.add(m);
    }
    public void deleteMember(String memberNum) {
        Iterator<Member> iterator = members.iterator();
        while (iterator.hasNext()) {
            Member m = iterator.next();

            if (m.getNumber().equals(memberNum)) {
                iterator.remove();
                return;
            }
        }
    }
    public void addProvider(Provider p) {
        providers.add(p);

    }
    public void deleteProvider(String providerNum) {
        Iterator<Provider> iterator = providers.iterator();
        while (iterator.hasNext()) {
            Provider p = iterator.next();

            if (p.getNumber().equals(providerNum)) {
                iterator.remove();
                return;
            }
        }
    }
    public void saveRecord(ServiceRecord r) {
        records.add(r);
    }
    public List<ServiceRecord> getWeekRecords() { //returns records saved during the last week
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.minusDays(7);
        List<ServiceRecord> weekRecords = new ArrayList();
        for (ServiceRecord r : records) {
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
