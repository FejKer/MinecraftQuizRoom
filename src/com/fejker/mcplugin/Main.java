package com.fejker.mcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.*;
import java.util.Random;

public class Main extends JavaPlugin implements Listener {
    String subject;
    String pytanie = "";
    String odpa = "";
    String odpb = "";
    String odpc = "";
    int wynik = 0;
    int odp;
    boolean busy = false;
    boolean failed = false;
    Location buttonvector;

    @Override
    public void onEnable() {
        super.onEnable();
        Bukkit.getConsoleSender().sendMessage("Plugin SchoolMiniGames zaladowany.");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Bukkit.getConsoleSender().sendMessage("Plugin SchoolMiniGames wylaczony.");
    }

    @EventHandler
    public void onLeverPull(PlayerInteractEvent e) throws SQLException, InterruptedException {
        Player player = e.getPlayer();
        if(!e.getClickedBlock().getType().name().equals("LEVER")) return;
        if(busy) {
            player.sendMessage("Sala do testow jest aktualnie zajeta!");                                            //check if quiz room is busy
            return;
        }
        if(e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock().getType().name().equals("LEVER")) {
            player.sendMessage("Zaczynamy!");
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    player.sendMessage("" + ChatColor.GREEN + ChatColor.BOLD + player.getName() + ChatColor.RESET + ChatColor.RED + " zostaniesz poddany sprawdzianowi z przedmiotu " + ChatColor.BOLD + ChatColor.BLUE + subject);
                    try {
                        test(player);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }, 20);
        }
    }

    @EventHandler
    public void onButtonPress(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!player.getDisplayName().equals("TEST")) return;                                        //if player not in the test quiz - ignore
        World world = Bukkit.getWorld("world");
        Block block = world.getBlockAt(buttonvector);
        if (e.getClickedBlock().getType().equals("BUTTON") && !e.getClickedBlock().getLocation().equals(block)) {
            player.sendMessage("Zla odpowiedz.");                                                                           //checking if player's answer is correct
            failed = true;
        }
        player.sendMessage("Dobra odpowiedz.");
    }

    public void test(Player player) throws SQLException, InterruptedException {
        busy = true;                                            //making quiz room busy
        String oldName = player.getDisplayName();               
        player.setDisplayName("TEST");                      
        Random random = new Random();
        int length;
        Location oldlocation = player.getLocation();
        Connection connection = DriverManager.getConnection("database_address", "username", "password");            //database connection
        // System.out.println("Połączono");
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("select count(*) from fizykat");           //getting questions from database
        rs.next();
        length = rs.getInt(1);
        int arr[] = new int[length];
        player.sendMessage("Przed toba " + length + " pytan!");
        odp = 0;
        BukkitTask bukkitRunnable = new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                try {
                    ResultSet rs = stmt.executeQuery("select * from fizyka");
                    for (int j = 0; j < random.nextInt(length) + 1; j++) {
                        rs.next();
                    }
                    pytanie = rs.getString(2);                          //getting possible answers
                    odpa = rs.getString(3);
                    odpb = rs.getString(4);
                    odpc = rs.getString(5);
                    odp = rs.getInt(6);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Location signLocation = new Location(player.getWorld(), -9, 80, 244);               //setting answers on signs
                World world = Bukkit.getWorld("world");
                Block block = world.getBlockAt(signLocation);
                Sign sign = (Sign) block.getState();
                sign.setLine(1, odpa);
                sign.update();
                signLocation = new Location(player.getWorld(), -9, 80, 243);
                block = world.getBlockAt(signLocation);
                sign = (Sign) block.getState();
                sign.setLine(1, odpb);
                sign.update();
                signLocation = new Location(player.getWorld(), -9, 80, 242);
                block = world.getBlockAt(signLocation);
                sign = (Sign) block.getState();
                sign.setLine(1, odpc);
                sign.update();
                switch (odp) {
                    case 3:
                        buttonvector = new Location(world, -9, 79, 244);
                    case 4:
                        buttonvector = new Location(world, -9, 79, 243);
                    case 5:
                        buttonvector = new Location(world, -9, 79, 242);
                }
                Location location = new Location(world, -8, 79, 244);
                location.setYaw(90);                                            //reset player's camera
                player.teleport(location);
                player.sendMessage("Pytanie nr " + (i + 1) + ": " + pytanie);                       //sending question to player
                if (i == (length - 1) || failed) {
                    player.sendMessage("tutaj");
                    busy = false;
                    failed = false;
                    wynik = 0;
                    player.setDisplayName(oldName);
                    player.teleport(oldlocation);
                    player.sendMessage("Dziekujemy za udzial w quizie!");
                    player.sendMessage("Twoj wynik to " + wynik + " poprawnych odpowiedzi na " + length);           //sending score to player
                    cancel();
                }
                i++;
            }
        }.runTaskTimer(this, 100, 100);
           // player.sendMessage("Czas na odpowiedz minal!");
           // if (!failed) {
           //     wynik++;
           // } else {
           //     player.teleport(oldlocation);
           //     player.sendMessage("Dziekujemy za udzial w quizie!");
           //     player.sendMessage("Twoj wynik to " + wynik + " poprawnych odpowiedzi na " + length);
           // }

        /*World world = Bukkit.getWorld("world");
        Location signLocation = new Location(world, -9, 80, 244);
        Block block = world.getBlockAt(signLocation);
        Sign sign = (Sign) block.getState();
        sign.setLine(1, "");
        sign.update();
        signLocation = new Location(world, -9, 80, 243);
        block = world.getBlockAt(signLocation);
        sign = (Sign) block.getState();
        sign.setLine(1, "");
        sign.update();
        signLocation = new Location(world, -9, 80, 242);
        block = world.getBlockAt(signLocation);
        sign = (Sign) block.getState();
        sign.setLine(1, "");
        sign.update();*/
    }
    
}
