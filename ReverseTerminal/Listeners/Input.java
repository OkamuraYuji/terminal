package ReverseTerminal.Listeners;

import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.logging.Level;

import ReverseTerminal.CommandHandle.Command;
import ReverseTerminal.CommandHandle.InstallWizard;
import ReverseTerminal.Main.Main;
import ReverseTerminal.Main.Configs;

public class Input implements Runnable {
    private static final Logger logger = Logger.getLogger(Input.class.getName());
    private static Thread thread;
    private static Scanner input;
    private static final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    public void start() {
        if (thread == null || !thread.isAlive()) {
            thread = new Thread(this);
            thread.start();
        }
    }
    
    public static void shutdown() {
        isRunning.set(false);
        if (input != null) {
            input.close();
        }
        if (thread != null) {
            thread.interrupt();
        }
    }
    
    @Override
    public void run() {
        input = new Scanner(System.in);
        isRunning.set(true);
        
        try {
            if (Configs.GetProp("ENABLE-WEB_SHELL").equalsIgnoreCase("true")) {
                InstallWizard.InstallShellWeb();
            }
            
            while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
                if (input.hasNextLine()) {
                    String line = input.nextLine().trim();
                    processCommand(line);
                } else {
                    Thread.sleep(100); // Prevent busy waiting
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Main.LOG("Input handler interrupted");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error in input handler", e);
            Main.ELOG("Input handler error: " + e.getMessage());
        } finally {
            cleanup();
        }
        
        Main.LOG("GOODBYE!");
        Main.LOG("Please don't DDOS with it or mine bitcoin");
    }
    
    private void processCommand(String line) {
        try {
            if (line.isEmpty()) return;
            
            String[] commandParts = line.split("\\s+");
            String command = commandParts[0].toLowerCase();
            
            // Convert numeric command to text command if needed
            command = convertNumericCommand(command);
            
            switch (command) {
                case "aptupgrade":
                    handleAptUpgrade();
                    break;
                case "updateserver":
                    handleUpdateServer();
                    break;
                case "reloadcfg":
                    handleReloadConfig();
                    break;
                case "runshellinweb":
                    handleRunShellInWeb();
                    break;
                case "installapp":
                    handleInstallApp(line);
                    break;
                case "uninstallos":
                    handleUninstallOS();
                    break;
                case "listos":
                    handleListOS();
                    break;
                case "resetos":
                    handleResetOS();
                    break;
                case "help":
                    handleHelp();
                    break;
                case "console":
                    handleConsole();
                    break;
                case "install":
                    handleInstall(commandParts);
                    break;
                case "reinstall":
                    handleReinstall();
                    break;
                case "closeit":
                    handleCloseIt();
                    break;
                default:
                    Main.ELOG("This command doesn't exist: " + line);
                    Main.LOG("Input |-> Help <-| For get list of available commands");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing command: " + line, e);
            Main.ELOG("Error processing command: " + e.getMessage());
        }
    }

    private String convertNumericCommand(String command) {
        switch (command) {
            // Installation Commands (1-4)
            case "1":
                return "install";
            case "2":
                return "installapp";
            case "3":
                return "reinstall";
            case "4":
                return "uninstallos";
                
            // System Update Commands (5-7)
            case "5":
                return "aptupgrade";
            case "6":
                return "updateserver";
            case "7":
                return "reloadcfg";
                
            // OS Management Commands (8-10)
            case "8":
                return "listos";
            case "9":
                return "resetos";
            case "10":
                return "runshellinweb";
                
            // Utility Commands (11-13)
            case "11":
                return "console";
            case "12":
                return "help";
            case "13":
                return "closeit";
                
            default:
                return command;
        }
    }
    
    private void handleAptUpgrade() {
        if (!Configs.GetProp("OS").equals("Default")) {
            InstallWizard.UpgradeOSApt();
        } else {
            Main.ELOG("Not available in Default Container");
        }
    }
    
    private void handleUpdateServer() {
        Main.LOG("Starting Update Process of Server.jar");
        InstallWizard.UpdateJAR();
    }
    
    private void handleReloadConfig() {
        Main.LOG("Reloading CONFIG file... And Recovering Missing options");
        Configs.Load();
    }
    
    private void handleRunShellInWeb() {
        Main.LOG("Starting Terminal In A Web");
        InstallWizard.InstallShellWeb();
    }
    
    private void handleInstallApp(String line) {
        String appName = line.toLowerCase().replace("installapp", "").trim();
        if (appName.isEmpty()) {
            Main.ELOG("Usage: installapp <app_name>");
            return;
        }
        
        if (Configs.GetProp("OS").equalsIgnoreCase("Default")) {
            InstallWizard.LOCALInstallAPP(appName);
        } else {
            InstallWizard.InstallAPP(appName);
        }
    }
    
    private void handleUninstallOS() {
        // Fixed logic: Can only uninstall if NOT Default
        if (!Configs.GetProp("OS").equalsIgnoreCase("Default")) {
            InstallWizard.UninstallOS();
        } else {
            Main.ELOG("Default container can't be deleted!");
        }
    }
    
    private void handleListOS() {
        int i = 0;
        Main.LOG("==========Available OS List============");
        for (Entry<String, String> vals : Main.OSurl.entrySet()) {
            if (vals.getKey().equals("Main")) continue;
            i++;
            Main.LOG(i + " " + vals.getKey());
        }
        Main.LOG("=======================================");
    }
    
    private void handleResetOS() {
        InstallWizard.ResetC();
    }
    
    private void handleHelp() {
        Main.LOG("===============================================");
        Main.LOGHELP();
        Main.LOG("===============================================");
    }
    
    private void handleConsole() {
        Main.LOG("Entering direct command mode!");
        Main.LOG("Now you can input all linux commands here");
        
        while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
            if (input.hasNextLine()) {
                String consoleLine = input.nextLine().trim();
                if (consoleLine.equalsIgnoreCase("ConsoleExit")) {
                    Main.LOG("Returning back....");
                    Main.LOG("Returned!");
                    break;
                }
                if (!consoleLine.isEmpty()) {
                    Command.WriteCmd(consoleLine);
                }
            }
        }
    }
    
    private void handleInstall(String[] commandParts) {
        if (commandParts.length == 2) {
            String osName = commandParts[1];
            if (Main.OSurl.containsKey(osName)) {
                InstallWizard.InstallOS(osName, false);
            } else {
                Main.ELOG("OS: " + osName + " Not found in library");
            }
        } else {
            Main.ELOG("Wrong usage use Install <OSName>");
        }
    }
    
    private void handleReinstall() {
        String currentOS = Configs.GetProp("OS");
        // Fixed logic: Can only reinstall if NOT Default
        if (!currentOS.equalsIgnoreCase("Default")) {
            if (Main.OSurl.containsKey(currentOS)) {
                InstallWizard.InstallOS(currentOS, true);
            } else {
                Main.ELOG("Installed OS: " + currentOS + " Not found in Downloaded OS's library");
            }
        } else {
            Main.ELOG("OS not INSTALLED! Nothing to reinstall");
        }
    }
    
    private void handleCloseIt() {
        isRunning.set(false);
    }
    
    private void cleanup() {
        if (input != null) {
            try {
                input.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error closing scanner", e);
            }
        }
    }
}