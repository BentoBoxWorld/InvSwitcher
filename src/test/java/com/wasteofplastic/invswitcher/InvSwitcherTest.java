package com.wasteofplastic.invswitcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.wasteofplastic.invswitcher.listeners.PlayerListener;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.Addon.State;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;
import world.bentobox.bentobox.managers.AddonsManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class InvSwitcherTest {

    private static File jFile;
    @Mock
    private BentoBox plugin;
    @Mock
    private AddonsManager am;

    @Mock
    private Settings pluginSettings;

    private InvSwitcher addon;

    @Mock
    private Logger logger;
    @Mock
    private World world;

    @BeforeClass
    public static void beforeClass() throws IOException {
        // Make the addon jar
        jFile = new File("addon.jar");
        // Copy over config file from src folder
        Path fromPath = Paths.get("src/main/resources/config.yml");
        Path path = Paths.get("config.yml");
        Files.copy(fromPath, path);
        try (JarOutputStream tempJarOutputStream = new JarOutputStream(new FileOutputStream(jFile))) {
            //Added the new files to the jar.
            try (FileInputStream fis = new FileInputStream(path.toFile())) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                JarEntry entry = new JarEntry(path.toString());
                tempJarOutputStream.putNextEntry(entry);
                while((bytesRead = fis.read(buffer)) != -1) {
                    tempJarOutputStream.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    /**
     */
    @Before
    public void setUp() {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());

        // The database type has to be created one line before the thenReturn() to work!
        DatabaseType value = DatabaseType.YAML;
        when(plugin.getSettings()).thenReturn(pluginSettings);
        when(pluginSettings.getDatabaseType()).thenReturn(value);

        // Addon
        addon = new InvSwitcher();
        File dataFolder = new File("addons/Level");
        addon.setDataFolder(dataFolder);
        addon.setFile(jFile);
        AddonDescription desc = new AddonDescription.Builder("bentobox", "InvSwitcher", "1.3").description("test").authors("tastybento").build();
        addon.setDescription(desc);
        // Addons manager
        when(plugin.getAddonsManager()).thenReturn(am);

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getWorld(anyString())).thenReturn(world);

        // World
        when(world.getName()).thenReturn("bskyblock-world");
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        deleteAll(new File("database"));
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        deleteAll(new File("database"));
        new File("addon.jar").delete();
        new File("config.yml").delete();
        deleteAll(new File("addons"));
    }

    private static void deleteAll(File file) throws IOException {
        if (file.exists()) {
            Files.walk(file.toPath())
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.InvSwitcher#onEnable()}.
     */
    @Test
    public void testOnEnable() {
        addon.onEnable();
        verify(plugin).logError("[InvSwitcher] This addon is incompatible with YAML database. Please use another type, like JSON.");
        assertEquals(State.DISABLED, addon.getState());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.InvSwitcher#onDisable()}.
     */
    @Test
    public void testOnDisable() {
        addon.onLoad();
        addon.getSettings().setWorlds(Set.of("bskyblock-world"));
        addon.allLoaded();
        addon.onDisable();
        PowerMockito.verifyStatic(Bukkit.class);
        Bukkit.getOnlinePlayers();
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.InvSwitcher#onLoad()}.
     */
    @Test
    public void testOnLoad() {
        addon.onLoad();
        File file = new File("config.yml");
        assertTrue(file.exists());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.InvSwitcher#allLoaded()}.
     */
    @Test
    public void testAllLoaded() {
        addon.onLoad();
        addon.getSettings().setWorlds(Set.of("bskyblock-world"));
        addon.allLoaded();
        verify(plugin).log("[InvSwitcher] Hooking into the following worlds:");
        verify(plugin, times(3)).log("[InvSwitcher] bskyblock-world");
        verify(am).registerListener(eq(addon), any(PlayerListener.class));

    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.InvSwitcher#allLoaded()}.
     */
    @Test
    public void testAllLoadedNoWorlds() {
        addon.onLoad();
        addon.getSettings().setWorlds(Collections.emptySet());
        addon.allLoaded();
        verify(plugin).logWarning("[InvSwitcher] Did not hook into any worlds - disabling addon!");
        assertEquals(State.DISABLED, addon.getState());
        verify(plugin, never()).log("Hooking into the following worlds:");
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.InvSwitcher#getStore()}.
     */
    @Test
    public void testGetStore() {
        assertNull(addon.getStore());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.InvSwitcher#getSettings()}.
     */
    @Test
    public void testGetSettings() {
        assertNull(addon.getSettings());
    }

    /**
     * Test method for {@link com.wasteofplastic.invswitcher.InvSwitcher#getWorlds()}.
     */
    @Test
    public void testGetWorlds() {
        assertTrue(addon.getWorlds().isEmpty());
    }

}
