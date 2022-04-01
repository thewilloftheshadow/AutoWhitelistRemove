/*
 * This file is part of AutoWhitelistRemove.
 *
 * AutoWhitelistRemove is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AutoWhitelistRemove is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AutoWhitelistRemove.  If not, see <https://www.gnu.org/licenses/>.
 */

package lol.shadowdev.autowhitelistremovedsrv;

import lol.hyper.githubreleaseapi.GitHubRelease;
import lol.hyper.githubreleaseapi.GitHubReleaseAPI;
import lol.shadowdev.autowhitelistremovedsrv.command.CommandAWRD;
import lol.shadowdev.autowhitelistremovedsrv.tools.WhitelistCheck;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public final class AutoWhitelistRemoveDSRV extends JavaPlugin {

    public final Logger logger = this.getLogger();
    public final File configFile = new File(this.getDataFolder(), "config.yml");
    public final File removalsFile = new File(this.getDataFolder(), "removals.json");
    public FileConfiguration config;
    public WhitelistCheck whitelistCheck;
    public CommandAWRD commandAWR;

    @Override
    public void onEnable() {
        whitelistCheck = new WhitelistCheck(this);
        commandAWR = new CommandAWRD(this);
        this.getCommand("awrd").setExecutor(commandAWR);
        loadConfig();
        if (config.getBoolean("autoremove-on-start")) {
            Bukkit.getScheduler().runTaskLater(this, () -> whitelistCheck.checkWhitelist(true), 50);
        }

        new Metrics(this, 11684);

        Bukkit.getScheduler().runTaskAsynchronously(this, this::checkForUpdates);
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            this.saveResource("config.yml", true);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        int CONFIG_VERSION = 1;
        if (config.getInt("config-version") != CONFIG_VERSION) {
            logger.warning("Your configuration is out of date! Some features may not work!");
        }
        boolean isCorrect = whitelistCheck.verifyTimeDuration(config.getString("inactive-period"));
        if (!isCorrect) {
            logger.warning("The time duration currently set is not valid! This will break everything!");
        }
    }

    public void checkForUpdates() {
        GitHubReleaseAPI api;
        try {
            api = new GitHubReleaseAPI("AutoWhitelistRemove-DSRV", "thewilloftheshadow");
        } catch (IOException e) {
            logger.warning("Unable to check updates!");
            e.printStackTrace();
            return;
        }
        GitHubRelease current = api.getReleaseByTag(this.getDescription().getVersion());
        GitHubRelease latest = api.getLatestVersion();
        if (current == null) {
            logger.warning("You are running a version that does not exist on GitHub. If you are in a dev environment, you can ignore this. Otherwise, this is a bug!");
            return;
        }
        int buildsBehind = api.getBuildsBehind(current);
        if (buildsBehind == 0) {
            logger.info("You are running the latest version.");
        } else {
            logger.warning("A new version is available (" + latest.getTagVersion() + ")! You are running version " + current.getTagVersion() + ". You are " + buildsBehind + " version(s) behind.");
        }
    }
}
