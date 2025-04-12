package org.florastudio.OreGen.command;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.florastudio.OreGen.OreGen;

public class Command implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            if (player.isOp()) {
                player.sendMessage(" §e/oregen reload");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (player.hasPermission("exafk.reload")) {
                OreGen.getInstance().reloadPlugin();
                player.sendMessage(" §a플러그인 리로드 완료");
            }
        } else {
            player.sendMessage("알 수 없는 명령어입니다.");
        }
        return true;
    }

}

