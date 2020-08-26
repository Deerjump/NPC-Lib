package io.github.deerjump.playernpcs;

import static net.minecraft.server.v1_16_R2.IRegistry.ENTITY_TYPE;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Lifecycle;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.event.entity.CreatureSpawnEvent;
import net.minecraft.server.v1_16_R2.AttributeDefaults;
import net.minecraft.server.v1_16_R2.AttributeProvider;
import net.minecraft.server.v1_16_R2.BlockPosition;
import net.minecraft.server.v1_16_R2.ChatComponentText;
import net.minecraft.server.v1_16_R2.DataWatcherObject;
import net.minecraft.server.v1_16_R2.DataWatcherRegistry;
import net.minecraft.server.v1_16_R2.EntityCreature;
import net.minecraft.server.v1_16_R2.EntityInsentient;
import net.minecraft.server.v1_16_R2.EntityPose;
import net.minecraft.server.v1_16_R2.EntityTypes;
import net.minecraft.server.v1_16_R2.IChatBaseComponent;
import net.minecraft.server.v1_16_R2.IRegistry;
import net.minecraft.server.v1_16_R2.MinecraftKey;
import net.minecraft.server.v1_16_R2.NBTTagCompound;
import net.minecraft.server.v1_16_R2.Packet;
import net.minecraft.server.v1_16_R2.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_16_R2.PlayerConnection;
import net.minecraft.server.v1_16_R2.ResourceKey;
import net.minecraft.server.v1_16_R2.World;

//This is just here to suppress the warning in the static init
@SuppressWarnings("unchecked")
public class NpcBase extends EntityCreature {
   private static final Map<EntityTypes<?>, AttributeProvider> DEFAULT_ATTRIBUTES;
   protected static final int ID_PLAYER = ENTITY_TYPE.a(EntityTypes.PLAYER);

   // Entity
   protected static DataWatcherObject<Byte> EFFECTS = DataWatcherRegistry.a.a(0);
   protected static DataWatcherObject<Integer> AIR = DataWatcherRegistry.b.a(1);
   protected static DataWatcherObject<Optional<IChatBaseComponent>> CUSTOM_NAME = DataWatcherRegistry.f.a(2);
   protected static DataWatcherObject<Boolean> CUSTOM_NAME_VISIBLE = DataWatcherRegistry.i.a(3);
   protected static DataWatcherObject<Boolean> IS_SILENT = DataWatcherRegistry.i.a(4);
   protected static DataWatcherObject<Boolean> NO_GRAVITY = DataWatcherRegistry.i.a(5);
   protected static DataWatcherObject<EntityPose> POSE = DataWatcherRegistry.s.a(6);

   // EntityLiving
   protected static DataWatcherObject<Byte> HAND_STATE = DataWatcherRegistry.a.a(7);
   protected static DataWatcherObject<Float> HEALTH = DataWatcherRegistry.c.a(8);
   protected static DataWatcherObject<Integer> POTION_EFFECT_COLOR = DataWatcherRegistry.b.a(9);
   protected static DataWatcherObject<Boolean> IS_POTION_AMBIENT = DataWatcherRegistry.i.a(10);
   protected static DataWatcherObject<Integer> ARROWS = DataWatcherRegistry.b.a(11);
   protected static DataWatcherObject<Integer> ABSORBTION_HEALTH = DataWatcherRegistry.b.a(12);
   protected static DataWatcherObject<Optional<BlockPosition>> UNKNOWN = DataWatcherRegistry.m.a(13);
   
   // EntityInsentient
   protected static DataWatcherObject<Byte> INSENTIENT = null;
   protected CraftEntity bukkitEntity;
   static {
      try {
         final Field modifiers = Field.class.getDeclaredField("modifiers");
         modifiers.setAccessible(true);
         final Field field = AttributeDefaults.class.getDeclaredField("b");
         modifiers.setInt(field, modifiers.getInt(field) & ~Modifier.FINAL);
         field.setAccessible(true);
         DEFAULT_ATTRIBUTES = new HashMap<>((Map<EntityTypes<?>, AttributeProvider>) field.get(null));
         field.set(null, DEFAULT_ATTRIBUTES);
      } catch (Throwable reason) {
         throw new RuntimeException(reason);
      }
   }

   public static <Entity extends NpcBase> EntityTypes<Entity> register(EntityTypes.b<Entity> entity, String name, EntityTypes<?> model) {
      //
      EntityTypes<Entity> type = ENTITY_TYPE.a(
         ENTITY_TYPE.a(model),
         ResourceKey.a(IRegistry.l, MinecraftKey.a(name)),
         new EntityTypes<Entity>(entity, model.e(), true, model.b(), model.c(), model.d(), ImmutableSet.of(),
            model.l(), model.getChunkRange(), model.getUpdateInterval()), Lifecycle.experimental()
      );
      DEFAULT_ATTRIBUTES.put(type, DEFAULT_ATTRIBUTES.get(model));
      DEFAULT_ATTRIBUTES.put(type, EntityInsentient.p().a());
      return type;
   }

   @Deprecated
   public static void unregister(EntityTypes<? extends NpcBase> entity){

   }
   
   @Override
   public CraftEntity getBukkitEntity() {
      if(this.bukkitEntity == null)
         this.bukkitEntity = new BaseNpc(this.world.getServer(), this);
      
      return this.bukkitEntity;
   }

   public static <Entity extends NpcBase> Entity spawn(EntityTypes<Entity> type, Location location) {
      final Entity entity = type.a(((CraftWorld) location.getWorld()).getHandle());
      entity.setPosition(location.getX(), location.getY(), location.getZ());
      entity.setYawPitch(location.getYaw(), location.getPitch());
      entity.world.addEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
      return entity;
   }

   protected NpcBase(EntityTypes<? extends NpcBase> type, World world) {
      super(type, world);
      
      setPersistent();
   }

   @Override
   protected void initDatawatcher() {

         // From EntityLiving
         this.datawatcher.register(HAND_STATE, (byte) 0);
         this.datawatcher.register(POTION_EFFECT_COLOR, 0);
         this.datawatcher.register(IS_POTION_AMBIENT, false);
         this.datawatcher.register(ARROWS, 0);
         this.datawatcher.register(ABSORBTION_HEALTH, 0);
         this.datawatcher.register(HEALTH, 1.0F);
         this.datawatcher.register(UNKNOWN, Optional.empty());
      
         // From EntityInsentient
         if (ENTITY_TYPE.a(getEntityType()) != ID_PLAYER) {
            INSENTIENT = DataWatcherRegistry.a.a(14);
            this.datawatcher.register(INSENTIENT, (byte)0);
         }
   }

   @Override
   @OverridingMethodsMustInvokeSuper
   public void saveData(NBTTagCompound nbttagcompound) {
      super.saveData(nbttagcompound);
      
   }

   @Override
   @OverridingMethodsMustInvokeSuper
   public void loadData(NBTTagCompound nbttagcompound) {
      super.loadData(nbttagcompound); 
      
   }

   @Override
   public String getName(){
      if(getCustomName() != null)
         return getCustomName().getText();
      return "";
   }

   public void setName(String name) {
      setCustomName(new ChatComponentText(name));
   }

   @Override
   public void setCustomName(IChatBaseComponent name) {
      super.setCustomName(name);
   }

   @Override public Packet<?> P() {         
      return new PacketPlayOutSpawnEntityLiving(this);
   }

   protected void sendPackets(Packet<?>...packets){
      Bukkit.getOnlinePlayers().forEach(player -> {
         PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
         for(Packet<?> packet : packets){
            connection.sendPacket(packet);
         }
      });
   }
}