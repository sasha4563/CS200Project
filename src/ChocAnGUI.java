import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Dimension;

/**
 * Graphical version of the ChocAn Data Processing System. Start it with:
 *
 *     java ChocAnGUI
 *
 * The window just holds an AppPanel, which has all of the screens. The screens use
 * the same controllers and data files as the command-line version (java MainMenu).
 *
 * @author Sydney Renninger
 */
public class ChocAnGUI extends JFrame {

    public ChocAnGUI() {
        super("ChocAn Data Processing System");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setContentPane(new AppPanel());
        setSize(1280, 800);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Ui.init();
            new ChocAnGUI().setVisible(true);
        });
    }
}
