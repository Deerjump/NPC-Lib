package io.github.deerjump.npclib.v1_16_R2;

import org.bukkit.craftbukkit.v1_16_R2.CraftServer;
import org.bukkit.entity.EntityType;

public class BaseHuman extends BaseNpc {

   public BaseHuman(CraftServer server, NpcBase entity) {
      super(server, entity);
   }

   @Override
   public HumanBase getHandle() {
      return (HumanBase) this.entity;
   }

   @Override
   public String toString() {
      return "BaseHuman";
   }

   @Override
   public EntityType getType() {
      return EntityType.PLAYER;
   }
   
   public void setSkin(String name){
      this.getHandle().setSkin(name);
   }   
}