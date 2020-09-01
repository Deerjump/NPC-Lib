package io.github.deerjump.npclib.v1_16_R2;

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
import net.minecraft.server.v1_16_R2.Entity;
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


   public static <Entity extends NpcBase> void setDefaultAttributes(EntityTypes<Entity> type){
      try {
         final Field modifiers = Field.class.getDeclaredField("modifiers");
         modifiers.setAccessible(true);
         final Field field = AttributeDefaults.class.getDeclaredField("b");
         modifiers.setInt(field, modifiers.getInt(field) & ~Modifier.FINAL);
         field.setAccessible(true);
         Map<EntityTypes<?>, AttributeProvider> attributes = new HashMap<>((Map<EntityTypes<?>, AttributeProvider>)field.get(null));
         attributes.put(type, EntityInsentient.p().a());
         field.set(null, attributes);
         field.setAccessible(false);
      } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
         e.printStackTrace();
      }

   }

   public static <Entity extends NpcBase> EntityTypes<Entity> register(EntityTypes.b<Entity> entity, String name, EntityTypes<?> model) {
      
      final EntityTypes<Entity> current = (EntityTypes<Entity>) ENTITY_TYPE.get(new MinecraftKey("custom", name));
      if(current != ENTITY_TYPE.get(null)){
         System.out.println(current + " already registered!");
         reloadEntities(current);
         return current; 
      }

      System.out.println("Registering new type for:" + name);
      EntityTypes<Entity> type = ENTITY_TYPE.a(
         ENTITY_TYPE.a(model),
         ResourceKey.a(IRegistry.l,  new MinecraftKey("custom", name)),
         new EntityTypes<Entity>(entity, model.e(), true, model.b(), model.c(), model.d(), ImmutableSet.of(),
            model.l(), model.getChunkRange(), model.getUpdateInterval()), Lifecycle.stable()
      );
      setDefaultAttributes(type);
      reloadEntities(type);
      return type;
   }

   @Deprecated
   private static <Entity extends NpcBase> void reloadEntities(EntityTypes<Entity> entityType){
      System.out.println("Reloading: " + entityType);
      Bukkit.getWorlds().forEach(world ->{
         world.getEntities().forEach(entity ->{
            net.minecraft.server.v1_16_R2.Entity nmsEntity = ((CraftEntity)entity).getHandle();
            net.minecraft.server.v1_16_R2.Entity typeModel = entityType.a(((CraftWorld)world).getHandle());

            if(nmsEntity.getClass().getSimpleName().equals(typeModel.getClass().getSimpleName())){
               Entity newEntity = spawn(entityType, entity.getLocation());
               NBTTagCompound nbt = new NBTTagCompound();
               nmsEntity.save(nbt);
               newEntity.load(nbt);
               entity.remove();
            }
         });
         
      });
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

   protected CraftEntity bukkitEntity;

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