package org.florastudio.OreGen.event;

import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.florastudio.OreGen.OreGen;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;

import java.math.BigDecimal;

public class OreCreateEvent implements Listener {
    private static final Map<String, String> triggerBlocksList = new HashMap<>();


    public static Map<String, Map<String, Double>> regenBlocks = new HashMap<>();

    public OreCreateEvent() {
        ConfigurationSection triggerBlocks = OreGen.getInstance().getConfig().getConfigurationSection("TriggerBlocks");
        if (triggerBlocks != null) {
            for (String key : triggerBlocks.getKeys(false)) {
                String tier = triggerBlocks.getString(key);

                Material material = Material.matchMaterial(key.toUpperCase());
                if (material != null) {
                    triggerBlocksList.put(material.name(), tier);
                    Bukkit.getLogger().info(tier + "등급 생성기: " + material.name() + " (바닐라)");
                    continue;
                }

                if (CustomBlock.getInstance(key) != null) {
                    triggerBlocksList.put(key, tier);
                    Bukkit.getLogger().info(tier + "등급 생성기: " + key + " (아이템에더)");
                } else {
                    Bukkit.getLogger().warning("설정 오류: '" + key + "' 는 존재하지 않는 바닐라 또는 ItemsAdder 블록입니다.");
                }
            }
        }




        ConfigurationSection oreGens = OreGen.getInstance().getConfig().getConfigurationSection("OreGens");

        if (oreGens != null) {
            for (String tier : oreGens.getKeys(false)) {
                Map<String, Double> genBlocks = new HashMap<>();
                ConfigurationSection vanillaSection = oreGens.getConfigurationSection(tier + ".VANILLA");
                ConfigurationSection itemAdderSection = oreGens.getConfigurationSection(tier + ".ITEMADDER");
                BigDecimal totalChance = BigDecimal.ZERO;

                if (vanillaSection != null) {
                    for (String blocks : vanillaSection.getKeys(false)) {
                        BigDecimal chance = BigDecimal.valueOf(vanillaSection.getDouble(blocks));
                        totalChance = totalChance.add(chance);
                        genBlocks.put(blocks, chance.doubleValue());
                        Bukkit.getLogger().info(tier + "등록" + blocks + "확률: " + chance);
                    }
                }

                if (itemAdderSection != null) {
                    for (String blocks : itemAdderSection.getKeys(false)) {
                        if (CustomStack.getInstance(blocks) != null) {
                            BigDecimal chance = BigDecimal.valueOf(itemAdderSection.getDouble(blocks));
                            totalChance = totalChance.add(chance);
                            genBlocks.put(blocks, chance.doubleValue());
                            Bukkit.getLogger().info(tier + "등록" + blocks + "확률: " + chance);
                        } else {
                            Bukkit.getLogger().warning(tier + "해당" + blocks + " 은 없는 id입니다. 대소문자 구분 해주세요.");
                        }
                    }
                }

                // 총합이 100과 가까운지 확인 (오차 0.01 이하 허용)
                if (totalChance.subtract(BigDecimal.valueOf(100.00)).abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
                    Bukkit.getLogger().warning(tier + " 등급 OreGen 확률 총합이 100이 아닙니다. 현재 총합: " + totalChance);
                    unregister();
                }
                regenBlocks.put(tier, genBlocks);
            }
        }

    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        CustomBlock cb = CustomBlock.byAlreadyPlaced(block);
        if (cb != null) {
            event.setCancelled(true);
            cb.playBreakSound();
            cb.playBreakParticles();

            List<ItemStack> drops = cb.getLoot(player.getInventory().getItemInMainHand(), true);
            for (ItemStack item : drops) {
                block.getWorld().dropItemNaturally(block.getLocation(), item);
            }
            cb.remove();
            block.setType(Material.STONE);
            block.setType(Material.AIR);
            block.getState().update(true, true);
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

            String triggerKey = null;
            CustomBlock cb = CustomBlock.byAlreadyPlaced(b2);
            if (cb != null) {
                triggerKey = cb.getNamespacedID();
            } else {
                // 2. 바닐라 블럭일 경우
                triggerKey = b2.getType().name();
            }


            if (triggerBlocksList.containsKey(triggerKey)) {
                String grade = triggerBlocksList.get(triggerKey);
                if (!regenBlocks.containsKey(grade)) return;
                e.setCancelled(true);
                String selectedMaterial = selectCustomMaterial(regenBlocks.get(grade));
                if (selectedMaterial == null) return;

                if(CustomStack.getInstance(selectedMaterial) != null) {
                    CustomBlock customBlock = CustomBlock.getInstance(selectedMaterial);
                    if (customBlock != null) {
                        customBlock.place(to.getLocation());
                        to.getState().update(true, true);
                    }
                } else {
                    Material material = Material.getMaterial(selectedMaterial);
                    if (material != null) {
                        to.setType(material);
                        to.getState().update(true, true);
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

    public void unregister() {
        BlockBreakEvent.getHandlerList().unregister(this);
        BlockFromToEvent.getHandlerList().unregister(this);
        Bukkit.getLogger().info("OreCreateEvent 리스너가 성공적으로 언레지스터되었습니다.");
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, OreGen.getInstance());
        Bukkit.getLogger().info("OreCreateEvent 리스너가 다시 등록되었습니다.");
    }
}

