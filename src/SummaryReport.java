import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Accounts Payable Summary for the manager: each provider to be paid,
 * their consultations and fee, and overall totals.
 *
 * Interface change: the constructor takes the database so provider names
 * can be looked up.
 *
 * @author Namita
 */
public class SummaryReport extends Report {

    private ChocAnDatabase db;
    private int totalProviders;
    private int totalConsultations;
    private double totalFee;

    public SummaryReport(ChocAnDatabase db) {
        this.db = db;
        this.folder = "summary";
        this.fileName = "Summary_" + reportDate + ".txt";
    }

    @Override
    public void generate(List<ServiceRecord> records) {
        lines.clear();
        totalProviders = 0;
        totalConsultations = 0;
        totalFee = 0;

        // provider number -> {consultations, fee}
        Map<String, double[]> perProvider = new LinkedHashMap<>();
        for (ServiceRecord r : records) {
            double[] t = perProvider.computeIfAbsent(r.getProviderNum(), k -> new double[2]);
            t[0] += 1;
            t[1] += r.getFee();
        }

        lines.add("ACCOUNTS PAYABLE SUMMARY - week ending " + reportDate);
        lines.add("");
        lines.add(pad("Provider Name", 27) + pad("Provider #", 12) + pad("Consultations", 15) + "Total Fee");
        for (Map.Entry<String, double[]> e : perProvider.entrySet()) {
            int count = (int) e.getValue()[0];
            double fee = e.getValue()[1];
            lines.add(pad(providerName(db, e.getKey()), 27) + pad(e.getKey(), 12)
                    + pad(String.valueOf(count), 15) + money(fee));
            totalProviders++;
            totalConsultations += count;
            totalFee += fee;
        }

        lines.add("");
        lines.add("Total number of providers who provided services: " + totalProviders);
        lines.add("Total number of consultations: " + totalConsultations);
        lines.add("Overall fee total: " + money(totalFee));
    }

    public int getTotalProviders()      { return totalProviders; }
    public int getTotalConsultations()  { return totalConsultations; }
    public double getTotalFee()         { return totalFee; }
}
