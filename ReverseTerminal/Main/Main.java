package ReverseTerminal.Main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

import ReverseTerminal.CommandHandle.Command;
import ReverseTerminal.Listeners.Input;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    
    public static volatile BufferedWriter bw;
    public static final Map<String, String> OSurl = new ConcurrentHashMap<>();
    private static Process process;
    private static ProcessBuilder processBuilder;
    private static Input inputHandler;
    
    public static void main(String[] args) {
        // Add shutdown hook for graceful cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(Main::cleanup));
        
        try {
            LOG("ReverseTerminal | Build: " + Configs.Build);
            LOG("Loading Configuration. And recovering missing lines");
            
            initializeDirectories();
            initializeProcess();
            
            Configs.Load();
            setupEnvironment();
            installRequiredTools();
            executeStartupCommands();
            loadOSDatabase();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize application", e);
            ELOG("Failed to initialize: " + e.getMessage());
            System.exit(1);
        }
        
        LOG("Starting application...");
        loop();
    }
    
    private static void initializeDirectories() {
        String[] directories = {
            "linux/usr/bin",
            "linux/bin", 
            "linux/usr/sbin",
            "linux/sbin"
        };
        
        for (String dir : directories) {
            File directory = new File(dir);
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    ELOG("Failed to create directory: " + dir);
                }
            }
        }
    }
    
    private static void initializeProcess() throws IOException {
        LOG("Initialising...");
        processBuilder = new ProcessBuilder();
        
        if (Configs.OSOut.contains("win")) {
            processBuilder.command("cmd")
                         .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                         .redirectError(ProcessBuilder.Redirect.INHERIT);
        } else {
            processBuilder.command("bash")
                         .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                         .redirectError(ProcessBuilder.Redirect.INHERIT);
        }
        
        process = processBuilder.start();
        bw = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
    }
    
    private static void setupEnvironment() {
        LOG("Exporting new Environment PATH!");
        String envCommand = String.format(
            "export HOST_IP=%s && export HOST_PORT=%s && " +
            "export LINE1=$(find $(pwd)/linux -type d | awk '{printf \"%%s:\", $0}') && " +
            "export LD_LIBRARY_PATH=$LINE1 && export LIBRARY_PATH=$LINE1 && " +
            "export PATH=$LINE1:$PATH && bash",
            Configs.GetProp("HOST-IP"), 
            Configs.GetProp("HOST-PORT")
        );
        Command.WriteCmd(envCommand);
    }
    
    private static void installRequiredTools() {
        if (!new File("linux/bin/apth").exists()) {
            LOG("Installing proot and apth And exporting new Environment PATH!");
            Command.WriteCmd("curl -o $(pwd)/linux/bin/apth https://raw.githubusercontent.com/OkamuraYuji/terminal/refs/heads/main/apth.txt");
            Command.WriteCmd("curl -o $(pwd)/linux/bin/systemctl https://raw.githubusercontent.com/gdraheim/docker-systemctl-replacement/master/files/docker/systemctl3.py");
            Command.WriteCmd("chmod +x $(pwd)/linux/bin/apth && $(pwd)/linux/bin/apth proot wget");
        } else {
            LOG("Apth, proot, wget already installed!");
        }
    }
    
    private static void executeStartupCommands() {
        Command.WriteCmd(Configs.GetProp("Startup-Command"));
        
        String autorunCommand = Configs.GetProp("Autorun-Command");
        if (!"NaN".equals(autorunCommand) && !autorunCommand.isEmpty()) {
            Command.WriteCmd("nohup " + autorunCommand);
        }
        LOG("Autorun-Command Command:> nohup " + autorunCommand);
    }
    
    private static void loadOSDatabase() {
        LOG("Loading OSs database from web");
        BufferedReader in = null;
        try {
            URL oracle = new URL("https://raw.githubusercontent.com/OkamuraYuji/terminal/refs/heads/main/OSs.txt");
            in = new BufferedReader(new InputStreamReader(oracle.openStream()));
            
            String line;
            int osCount = 0;
            while ((line = in.readLine()) != null) {
                String[] parts = line.trim().split("\\s+", 2);
                if (parts.length == 2) {
                    OSurl.put(parts[0], parts[1]);
                    if (!"Main".equals(parts[0])) {
                        LOG("Found OS: " + parts[0] + " URL: " + parts[1]);
                        osCount++;
                    }
                }
            }
            
            LOG("Available to install: " + osCount + " OSs");
            
            if (Configs.CheckUpdate()) {
                LOG("Found Update! You can update your server.jar with command -> | UpdateServer |");
            }
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load OS database", e);
            ELOG("Failed to load OS database: " + e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to close input stream", e);
                }
            }
        }
    }
    
    private static void loop() {
        LOG("===========REVERSE TERMINAL CONTROL===========");
        LOG("");
        LOGHELP();
        LOG("===============================================");
        LOG("Input Option for to do");
        
        inputHandler = new Input();
        inputHandler.start();
    }
    public static void LOGHELP() {
        LOG("=== Available Commands ===");
        LOG("");
        LOG("📦 INSTALLATION COMMANDS:");
        LOG("1  | install        - Install package");
        LOG("2  | installapp     - Install application");
        LOG("3  | reinstall      - Reinstall system");
        LOG("4  | uninstallos    - Uninstall operating system");
        LOG("");
        LOG("🔄 SYSTEM UPDATE COMMANDS:");
        LOG("5  | aptupgrade     - Upgrade system packages");
        LOG("6  | updateserver   - Update server configuration");
        LOG("7  | reloadcfg      - Reload configuration files");
        LOG("");
        LOG("💻 OS MANAGEMENT COMMANDS:");
        LOG("8  | listos         - List available OS");
        LOG("9  | resetos        - Reset operating system");
        LOG("10 | runshellinweb  - Run shell in web interface");
        LOG("");
        LOG("🛠️ UTILITY COMMANDS:");
        LOG("11 | console        - Open console");
        LOG("12 | help           - Show this help menu");
        LOG("13 | closeit        - Close application");
        LOG("");
        LOG("========================");
        LOG("💡 You can use either numbers (1-13) or command names");
    }
    
    public static synchronized void LOG(String text) {
        System.out.println("[INFO] " + text);
    }
    
    public static synchronized void ELOG(String text) {
        System.out.println("[ERROR] " + text);
    }
    
    private static void cleanup() {
        LOG("Shutting down application...");
        
        // Shutdown input handler
        if (inputHandler != null) {
            Input.shutdown();
        }
        
        // Close BufferedWriter
        if (bw != null) {
            try {
                bw.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to close BufferedWriter", e);
            }
        }
        
        // Destroy process
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        
        LOG("Application shutdown complete");
    }
}