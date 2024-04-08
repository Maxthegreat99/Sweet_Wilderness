package sweet.wilderness.entity.entities.capybara;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import sweet.wilderness.entity.ModEntities;
import sweet.wilderness.huh.EntityUtils;

import java.util.Iterator;
import java.util.List;

public class CapybaraEntity extends TameableEntity implements GeoEntity {
    private AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final float MOVEMENT_SPEED = .5201f;
    private float ticksUnderwater;

    public CapybaraEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super((EntityType<? extends TameableEntity>) entityType, world);
    }

    public static DefaultAttributeContainer.Builder setAttributes() {
        return AnimalEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 15)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, MOVEMENT_SPEED);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F, false));
        this.goalSelector.add(3, new AnimalMateGoal(this, 1.0));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 0.75f, 1));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return ModEntities.CAPYBARA.create(world);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::predicate));
    }

    private <E extends GeoAnimatable> PlayState predicate(AnimationState<E> animationState) {
        if (animationState.isMoving()) {
            animationState.getController().
                    setAnimation(RawAnimation.begin().then("move", Animation.LoopType.LOOP));
        }
        else{
            animationState.getController().
                    setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
        }

        return PlayState.CONTINUE;
    }

    protected Vector3f getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return new Vector3f(0.0F, dimensions.height * 0.8888889F, 0.0f);
    }

    @Override
    public void tick() {
        if (!isUnderWater()) this.ticksUnderwater = 0.0F;
        else ++this.ticksUnderwater;

        if (!this.getWorld().isClient && this.ticksUnderwater >= 60.0F)
            this.removeAllPassengers();

        super.tick();

        this.checkBlockCollision();
        List<Entity> list = this.getWorld().getOtherEntities(this, this.getBoundingBox().expand(0.20000000298023224, -0.009999999776482582, 0.20000000298023224), EntityPredicates.canBePushedBy(this));

        if (list.isEmpty()) return;

        Iterator var10 = list.iterator();

        while (true) {
            Entity entity;

            do
            {
                if (!var10.hasNext()) return;

                entity = (Entity) var10.next();
            } while (entity.hasPassenger(this));

            if (!this.getWorld().isClient && this.getPassengerList().isEmpty()
                && !entity.hasVehicle() && entity instanceof LivingEntity
                && !(entity instanceof PlayerEntity))
            {
                entity.startRiding(this);
            } else this.pushAwayFrom(entity);

        }


    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }


    @Override
    public EntityView method_48926() {
        return this.getWorld();
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isOf(Items.APPLE);
    }

    public ActionResult interactMob(PlayerEntity player, Hand hand) {

        var actionRseult = EntityUtils.tameOrHealHandle(this, player, hand, Items.GLOW_BERRIES, 3);

        return (actionRseult == null) ? super.interactMob(player, hand) : actionRseult;
    }

    private boolean isUnderWater() {
        Box box = this.getBoundingBox();
        double d = box.maxY + 0.001;
        int i = MathHelper.floor(box.minX);
        int j = MathHelper.ceil(box.maxX);
        int k = MathHelper.floor(box.maxY);
        int l = MathHelper.ceil(d);
        int m = MathHelper.floor(box.minZ);
        int n = MathHelper.ceil(box.maxZ);

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for(int o = i; o < j; ++o) {
            for(int p = k; p < l; ++p) {
                for(int q = m; q < n; ++q) {
                    mutable.set(o, p, q);
                    FluidState fluidState = this.getWorld().getFluidState(mutable);

                    if (fluidState.isIn(FluidTags.WATER)
                        && d < (double)((float)mutable.getY() + fluidState.getHeight(this.getWorld(), mutable)))
                        return true;

                }
            }
        }

        return false;
    }
}
