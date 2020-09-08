package io.github.deerjump.npclib.v1_16_R2;

import java.io.InputStreamReader;
import java.net.URL;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;

import net.minecraft.server.v1_16_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftEntity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.server.v1_16_R2.PacketPlayOutPlayerInfo.EnumPlayerInfoAction;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;

public class HumanBase extends NpcBase {

   // From EntityHuman
   protected static final DataWatcherObject<Float> EXTRA_HEARTS = DataWatcherRegistry.c.a(14);
   protected static final DataWatcherObject<Integer> SCORE = DataWatcherRegistry.b.a(15);
   protected static final DataWatcherObject<Byte> SKIN_PARTS = DataWatcherRegistry.a.a(16);
   protected static final DataWatcherObject<Byte> MAIN_HAND = DataWatcherRegistry.a.a(17);
   protected static final DataWatcherObject<NBTTagCompound> LEFT_SHOULDER_ENTITY = DataWatcherRegistry.p.a(18);
   protected static final DataWatcherObject<NBTTagCompound> RIGHT_SHOULDER_ENTITY = DataWatcherRegistry.p.a(19);
   private static final int REMOVE_DELAY = 5;

   private final int ping;
   protected int removeCounter;
   protected Property skin;
   private EnumGamemode gamemode;
   
   public HumanBase(EntityTypes<? extends HumanBase> type, World world) {
      super(type, world);
      this.datawatcher.set(SKIN_PARTS, (byte) 127);
      this.setGamemode(EnumGamemode.NOT_SET);
      this.skin = new Property("textures", "", "");
      this.ping = 1;
   }

   @Override
   public CraftEntity getBukkitEntity() {
      if(this.bukkitEntity == null)
         this.bukkitEntity = new BaseHuman(this.world.getServer(), this);
      
      return this.bukkitEntity;
   }

   @Override
   protected void initDatawatcher() {
      super.initDatawatcher();

      this.datawatcher.register(EXTRA_HEARTS, 0.0f);
      this.datawatcher.register(SCORE, 0);
      this.datawatcher.register(SKIN_PARTS, (byte) 0);
      this.datawatcher.register(MAIN_HAND, (byte) 1);
      this.datawatcher.register(LEFT_SHOULDER_ENTITY, new NBTTagCompound());
      this.datawatcher.register(RIGHT_SHOULDER_ENTITY, new NBTTagCompound());
   }

   @Override
   public void tick() {
      this.yaw = getHeadRotation();
 
      if(removeCounter == 0){
         sendPackets(getInfoPacket(false));
         removeCounter--;
      }else if (removeCounter >= 0)
         removeCounter--;
 
      super.tick();
   }

   public EnumGamemode getGamemode(){
      return this.gamemode;
   }

   public void setGamemode(EnumGamemode gamemode) {
      this.gamemode = gamemode;
   }

   @Override
   public void setName(String name) {
      if (name.length() > 16)
         throw new IllegalArgumentException("Name must be 16 characters or less! Provided: " + name.length());
      else
         setCustomName(new ChatComponentText(name));
      updateProfile();
   }

   public void setSkin(String name) {
      try {
         URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
         InputStreamReader reader = new InputStreamReader(url.openStream());
         String uuid = new JsonParser().parse(reader).getAsJsonObject().get("id").getAsString();

         URL url2 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
         InputStreamReader reader2 = new InputStreamReader(url2.openStream());

         JsonObject property = new JsonParser().parse(reader2).getAsJsonObject().get("properties").getAsJsonArray()
               .get(0).getAsJsonObject();
         String texture = property.get("value").getAsString();
         String signature = property.get("signature").getAsString();

         setSkin(texture, signature);
         updateProfile();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private void setSkin(String texture, String signature) {
      this.skin = new Property("textures", texture, signature);
   }

   public void updateProfile() {
      PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(getId(), getDataWatcher(), true);
      PacketPlayOutPlayerInfo playerInfo = getInfoPacket(true);
      sendPackets(new PacketPlayOutEntityDestroy(this.getId()), playerInfo, getSpawnPacket(), metadata);

      resetCounter();
   }

   @Override
   @OverridingMethodsMustInvokeSuper
   public void saveData(NBTTagCompound nbttagcompound) {
      super.saveData(nbttagcompound);
      if(skin != null){
         nbttagcompound.setString("skinTexture", skin.getValue());
         nbttagcompound.setString("skinSignature", skin.getSignature());
      }
   }

   @Override
   @OverridingMethodsMustInvokeSuper
   public void loadData(NBTTagCompound nbttagcompound) {
      super.loadData(nbttagcompound);
      if(nbttagcompound.hasKey("skinTexture")  && nbttagcompound.hasKey("skinSignature")){
         String skinTexture = nbttagcompound.getString("skinTexture");
         String skinSignature = nbttagcompound.getString("skinSignature");
         
         setSkin(skinTexture, skinSignature);
      }
      updateProfile();
   }

   @Override public void b(EntityPlayer player) {
      updateProfile();                    
   }

   public Packet<?> getSpawnPacket(){
      try{
         final ByteBuf buffer = Unpooled.buffer();
         final PacketDataSerializer data = new PacketDataSerializer(buffer);
         data.d(getId());
         data.a(getUniqueID());
         data.writeDouble(locX());
         data.writeDouble(locY());
         data.writeDouble(locZ());
         data.writeByte((byte)((int)(yaw * 256.0F / 360.0F)));
         data.writeByte((byte)((int)(pitch * 256.0F / 360.0F)));
         final Packet<?> packet = new PacketPlayOutNamedEntitySpawn();
         packet.a(data); buffer.release();
         return packet;
      } catch (Throwable reason) {throw new RuntimeException(reason);}
   }

//   @SuppressWarnings("unchecked")
//   protected PacketPlayOutPlayerInfo getInfoPacket(EnumPlayerInfoAction action) {
//      try {
//         if(action == EnumPlayerInfoAction.ADD_PLAYER)
//            resetCounter();
//         final PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(action);
//         ((List<PacketPlayOutPlayerInfo.PlayerInfoData>) FIELD_DATA.get(packet)).add(packet.new PlayerInfoData(
//            getPseudoProfile(), ping, this.gamemode, getCustomName()
//         ));
//
//         return packet;
//      } catch (Throwable reason) { throw new RuntimeException(reason); }
//   }

   private PacketPlayOutPlayerInfo getInfoPacket(boolean add) {
      try {
         System.out.println("Getting Packet");
         EnumPlayerInfoAction action = add ? EnumPlayerInfoAction.ADD_PLAYER : EnumPlayerInfoAction.REMOVE_PLAYER;
         IChatBaseComponent name = this.getCustomName();
         final PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(action);
         final PacketDataSerializer data = new PacketDataSerializer(Unpooled.buffer());
         data.a(add ? EnumPlayerInfoAction.ADD_PLAYER : EnumPlayerInfoAction.REMOVE_PLAYER);
         data.d(1).a(this.getUniqueID());
         if (add) {
            data.a(name == null ? "" : name.getText()).d(1);
            data.a(skin.getName()).a(skin.getValue());
            data.writeBoolean(skin.hasSignature());
            if (skin.hasSignature()) data.a(skin.getSignature());
            data.d(gamemode.getId()).d(ping).writeBoolean(name != null);
            if (name != null) data.a(name);
         }
         packet.a(data); data.release(); return packet;
      } catch (Exception e) { e.printStackTrace(); return null;}
   }

   protected void sendPackets(Packet<?>...packets){
      Bukkit.getOnlinePlayers().forEach(player -> {
         PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
         for(Packet<?> packet : packets){
            connection.sendPacket(packet);
         }
      });
   }

   @Override
   public Packet<?> P() {
      return getInfoPacket(true);
   }

   @Override public void c(EntityPlayer player) {
      player.playerConnection.sendPacket(getInfoPacket(true));
   }

   protected void resetCounter(){
      removeCounter = REMOVE_DELAY;
   }

   @Override public boolean isNoAI() {
      return false;
   }
   
   @Override public void setNoAI(boolean flag) {

   }

   @Override public boolean isAggressive() {
      return false;
   }

   @Override public void setAggressive(boolean flag) {

   }

   @Override public boolean isLeftHanded() {
      return false;
   }

   @Override public void setLeftHanded(boolean flag) {

   }
}