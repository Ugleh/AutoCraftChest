package com.ugleh.autocraftchest.command;

import com.ugleh.autocraftchest.AutoCraftChest;
import com.ugleh.autocraftchest.util.XSound;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandAutoCraftChest implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (args.length == 0) return notEnoughArgs(sender);
        else return enoughArgs(sender, args);
    }

    private boolean enoughArgs(CommandSender sender, String[] args) {
        if (args[0].equalsIgnoreCase("give")) {
            return giveCommand(sender, args);
        }else if (args[0].equalsIgnoreCase("reload")) {
            return reloadCOmmand(sender, args);
        }
        return true;
    }

    private boolean reloadCOmmand(CommandSender sender, String[] args) {
        if(!sender.hasPermission("autocraftchest.reload")) return notAllowed(sender);
        playNotificationSound(sender, true);
        AutoCraftChest.getACCConfig().reload();
        AutoCraftChest.getStorage().reload();
        AutoCraftChest.getLanguage().reload();
        sender.sendMessage(AutoCraftChest.getLanguageNode("command.reload-successful"));
        return true;
    }

    private boolean giveCommand(CommandSender sender, String[] args) {
        if(!sender.hasPermission("autocraftchest.give")) return notAllowed(sender);
        if (args.length <= 1) return notEnoughArgs(sender);
        Player givePlayer = Bukkit.getPlayer(args[1]);
        if (givePlayer == null) return playerUnknown(sender);
        int amount = 1;
        if (args.length > 2) {
            if(!tryParseInt(args[2])) return notEnoughArgs(sender);
            amount = Integer.parseInt(args[2]);
        }

        giveItemToPlayer(sender, givePlayer, amount);
        return true;
    }

    private boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void giveItemToPlayer(CommandSender sender, Player givePlayer, int amount) {
        ItemStack accItemStack = getInstance().getAccItemStack().clone();
        accItemStack.setAmount(amount);
        givePlayer.getInventory().addItem(accItemStack);
        playNotificationSound(sender, true);
        sender.sendMessage(String.format(AutoCraftChest.getLanguageNode("command.gave-successful"), amount, givePlayer.getName()));
        if(!(sender instanceof Player)) {
            String consoleName = AutoCraftChest.getLanguageNode("command.given-console-name");
            givePlayer.sendMessage(String.format(AutoCraftChest.getLanguageNode("command.given-successful"), amount, consoleName));
        }else if(!((Player) sender).getUniqueId().equals(givePlayer.getUniqueId())) {
            givePlayer.sendMessage(String.format(AutoCraftChest.getLanguageNode("command.given-successful"), amount, sender.getName()));
        }
    }

    private boolean playerUnknown(CommandSender sender) {
        playNotificationSound(sender, false);
        sender.sendMessage(AutoCraftChest.getLanguageNode("command.player-unknown"));
        return true;
    }

    private boolean notAllowed(CommandSender sender) {
        playNotificationSound(sender, false);
        sender.sendMessage(AutoCraftChest.getLanguageNode("chat.no-permission-command"));
        return true;
    }

    private boolean notEnoughArgs(CommandSender sender) {
        playNotificationSound(sender, false);
        sender.sendMessage(AutoCraftChest.getLanguageNode("command.invalid-usage"));
        if(sender.hasPermission("autocraftchest.give"))
            sender.sendMessage( "/autocraftchest give [Player] <amount>");
        return false;
    }

    private AutoCraftChest getInstance() {
        return AutoCraftChest.getInstance();
    }


    private void playNotificationSound(CommandSender sender, boolean positive) {
        if(!(sender instanceof Player))return;
        if(!AutoCraftChest.getACCConfig().isSoundsEnabled()) return;
        Player player = (Player) sender;
        if(positive) {
            player.playSound(player.getLocation(), XSound.ENTITY_EXPERIENCE_ORB_PICKUP.parseSound(), 1.0f, 1.0f);
        }else {
            player.playSound(player.getLocation(), XSound.BLOCK_ANVIL_LAND.parseSound(), 0.2f, 1f);
        }
    }
}
