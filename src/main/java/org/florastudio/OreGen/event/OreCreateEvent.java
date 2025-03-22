package org.florastudio.OreGen.event;

import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.scheduler.BukkitTask;
import org.florastudio.OreGen.OreGen;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;

public class OreCreateEvent implements Listener {
    private static final Map<Material, String> triggerBlocksList = new HashMap<>();

    public static Map<String, Map<String, Double>> regenBlocks = new HashMap<>();

    public OreCreateEvent() {
        ConfigurationSection triggerBlocks = OreGen.getInstance().getConfig().getConfigurationSection("TriggerBlocks");
        if (triggerBlocks != null) {
            for (String key : triggerBlocks.getKeys(false)) {
                Material block = Material.getMaterial(key.toUpperCase());
                if (block != null) {
                    String tier = triggerBlocks.getString(key);
                    triggerBlocksList.put(block, tier);
                } else {
                    Bukkit.getLogger().warning("설정 오류: " + key + "는 존재하지 않는 블록입니다.");
                }
            }
        }


        ConfigurationSection oreGens = OreGen.getInstance().getConfig().getConfigurationSection("OreGens");

        if (oreGens != null) {
            for (String tier : oreGens.getKeys(false)) {
                Map<String, Double> genBlocks = new HashMap<>();
                ConfigurationSection vanillaSection = oreGens.getConfigurationSection(tier + ".VANILLA");
                ConfigurationSection itemAdderSection = oreGens.getConfigurationSection(tier + ".ITEMADDER");
                Double totalchance = 0.00D;
                if (vanillaSection != null) {
                    for (String blocks : vanillaSection.getKeys(false)) {
                        Double chance = vanillaSection.getDouble(blocks);
                        totalchance += chance;
                        genBlocks.put(blocks, chance);
                        Bukkit.getLogger().info(tier + "등록" + blocks + "확률: " + chance);
                    }
                }
                if (itemAdderSection != null) {
                    for (String blocks : itemAdderSection.getKeys(false)) {
                        if (CustomStack.getInstance(blocks) != null) {
                            Double chance = itemAdderSection.getDouble(blocks);
                            totalchance += chance;
                            genBlocks.put(blocks, chance);
                            Bukkit.getLogger().info(tier + "등록" + blocks + "확률: " +chance);
                        } else {
                            Bukkit.getLogger().warning(tier + "해당" + blocks + " 은 없는 id입니다. 대소문자 구분 해주세요.");
                        }
                    }
                }
                if (Math.abs(totalchance - 100.00) > 0.01) {
                    Bukkit.getLogger().warning(tier + " 등급 OreGen 확률 총합이 100이 아닙니다. 현재 총합: " + totalchance);
                    Bukkit.getLogger().warning("OreGen 플러그인 비활성화...");
                    Bukkit.getPluginManager().disablePlugin(OreGen.getInstance());
                }
                regenBlocks.put(tier, genBlocks);
            }
        }

    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block brokenBlock = e.getBlock();
        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(brokenBlock);
        if (customBlock != null) {
            customBlock.remove();
            brokenBlock.setType(Material.STONE);
            Bukkit.getScheduler().runTaskLater(OreGen.getInstance(), () -> {
                brokenBlock.setType(Material.AIR);
            }, 1L);
        }
    }







    @EventHandler
    public void onGen(BlockFromToEvent e) {
        Block b = e.getBlock();
        Block to = e.getToBlock();
        if (b.getType() == Material.WATER) {
            int x = 0, y = 0, z = 0;
            if (e.getFace() == BlockFace.DOWN)
                y = -1;
            if (e.getFace() == BlockFace.UP)
                y = 1;
            if (e.getFace() == BlockFace.NORTH)
                z = -1;
            if (e.getFace() == BlockFace.SOUTH)
                z = 1;
            if (e.getFace() == BlockFace.EAST)
                x = 1;
            if (e.getFace() == BlockFace.WEST)
                x = -1;
            Block b2 = to.getLocation().clone().add(x, y, z).getBlock();
            if (triggerBlocksList.containsKey(b2.getType())) {
                String grade = triggerBlocksList.get(b2.getType());
                if (!regenBlocks.containsKey(grade)) return;
                e.setCancelled(true);
                String selectedMaterial = selectCustomMaterial(regenBlocks.get(grade));
                if (selectedMaterial == null) return;

                if(CustomStack.getInstance(selectedMaterial) != null) {
                    CustomBlock customBlock = CustomBlock.getInstance(selectedMaterial);
                    if (customBlock != null) {
                        customBlock.place(to.getLocation());
                    } 
                } else {
                    Material material = Material.getMaterial(selectedMaterial);
                    if (material != null) {
                        to.setType(material);
                    }
                }

            }
        }
    }

    public String selectCustomMaterial(Map<String, Double> regenblocks) {
        if (regenblocks == null || regenblocks.isEmpty()) return null;
        NavigableMap<Double, String> map = new TreeMap<>();
        Random random = new Random();
        double totalWeight = 0;

        for (Map.Entry<String, Double> entry : regenblocks.entrySet()) {
            totalWeight += entry.getValue();
            map.put(totalWeight, entry.getKey());
        }
        double rand = random.nextDouble() * totalWeight;
        return map.ceilingEntry(rand).getValue();
    }
}

