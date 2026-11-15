import controller.Controller;
import view.Window;

import javax.swing.SwingUtilities;

import static javax.swing.SwingUtilities.invokeLater;

public class Main {

    public static void main(String[] args) {
        invokeLater(() -> {
            Window window = new Window(1600, 900);
            new Controller(window.getCanvas()).init();
        });
    }

}
