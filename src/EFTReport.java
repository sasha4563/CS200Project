import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EFT data file: provider name, provider number, and amount to transfer
 * for each provider paid this week. One line per provider.
 *
 * Interface change: the constructor takes the database so provider names
 * can be looked up.
 *
 * @author Namita
 */
public class EFTReport extends Report {

    private ChocAnDatabase db;
    private List<String> payments = new ArrayList<>();   // "providerName,providerNumber,amount"

    public EFTReport(ChocAnDatabase db) {
        this.db = db;
        this.folder = "eft";
        this.fileName = "EFT_" + reportDate + ".txt";
    }

    @Override
    public void generate(List<ServiceRecord> records) {
        lines.clear();
        payments.clear();

        Map<String, Double> totals = new LinkedHashMap<>();
        for (ServiceRecord r : records) {
            totals.merge(r.getProviderNum(), r.getFee(), Double::sum);
        }
        for (Map.Entry<String, Double> e : totals.entrySet()) {
            payments.add(providerName(db, e.getKey()) + "," + e.getKey() + ","
                    + String.format(Locale.US, "%.2f", e.getValue()));
        }
        lines.addAll(payments);
    }

    public List<String> getPayments() {
        return payments;
    }
}
