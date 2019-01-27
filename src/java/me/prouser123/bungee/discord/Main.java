package me.prouser123.bungee.discord;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.util.NoSuchElementException;

import com.google.common.io.ByteStreams;

import me.prouser123.bstatsplus.bungee.MetricsLite;
import me.prouser123.bungee.discord.base.Test;
// Since we need all the commands here, this is fine.
import me.prouser123.bungee.discord.commands.*;

import me.prouser123.bungee.discord.commands.sub.SubCommandLoader;

public class Main extends Plugin {

	// Instancing
	private static Main instance;
	private static Configuration configuration;
	private static Configuration botCommandConfiguration;
	
    public static Main inst() {
    	  return instance;
    }
    
    public static Configuration getConfig() {
    	return configuration;
    }
    
    public static Configuration getConfigBotCommand() {
    	return botCommandConfiguration;
    }
	
	@Override
	public void onEnable() {
		// Instancing
		instance = this;
		
		getLogger().info("Welcome!");
		
		Test t = new Test();
		t.new Test2();
		
		// Start bStats
		new MetricsLite(this, true);
		
		// Setup config
		loadResource(this, "config.yml");
		try {
			configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
		} catch (IOException e) {
			getLogger().severe("Error loading config.yml");
		}
		
		// Setup bot commands config
		loadResource(this, "bot-command-options.yml");
		try {
			botCommandConfiguration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "bot-command-options.yml"));
		} catch (IOException e) {
			getLogger().severe("Error loading bot-command-options.yml");
		}
        
        new Discord(getConfig().getString("token"));
        
        // Cache a maximum of 10 messages per channel for and remove messages older than 1 hour
        Discord.api.setMessageCacheSize(10, 60*60);
        
        getLogger().info("Registering commands...");
		Discord.api.addMessageCreateListener(new MainCommand());
		
		// Register customizable bot commands from the config, falling back to hard-coded defaults
		registerBotCommands();
		
		// generate on demand (GoD) - copy owner avatar
		Discord.api.addMessageCreateListener(new CopyOwnerAvatar(3, "!getOwnerAvatar", "GoD.copyOwnerAvatar"));
		
		new SubCommandLoader(Discord.api);
		
		// Register Bungee Player Join/Leave Listeners
		String jlcID = getConfig().getString("join-leave-chat-id");
		
		try {
			getProxy().getPluginManager().registerListener(this, new JoinLeave(jlcID));
			getLogger().info("Join Leave Chat enabled for channel: #" + Discord.api.getChannelById(jlcID).toString().replaceAll(".*\\[|\\].*", "") + " (id: " + jlcID + ")");
		} catch (NoSuchElementException e) {

			getLogger().info("Join Leave Chat disabled. Did you put a valid channel ID in the config?");
			return;
		}
	}
	
	private void registerBotCommands() {
		// Attempt to register server-info from the config, falling back to the defaults.
		if (getConfigBotCommand().contains("server-info")) {
			Discord.api.addMessageCreateListener(new ServerInfo(0, getConfigBotCommand().getString("server-info.command"), getConfigBotCommand().getString("server-info.description")));	
		} else {
				this.getLogger().warning("[Bot Command Options] Missing the server-info path. You will not be able to customize the !serverinfo command.");
				Discord.api.addMessageCreateListener(new ServerInfo(0, "!serverinfo", "Show server information."));
		}
		
		// Attempt to register bot-info from the config, falling back to the defaults.
		if (getConfigBotCommand().contains("bot-info")) {
			Discord.api.addMessageCreateListener(new BotInfo(1, getConfigBotCommand().getString("bot-info.command"), getConfigBotCommand().getString("bot-info.description")));	
		} else {
				this.getLogger().warning("[Bot Command Options] Missing the bot-info path. You will not be able to customize the !botinfo command.");
				Discord.api.addMessageCreateListener(new BotInfo(1, "!botinfo", "Show bot information."));
		}
		
		// Attempt to register players from the config, falling back to the defaults.
		if (getConfigBotCommand().contains("players")) {
			Discord.api.addMessageCreateListener(new Players(2, getConfigBotCommand().getString("players.command"), getConfigBotCommand().getString("players.description")));
		} else {
				this.getLogger().warning("[Bot Command Options] Missing the players path. You will not be able to customize the !players command.");
				Discord.api.addMessageCreateListener(new Players(2, "!players", "Show players currently on the network and their servers."));
		}
		
		
	}
	
	public static File loadResource(Plugin plugin, String resource) {
        File folder = plugin.getDataFolder();
        if (!folder.exists())
            folder.mkdir();
        File resourceFile = new File(folder, resource);
        try {
            if (!resourceFile.exists()) {
                resourceFile.createNewFile();
                try (InputStream in = plugin.getResourceAsStream(resource);
                     OutputStream out = new FileOutputStream(resourceFile)) {
                    ByteStreams.copy(in, out);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resourceFile;
    }
	
	@Override
	public void onDisable() {
		if (Discord.api != null) {
			Discord.api.disconnect();
		}
	}
}