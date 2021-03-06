package io.github.deerjump.npclib.v1_16_R2;

import org.bukkit.craftbukkit.v1_16_R2.CraftServer;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftCreature;

import net.minecraft.server.v1_16_R2.EntityCreature;
import org.bukkit.entity.EntityType;
import org.graalvm.compiler.serviceprovider.ServiceProvider;

public class BaseNpc extends CraftCreature {

   public BaseNpc(CraftServer server, EntityCreature entity) {
      super(server, entity);
   }

   @Override
   public NpcBase getHandle() {
      return (NpcBase)this.entity;
   }
   
   @Override
   public String toString() {
      return "BaseEntity";
   }

   @Override
   public EntityType getType() {
      return super.getType();
   }

   public void setName(String name){
      this.getHandle().setName(name);
   }

   public void getName(String name){
      this.getHandle().getName();
   }
}
    
