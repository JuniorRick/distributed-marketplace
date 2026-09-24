package com.marketplace.e2e;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;

final class ComposeEnvironment implements AutoCloseable {

    private static final List<String> BACKEND_SERVICES =
        List.of(
            "catalog-backend", "cart-backend", "orders-backend", "inventory-backend",
            "payments-backend", "notifications-backend"
        );
    private static final Duration COMMAND_TIMEOUT = Duration.ofMinutes(15);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    private final Path marketplaceRoot;
    private final String projectName;
    private final List<String> composeCommand;
    private final Map<String, String> environment;
    private final Map<String, Integer> apiPorts;
    private boolean started;
    private boolean logsCaptured;
    private String paymentCaptureOutcome = "CAPTURED";
    private String paymentRefundOutcome = "REFUNDED";

    private ComposeEnvironment(
        Path marketplaceRoot,
        String projectName,
        List<String> composeCommand,
        Map<String, String> environment,
        Map<String, Integer> apiPorts
    ) {
        this.marketplaceRoot = marketplaceRoot;
        this.projectName = projectName;
        this.composeCommand = composeCommand;
        this.environment = environment;
        this.apiPorts = apiPorts;
    }

    static ComposeEnvironment create() {
        Path root = Path.of(System.getProperty("marketplace.root", "..")).toAbsolutePath().normalize();
        Map<String, Integer> ports = allocatePorts();
        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("POSTGRES_PORT", ports.get("postgres").toString());
        environment.put("RABBITMQ_PORT", ports.get("rabbitmq").toString());
        environment.put("RABBITMQ_MANAGEMENT_PORT", ports.get("rabbitmq-management").toString());
        environment.put("KAFKA_PORT", ports.get("kafka").toString());
        environment.put("CATALOG_API_PORT", ports.get("catalog").toString());
        environment.put("CART_API_PORT", ports.get("cart").toString());
        environment.put("ORDERS_API_PORT", ports.get("orders").toString());
        environment.put("INVENTORY_API_PORT", ports.get("inventory").toString());
        environment.put("PAYMENTS_API_PORT", ports.get("payments").toString());
        environment.put("NOTIFICATIONS_API_PORT", ports.get("notifications").toString());
        environment.put("PAYMENT_SIMULATOR_OUTCOME", "CAPTURED");
        environment.put("PAYMENT_SIMULATOR_REFUND_OUTCOME", "REFUNDED");

        String projectName = "marketplace-e2e-" + UUID.randomUUID().toString().substring(0, 8);
        return new ComposeEnvironment(root, projectName, resolveComposeCommand(root), environment, ports);
    }

    void start() {
        System.out.printf(
            "Starting Compose project %s (catalog=%d, cart=%d, orders=%d, inventory=%d, payments=%d, notifications=%d)%n",
            projectName,
            port("catalog"),
            port("cart"),
            port("orders"),
            port("inventory"),
            port("payments"),
            port("notifications")
        );

        List<String> arguments = new ArrayList<>(List.of("up", "--detach", "--build", "--wait"));
        arguments.addAll(BACKEND_SERVICES);
        started = true;
        run(arguments, COMMAND_TIMEOUT, true);

        Awaitility.await("all marketplace backends to become healthy")
            .atMost(Duration.ofMinutes(3))
            .pollInterval(Duration.ofSeconds(2))
            .ignoreExceptions()
            .until(() -> BACKEND_SERVICES.stream().allMatch(this::isHealthy));
    }

    URI apiUri(String service, String path) {
        return URI.create("http://localhost:" + port(service) + path);
    }

    void restartPayments(String captureOutcome, String refundOutcome) {
        if (paymentCaptureOutcome.equals(captureOutcome) && paymentRefundOutcome.equals(refundOutcome)) {
            return;
        }
        environment.put("PAYMENT_SIMULATOR_OUTCOME", captureOutcome);
        environment.put("PAYMENT_SIMULATOR_REFUND_OUTCOME", refundOutcome);
        run(List.of("up", "--detach", "--no-deps", "--force-recreate", "--wait", "payments-backend"), Duration.ofMinutes(3), true);

        Awaitility.await("payments backend to become healthy")
            .atMost(Duration.ofMinutes(1))
            .pollInterval(Duration.ofSeconds(1))
            .until(() -> isHealthy("payments-backend"));

        paymentCaptureOutcome = captureOutcome;
        paymentRefundOutcome = refundOutcome;
    }

    void stopRabbitMq() {
        run(List.of("stop", "rabbitmq"), Duration.ofMinutes(1), true);
    }

    void startRabbitMq() {
        run(List.of("up", "--detach", "--no-deps", "--wait", "rabbitmq"), Duration.ofMinutes(2), true);
    }

    void captureLogs() {
        if (logsCaptured) {
            return;
        }
        Path output = marketplaceRoot.resolve("marketplace-e2e/target/compose.log");
        try {
            String logs = run(List.of("logs", "--no-color"), Duration.ofMinutes(2), false);
            if (logs.isBlank()) {
                return;
            }
            Files.createDirectories(output.getParent());
            Files.writeString(output, logs, StandardCharsets.UTF_8);
            logsCaptured = true;
            System.err.println("Compose logs written to " + output);
        } catch (RuntimeException | IOException exception) {
            System.err.println("Could not capture Compose logs: " + exception.getMessage());
        }
    }

    @Override
    public void close() {
        if (!started || Boolean.parseBoolean(System.getenv("E2E_KEEP_ENVIRONMENT"))) {
            if (started) {
                System.out.println("Keeping Compose project " + projectName);
            }
            return;
        }
        try {
            run(List.of("down", "--volumes", "--remove-orphans"), Duration.ofMinutes(3), true);
        } finally {
            started = false;
        }
    }

    private boolean isHealthy(String service) {
        String apiName = service.substring(0, service.indexOf('-'));
        try {
            HttpRequest request = HttpRequest.newBuilder(apiUri(apiName, "/actuator/health")).timeout(Duration.ofSeconds(2)).GET().build();
            return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private int port(String service) {
        return apiPorts.get(service);
    }

    private String run(List<String> arguments, Duration timeout, boolean printOutput) {
        List<String> command = new ArrayList<>(composeCommand);
        command.add("--project-name");
        command.add(projectName);
        command.add("--file");
        command.add(marketplaceRoot.resolve("compose.yaml").toString());
        command.addAll(arguments);
        return execute(command, marketplaceRoot, environment, timeout, printOutput);
    }

    private static List<String> resolveComposeCommand(Path root) {
        for (List<String> candidate : List.of(List.of("docker", "compose"), List.of("docker-compose"))) {
            List<String> command = new ArrayList<>(candidate);
            command.add("version");
            try {
                execute(command, root, Map.of(), Duration.ofSeconds(15), false);
                return candidate;
            } catch (RuntimeException ignored) {
                // Try the legacy executable when the Docker CLI plugin is unavailable.
            }
        }
        throw new IllegalStateException("Docker Compose is required (docker compose or docker-compose)");
    }

    private static String execute(
        List<String> command,
        Path workingDirectory,
        Map<String, String> environment,
        Duration timeout,
        boolean printOutput
    ) {
        try {
            Path outputFile = Files.createTempFile("marketplace-e2e-command-", ".log");
            ProcessBuilder processBuilder =
                new ProcessBuilder(command).directory(workingDirectory.toFile()).redirectErrorStream(true).redirectOutput(outputFile.toFile());
            processBuilder.environment().putAll(environment);
            try {
                Process process = processBuilder.start();
                if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    throw new IllegalStateException("Command timed out: " + String.join(" ", command));
                }
                String output = Files.readString(outputFile, StandardCharsets.UTF_8);
                if (printOutput && !output.isBlank()) {
                    System.out.print(output);
                }
                if (process.exitValue() != 0) {
                    throw new IllegalStateException(
                        "Command failed with exit code " + process.exitValue() + ": " + String.join(" ", command) + System.lineSeparator() + output);
                }
                return output;
            } finally {
                Files.deleteIfExists(outputFile);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not execute: " + String.join(" ", command), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running: " + String.join(" ", command), exception);
        }
    }

    private static Map<String, Integer> allocatePorts() {
        Map<String, Integer> ports = new LinkedHashMap<>();
        List<ServerSocket> reservations = new ArrayList<>();
        try {
            for (String name : List.of(
                "postgres",
                "rabbitmq",
                "rabbitmq-management",
                "kafka",
                "catalog",
                "cart",
                "orders",
                "inventory",
                "payments",
                "notifications"
            )) {
                ServerSocket socket = new ServerSocket(0);
                socket.setReuseAddress(false);
                reservations.add(socket);
                ports.put(name, socket.getLocalPort());
            }
            return ports;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not allocate E2E ports", exception);
        } finally {
            reservations.forEach(ComposeEnvironment::closeQuietly);
        }
    }

    private static void closeQuietly(ServerSocket socket) {
        try {
            socket.close();
        } catch (IOException exception) {
            // The operating system will reclaim the socket when this JVM exits.
        }
    }
}
