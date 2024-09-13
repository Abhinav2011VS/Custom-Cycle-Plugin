package net.abhinav.ccp;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public final class CustomCyclePlugin extends JavaPlugin {

    private long dayLengthTicks;
    private long nightLengthTicks;
    private long cycleIntervalTicks;
    private boolean weatherSynchronization;
    private int sunriseTime;
    private int sunsetTime;
    private boolean notifyPlayers;
    private String notificationMessage;
    private boolean worldSpecificSettings;
    private double cycleSpeedMultiplier;

    @Override
    public void onEnable() {
        // Load configuration
        saveDefaultConfig();
        loadConfig();

        // Start the day/night cycle task
        startCycleTask();

        // Register commands
        getCommand("setcycle").setExecutor(new CycleCommandExecutor());
        getCommand("settime").setExecutor(new TimeCommandExecutor());
        getCommand("querytime").setExecutor(new QueryTimeCommandExecutor());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void startCycleTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    long time = world.getTime();
                    long cycleTime = (time + cycleIntervalTicks) % (dayLengthTicks + nightLengthTicks);

                    if (cycleTime < dayLengthTicks) {
                        world.setTime(cycleTime);
                    } else {
                        world.setTime(cycleTime - dayLengthTicks);
                    }

                    if (weatherSynchronization) {
                        synchronizeWeather(world);
                    }

                    if (notifyPlayers) {
                        for (Player player : world.getPlayers()) {
                            player.sendMessage(notificationMessage);
                        }
                    }

                    // Trigger events
                    checkForEvents(world, time);
                }
            }
        }.runTaskTimer(this, 0L, cycleIntervalTicks);
    }

    private void synchronizeWeather(World world) {
        long time = world.getTime();
        if (time >= sunriseTime && time < sunsetTime) {
            world.setStorm(false);
            world.setWeatherDuration(0);
        } else {
            world.setStorm(true);
            world.setWeatherDuration(1000);
        }
    }

    private void checkForEvents(World world, long time) {
        if (time % dayLengthTicks == sunriseTime) {
            Bukkit.getScheduler().runTask(this, () -> Bukkit.broadcastMessage("Sunrise event triggered!"));
        }
        if (time % dayLengthTicks == sunsetTime) {
            Bukkit.getScheduler().runTask(this, () -> Bukkit.broadcastMessage("Sunset event triggered!"));
        }
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        dayLengthTicks = config.getLong("day-length-ticks", 24000L);
        nightLengthTicks = config.getLong("night-length-ticks", 12000L);
        cycleIntervalTicks = config.getLong("cycle-interval-ticks", 600L);
        weatherSynchronization = config.getBoolean("weather-synchronization", true);
        sunriseTime = config.getInt("sunrise-time", 1000);
        sunsetTime = config.getInt("sunset-time", 12000);
        notifyPlayers = config.getBoolean("notify-players", true);
        notificationMessage = config.getString("notification-message", "The day/night cycle has been updated!");
        worldSpecificSettings = config.getBoolean("world-specific-settings", false);
        cycleSpeedMultiplier = config.getDouble("cycle-speed-multiplier", 1.0);
    }

    public class CycleCommandExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof ConsoleCommandSender) {
                if (args.length == 3) {
                    try {
                        dayLengthTicks = Long.parseLong(args[0]);
                        nightLengthTicks = Long.parseLong(args[1]);
                        cycleIntervalTicks = Long.parseLong(args[2]);

                        getConfig().set("day-length-ticks", dayLengthTicks);
                        getConfig().set("night-length-ticks", nightLengthTicks);
                        getConfig().set("cycle-interval-ticks", cycleIntervalTicks);
                        saveConfig();

                        sender.sendMessage("Cycle times updated.");
                        return true;
                    } catch (NumberFormatException e) {
                        sender.sendMessage("Invalid number format.");
                    }
                } else {
                    sender.sendMessage("Usage: /setcycle <day-length> <night-length> <interval>");
                }
                return true;
            }
            sender.sendMessage("Only console can execute this command.");
            return false;
        }
    }

    public class TimeCommandExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 1) {
                try {
                    long time = Long.parseLong(args[0]);
                    for (World world : Bukkit.getWorlds()) {
                        world.setTime(time);
                    }
                    sender.sendMessage("World time set to " + time);
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid time format.");
                }
            } else {
                sender.sendMessage("Usage: /settime <time>");
            }
            return false;
        }
    }

    public class QueryTimeCommandExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            for (World world : Bukkit.getWorlds()) {
                sender.sendMessage("Current world time in " + world.getName() + ": " + world.getTime());
            }
            return true;
        }
    }
}