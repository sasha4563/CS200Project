/**
 * EFT data file: provider name, provider number, and amount to transfer
 * for each provider paid this week.
 *
 * @author
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EFTReport extends Report {

    private List<String> payments;
    private ChocAnDatabase db;

    public EFTReport(ChocAnDatabase db) {

        this.db = db;
        this.fileName = "EFTReport_" + reportDate + ".txt";
        payments = new ArrayList<>();
    }

    @Override
    public void generate(List<ServiceRecord> records) {

        Map<String, Double> providerTotals = new HashMap<>();

        // calculates total fee for each provider
        for (ServiceRecord record : records) {

            String providerNum = record.getProviderNum();

            providerTotals.put(providerNum, providerTotals.getOrDefault(providerNum, 0.0)+ record.getFee()
            );
        }

        payments = new ArrayList<>();

        // returns provider names from database
        for (Map.Entry<String, Double> entry : providerTotals.entrySet()) {

            String providerNum = entry.getKey();
            double amount = entry.getValue();

            String providerName = db.getProviderName(providerNum);

            if (providerName == null) {
                providerName = "Unknown Provider";
            }

            String payment = providerName + "," + providerNum + "," + String.format("%.2f", amount);

            payments.add(payment);
        }

        // writes EFT file
        StringBuilder content = new StringBuilder();

        content.append("Provider Name,Provider Number,Amount\n");

        for (String payment : payments) {
            content.append(payment).append("\n");
        }

        writeFile(content.toString());
    }

    public List<String> getPayments() {
        return payments;
    }
}