package ReverseTerminal.CommandHandle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.logging.Level;

import ReverseTerminal.Main.Main;
import ReverseTerminal.Main.Configs;

public class Command {
    private static final Logger logger = Logger.getLogger(Command.class.getName());
    private static final ReentrantLock writeLock = new ReentrantLock();
    
    public static void WriteCmd(String cmd) {
        writeLock.lock();
        try {
            String currentDir = getCurrentDirectory();
            String logMessage = Configs.GetProp("WEB-USERNAME") + "@" + currentDir + ": " + cmd;
            Main.LOG(logMessage);
            
            if (Main.bw != null) {
                Main.bw.write(cmd);
                Main.bw.newLine();
                Main.bw.flush();
            } else {
                Main.ELOG("BufferedWriter is not initialized");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write command: " + cmd, e);
            Main.ELOG("Failed to execute command: " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }
    
    private static String getCurrentDirectory() {
        Process process = null;
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime().exec("pwd");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();
            return result != null ? result : "unknown";
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to get current directory", e);
            return "unknown";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to close reader", e);
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
    }
}