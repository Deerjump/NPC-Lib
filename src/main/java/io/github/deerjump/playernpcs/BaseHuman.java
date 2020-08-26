package io.github.deerjump.playernpcs;

import org.bukkit.craftbukkit.v1_16_R2.CraftServer;

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

   public void setSkin(String name){
      this.getHandle().setSkin(name);
   }   
}