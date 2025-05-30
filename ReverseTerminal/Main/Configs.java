package ReverseTerminal.Main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Configs {
    private static final Logger logger = Logger.getLogger(Configs.class.getName());
    private static final Properties prop = new Properties();
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    public static final File configF = new File("config.ini");
    public static final String OSOut = System.getProperty("os.name").toLowerCase();
    public static final String DefaultStartUpLin = "bash";
    public static final String DefaultStartUpWin = "cmd";
    public static final String DefaultAutoRunUp = "NaN";
    public static final String OSSHELL = "Default";
    public static final String Build = "20250530";
    
    public static void Load() {
        lock.writeLock().lock();
        try {
            Main.LOG("Checking file");
            Main.LOG("Checking outside OS: " + OSOut);
            
            if (OSOut.contains("win")) {
                Main.ELOG("Windows Server! Config Not Implement");
            }
            
            Main.LOG("Loading config data from file: config.ini");
            
            if (!configF.exists()) {
                Main.ELOG("File: config.ini |> doesn't exist! If it first launch it's normal! We creating it for you");
                createDefaultConfig();
            } else {
                Main.LOG("File Exist. OK");
            }
            
            loadPropertiesFromFile();
            LoadDefaultsConfig();
            logConfigData();
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to load File: config.ini", ex);
            Main.ELOG("Failed to load File: config.ini : " + ex.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private static void createDefaultConfig() throws IOException {
        try (FileOutputStream fos = new FileOutputStream(configF)) {
            prop.store(fos, "Default configuration created");
        }
    }
    
    private static void loadPropertiesFromFile() throws IOException {
        try (FileInputStream fis = new FileInputStream(configF)) {
            prop.load(fis);
        }
    }
    
    private static void logConfigData() {
        Main.LOG("Loaded Config.ini Data: ");
        Main.LOG("		- Startup-Command: " + GetProp("Startup-Command"));
        Main.LOG("		- Autorun-Command: " + GetProp("Autorun-Command"));
        Main.LOG("		- Public-IP: " + GetProp("Public-IP"));
        Main.LOG("		- Private-IP: " + GetProp("Private-IP"));
        Main.LOG("		- HOST_IP: " + GetProp("HOST-IP") + " : IP For host your Apps in your (bash, .sh) code files");
        Main.LOG("		- HOST_PORT: " + GetProp("HOST-PORT") + " : PORT For host your Apps in your (bash, .sh) code files");
        Main.LOG("		- ENABLE-WEB_SHELL: " + GetProp("ENABLE-WEB_SHELL") + " : Enable or disable web terminal (True/False)");
        Main.LOG("		- WEB-USERNAME: " + GetProp("WEB-USERNAME") + " : Username For Web Terminal");
        Main.LOG("		- WEB-PASSWORD: " + GetProp("WEB-PASSWORD") + " : Password For Web Terminal");
        Main.LOG("Example FOR host some web or another app:");
        Main.LOG("[1] php -S $HOST_IP:$HOST_PORT");
        Main.LOG("[2] java -jar file.jar -port $HOST_PORT");
    }
    
    public static Boolean CheckUpdate() {
        lock.readLock().lock();
        try {
            Main.LOG("Checking Update");
            String currentVersion = Main.OSurl.get("Main");
            return currentVersion != null && !Build.equals(currentVersion);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public static String GetProp(String property) {
        lock.readLock().lock();
        try {
            return prop.getProperty(property, "");
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public static void WriteProp(String property, String value) {
        if (property == null || value == null) {
            Main.ELOG("Property or value cannot be null");
            return;
        }
        
        lock.writeLock().lock();
        try {
            prop.setProperty(property, value);
            try (FileOutputStream fos = new FileOutputStream(configF)) {
                prop.store(fos, "Updated property: " + property);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save property: " + property, e);
            Main.ELOG("Failed TO save PROPERTY: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public static Boolean isExist(String key) {
        lock.readLock().lock();
        try {
            return key != null && prop.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private static void LoadDefaultsConfig() {
        try {
            boolean needsSave = false;
            
            if (!isExist("OS")) {
                prop.setProperty("OS", OSSHELL);
                needsSave = true;
            }
            if (!isExist("HOST-IP")) {
                prop.setProperty("HOST-IP", "0.0.0.0");
                needsSave = true;
            }
            if (!isExist("HOST-PORT")) {
                String serverPort = System.getenv("SERVER_PORT");
                prop.setProperty("HOST-PORT", serverPort != null ? serverPort : "8080");
                needsSave = true;
            }
            if (!isExist("Public-IP")) {
                String serverIP = System.getenv("SERVER_IP");
                prop.setProperty("Public-IP", serverIP != null ? serverIP : "localhost");
                needsSave = true;
            }
            if (!isExist("Private-IP")) {
                String internalIP = System.getenv("INTERNAL_IP");
                prop.setProperty("Private-IP", internalIP != null ? internalIP : "127.0.0.1");
                needsSave = true;
            }
            if (!isExist("Startup-Command")) {
                prop.setProperty("Startup-Command", OSOut.contains("win") ? DefaultStartUpWin : DefaultStartUpLin);
                needsSave = true;
            }
            if (!isExist("Autorun-Command")) {
                prop.setProperty("Autorun-Command", DefaultAutoRunUp);
                needsSave = true;
            }
            if (!isExist("ENABLE-WEB_SHELL")) {
                prop.setProperty("ENABLE-WEB_SHELL", "False");
                needsSave = true;
            }
            if (!isExist("WEB-USERNAME")) {
                prop.setProperty("WEB-USERNAME", "ADMIN");
                needsSave = true;
            }
            if (!isExist("WEB-PASSWORD")) {
                prop.setProperty("WEB-PASSWORD", "12345");
                needsSave = true;
            }
            
            if (needsSave) {
                try (FileOutputStream fos = new FileOutputStream(configF)) {
                    prop.store(fos, "Default configuration loaded");
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while creating default config", e);
            Main.ELOG("Error while creating config.ini: " + e.getMessage());
        }
    }
}