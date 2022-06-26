package me.duncanruns.pocketsand.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    @Shadow
    public abstract void sendMessage(Text message);

    @Shadow
    @Final
    public MinecraftServer server;

    @Shadow
    public abstract ServerWorld getWorld();

    @Shadow
    public abstract void playSound(SoundEvent event, SoundCategory category, float volume, float pitch);

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile, @Nullable PlayerPublicKey publicKey) {
        super(world, pos, yaw, gameProfile, publicKey);
    }

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void pocketSandMixin(boolean entireStack, CallbackInfoReturnable<Boolean> info) {
        if (!entireStack) {
            ItemStack itemStack = getMainHandStack();
            if (itemStack.getItem().equals(Items.RED_SAND) && !itemStack.hasNbt()) {
                itemStack.decrement(1);
                goPocketSand();
                info.setReturnValue(true);
            }
        }
    }


    private void goPocketSand() {
        sendMessage(Text.literal("POCKET SAND!"), true);

        final ServerWorld serverWorld = getWorld();

        Vec3d effectPos = getEyePos();
        final Vec3d diff = Vec3d.fromPolar(getPitch(), getYaw()).multiply(0.5);


        for (int i = 0; i < 10; i++) {
            effectPos = effectPos.add(diff);
            if (i == 5)
                world.playSound(null, effectPos.x, effectPos.y, effectPos.z, Blocks.SAND.getDefaultState().getSoundGroup().getPlaceSound(), SoundCategory.PLAYERS, 1f, 1f, random.nextLong());
            ((ServerWorld) world).spawnParticles(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.RED_SAND.getDefaultState()), effectPos.x, effectPos.y, effectPos.z, i / 2, i / 10.0, i / 10.0, i / 10.0, 0);
            for (Entity entity : serverWorld.getOtherEntities(this, new Box(effectPos.subtract(1, 10, 1), effectPos.add(1, 1, 1)))) {
                if (entity instanceof LivingEntity && entity.getEyePos().squaredDistanceTo(effectPos) < 1) {
                    ((LivingEntity) entity).addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100), this);
                    ((LivingEntity) entity).addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100), this);
                }
            }
        }
        playSound(Blocks.SAND.getDefaultState().getSoundGroup().getPlaceSound(), 1.0f, 1.0f);
    }
}
