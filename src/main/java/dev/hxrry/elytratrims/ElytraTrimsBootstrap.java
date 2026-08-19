package dev.hxrry.elytratrims;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class ElytraTrimsBootstrap implements PluginBootstrap {

    private static final String PACK_ID = "elytratrims";
    private static final String PACK_RESOURCE = "/datapack";

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
            URL resource = ElytraTrimsBootstrap.class.getResource(PACK_RESOURCE);
            if (resource == null) {
                context.getLogger().error("The bundled datapack is missing from the plugin jar; "
                        + "elytra trims and dyeing will not work.");
                return;
            }

            try {
                URI uri = resource.toURI();
                if (event.registrar().discoverPack(uri, PACK_ID) == null) {
                    context.getLogger().error("The server rejected the bundled datapack; "
                            + "elytra trims and dyeing will not work.");
                }
            } catch (URISyntaxException | IOException e) {
                context.getLogger().error("Could not load the bundled datapack: {}", e.getMessage(), e);
            }
        });
    }
}
