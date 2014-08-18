package com.github.thorqin.webapi.smc;

import java.awt.Image;
import java.util.logging.Logger;
import javax.swing.ImageIcon;

/**
 *
 * @author nuo.qin
 */
public class ConfigManager {
	private static final Logger logger = Logger.getLogger(ConfigManager.class.getName());
	
	public static void main(final String[] argc) {
		String winFeel = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
		try {
			javax.swing.UIManager.setLookAndFeel(winFeel);
		} catch (Exception ex) {
			System.out.println("Can not use 'WindowsLookAndFeel'.");
		}
		
		if (argc.length > 1) {
			System.out.println("Usage: java -jar WebApi.jar <Config File>");
		}
		final Image img = new ImageIcon(EditorDialog.class.getResource("setting.png")).getImage();
		String configFile;
		if (argc.length == 0 || argc.length > 1) {
			for(;;) {
				FirstDialog dialog = new FirstDialog();
				dialog.setIconImage(img);
				dialog.setVisible(true);
				if (dialog.isOK())
					configFile = dialog.getConfigFile();
				else
					return;
				EditorDialog editor = new EditorDialog(null);
				editor.setConfigFile(configFile);
				editor.setIconImage(img);
				editor.setVisible(true);
			}
		} else if (argc.length == 1) {
			configFile = argc[0];
			EditorDialog dialog = new EditorDialog(null);
			dialog.setConfigFile(configFile);
			dialog.setIconImage(img);
			dialog.setVisible(true);
		}
	}
}
