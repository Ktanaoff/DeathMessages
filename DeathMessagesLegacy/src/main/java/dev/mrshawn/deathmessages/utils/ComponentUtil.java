package dev.mrshawn.deathmessages.utils;

import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import dev.mrshawn.deathmessages.DeathMessages;
import dev.mrshawn.deathmessages.api.PlayerManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComponentUtil {

    private static final TextComponent[] EMPTY = new TextComponent[]{Component.empty(), Component.empty()};

    /*
        Process hover event string in message
        If found, add string to rawEvents list, then replace them to placeholder like %example% in msg
        If there are multiple event string in message, the message should be like `%hover_event_0%, ..., %hover_event_N%` after the replacing
     */
    public static String sortHoverEvents(String msg, List<String> rawEvents) {
        // If contains event string, process, otherwise return original msg directly
        if (msg.contains("[")) {
            int index = 0;
            Matcher matcher = Util.DM_HOVER_EVENT_PATTERN.matcher(msg);

            while (matcher.find()) {
                String group = matcher.group(1);
                String replacement = "%hover_event_" + index + "%";

                // Filter string like [aaa] or [aaa::]
                if (group.split("::").length <= 1) continue;

                // Added in raw Events list
                rawEvents.add(group);
                // Replace original message
                msg = msg.replace("[" + matcher.group(1) + "]", replacement);
                // Update index
                index++;
            }
        }

        return msg;
    }

    public static Component buildItemHover(Player player, ItemStack i, Component displayName) {
        // Early return, to prevent no component message sent to player caused by air hover
        if (MaterialUtil.isAir(i)) {
            return displayName;
        }

        HoverEvent<HoverEvent.ShowItem> showItem;
        String iNamespace = XMaterial.matchXMaterial(i.getType().name()).get().name().toLowerCase();

        // Eco item process
        if (DeathMessages.getHooks().ecoEnchantsEnabled && DeathMessages.getHooks().ecoExtension.isEcoEnchantsItem(i)) {
            i = DeathMessages.getHooks().ecoExtension.getEcoEnchantsItem(i, player);
        }

        // For <= 1.20.4
        ReadWriteNBT nbt = NBT.itemStackToNBT(i).getCompound("tag");
        showItem = i.hasItemMeta() && nbt != null && !nbt.toString().isEmpty()
                // Item with NBT
                ? HoverEvent.showItem(Key.key(iNamespace), i.getAmount(), BinaryTagHolder.binaryTagHolder(nbt.toString()))
                // Item without NBT (tag compound)
                : HoverEvent.showItem(Key.key(iNamespace), i.getAmount());

        return displayName.hoverEvent(showItem);
    }

    // TODO: Check whether needed
    /*
    public static Component buildEntityHover(Entity entity, Component name) {
        HoverEvent<HoverEvent.ShowEntity> showEntity;
        String iNamespace = XEntityType.of(entity).get().name().toLowerCase();

        showEntity = entity.getCustomName() != null
                ? HoverEvent.showEntity(Key.key(iNamespace), entity.getUniqueId(), name)
                : HoverEvent.showEntity(Key.key(iNamespace), entity.getUniqueId());

        return name.hoverEvent(showEntity);
    }
     */

    /*
        Process and build hover events from raw events list
        Only for playerDeath: pm, e, Only for EntityDeath: p, e, hasOwner
     */
    public static Component buildHoverEvents(
            String rawEvent,
            PlayerManager pm,
            Player p,
            Entity e,
            boolean hasOwner,
            boolean isPlayerDeath
    ) {
        rawEvent = rawEvent.replace("[", "").replace("]", "");
        String[] rawHover = rawEvent.split("::");
        TextComponent.Builder event = Component.text();

        // Append base message which has the hover text and events
        event.append(Util.convertFromLegacy(rawHover[0]));

        // Append hover text if exists
        if (rawHover.length >= 2 && !rawHover[1].isEmpty()) {
            HoverEvent<Component> showText = HoverEvent.showText(Util.convertFromLegacy(rawHover[1]));
            event.hoverEvent(showText);
        }

        // Append hover click events if exists
        if (rawHover.length == 4) {
            ClickEvent click = null;
            final String content = isPlayerDeath
                    ? Assets.playerDeathPlaceholders(rawHover[3], pm, e)
                    : Assets.entityDeathPlaceholders(rawHover[3], p, e, hasOwner);

            switch (rawHover[2]) {
                case "COPY_TO_CLIPBOARD":
                    click = ClickEvent.copyToClipboard(content);
                    break;
                case "OPEN_URL":
                    click = ClickEvent.openUrl(content);
                    break;
                case "RUN_COMMAND":
                    click = ClickEvent.runCommand("/" + content);
                    break;
                case "SUGGEST_COMMAND":
                    click = ClickEvent.suggestCommand("/" + content);
                    break;
                default:
                    DeathMessages.LOGGER.error("Unknown hover event action: {}", rawHover[2]);
                    break;
            }

            event.clickEvent(click);
        }

        return event.build();
    }

    public static Component getItemStackDisplayName(ItemStack i) {
        // Legacy method
        // RGB / Hex color of item stack display name will not display correctly
        return Util.convertFromLegacy(i.getItemMeta().getDisplayName());
    }

    public static void sendMessage(CommandSender sender, Component component) {
        DeathMessages.getInstance().adventure().sender(sender).sendMessage(component);
    }

    public static void sendMessage(Player player, Component component) {
        DeathMessages.getInstance().adventure().player(player).sendMessage(component);
    }

    public static void sendConsoleMessage(Component component) {
        Util.CONSOLE.sendMessage(component);
    }

    public static TextComponent[] empty() {
        return EMPTY.clone();
    }

    public static boolean isMessageEmpty(TextComponent[] components) {
        return Arrays.equals(components, EMPTY) || components[1].equals(Component.empty());
    }
}
