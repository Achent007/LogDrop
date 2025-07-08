package be.achent.logdrop.Utils;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemHoverUtil {

    public static String getHover(ItemStack stack) {
        StringBuilder hover = new StringBuilder();

        if (stack == null) return "";

        ItemMeta meta = stack.getItemMeta();

        if (meta instanceof BlockStateMeta) {
            BlockStateMeta bsm = (BlockStateMeta) meta;
            if (bsm.getBlockState() instanceof ShulkerBox) {
                ShulkerBox box = (ShulkerBox) bsm.getBlockState();
                hover.append("§7Contenu de la shulker:\n");
                for (ItemStack item : box.getInventory().getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                                ? item.getItemMeta().getDisplayName()
                                : item.getType().name().toLowerCase().replace("_", " ");
                        hover.append("§e• x").append(item.getAmount()).append(" ").append(name).append("\n");
                    }
                }
                return hover.toString().trim();
            }
        }

        if (meta != null) {
            hover.append("§7").append(stack.getType().name());
            if (meta.hasDisplayName()) {
                hover.append("\n§fNom: ").append(meta.getDisplayName());
            }
            if (meta.hasLore()) {
                hover.append("\n§fLore:");
                for (String loreLine : meta.getLore()) {
                    hover.append("\n§7").append(loreLine);
                }
            }
        }

        return hover.toString().trim();
    }
}