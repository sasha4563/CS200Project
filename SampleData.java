import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes the fake test data files the first time the program runs. A file is
 * only written if it does not exist yet, so delete the data folder (or one
 * file in it) to get fresh sample data back.
 *
 * Files created in the data folder:
 *   member.txt        name|number|address|city|state|zip|status
 *   provider.txt      hospital/clinic name|number|address|city|state|zip|type (also the provider login list)
 *   operator.txt      one operator number per line               (operator login list)
 *   manager.txt       one manager number per line                (manager login list)
 *   services.txt      code|name|fee                              (Provider Directory)
 *   records.txt       received|serviceDate|provider#|member#|code|comments (dates are made relative to today)
 *   acmesimulation.txt memberNumber,status,MM-DD-YYYY HH:MM:SS  (simulated Acme file)
 *
 * @author Sameeksha
 */
public class SampleData {

    private static boolean force = false;   // true = overwrite existing files

    /** Run "java SampleData" to reset every data file to fresh sample data. */
    public static void main(String[] args) {
        force = true;
        ensureFiles(AppFolders.dataDir());
        System.out.println("Sample data files rewritten in: " + AppFolders.dataDir());
    }

    private static final String[][] SERVICES = {
        {"598470", "Dietitian session",   "85.00"},
        {"883948", "Aerobics exercise",   "45.50"},
        {"201114", "Internist checkup",  "120.00"},
        {"310250", "Nutrition counseling", "70.00"},
        {"447781", "Weight training",     "55.25"},
        {"562903", "Craving assessment",  "95.00"},
        {"674120", "Yoga session",        "40.00"},
        {"719385", "Group therapy",       "60.00"},
        {"845026", "Blood panel review", "110.75"},
        {"932617", "Follow-up visit",     "65.00"},
    };

    // daysAgo | provider | member | service code | comments  (oldest first)
    private static final String[][] RECORDS = {
        {"6", "000099999", "555123456", "447781", ""},
        {"5", "000022222", "123456789", "674120", "First yoga class"},
        {"5", "000012345", "444555666", "932617", ""},
        {"4", "000011111", "222333444", "310250", ""},
        {"4", "000054321", "333444555", "845026", "Lab results discussed"},
        {"3", "000099999", "111222333", "883948", ""},
        {"3", "000099999", "444555666", "883948", "Beginner class"},
        {"2", "000012345", "555123456", "598470", ""},
        {"2", "000054321", "123456789", "201114", "Annual checkup"},
        {"1", "000012345", "123456789", "598470", "Initial consult"},
        {"1", "000012345", "111222333", "598470", ""},
    };

    public static void ensureFiles(String dir) {
        try {
            Files.createDirectories(Paths.get(dir));

            writeIfMissing(dir, "member.txt", Arrays.asList(
                "Alice Brown|123456789|101 Main St|Tuscaloosa|AL|35401|Active",
                "Ben Carter|987654321|22 Elm Ave|Northport|AL|35473|Suspended",
                "Cara Diaz|555123456|9 Oak Ct|Birmingham|AL|35203|Active",
                "Derek Hall|111222333|48 Pine Rd|Tuscaloosa|AL|35405|Active",
                "Elena Ruiz|444555666|730 Lake Dr|Hoover|AL|35216|Active",
                "Frank Moore|777888999|15 Cedar Ln|Northport|AL|35476|Suspended",
                "Grace Kim|222333444|60 Maple Blvd|Birmingham|AL|35209|Active",
                "Hector Lee|333444555|8 Birch Way|Tuscaloosa|AL|35406|Active"));

            writeIfMissing(dir, "provider.txt", Arrays.asList(
                "Druid City Nutrition|000012345|500 Health Way|Tuscaloosa|AL|35401|Dietitian",
                "Northport Medical Group|000054321|77 Clinic Rd|Northport|AL|35476|Internist",
                "Crimson Fitness Center|000099999|3 Gym Ln|Tuscaloosa|AL|35404|Exercise expert",
                "Magic City Wellness|000011111|12 Wellness Ct|Birmingham|AL|35205|Dietitian",
                "Hoover Health & Fitness|000022222|90 Fit Ave|Hoover|AL|35244|Exercise expert"));

            writeIfMissing(dir, "operator.txt", Arrays.asList(
                "# one nine-digit operator number per line (200000-299999)",
                "000250000",
                "000250001"));

            writeIfMissing(dir, "manager.txt", Arrays.asList(
                "# one nine-digit manager number per line (100000-199999)",
                "000150000",
                "000150001"));

            List<String> services = new ArrayList<>();
            Map<String, String[]> byCode = new HashMap<>();
            for (String[] s : SERVICES) {
                services.add(String.join("|", s));
                byCode.put(s[0], s);
            }
            writeIfMissing(dir, "services.txt", services);

            if (force || !Files.exists(Paths.get(dir, "records.txt"))) {
                List<String> records = new ArrayList<>();
                LocalDateTime now = LocalDateTime.now();
                for (int i = 0; i < RECORDS.length; i++) {
                    String[] r = RECORDS[i];
                    String[] svc = byCode.get(r[3]);
                    LocalDateTime received = now.minusDays(Long.parseLong(r[0]))
                            .minusMinutes(60L * (RECORDS.length - i));
                    // every third visit is billed the day after it happened
                    java.time.LocalDate served = (i % 3 == 0)
                            ? received.toLocalDate().minusDays(1) : received.toLocalDate();
                    Service s = new Service(svc[0], svc[1], Double.parseDouble(svc[2]));
                    ServiceRecord rec = new ServiceRecord(r[2], r[1], served.format(ServiceRecord.DATE),
                            s, r[4], received.format(ServiceRecord.DATE_TIME));
                    records.add(rec.toLine());
                }
                writeIfMissing(dir, "records.txt", records);
            }

            LocalDateTime n = LocalDateTime.now();
            String yesterday9pm = n.minusDays(1).withHour(21).withMinute(0).withSecond(0).format(ServiceRecord.DATE_TIME);
            String today9am     = n.withHour(9).withMinute(0).withSecond(0).format(ServiceRecord.DATE_TIME);
            String tomorrow9pm  = n.plusDays(1).withHour(21).withMinute(0).withSecond(0).format(ServiceRecord.DATE_TIME);
            writeIfMissing(dir, "acmesimulation.txt", Arrays.asList(
                "# memberNumber,status,when it takes effect (MM-DD-YYYY HH:MM:SS)",
                "# Each line is applied once, the first time the program checks after that time.",
                "# Applied lines get ,APPLIED added. Edit a time to a minute from now to test.",
                "987654321,Active," + yesterday9pm,
                "555123456,Suspended," + today9am,
                "777888999,Active," + tomorrow9pm));
        } catch (IOException e) {
            System.out.println("Could not create sample data: " + e.getMessage());
        }
    }

    private static void writeIfMissing(String dir, String file, List<String> lines) throws IOException {
        Path p = Paths.get(dir, file);
        if (force || !Files.exists(p)) {
            Files.write(p, lines, StandardCharsets.UTF_8);
        }
    }
}
