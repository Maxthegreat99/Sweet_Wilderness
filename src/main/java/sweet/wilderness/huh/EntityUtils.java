package sweet.wilderness.huh;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class EntityUtils {

    public static void RemoveEntity(World world, Entity entity){
        entity.discard();
    }

    @Nullable
    public static ActionResult tameOrHealHandle(TameableEntity ent, PlayerEntity player,
                                                Hand hand, Item tamingItem, int tameBound) {
        ItemStack itemStack = player.getStackInHand(hand);
        Item item = itemStack.getItem();

        if (ent.getWorld().isClient) {
            boolean shouldConsume = ent.isOwner(player)
                    || ent.isTamed()
                    || itemStack.isOf(tamingItem)
                    && !ent.isTamed();
            return shouldConsume ? ActionResult.CONSUME : ActionResult.PASS;
        }

        // serverside

        if (ent.isTamed()
                && ent.isBreedingItem(itemStack)
                && ent.getHealth() < ent.getMaxHealth())
        {
            if (!player.getAbilities().creativeMode)
                itemStack.decrement(1);

            ent.heal((float) item.getFoodComponent().getHunger());
            return ActionResult.SUCCESS;

        }
        else if (itemStack.isOf(tamingItem)) {
            if (!player.getAbilities().creativeMode)
                itemStack.decrement(1);

            if (ent.getWorld().random.nextInt(tameBound) == 0) {
                ent.setOwner(player);
                ent.getNavigation().stop();
                ent.getWorld().sendEntityStatus(ent, (byte) 7);
            }
            else
                ent.getWorld().sendEntityStatus(ent, (byte) 6);

            return ActionResult.SUCCESS;
        }

        return null;
    }

}
