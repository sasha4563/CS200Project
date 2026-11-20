/**
 * Accounts Payable Summary for the manager: each provider to be paid,
 * their consultations and fee, and overall totals.
 *
 * @author
 */
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class SummaryReport extends Report {

    private int totalProviders;
    private int totalConsultations;
    private double totalFee;

    public SummaryReport() {
        fileName = "SummaryReport_" + reportDate + ".txt";
    }

    @Override
    public void generate(List<ServiceRecord> records) {

        Map<String, Integer> providerConsultations = new HashMap<>();

        Map<String, Double> providerFees = new HashMap<>();

        totalFee = 0.0;
        totalConsultations = 0;

        for (ServiceRecord record : records) {

            String providerNum = record.getProviderNum();

            providerConsultations.put(providerNum,providerConsultations.getOrDefault(providerNum, 0) + 1);

            providerFees.put(providerNum,providerFees.getOrDefault(providerNum, 0.0)+ record.getFee());

            totalFee += record.getFee();
            totalConsultations++;
        }

        totalProviders = providerConsultations.size();

        StringBuilder content = new StringBuilder();

        content.append("Accounts Payable Summary Report\n");
        content.append("Report Date: ").append(reportDate).append("\n\n");

        content.append("Provider Number | Consultations | Total Fee\n");
        content.append("------------------------------------------\n");

        for (String providerNum : providerConsultations.keySet()) {

            content.append(providerNum).append(" | ");

            content.append(providerConsultations.get(providerNum)).append(" | $");

            content.append(String.format("%.2f",providerFees.get(providerNum)));

            content.append("\n");
        }

        content.append("\nTotal Providers: ").append(totalProviders).append("\n");

        content.append("Total Consultations: ").append(totalConsultations).append("\n");

        content.append("Overall Fee Total: $").append(String.format("%.2f", totalFee))append("\n");

        writeFile(content.toString());
    }

    public int getTotalProviders() {
        return totalProviders;
    }

    public int getTotalConsultations() {
        return totalConsultations;
    }

    public double getTotalFee() {
        return totalFee;
    }
}
