import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Weekly report for one member: member info plus each service received
 * (date, provider name, service name), sorted by service date.
 *
 * Interface change: the constructor also takes the database so provider
 * names can be looked up.
 *
 * @author Sameeksha
 */
public class MemberReport extends Report {

    private Member member;
    private ChocAnDatabase db;

    public MemberReport(Member m, ChocAnDatabase db) {
        this.member = m;
        this.db = db;
        this.folder = "members";
        this.fileName = safeName(m.getName()) + "_" + reportDate + ".txt";
    }

    @Override
    public void generate(List<ServiceRecord> records) {
        lines.clear();
        lines.add("MEMBER REPORT - week ending " + reportDate);
        lines.add("");
        lines.add("Member name:    " + member.getName());
        lines.add("Member number:  " + member.getNumber());
        lines.add("Street address: " + member.getAddress());
        lines.add("City:           " + member.getCity());
        lines.add("State:          " + member.getState());
        lines.add("ZIP code:       " + member.getZip());
        lines.add("");
        lines.add("Services received:");

        List<ServiceRecord> mine = new ArrayList<>();
        for (ServiceRecord r : records) {
            if (r.getMemberNum().equals(member.getNumber())) {
                mine.add(r);
            }
        }
        mine.sort(Comparator.comparing(ServiceRecord::getServiceLocalDate));   // stable sort

        if (mine.isEmpty()) {
            lines.add("  (none)");
            return;
        }
        lines.add("  " + pad("Date of Service", 16) + pad("Provider Name", 27) + "Service Name");
        for (ServiceRecord r : mine) {
            lines.add("  " + pad(r.getServiceDate(), 16)
                    + pad(providerName(db, r.getProviderNum()), 27)
                    + r.getService().getName());
        }
    }
}
