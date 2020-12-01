package ru.falseresync.banTrigger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BanTrigger implements ModInitializer {
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    public static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(3);
    public static final LinkedBlockingQueue<LogEntry> BLOCK_BREAKS = new LinkedBlockingQueue<>();
    public static final LinkedBlockingQueue<LogEntry> CONTAINER_OPENS = new LinkedBlockingQueue<>();

    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            EXECUTOR_SERVICE.submit(() -> {
                try {
                    BLOCK_BREAKS.put(new LogEntry(pos.getX(), pos.getY(), pos.getZ(), player.getUuid().toString()));
                } catch (InterruptedException ignored) {
                }
            });
        });

        Path basePath = Paths.get(FabricLoader.getInstance().getConfigDir().toString(), "ban-trigger");
        basePath = Paths.get(basePath.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(LocalDateTime.now()));
        basePath.toFile().mkdirs();

        Path blockBreaksPath = Paths.get(basePath.toString(), "block-breaks.csv");
        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            try {
                FileWriter writer = new FileWriter(blockBreaksPath.toFile(), true);
                List<LogEntry> beans = new ArrayList<>();
                BLOCK_BREAKS.drainTo(beans);
                for (LogEntry it : beans) {
                    writer.write(it.toCsvEntry());
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 20, 5, TimeUnit.SECONDS);

        Path containerOpensPath = Paths.get(basePath.toString(), "container-opens.csv");
        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            try {
                FileWriter writer = new FileWriter(containerOpensPath.toFile(), true);
                List<LogEntry> beans = new ArrayList<>();
                CONTAINER_OPENS.drainTo(beans);
                for (LogEntry it : beans) {
                    writer.write(it.toCsvEntry());
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 20, 5, TimeUnit.SECONDS);


        ServerLifecycleEvents.SERVER_STOPPED.register(minecraftServer -> {
            SCHEDULED_EXECUTOR_SERVICE.shutdown();
            try {
                System.out.println("BanTrigger is waiting for scheduled loggers to finish");
                SCHEDULED_EXECUTOR_SERVICE.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            EXECUTOR_SERVICE.shutdown();
        });
    }

    public static class LogEntry {
        private final String timestamp;
        private final int x;
        private final int y;
        private final int z;
        private final String uuid;

        public LogEntry(int x, int y, int z, String uuid) {
            this.timestamp = String.valueOf(Instant.now().getEpochSecond());
            this.x = x;
            this.y = y;
            this.z = z;
            this.uuid = uuid;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public String getUuid() {
            return uuid;
        }

        public String toCsvEntry() {
            return String.format("%s,%s,%s,%s,%s%n", getTimestamp(), getX(), getY(), getZ(), getUuid());
        }
    }
}
