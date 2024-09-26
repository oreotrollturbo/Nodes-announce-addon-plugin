package org.oreo.nation_announcements.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import phonon.nodes.Nodes;
import phonon.nodes.objects.Resident;


public class IncomeSummary implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Only players can use this command");
            return true; // Exit early if the sender is not a player
        }

        Player sender = (Player) commandSender;
        Resident resident = Nodes.INSTANCE.getResident(sender);

        if (resident.getTown() == null){
            commandSender.sendMessage(ChatColor.RED + "You need to be part of a town to use this command");
            return true;
        }

        // Make sure the player is officer or above
        if (!resident.getTown().getOfficers().contains(resident)) {
            assert resident.getTown().getLeader() != null;
            if (!resident.getTown().getLeader().equals(resident)) {
                sender.sendMessage(ChatColor.RED + "Only officers and above can use this command");
                return true; // Exit early if the player is not an officer or leader
            }
        }

        ItemStack[] inventory = resident.getTown().getIncome().getInventory().getStorageContents();

        sender.sendMessage(ChatColor.AQUA + "------Income summary------");

        Material prevStackMaterial = null;
        int prevStackAmount = 0;

        for (ItemStack stack : inventory) {

            if (stack != null && stack.getType() != Material.AIR) { // Check for valid stack

                if (prevStackMaterial == null || prevStackMaterial != stack.getType()) {
                    // If we're moving to a new item type, display the previous one
                    if (prevStackMaterial != null) {
                        sender.sendMessage(ChatColor.AQUA + prevStackMaterial.toString() + " - " + prevStackAmount);
                    }
                    // Start counting the new item type
                    prevStackMaterial = stack.getType();
                    prevStackAmount = stack.getAmount();
                } else {
                    // If it's the same item type, just add the amounts
                    prevStackAmount += stack.getAmount();
                }
            }
        }

        // Output the last item stack summary if it exists
        if (prevStackMaterial != null) {
            sender.sendMessage(ChatColor.AQUA + prevStackMaterial.toString() + " - " + prevStackAmount);
        } else {
            sender.sendMessage(ChatColor.AQUA + "No items found in the income inventory.");
        }

        return true;
    }
}
