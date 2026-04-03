package com.lancode;

import com.lancode.tools.ToolRegistry;
import java.util.Scanner;

public class Main {

    static final String BANNER = """
          ╔══════════════════════════════════════╗
          ║         lancode v0.1.0              ║
          ║   Distilled Agent Loop Framework    ║
          ╚══════════════════════════════════════╝

          Type your message to start. Commands:
            /tools   -- list available tools
            /mode    -- show/change permission mode
            /help    -- show help
            /quit    -- exit
        """;

    public static void main(String[] args) {
        Config config = new Config();
        String oneShot = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--model" -> { if (i + 1 < args.length) config.model = args[++i]; }
                case "--mode"  -> { if (i + 1 < args.length) config.permissionMode = Config.PermissionMode.valueOf(args[++i].toUpperCase()); }
                case "--max-turns" -> { if (i + 1 < args.length) config.maxTurns = Integer.parseInt(args[++i]); }
                default -> { if (!args[i].startsWith("--")) oneShot = args[i]; }
            }
        }

        ToolRegistry registry = ToolRegistry.defaultRegistry(config);
        ConversationContext context = new ConversationContext(config);
        PermissionGate gate = new PermissionGate(config);
        AgentLoop agent = new AgentLoop(config, context, registry, gate);

        if (oneShot != null) {
            try {
                agent.run(oneShot);
                System.out.println();
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                System.exit(1);
            }
            return;
        }

        runInteractive(agent, config, registry);
    }

    static void runInteractive(AgentLoop agent, Config config, ToolRegistry registry) {
        System.out.println(BANNER);
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("\n> ");
            System.out.flush();
            if (!sc.hasNextLine()) {
                System.out.println("\nGoodbye!");
                break;
            }
            String input = sc.nextLine().strip();
            if (input.isEmpty()) continue;

            if (input.startsWith("/")) {
                String[] parts = input.split("\\s+");
                String cmd = parts[0].toLowerCase();
                switch (cmd) {
                    case "/quit", "/exit", "/q" -> { System.out.println("Goodbye!"); return; }
                    case "/tools" -> {
                        System.out.println("\nAvailable tools:");
                        registry.allTools().forEach(t ->
                            System.out.println("  - " + t.name() + ": " + t.description()));
                    }
                    case "/mode" -> {
                        if (parts.length > 1) {
                            try {
                                config.permissionMode = Config.PermissionMode.valueOf(parts[1].toUpperCase());
                                System.out.println("Mode changed to: " + parts[1]);
                            } catch (IllegalArgumentException e) {
                                System.out.println("Unknown mode. Use: ask, auto, plan");
                            }
                        } else {
                            System.out.println("Current mode: " + config.permissionMode.name().toLowerCase());
                            System.out.println("Usage: /mode [ask|auto|plan]");
                        }
                    }
                    case "/help" -> System.out.println(BANNER);
                    default -> System.out.println("Unknown command: " + cmd + ". Type /help for help.");
                }
                continue;
            }

            System.out.println();
            try {
                agent.run(input);
            } catch (Exception e) {
                System.err.println("\nError: " + e.getMessage());
            }
        }
    }
}
