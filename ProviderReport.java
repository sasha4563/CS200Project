import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Weekly report for one provider: provider info, each service provided
 * (in the order the data were received), total consultations, and total fee.
 *
 * Interface change: the constructor also takes the database so member
 * names can be looked up.
 *
 * @author Sameeksha
 */
public class ProviderReport extends Report {

    private Provider provider;
    private ChocAnDatabase db;
    private double totalFee;

    public ProviderReport(Provider p, ChocAnDatabase db) {
        this.provider = p;
        this.db = db;
        this.folder = "providers";
        this.fileName = safeName(p.getName()) + "_" + reportDate + ".txt";
    }

    @Override
    public void generate(List<ServiceRecord> records) {
        lines.clear();
        totalFee = 0;

        lines.add("PROVIDER REPORT - week ending " + reportDate);
        lines.add("");
        lines.add(pad("Provider name:", 17)  + provider.getName());
        lines.add(pad("Provider number:", 17) + provider.getNumber());
        lines.add(pad("Street address:", 17) + provider.getAddress());
        lines.add(pad("City:", 17) + provider.getCity());
        lines.add(pad("State:", 17) + provider.getState());
        lines.add(pad("ZIP code:", 17) + provider.getZip());
        lines.add("");
        lines.add("Services provided:");

        List<ServiceRecord> mine = new ArrayList<>();
        for (ServiceRecord r : records) {
            if (r.getProviderNum().equals(provider.getNumber())) {
                mine.add(r);
            }
        }
        mine.sort(Comparator.comparing(ServiceRecord::getReceivedDateTime));   // order received

        if (mine.isEmpty()) {
            lines.add("  (none)");
        } else {
            lines.add("  " + pad("Service Date", 14) + pad("Received", 21) + pad("Member Name", 27)
                    + pad("Member #", 11) + pad("Code", 8) + "Fee");
            for (ServiceRecord r : mine) {
                lines.add("  " + pad(r.getServiceDate(), 14)
                        + pad(r.getDateReceived(), 21)
                        + pad(memberName(db, r.getMemberNum()), 27)
                        + pad(r.getMemberNum(), 11)
                        + pad(r.getServiceCode(), 8)
                        + money(r.getFee()));
                totalFee += r.getFee();
            }
        }

        lines.add("");
        lines.add("Total number of consultations: " + String.format("%03d", mine.size()));
        lines.add("Total fee for week: " + money(totalFee));
    }

    public double getTotalFee() {
        return totalFee;
    }
}
