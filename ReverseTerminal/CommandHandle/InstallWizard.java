package ReverseTerminal.CommandHandle;

import ReverseTerminal.Main.Main;
import ReverseTerminal.Main.Configs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.logging.Level;

public class InstallWizard {
    private static final Logger logger = Logger.getLogger(InstallWizard.class.getName());
    private static final ReentrantLock installLock = new ReentrantLock();
    
    public static void InstallOS(String os, boolean reset) {
        if (os == null || os.trim().isEmpty()) {
            Main.ELOG("OS name cannot be null or empty");
            return;
        }
        
        installLock.lock();
        try {
            Main.LOG("Installing " + os);
            
            String osUrl = Main.OSurl.get(os);
            if (osUrl == null) {
                Main.ELOG("OS URL not found for: " + os);
                return;
            }
            
            if (!reset) {
                String installCommand = String.format(
                    "curl -# -SLo 1.tar.xz \"%s\" && cd $(pwd) && tar xvf 1.tar.xz && rm 1.tar.xz && echo %s && proot -S . bash",
                    osUrl, "\"[INFO] Installation done!\""
                );
                Command.WriteCmd(installCommand);
            } else {
                String reinstallCommand = String.format(
                    "ls | grep -v linux | grep -v config.ini | grep -v server.jar | xargs rm -rf && " +
                    "curl -# -SLo 1.tar.xz \"%s\" && cd $(pwd) && tar xvf 1.tar.xz && rm 1.tar.xz && echo %s && proot -S . bash",
                    osUrl, "\"[INFO] Installation done!\""
                );
                Command.WriteCmd(reinstallCommand);
            }
            
            Configs.WriteProp("OS", os);
            Configs.WriteProp("Startup-Command", "proot -S . bash");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to install OS: " + os, e);
            Main.ELOG("Failed to install OS: " + e.getMessage());
        } finally {
            installLock.unlock();
        }
    }
    
    public static void LOCALInstallAPP(String apps) {
        if (apps == null || apps.trim().isEmpty()) {
            Main.ELOG("App name cannot be null or empty");
            return;
        }
        
        installLock.lock();
        try {
            Main.LOG("Installing apps on non root container: " + apps + " && [Server Warning] Action can't be verified");
            Command.WriteCmd("apth install " + apps);
            updateEnvironmentPaths();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to install local app: " + apps, e);
            Main.ELOG("Failed to install local app: " + e.getMessage());
        } finally {
            installLock.unlock();
        }
    }
    
    public static void UpdateJAR() {
        installLock.lock();
        try {
            Main.LOG("Downloading Latest server.jar");
            
            if (Configs.CheckUpdate()) {
                File currentJar = new File("server.jar");
                File backupJar = new File("server-bak.jar");
                
                // Create backup of current jar
                if (currentJar.exists()) {
                    if (!currentJar.renameTo(backupJar)) {
                        Main.ELOG("Failed to create backup of current server.jar");
                        return;
                    }
                }
                
                String currentVersion = Configs.Build;
                String newVersion = Main.OSurl.get("Main");
                
                Main.LOG("<- Current Version /  New Version ->");
                Main.LOG(currentVersion + " / " + newVersion);
                
                String updateCommand = "curl -# -o server.jar https://github.com/OkamuraYuji/terminal/releases/download/20250530/server.jar && " +
                                     "rm -f server-bak.jar && " +
                                     "echo \"[Server WARNING] Update server.jar maybe successful! Need Restart\"";
                Command.WriteCmd(updateCommand);
                
                Main.LOG("Restarting After 7 secs!");
                
                // Use a separate thread for shutdown to avoid blocking
                Thread shutdownThread = new Thread(() -> {
                    try {
                        Thread.sleep(7000);
                        System.exit(0);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.log(Level.WARNING, "Shutdown thread interrupted", e);
                    }
                });
                shutdownThread.setDaemon(true);
                shutdownThread.start();
                
            } else {
                String currentVersion = Configs.Build;
                String latestVersion = Main.OSurl.get("Main");
                Main.LOG("<- Current Version /  New Version ->");
                Main.LOG(currentVersion + " / " + latestVersion + " :> This server.jar already Up To Date");
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while downloading latest jar", e);
            Main.ELOG("Error While downloading Latest.jar: " + e.getMessage());
            
            // Restore backup if update failed
            File backupJar = new File("server-bak.jar");
            File currentJar = new File("server.jar");
            if (backupJar.exists() && !currentJar.exists()) {
                if (backupJar.renameTo(currentJar)) {
                    Main.LOG("Restored backup server.jar due to update failure");
                } else {
                    Main.ELOG("Failed to restore backup server.jar");
                }
            }
        } finally {
            installLock.unlock();
        }
    }
    
    public static void InstallAPP(String apps) {
        if (apps == null || apps.trim().isEmpty()) {
            Main.ELOG("App name cannot be null or empty");
            return;
        }
        
        installLock.lock();
        try {
            Main.LOG("Installing app for fake rooted Container: " + apps);
            String installCommand = "apt-get update && apt-get install -y " + apps + 
                                  " && echo \"[Server Warning] Action can't be verified\"";
            Command.WriteCmd(installCommand);
            updateEnvironmentPaths();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to install app: " + apps, e);
            Main.ELOG("Failed to install app: " + e.getMessage());
        } finally {
            installLock.unlock();
        }
    }
    
    public static void InstallShellWeb() {
        installLock.lock();
        try {
            File gottyDir = new File("linux/usr/bin/gotty");
            File gottyExecutable = new File("linux/usr/bin/gotty");
            boolean gottyExists = gottyExecutable.exists() && gottyExecutable.canExecute();
            
            Main.LOG("GOTTY Installed: " + gottyExists);
            
            if (!gottyExists) {
                Main.LOG("Installing GOTTY...");
                ensureDirectoryExists("linux/usr/bin");
                
                String installGottyCommand = "curl -# -SLo gotty.tar.gz " +
                    "https://github.com/yudai/gotty/releases/latest/download/gotty_linux_amd64.tar.gz && " +
                    "tar xf gotty.tar.gz -C $(pwd)/linux/usr/bin && " +
                    "chmod +x $(pwd)/linux/usr/bin/gotty && " +
                    "rm -f gotty.tar.gz";
                Command.WriteCmd(installGottyCommand);
            }
            
            String username = Configs.GetProp("WEB-USERNAME");
            String password = Configs.GetProp("WEB-PASSWORD");
            
            if (username.isEmpty()) username = "ADMIN";
            if (password.isEmpty()) password = "12345";
            
            String runGottyCommand = String.format(
                "nohup gotty -w -p $SERVER_PORT -c \"%s:%s\" bash > /dev/null 2>&1 &",
                username, password
            );
            Command.WriteCmd(runGottyCommand);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to install shell web", e);
            Main.ELOG("Failed to install shell web: " + e.getMessage());
        } finally {
            installLock.unlock();
        }
    }
    
    public static void UpgradeOSApt() {
        installLock.lock();
        try {
            String currentOS = Configs.GetProp("OS");
            if ("Default".equalsIgnoreCase(currentOS)) {
                Main.ELOG("Apt upgrade not available in Default Container");
                return;
            }
            
            Main.LOG("Apt Upgrade process started LOCAL@: apt-get update && apt-get upgrade -y");
            Command.WriteCmd("apt-get update && apt-get upgrade -y");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to upgrade OS apt", e);
            Main.ELOG("Failed to upgrade OS apt: " + e.getMessage());
        } finally {
            installLock.unlock();
        }
    }
    
    public static void UninstallOS() {
        installLock.lock();
        try {
            String currentOS = Configs.GetProp("OS");
            if ("Default".equalsIgnoreCase(currentOS)) {
                Main.ELOG("Default container can't be uninstalled!");
                return;
            }
            
            File currentDir = new File(".");
            File[] files = currentDir.listFiles();
            
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (fileName.equals("config.ini") || 
                        fileName.equals("server.jar") || 
                        fileName.equals("Server.jar") || 
                        fileName.equals("linux")) {
                        Main.LOG("Skip file deletion for: " + fileName);
                    } else {
                        deleteFileOrDirectory(file);
                    }
                }
            }
            
            Main.LOG("OS Uninstalled!");
            
            String defaultStartup = Configs.OSOut.contains("win") ? 
                Configs.DefaultStartUpWin : Configs.DefaultStartUpLin;
            Configs.WriteProp("Startup-Command", defaultStartup);
            Configs.WriteProp("OS", "Default");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to uninstall OS", e);
            Main.ELOG("Failed to uninstall OS: " + e.getMessage());
        } finally {
            installLock.unlock();
        }
    }
    
    public static void ResetC() {
        installLock.lock();
        try {
            Main.LOG("Resetting This Container!");
            
            File currentDir = new File(".");
            File[] files = currentDir.listFiles();
            
            if (files != null) {
                Main.LOG("Removing " + files.length + " files");
                
                for (File file : files) {
                    String fileName = file.getName();
                    if (fileName.equals("config.ini") || 
                        fileName.equals("server.jar") || 
                        fileName.equals("Server.jar")) {
                        Main.LOG("Skip file deletion for: " + fileName);
                    } else {
                        deleteFileOrDirectory(file);
                    }
                }
            }
            
            // Ensure required directories exist
            ensureDirectoryExists("linux/usr/bin");
            ensureDirectoryExists("linux/bin");
            ensureDirectoryExists("linux/usr/sbin");
            ensureDirectoryExists("linux/sbin");
            
            // Reinstall apth
            String installApthCommand = "curl -o $(pwd)/linux/bin/apth https://raw.githubusercontent.com/OkamuraYuji/terminal/refs/heads/main/apth.txt && " +
                                      "chmod +x $(pwd)/linux/bin/apth";
            Command.WriteCmd(installApthCommand);
            
            // Reset configuration
            String defaultStartup = Configs.OSOut.contains("win") ? 
                Configs.DefaultStartUpWin : Configs.DefaultStartUpLin;
            Configs.WriteProp("Startup-Command", defaultStartup);
            Configs.WriteProp("Autorun-Command", Configs.DefaultAutoRunUp);
            Configs.WriteProp("OS", "Default");
            
            Main.ELOG("Please restart to complete action");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to reset container", e);
            Main.ELOG("Failed to reset container: " + e.getMessage());
        } finally {
            installLock.unlock();
        }
    }
    
    private static void updateEnvironmentPaths() {
        String envCommand = "export LINE1=$(find $(pwd)/linux -type d | awk '{printf \"%s:\", $0}') && " +
                          "export LD_LIBRARY_PATH=$LINE1 && export LIBRARY_PATH=$LINE1";
        Command.WriteCmd(envCommand);
    }
    
    private static void ensureDirectoryExists(String dirPath) {
        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                Main.LOG("Created directory: " + dirPath);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create directory: " + dirPath, e);
            Main.ELOG("Failed to create directory: " + dirPath + " - " + e.getMessage());
        }
    }
    
    private static void deleteFileOrDirectory(File file) {
        try {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        deleteFileOrDirectory(child);
                    }
                }
            }
            
            if (!file.delete()) {
                Main.ELOG("Failed to delete: " + file.getName());
            } else {
                Main.LOG("Deleted: " + file.getName());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to delete file: " + file.getName(), e);
            Main.ELOG("Failed to delete file: " + file.getName() + " - " + e.getMessage());
        }
    }
}
