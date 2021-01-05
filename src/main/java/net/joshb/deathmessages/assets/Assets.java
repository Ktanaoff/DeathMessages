package net.joshb.deathmessages.assets;

import de.tr7zw.changeme.nbtapi.NBTItem;
import me.clip.placeholderapi.PlaceholderAPI;
import net.joshb.deathmessages.DeathMessages;
import net.joshb.deathmessages.config.EntityDeathMessages;
import net.joshb.deathmessages.config.Messages;
import net.joshb.deathmessages.config.PlayerDeathMessages;
import net.joshb.deathmessages.config.Settings;
import net.joshb.deathmessages.manager.PlayerManager;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Assets {

    public static String formatMessage(String path){
        return ChatColor.translateAlternateColorCodes('&',
                Messages.getInstance().getConfig().getString(path)
                        .replaceAll("%prefix%",  Messages.getInstance().getConfig().getString("Prefix")));
    }

    public static List<String> formatMessage(List<String> list){
        List<String> newList = new ArrayList<>();
        for(String s : list){
            newList.add(ChatColor.translateAlternateColorCodes('&', s
                    .replaceAll("%prefix%",  Messages.getInstance().getConfig().getString("Prefix"))));
        }
        return newList;
    }

    public static boolean isClimable(Material material){
        return material.name().contains("LADDER") || material.name().contains("VINE") || material.equals(Material.SCAFFOLDING);
    }

    public static boolean isWeapon(ItemStack itemStack){
        return itemStack.getType().toString().contains("SHOVEL")
                || itemStack.getType().toString().contains("PICKAXE")
                || itemStack.getType().toString().contains("AXE")
                || itemStack.getType().toString().contains("HOE")
                || itemStack.getType().toString().contains("SWORD")
                || itemStack.getType().toString().contains("BOW");
    }

    static String lastColor = null;

    public static TextComponent deathMessage(PlayerManager pm, boolean gang) {
        lastColor = null;
        LivingEntity mob = (LivingEntity) pm.getLastEntityDamager();
        boolean hasWeapon;
        if (mob.getEquipment() == null){
            hasWeapon = false;
        } else if(!isWeapon(mob.getEquipment().getItemInMainHand())) {
            hasWeapon = false;
        } else if(pm.getLastDamage().equals(EntityDamageEvent.DamageCause.THORNS)){
            hasWeapon = false;
        } else {
            hasWeapon = true;
        }
        if(pm.getLastDamage().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)
                && pm.getLastExplosiveEntity() instanceof EnderCrystal){
            return get(gang, pm, mob, "End-Crystal");
        }
        if (hasWeapon) {
            if (pm.getLastDamage().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
                return getWeapon(gang, pm, mob);
            } else if (pm.getLastDamage().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
                return getProjectile(gang, pm, mob);
            } else {
                return null;
            }
        } else {
            for (EntityDamageEvent.DamageCause dc : EntityDamageEvent.DamageCause.values()) {
                if (pm.getLastDamage().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
                    return getProjectile(gang, pm, mob);
                }
                if (pm.getLastDamage().equals(dc)) {
                    return get(gang, pm, mob, getSimpleCause(dc));
                }
            }
            return null;
        }
    }


    public static TextComponent getNaturalDeath(PlayerManager pm, String damageCause) {
        Random random = new Random();
        List<String> msgs = getPlayerDeathMessages().getStringList("Natural-Cause." + damageCause);
        if (msgs.isEmpty()) return null;
        String msg = msgs.get(random.nextInt(msgs.size()));
        TextComponent tc = new TextComponent();
        String[] sec = msg.split("::");
        String firstSection;
        if (msg.contains("::")) {
            firstSection = sec[0];
        } else {
            firstSection = msg;
        }
        for (String splitMessage : firstSection.split(" ")) {
            if (splitMessage.contains("%block%") && pm.getLastEntityDamager() instanceof FallingBlock) {
                FallingBlock fb = (FallingBlock) pm.getLastEntityDamager();
                String mssa = Assets.colorize(splitMessage.replaceAll("%block%", convertString(fb.getBlockData().getMaterial().name())));
                tc.addExtra(mssa);
                lastColor = ChatColor.getLastColors(lastColor + mssa);
            } else {
                if (lastColor != null) {
                    String mssa = Assets.colorize(splitMessage);
                    tc.addExtra(Assets.colorize(playerDeathPlaceholders(lastColor + mssa, pm, null)) + " ");
                    lastColor = ChatColor.getLastColors(lastColor + mssa);
                } else {
                    tc.addExtra(Assets.colorize(playerDeathPlaceholders(splitMessage, pm, null)) + " ");
                    lastColor = ChatColor.getLastColors(splitMessage);
                }
            }
        }
        if (sec.length >= 2) {
            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(playerDeathPlaceholders(sec[1], pm, null))));
        }
        if (sec.length == 3) {
            if (sec[2].startsWith("COMMAND:")) {
                String cmd = sec[2].split(":")[1];
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + playerDeathPlaceholders(cmd, pm, null)));
            } else if (sec[2].startsWith("SUGGEST_COMMAND:")) {
                String cmd = sec[2].split(":")[1];
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + playerDeathPlaceholders(cmd, pm, null)));
            }
        }
        return tc;
    }

    public static TextComponent getWeapon(boolean gang, PlayerManager pm, LivingEntity mob) {
        Random random = new Random();
        List<String> msgs;
        if (gang) {
            msgs = getPlayerDeathMessages().getStringList("Mobs." +
                    mob.getType().getEntityClass().getSimpleName().toLowerCase() + ".Gang.Weapon");
        } else {
            msgs = getPlayerDeathMessages().getStringList("Mobs." +
                    mob.getType().getEntityClass().getSimpleName().toLowerCase() + ".Solo.Weapon");
        }
        if (msgs.isEmpty()) return null;
        String msg = msgs.get(random.nextInt(msgs.size()));
        TextComponent tc = new TextComponent();
        String[] sec = msg.split("::");
        String firstSection;
        if (msg.contains("::")) {
            firstSection = sec[0];
        } else {
            firstSection = msg;
        }
        for (String splitMessage : firstSection.split(" ")) {
            if (splitMessage.contains("%weapon%")) {
                ItemStack i = mob.getEquipment().getItemInMainHand();
                String displayName;
                if(!i.hasItemMeta() && !i.getItemMeta().hasDisplayName() || i.getItemMeta().getDisplayName().equals("")){
                    if(Settings.getInstance().getConfig().getBoolean("Disable-Weapon-Kill-With-No-Custom-Name")){
                        return get(gang, pm, mob, getSimpleCause(EntityDamageEvent.DamageCause.ENTITY_ATTACK));
                    }
                    displayName = Assets.convertString(i.getType().name());
                } else {
                    displayName = i.getItemMeta().getDisplayName();
                }
                String[] spl = splitMessage.split("%weapon%");
                if(spl.length != 0 && spl[0] != null && !spl[0].equals("")){
                    displayName = spl[0] + displayName;
                }
                if(spl.length != 0 && spl.length != 1 && spl[1] != null && !spl[1].equals("")){
                    displayName = displayName + spl[1];
                }
                displayName = Assets.colorize(displayName);
                TextComponent weaponComp = new TextComponent(displayName);
                BaseComponent[] hoverEventComponents = new BaseComponent[]{
                        new TextComponent(NBTItem.convertItemtoNBT(i).getCompound().toString())
                };
                weaponComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, hoverEventComponents));
                tc.addExtra(weaponComp);
            } else {
                if (lastColor != null) {
                    String mssa = Assets.colorize(splitMessage);
                    tc.addExtra(Assets.colorize(playerDeathPlaceholders(lastColor + mssa, pm, mob)) + " ");
                    lastColor = ChatColor.getLastColors(lastColor + mssa);
                } else {
                    tc.addExtra(Assets.colorize(playerDeathPlaceholders(splitMessage, pm, mob)) + " ");
                    lastColor = ChatColor.getLastColors(splitMessage);
                }
            }
        }
        if (sec.length >= 2) {
            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(playerDeathPlaceholders(sec[1], pm, mob))));
        }
        if (sec.length == 3) {
            if (sec[2].startsWith("COMMAND:")) {
                String cmd = sec[2].split(":")[1];
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + playerDeathPlaceholders(cmd, pm, mob)));
            } else if (sec[2].startsWith("SUGGEST_COMMAND:")) {
                String cmd = sec[2].split(":")[1];
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + playerDeathPlaceholders(cmd, pm, mob)));
            }
        }
        return tc;
    }

    public static TextComponent get(boolean gang, PlayerManager pm, LivingEntity mob, String damageCause) {
        Random random = new Random();
        List<String> msgs;
        if (gang) {
            msgs = getPlayerDeathMessages().getStringList("Mobs." +
                    mob.getType().getEntityClass().getSimpleName().toLowerCase() + ".Gang." + damageCause);
        } else {
            msgs = getPlayerDeathMessages().getStringList("Mobs." +
                    mob.getType().getEntityClass().getSimpleName().toLowerCase() + ".Solo." + damageCause);
        }

        if (msgs.isEmpty()) {
            if(Settings.getInstance().getConfig().getBoolean("Default-Natural-Death-Not-Defined")){
                return getNaturalDeath(pm, damageCause);
            }
            if(Settings.getInstance().getConfig().getBoolean("Default-Melee-Last-Damage-Not-Defined")){
                return get(gang, pm, mob, getSimpleCause(EntityDamageEvent.DamageCause.ENTITY_ATTACK));
            }
            return null;
        }
        String msg = msgs.get(random.nextInt(msgs.size()));
        TextComponent tc = new TextComponent();
        String[] sec = msg.split("::");
        String firstSection;
        if (msg.contains("::")) {
            firstSection = sec[0];
        } else {
            firstSection = msg;
        }
        for (String splitMessage : firstSection.split(" ")) {
            if (lastColor != null) {
                String mssa = Assets.colorize(splitMessage);
                tc.addExtra(Assets.colorize(playerDeathPlaceholders(lastColor + mssa, pm, mob)) + " ");
                lastColor = ChatColor.getLastColors(lastColor + mssa);
            } else {
                tc.addExtra(Assets.colorize(playerDeathPlaceholders(splitMessage, pm, mob)) + " ");
                lastColor = ChatColor.getLastColors(splitMessage);
            }
        }
        if (sec.length >= 2) {
            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(playerDeathPlaceholders(sec[1], pm, mob))));
        }
        if (sec.length == 3) {
            if (sec[2].startsWith("COMMAND:")) {
                String cmd = sec[2].split(":")[1];
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + playerDeathPlaceholders(cmd, pm, mob)));
            } else if (sec[2].startsWith("SUGGEST_COMMAND:")) {
                String cmd = sec[2].split(":")[1];
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + playerDeathPlaceholders(cmd, pm, mob)));
            }
        }
        return tc;
    }

    public static TextComponent getProjectile(boolean gang, PlayerManager pm, LivingEntity mob) {
        Random random = new Random();
        List<String> msgs;
        if (gang) {
            msgs = getPlayerDeathMessages().getStringList("Mobs." +
                    mob.getType().getEntityClass().getSimpleName().toLowerCase() + ".Gang.Projectile");
        } else {
            msgs = getPlayerDeathMessages().getStringList("Mobs." +
                    mob.getType().getEntityClass().getSimpleName().toLowerCase() + ".Solo.Projectile");
        }
        String msg = msgs.get(random.nextInt(msgs.size()));
        TextComponent tc = new TextComponent();
        String[] sec = msg.split("::");
        String firstSection;
        if (msg.contains("::")) {
            firstSection = sec[0];
        } else {
            firstSection = msg;
        }
        for (String splitMessage : firstSection.split(" ")) {
            if (splitMessage.equalsIgnoreCase("%weapon%")) {
                ItemStack i = mob.getEquipment().getItemInMainHand();
                String displayName;
                if(!i.hasItemMeta() && !i.getItemMeta().hasDisplayName() || i.getItemMeta().getDisplayName().equals("")){
                    if(Settings.getInstance().getConfig().getBoolean("Disable-Weapon-Kill-With-No-Custom-Name")){
                        return get(gang, pm, mob, getSimpleCause(EntityDamageEvent.DamageCause.ENTITY_ATTACK));
                    }
                    displayName = Assets.convertString(i.getType().name());
                } else {
                    displayName = i.getItemMeta().getDisplayName();
                }
                String[] spl = splitMessage.split("%weapon%");
                if(spl.length != 0 && spl[0] != null && !spl[0].equals("")){
                    displayName = spl[0] + displayName;
                }
                if(spl.length != 0 && spl.length != 1 && spl[1] != null && !spl[1].equals("")){
                    displayName = displayName + spl[1];
                }
                displayName = Assets.colorize(displayName);
                TextComponent weaponComp = new TextComponent(displayName);
                BaseComponent[] hoverEventComponents = new BaseComponent[]{
                        new TextComponent(NBTItem.convertItemtoNBT(i).getCompound().toString())
                };
                weaponComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, hoverEventComponents));
                tc.addExtra(weaponComp);
            } else {
                if (lastColor != null) {
                    String mssa = Assets.colorize(splitMessage);
                    tc.addExtra(Assets.colorize(playerDeathPlaceholders(lastColor + mssa, pm, mob)) + " ");
                    lastColor = ChatColor.getLastColors(lastColor + mssa);
                } else {
                    tc.addExtra(Assets.colorize(playerDeathPlaceholders(splitMessage, pm, mob)) + " ");
                    lastColor = ChatColor.getLastColors(splitMessage);
                }
            }
        }
        if (sec.length >= 2) {
            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(playerDeathPlaceholders(sec[1], pm, mob))));
        }
        if (sec.length == 3) {
            if (sec[2].startsWith("COMMAND:")) {
                String cmd = sec[2].split(":")[1];
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + playerDeathPlaceholders(cmd, pm, mob)));
            } else if (sec[2].startsWith("SUGGEST_COMMAND:")) {
                String cmd = sec[2].split(":")[1];
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + playerDeathPlaceholders(cmd, pm, mob)));
            }
        }
        return tc;
    }

    public static TextComponent getTamable(PlayerManager pm, Tameable tameable) {
        Random random = new Random();
        List<String> msgs = getEntityDeathMessages().getStringList("Tamable");
        if (msgs.isEmpty()) return null;
        String msg = msgs.get(random.nextInt(msgs.size()));
        TextComponent tc = new TextComponent();
        String[] sec = msg.split("::");
        String firstSection;
        if (msg.contains("::")) {
            firstSection = sec[0];
        } else {
            firstSection = msg;
        }
        for (String splitMessage : firstSection.split(" ")) {
            if (lastColor != null) {
                String mssa = Assets.colorize(splitMessage);
                tc.addExtra(Assets.colorize(entityDeathPlaceholders(lastColor + mssa, pm, tameable)) + " ");
                lastColor = ChatColor.getLastColors(lastColor + mssa);
            } else {
                tc.addExtra(Assets.colorize(entityDeathPlaceholders(splitMessage, pm, tameable)) + " ");
                lastColor = ChatColor.getLastColors(splitMessage);
            }
        }
        if (sec.length >= 2) {
            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(entityDeathPlaceholders(sec[1], pm, tameable))));
        }
        if (sec.length == 3) {
            if (sec[2].startsWith("COMMAND:")) {
                String cmd = sec[2].split(":")[1];
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + entityDeathPlaceholders(cmd, pm, tameable)));
            } else if (sec[2].startsWith("SUGGEST_COMMAND:")) {
                String cmd = sec[2].split(":")[1];
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + entityDeathPlaceholders(cmd, pm, tameable)));
            }
        }
        return tc;
    }

    public static String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String entityDeathPlaceholders(String msg, PlayerManager killer, Tameable tameable){
        msg = ChatColor.translateAlternateColorCodes('&', msg
                .replaceAll("%killer%", killer.getName())
                .replaceAll("%biome%", tameable.getLocation().getBlock().getBiome().name())
                .replaceAll("%world%", tameable.getLocation().getWorld().getName())
                .replaceAll("%tamable%", tameable.getType().getEntityClass().getSimpleName())
                .replaceAll("%tamable_displayname%", tameable.getName())
                .replaceAll("%owner%", tameable.getOwner().getName())
                .replaceAll("%x%", String.valueOf(tameable.getLocation().getBlock().getX()))
                .replaceAll("%y%", String.valueOf(tameable.getLocation().getBlock().getY()))
                .replaceAll("%z%", String.valueOf(tameable.getLocation().getBlock().getZ())));
        if (DeathMessages.plugin.placeholderAPIEnabled) {
            msg = PlaceholderAPI.setPlaceholders(killer.getPlayer(), msg);
        }
        return msg;
    }

    public static String playerDeathPlaceholders(String msg, PlayerManager pm, LivingEntity mob) {
        if (mob == null) {
            msg = ChatColor.translateAlternateColorCodes('&', msg
                    .replaceAll("%player%", pm.getName())
                    .replaceAll("%player_display%", pm.getDisplayName())
                    .replaceAll("%biome%", pm.getLastLocation().getBlock().getBiome().name())
                    .replaceAll("%world%", pm.getLastLocation().getWorld().getName())
                    .replaceAll("%x%", String.valueOf(pm.getLastLocation().getBlock().getX()))
                    .replaceAll("%y%", String.valueOf(pm.getLastLocation().getBlock().getY()))
                    .replaceAll("%z%", String.valueOf(pm.getLastLocation().getBlock().getZ())));
            if (DeathMessages.plugin.placeholderAPIEnabled) {
                msg = PlaceholderAPI.setPlaceholders(pm.getPlayer(), msg);
            }
            return msg;
        } else {
            msg = ChatColor.translateAlternateColorCodes('&', msg
                    .replaceAll("%player%", pm.getName())
                    .replaceAll("%player_display%", pm.getDisplayName())
                    .replaceAll("%killer%", mob.getName())
                    .replaceAll("%biome%", pm.getLastLocation().getBlock().getBiome().name())
                    .replaceAll("%world%", pm.getLastLocation().getWorld().getName())
                    .replaceAll("%x%", String.valueOf(pm.getLastLocation().getBlock().getX()))
                    .replaceAll("%y%", String.valueOf(pm.getLastLocation().getBlock().getY()))
                    .replaceAll("%z%", String.valueOf(pm.getLastLocation().getBlock().getZ())));
            if (DeathMessages.plugin.placeholderAPIEnabled) {
                msg = PlaceholderAPI.setPlaceholders(pm.getPlayer(), msg);
            }
            return msg;
        }
    }

    public static String convertString(String string) {
        string = string.replaceAll("_", " ").toLowerCase();
        String[] spl = string.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spl.length; i++) {
            if (i == spl.length - 1) {
                sb.append(StringUtils.capitalize(spl[i]));
            } else {
                sb.append(StringUtils.capitalize(spl[i]) + " ");
            }
        }
        return sb.toString();
    }

    public static String getSimpleCause(EntityDamageEvent.DamageCause damageCause) {
        switch (damageCause) {
            case CONTACT:
                return "Contact";
            case ENTITY_ATTACK:
                return "Melee";
            case PROJECTILE:
                return "Projectile";
            case SUFFOCATION:
                return "Suffocation";
            case FALL:
                return "Fall";
            case FIRE:
                return "Fire";
            case FIRE_TICK:
                return "Fire-Tick";
            case MELTING:
                return "Melting";
            case LAVA:
                return "Lava";
            case DROWNING:
                return "Drowning";
            case BLOCK_EXPLOSION:
            case ENTITY_EXPLOSION:
                return "Explosion";
            case VOID:
                return "Void";
            case LIGHTNING:
                return "Lightning";
            case SUICIDE:
                return "Suicide";
            case STARVATION:
                return "Starvation";
            case POISON:
                return "Poison";
            case MAGIC:
                return "Magic";
            case WITHER:
                return "Wither";
            case FALLING_BLOCK:
                return "Falling-Block";
            case THORNS:
                return "Thorns";
            case DRAGON_BREATH:
                return "Dragon-Breath";
            case CUSTOM:
                return "Custom";
            case FLY_INTO_WALL:
                return "Fly-Into-Wall";
            case HOT_FLOOR:
                return "Hot-Floor";
            case CRAMMING:
                return "Cramming";
            case DRYOUT:
                return "Dryout";
            default:
                return "Unknown";
        }
    }

    public static EntityDamageEvent.DamageCause getDamageCause(String cause) {
        switch (cause) {
            case "Contact":
                return EntityDamageEvent.DamageCause.CONTACT;
            case "Attack":
                return EntityDamageEvent.DamageCause.ENTITY_ATTACK;
            case "Projectile":
                return EntityDamageEvent.DamageCause.PROJECTILE;
            case "Suffocation":
                return EntityDamageEvent.DamageCause.SUFFOCATION;
            case "Fall":
                return EntityDamageEvent.DamageCause.FALL;
            case "Fire":
                return EntityDamageEvent.DamageCause.FIRE;
            case "Fire-Tick":
                return EntityDamageEvent.DamageCause.FIRE_TICK;
            case "Melting":
                return EntityDamageEvent.DamageCause.MELTING;
            case "Lava":
                return EntityDamageEvent.DamageCause.LAVA;
            case "Drowning":
                return EntityDamageEvent.DamageCause.DROWNING;
            case "Explosion-Self":
            case "Explosion-Other":
                return EntityDamageEvent.DamageCause.BLOCK_EXPLOSION;
            case "Void":
                return EntityDamageEvent.DamageCause.VOID;
            case "Lightning":
                return EntityDamageEvent.DamageCause.LIGHTNING;
            case "Suicide":
                return EntityDamageEvent.DamageCause.SUICIDE;
            case "Starvation":
                return EntityDamageEvent.DamageCause.STARVATION;
            case "Poison":
                return EntityDamageEvent.DamageCause.POISON;
            case "Magic":
                return EntityDamageEvent.DamageCause.MAGIC;
            case "Wither":
                return EntityDamageEvent.DamageCause.WITHER;
            case "Falling-Block":
                return EntityDamageEvent.DamageCause.FALLING_BLOCK;
            case "Dragon-Breath":
                return EntityDamageEvent.DamageCause.DRAGON_BREATH;
            case "Fly-Into-Wall":
                return EntityDamageEvent.DamageCause.LIGHTNING;
            case "Hot-Floor":
                return EntityDamageEvent.DamageCause.SUICIDE;
            case "Cramming":
                return EntityDamageEvent.DamageCause.STARVATION;
            case "Dryout":
                return EntityDamageEvent.DamageCause.POISON;
            default:
                return EntityDamageEvent.DamageCause.CUSTOM;
        }
    }

    public static FileConfiguration getPlayerDeathMessages() {
        return PlayerDeathMessages.getInstance().getConfig();
    }

    public static FileConfiguration getEntityDeathMessages() {
        return EntityDeathMessages.getInstance().getConfig();
    }
}
