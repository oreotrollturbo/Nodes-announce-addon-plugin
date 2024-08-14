package org.oreo.nation_announcements.commands;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.oreo.nation_announcements.Nation_announcements;
import phonon.nodes.Nodes;
import phonon.nodes.objects.Nation;
import phonon.nodes.objects.Resident;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class AnnounceCommand implements CommandExecutor, TabCompleter {

    private Map<String, List<String>> nationPlayersMap = new HashMap<>();
    private final File saveFile;
    private final Gson gson = new Gson();
    private final Nation_announcements plugin;

    public AnnounceCommand(JavaPlugin plugin, Nation_announcements plugin1) {
        // Use the plugin's data folder to store the JSON file
        this.saveFile = new File(plugin.getDataFolder(), "nationPlayersMap.json");
        this.plugin = plugin1;

        loadNationPlayersMap();
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "You can't use this command in the console!");
            return false;
        }

        Player sender = (Player) commandSender;
        Resident resident = Nodes.INSTANCE.getResident(sender);

        if (resident == null || resident.getNation() == null) {
            sender.sendMessage(ChatColor.RED + "You are not part of any nation!");
            return false;
        }

        Nation nation = resident.getNation();

        // Check if the sender is the nation leader
        if (!nation.getCapital().getLeader().getName().equals(resident.getName())) {
            sender.sendMessage(ChatColor.RED + "You aren't a nation leader.");
            return false;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "You need to provide a subcommand (announce/add/remove/clear/list).");
            return false;
        }

        String subCommand = args[0].toLowerCase();
        String nationName = nation.getName(); // Use the nation name as the key

        switch (subCommand) {
            case "announce":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Please provide a message to announce.");
                    return false;
                }

                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                for (String playerName : nationPlayersMap.getOrDefault(nationName, Collections.emptyList())) {
                    Player player = sender.getServer().getPlayer(playerName);
                    if (player != null && player.isOnline()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1f);
                        sendActionBar(player, message);
                    }
                }
                break;

            case "add":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Please provide a player name to add.");
                    return false;
                }

                String playerNameToAdd = args[1];
                Player playerToAdd = commandSender.getServer().getPlayer(playerNameToAdd);

                if (playerToAdd == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found or not online.");
                    return false;
                }

                List<String> playersListToAdd = nationPlayersMap.computeIfAbsent(nationName, k -> new ArrayList<>());

                if (playersListToAdd.contains(playerNameToAdd)) {
                    sender.sendMessage(ChatColor.RED + "Player is already added to the list.");
                } else {
                    playersListToAdd.add(playerNameToAdd);
                    saveNationPlayersMap(); // Save after adding
                    sender.sendMessage(ChatColor.GREEN + playerToAdd.getName() + " has been added to your nation's announcement list.");
                }
                break;

            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Please provide a player name to remove.");
                    return false;
                }

                String playerNameToRemove = args[1];
                List<String> playersListToRemove = nationPlayersMap.get(nationName);

                if (playersListToRemove == null || !playersListToRemove.contains(playerNameToRemove)) {
                    sender.sendMessage(ChatColor.RED + "Player is not in the list.");
                } else {
                    playersListToRemove.remove(playerNameToRemove);
                    saveNationPlayersMap(); // Save after removing
                    sender.sendMessage(ChatColor.GREEN + playerNameToRemove + " has been removed from your nation's announcement list.");
                }
                break;

            case "clear":
                // Clear the entire list for this nation
                List<String> playersListToClear = nationPlayersMap.get(nationName);

                if (playersListToClear != null) {
                    playersListToClear.clear();
                    saveNationPlayersMap(); // Save after clearing
                    sender.sendMessage(ChatColor.GREEN + "All players have been removed from your nation's announcement list.");
                } else {
                    sender.sendMessage(ChatColor.RED + "There are no players in the list to clear.");
                }
                break;

            case "list":
                // List all players currently added
                List<String> playersList = nationPlayersMap.get(nationName);

                if (playersList == null || playersList.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "No players are currently added to your nation's announcement list.");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "Players in your nation's announcement list:");
                    for (String playerName : playersList) {
                        sender.sendMessage(ChatColor.AQUA + "- " + playerName);
                    }
                }
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use 'announce', 'add', 'remove', 'clear', or 'list'.");
                return false;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(commandSender instanceof Player)) {
            return completions;
        }

        Player sender = (Player) commandSender;
        Resident resident = Nodes.INSTANCE.getResident(sender);

        if (resident == null || resident.getNation() == null) {
            return completions;
        }

        Nation nation = resident.getNation();
        String nationName = nation.getName(); // Use the nation name as the key

        if (!nation.getCapital().getLeader().getName().equals(resident.getName())) {
            return completions;
        }

        if (args.length == 1) {
            completions.add("announce");
            completions.add("add");
            completions.add("remove");
            completions.add("clear");
            completions.add("list");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add")) {
                // Autocomplete player names within the nation who are online
                for (Player player : nation.getPlayersOnline()) {
                    completions.add(player.getName());
                }
            } else if (args[0].equalsIgnoreCase("remove")) {
                // Autocomplete only players that are already in the nation's list
                List<String> playersList = nationPlayersMap.get(nationName);
                if (playersList != null) {
                    completions.addAll(playersList);
                }
            }
        }

        return completions;
    }

    // Method to send a message above the player's hotbar
    public void sendActionBar(Player player, String message) {
        String formattedMessage = ChatColor.AQUA + "" + ChatColor.BOLD + message.toUpperCase();
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(formattedMessage));
    }

    private void loadNationPlayersMap() {
        if (!saveFile.exists()) {
            initializeSaveFile(); // Create the file with default content if it does not exist
        }

        try (FileReader reader = new FileReader(saveFile)) {
            Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();
            Map<String, List<String>> loadedMap = gson.fromJson(reader, mapType);

            if (loadedMap != null) {
                nationPlayersMap = loadedMap;
            }
        } catch (IOException e) {
            plugin.getLogger().info("File not found creating save file");
            initializeSaveFile();
            // Handle file read errors, such as file not being found or inaccessible
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            // Handle JSON syntax errors, possibly reset file or log error
        }
    }

    // Save the nationPlayersMap to file
    private void saveNationPlayersMap() {
        try (FileWriter writer = new FileWriter(saveFile)) {
            gson.toJson(nationPlayersMap, writer);
            plugin.getLogger().info("File saved successfully");
        } catch (IOException e) {
            plugin.getLogger().info("Save file does not exist");
            e.printStackTrace();
        }
    }

    private void initializeSaveFile() {
        if (!saveFile.exists()) {
            try {
                if (saveFile.createNewFile()) {
                    plugin.getLogger().info("Created new file at: " + saveFile.getAbsolutePath());
                    try (FileWriter writer = new FileWriter(saveFile)) {
                        writer.write("{}"); // Write an empty JSON object to the file
                        loadNationPlayersMap();
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().info("Cant create save file");
                e.printStackTrace();
                // Handle file creation errors
            }
        } else {
            plugin.getLogger().info("Save file found");
        }
    }
}
