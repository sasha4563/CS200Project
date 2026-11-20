import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Finds the one folder that holds "data" and puts every output folder next to it,
 * so it does not matter which folder the program is started from (command line
 * or IDE). Looks for data/member.txt in the current folder first, then in the
 * folder the compiled classes are in and the folders above it. If nothing is
 * found, a new data folder is created in the current folder.
 *
 *   <base>/data/               member.txt, provider.txt, ...
 *   <base>/reports/...         manager reports
 *   <base>/ProviderDirectory/  Provider Directory file
 *
 * @author Sameeksha
 */
public class AppFolders {

    private static Path base;

    public static Path base() {
        if (base == null) {
            base = findBase();
        }
        return base;
    }

    public static String dataDir()      { return base().resolve("data").toString(); }
    public static String reportsDir()   { return base().resolve("reports").toString(); }
    public static String directoryDir() { return base().resolve("ProviderDirectory").toString(); }

    private static Path findBase() {
        Path cwd = Paths.get("").toAbsolutePath();
        if (hasData(cwd)) {
            return cwd;
        }
        try {
            Path code = Paths.get(AppFolders.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path p = Files.isRegularFile(code) ? code.getParent() : code;
            for (int i = 0; i < 5 && p != null; i++, p = p.getParent()) {
                if (hasData(p)) {
                    return p;
                }
            }
        } catch (Exception e) {
            // fall through to the current folder
        }
        return cwd;
    }

    private static boolean hasData(Path folder) {
        return Files.exists(folder.resolve("data").resolve("member.txt"));
    }
}
