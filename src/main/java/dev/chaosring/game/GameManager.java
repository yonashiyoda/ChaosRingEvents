package dev.chaosring.game;

import dev.chaosring.anomaly.AnomalyManager;
import dev.chaosring.config.Settings;
import dev.chaosring.ring.RingManager;
import dev.chaosring.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class GameManager {

    private static final long RESET_DELAY_TICKS = 200L;

    private final Plugin plugin;
    private final Settings settings;
    private final RingManager ring;
    private final AnomalyManager anomalies;

    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Set<UUID> alive = new HashSet<>();
    private final Set<UUID> aliveView = Collections.unmodifiableSet(alive);

    private GameState state = GameState.IDLE;
    private World world;
    private BukkitTask countdownTask;
    private BukkitTask resetTask;
    private int countdownLeft;

    public GameManager(Plugin plugin, Settings settings, RingManager ring, AnomalyManager anomalies) {
        this.plugin = plugin;
        this.settings = settings;
        this.ring = ring;
        this.anomalies = anomalies;
    }

    public GameState state() {
        return state;
    }

    public boolean isAlive(UUID id) {
        return state == GameState.RUNNING && alive.contains(id);
    }

    public boolean isEliminated(UUID id) {
        return (state == GameState.RUNNING || state == GameState.ENDED)
                && participants.contains(id) && !alive.contains(id);
    }

    public int aliveCount() {
        return alive.size();
    }

    public int participantCount() {
        return participants.size();
    }

    public Location spectatorSpawn() {
        World target = world != null ? world : Bukkit.getWorlds().get(0);
        int x = (int) settings.centerX();
        int z = (int) settings.centerZ();
        return new Location(target, settings.centerX(), target.getHighestBlockYAt(x, z) + 15, settings.centerZ());
    }

    public void join(Player player) {
        if (state != GameState.IDLE) {
            player.sendMessage(Msg.of("<red>Şu an katılım kapalı."));
            return;
        }
        if (!participants.add(player.getUniqueId())) {
            player.sendMessage(Msg.of("<red>Zaten katıldın."));
            return;
        }
        tell(Msg.of("<yellow>" + player.getName() + " katıldı <gray>(" + participants.size() + ")"));
    }

    public void leave(Player player) {
        UUID id = player.getUniqueId();
        if (state == GameState.RUNNING && alive.contains(id)) {
            eliminate(player, true);
            return;
        }
        if (state == GameState.IDLE || state == GameState.COUNTDOWN) {
            if (participants.remove(id)) {
                player.sendMessage(Msg.of("<gray>Oyundan ayrıldın."));
            }
        }
    }

    public void handleQuit(Player player) {
        UUID id = player.getUniqueId();
        if (state == GameState.RUNNING && alive.contains(id)) {
            eliminate(player, false);
        } else if (state == GameState.IDLE || state == GameState.COUNTDOWN) {
            participants.remove(id);
        }
    }

    public void start(CommandSender sender) {
        if (state != GameState.IDLE) {
            sender.sendMessage(Msg.of("<red>Oyun zaten çalışıyor."));
            return;
        }
        World target = Bukkit.getWorld(settings.worldName());
        if (target == null) {
            sender.sendMessage(Msg.of("<red>Dünya bulunamadı: " + settings.worldName()));
            return;
        }
        participants.removeIf(id -> Bukkit.getPlayer(id) == null);
        if (participants.size() < settings.minPlayers()) {
            sender.sendMessage(Msg.of("<red>En az " + settings.minPlayers() + " oyuncu gerekli."));
            return;
        }

        world = target;
        state = GameState.COUNTDOWN;
        countdownLeft = settings.countdownSeconds();
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (countdownLeft <= 0) {
                    cancel();
                    countdownTask = null;
                    begin();
                    return;
                }
                if (countdownLeft <= 5 || countdownLeft % 5 == 0) {
                    tell(Msg.of("<yellow>Oyun <white>" + countdownLeft + "<yellow> saniye sonra başlıyor"));
                }
                countdownLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void stop() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (resetTask != null) {
            resetTask.cancel();
            resetTask = null;
        }
        ring.stop();
        anomalies.stop();
        reset();
    }

    private void begin() {
        participants.removeIf(id -> Bukkit.getPlayer(id) == null);
        if (participants.size() < settings.minPlayers()) {
            tell(Msg.of("<red>Yeterli oyuncu kalmadı, oyun iptal edildi."));
            reset();
            return;
        }

        alive.addAll(participants);
        anomalies.start(world, aliveView);
        ring.start(world);

        double half = settings.initialSize() / 2 * 0.8;
        for (UUID id : participants) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                continue;
            }
            prepare(player);
            scatter(player, half);
        }
        state = GameState.RUNNING;
        tell(Msg.of("<green>Oyun başladı! Son hayatta kalan kazanır."));
    }

    private void prepare(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            player.setHealth(maxHealth.getValue());
        }
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    }

    private void scatter(Player player, double half) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int bx = (int) Math.floor(settings.centerX() + rnd.nextDouble(-half, half));
        int bz = (int) Math.floor(settings.centerZ() + rnd.nextDouble(-half, half));
        World target = world;
        target.getChunkAtAsync(bx >> 4, bz >> 4).thenAccept(chunk ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    int y = target.getHighestBlockYAt(bx, bz) + 1;
                    player.teleportAsync(new Location(target, bx + 0.5, y, bz + 0.5));
                }));
    }

    public void eliminate(Player player, boolean spectateNow) {
        if (!alive.remove(player.getUniqueId())) {
            return;
        }
        anomalies.release(player);
        tell(Msg.of("<red>" + player.getName() + " elendi! <gray>Kalan: <white>" + alive.size()));
        if (spectateNow && player.isOnline()) {
            player.setGameMode(GameMode.SPECTATOR);
        }
        if (state == GameState.RUNNING && alive.size() <= 1) {
            finish();
        }
    }

    private void finish() {
        state = GameState.ENDED;

        Component headline;
        if (alive.isEmpty()) {
            headline = Msg.raw("<gray>Kazanan yok");
        } else {
            String name = Objects.requireNonNullElse(
                    Bukkit.getOfflinePlayer(alive.iterator().next()).getName(), "Bilinmeyen");
            headline = Msg.raw("<gold><bold>" + name + " kazandı!");
        }
        Title title = Title.title(headline, Component.empty());
        for (UUID id : participants) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.showTitle(title);
                player.sendMessage(headline);
            }
        }

        ring.stop();
        anomalies.stop();
        resetTask = Bukkit.getScheduler().runTaskLater(plugin, this::reset, RESET_DELAY_TICKS);
    }

    private void reset() {
        World spawnWorld = world;
        for (UUID id : participants) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                continue;
            }
            player.setGameMode(GameMode.SURVIVAL);
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            if (spawnWorld != null) {
                player.teleportAsync(spawnWorld.getSpawnLocation());
            }
        }
        participants.clear();
        alive.clear();
        resetTask = null;
        world = null;
        state = GameState.IDLE;
    }

    private void tell(Component message) {
        for (UUID id : participants) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }
}
