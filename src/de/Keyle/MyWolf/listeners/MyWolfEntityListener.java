/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.listeners;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.entity.CraftMyWolf;
import de.Keyle.MyWolf.event.MyWolfLeashEvent;
import de.Keyle.MyWolf.skill.skills.Behavior;
import de.Keyle.MyWolf.util.*;
import net.minecraft.server.EntityWolf;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftWolf;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class MyWolfEntityListener implements Listener
{
    @EventHandler
    public void onEntityDamage(final EntityDamageEvent event)
    {
        if (!(event instanceof EntityDamageByEntityEvent) || event.isCancelled())
        {
            return;
        }
        EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
        if (event.getEntity() instanceof Wolf)
        {
            if (e.getDamager() instanceof Player)
            {
                Player damager = (Player) e.getDamager();

                if (!MyWolfList.hasMyWolf(damager) && !(event.getEntity() instanceof CraftMyWolf))
                {
                    if (!MyWolfPermissions.has(damager, "MyWolf.user.leash") || damager.getItemInHand().getType() != MyWolfConfig.LeashItem)
                    {
                        return;
                    }
                    Wolf TargetWolf = (Wolf) event.getEntity();

                    String OwnerOfTheWolf = ((CraftWolf) TargetWolf).getHandle().getOwnerName();
                    Player Attacker = (Player) e.getDamager();

                    boolean isTarmed = TargetWolf.isTamed();

                    if (isTarmed && OwnerOfTheWolf.equals(Attacker.getName()))
                    {
                        event.setCancelled(true);
                        MyWolf MWolf = new MyWolf(damager);
                        MyWolfUtil.getServer().getPluginManager().callEvent(new MyWolfLeashEvent(MWolf));
                        MyWolfList.addMyWolf(MWolf);
                        MWolf.createWolf((Wolf) event.getEntity());
                        MyWolfPlugin.getPlugin().saveWolves(MyWolfPlugin.NBTWolvesFile);
                        damager.sendMessage(MyWolfUtil.setColors(MyWolfLanguage.getString("Msg_AddLeash")));
                    }
                }
                if (MyWolfList.isMyWolf(event.getEntity().getEntityId()))
                {
                    MyWolf MWolf = MyWolfList.getMyWolf(event.getEntity().getEntityId());
                    MWolf.ResetSitTimer();
                    if (damager.getItemInHand().getType() == MyWolfConfig.LeashItem)
                    {
                        String msg;
                        if (MWolf.getHealth() > MWolf.getMaxHealth() / 3 * 2)
                        {
                            msg = "" + ChatColor.GREEN + MWolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + MWolf.getMaxHealth() + ChatColor.WHITE;
                        }
                        else if (MWolf.getHealth() > MWolf.getMaxHealth() / 3)
                        {
                            msg = "" + ChatColor.YELLOW + MWolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + MWolf.getMaxHealth() + ChatColor.WHITE;
                        }
                        else
                        {
                            msg = "" + ChatColor.RED + MWolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + MWolf.getMaxHealth() + ChatColor.WHITE;
                        }
                        damager.sendMessage(MyWolfUtil.setColors("%aqua%%wolfname%%white% HP: %hp%").replace("%wolfname%", MWolf.Name).replace("%hp%", msg));
                        if (MyWolfConfig.LevelSystem)
                        {
                            int lvl = MWolf.Experience.getLevel();
                            double EXP = MWolf.Experience.getCurrentExp();
                            double reqEXP = MWolf.Experience.getRequiredExp();
                            damager.sendMessage(MyWolfUtil.setColors("%aqua%%wolfname%%white% (Lv%lvl%) (%proz%%) EXP:%exp%/%reqexp%").replace("%wolfname%", MWolf.Name).replace("%exp%", String.format("%1.2f", EXP)).replace("%lvl%", "" + lvl).replace("%reqexp%", String.format("%1.2f", reqEXP)).replace("%proz%", String.format("%1.2f", EXP * 100 / reqEXP)));
                        }

                        if (MWolf.Wolf.isSitting())
                        {
                            MWolf.Wolf.setSitting(true);
                        }
                        event.setCancelled(true);
                    }
                    if (!event.getEntity().getLocation().getWorld().getPVP())
                    {
                        event.setCancelled(true);
                    }
                    if (!MyWolfUtil.canHurtWorldGuard(MWolf.getOwner().getPlayer()))
                    {
                        event.setCancelled(true);
                    }
                    if (!MyWolfUtil.canHurtFactions(damager, MWolf.getOwner().getPlayer()))
                    {
                        event.setCancelled(true);
                    }
                    if (!MyWolfUtil.canHurtTowny(damager, MWolf.getOwner().getPlayer()))
                    {
                        event.setCancelled(true);
                    }
                }
            }
        }
        if (event.getEntity() instanceof LivingEntity)
        {
            if (e.getDamager() instanceof Player)
            {
                Player damager = (Player) e.getDamager();
                if (MyWolfList.hasMyWolf(damager))
                {
                    MyWolf MWolf = MyWolfList.getMyWolf(damager);
                    if (MWolf.Status == WolfState.Here && event.getEntity() != MWolf.Wolf)
                    {
                        MyWolfList.getMyWolf(damager).Wolf.getHandle().Goaltarget = ((CraftLivingEntity) event.getEntity()).getHandle();
                    }

                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(final EntityDeathEvent event)
    {
        if (event.getEntity() instanceof CraftMyWolf)
        {
            if (MyWolfList.isMyWolf(event.getEntity().getEntityId()))
            {
                MyWolf MWolf = MyWolfList.getMyWolf(event.getEntity().getEntityId());
                MWolf.Status = WolfState.Dead;
                MWolf.RespawnTime = MyWolfConfig.RespawnTimeFixed + (MWolf.Experience.getLevel() * MyWolfConfig.RespawnTimeFactor);
                if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
                {
                    EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
                    if (!(e.getDamager() instanceof Player && MWolf.getOwner() != e.getDamager()))
                    {
                        event.setDroppedExp(0);
                    }
                }
                SendDeathMessage(event);
                MWolf.sendMessageToOwner(MyWolfUtil.setColors(MyWolfLanguage.getString("Msg_RespawnIn").replace("%wolfname%", MWolf.Name).replace("%time%", "" + MWolf.RespawnTime)));
            }
        }
        if (MyWolfConfig.LevelSystem && event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
        {
            if (((EntityDamageByEntityEvent) event.getEntity().getLastDamageCause()).getDamager() instanceof CraftMyWolf)
            {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
                if (MyWolfList.isMyWolf(e.getDamager().getEntityId()))
                {
                    MyWolf MWolf = MyWolfList.getMyWolf(e.getDamager().getEntityId());
                    event.setDroppedExp(MWolf.Experience.addExp(e.getEntity().getType()));
                }
            }
        }
    }

    @EventHandler
    public void onEntityTarget(final EntityTargetEvent event)
    {
        if (!event.isCancelled())
        {
            if (event.getEntity() instanceof Wolf)
            {
                if (MyWolfList.isMyWolf(event.getEntity().getEntityId()))
                {
                    MyWolf MWolf = MyWolfList.getMyWolf(event.getEntity().getEntityId());
                    MWolf.ResetSitTimer();
                    if (MWolf.SkillSystem.hasSkill("Behavior"))
                    {
                        Behavior behavior = (Behavior) MWolf.SkillSystem.getSkill("Behavior");
                        if (behavior.getLevel() > 0)
                        {
                            if (behavior.getBehavior() == Behavior.BehaviorState.Friendly)
                            {
                                event.setCancelled(true);
                            }
                            else if (event.getTarget() instanceof Player && ((Player) event.getTarget()).getName().equals(MWolf.getOwner().getName()))
                            {
                                event.setCancelled(true);
                            }
                            else if (behavior.getBehavior() == Behavior.BehaviorState.Raid)
                            {
                                if (event.getTarget() instanceof Player || (event.getTarget() instanceof Wolf && ((Wolf) event.getTarget()).isTamed()))
                                {
                                    event.setCancelled(true);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntitySpawn(CreatureSpawnEvent event)
    {
        if (event.getEntity() instanceof CraftMyWolf)
        {
            CraftMyWolf MWolf = (CraftMyWolf) event.getEntity();
            if (!MWolf.getHandle().isMyWolf())
            {
                event.setCancelled(true);
                net.minecraft.server.World mcWorld = ((CraftWorld) event.getLocation().getWorld()).getHandle();
                EntityWolf entityWolf = new EntityWolf(mcWorld);
                Location loc = event.getLocation();
                entityWolf.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                mcWorld.addEntity(entityWolf, CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
            }
        }
    }

    private void SendDeathMessage(final EntityDeathEvent event)
    {
        MyWolf MWolf = MyWolfList.getMyWolf(event.getEntity().getEntityId());
        String Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Unknow"));
        if (MWolf != null)
        {
            if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
            {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();

                if (e.getDamager().getType() == EntityType.PLAYER)
                {
                    if (e.getDamager() == MWolf.getOwner())
                    {
                        Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("You"));
                    }
                    else
                    {
                        Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Player")).replace("%player%", ((Player) e.getDamager()).getName());
                    }
                }
                else if (e.getDamager().getType() == EntityType.ZOMBIE)
                {
                    Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Zombie"));
                }
                else if (e.getDamager().getType() == EntityType.CREEPER)
                {
                    Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Creeper"));
                }
                else if (e.getDamager().getType() == EntityType.SPIDER)
                {
                    Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Spider"));
                }
                else if (e.getDamager().getType() == EntityType.SLIME)
                {
                    Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Slime"));
                }
                else if (e.getDamager().getType() == EntityType.GIANT)
                {
                    Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Giant"));
                }
                else if (e.getDamager().getType() == EntityType.SKELETON)
                {
                    Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Skeleton"));
                }
                else if (e.getDamager().getType() == EntityType.CAVE_SPIDER)
                {
                    Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("CaveSpider"));
                }
                else if (e.getDamager().getType() == EntityType.ENDERMAN)
                {
                    Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Enderman"));
                }
                else if (e.getDamager().getType() == EntityType.PIG_ZOMBIE)
                {
                    Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("PigZombie"));
                }
                else if (e.getDamager().getType() == EntityType.SILVERFISH)
                {
                    Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Silverfish"));
                }
                else if (e.getDamager().getType() == EntityType.SNOWMAN)
                {
                    Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Snowman"));
                }
                else if (e.getDamager().getType() == EntityType.ENDER_DRAGON)
                {
                    Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("EnderDragon"));
                }
                else if (e.getDamager().getType() == EntityType.BLAZE)
                {
                    Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Blaze"));
                }
                else if (e.getDamager().getType() == EntityType.MAGMA_CUBE)
                {
                    Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("MagmaCube"));
                }
                else if (e.getDamager().getType() == EntityType.WOLF)
                {
                    Wolf w = (Wolf) e.getDamager();
                    if (w.isTamed())
                    {
                        if (MyWolfList.isMyWolf(w.getEntityId()))
                        {
                            Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("MyWolf")).replace("%player%", MyWolfList.getMyWolf(w.getEntityId()).getOwner().getName()).replace("%wolfname%", MyWolfList.getMyWolf(w.getEntityId()).Name);
                        }
                        else
                        {
                            Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("OwnedWolf")).replace("%player%", ((CraftWolf) w).getHandle().getOwnerName());
                        }
                    }
                    else
                    {
                        Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Wolf"));
                    }
                }
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.BLOCK_EXPLOSION))
            {
                Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Explosion"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.DROWNING))
            {
                Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Drowning"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.FALL))
            {
                Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Fall"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.FIRE))
            {
                Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Fire"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.LAVA))
            {
                Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Lava"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.LIGHTNING))
            {
                Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("Lightning"));
            }
            else if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.VOID))
            {
                Killer = MyWolfUtil.setColors(MyWolfLanguage.getString("kvoid"));
            }

            MWolf.sendMessageToOwner(MyWolfUtil.setColors(MyWolfLanguage.getString("Msg_DeathMessage")).replace("%wolfname%", MWolf.Name) + Killer);
        }
    }
}
