package net.craftmaster08.cm08statscore.platform;

import java.util.ServiceLoader;

public final class Services {
    public static final PlatformHelper PLATFORM = load(PlatformHelper.class);

    private Services() {}

    private static <T> T load(Class<T> service) {
        return ServiceLoader.load(service)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No " + service.getName() + " implementation found on the classpath"));
    }
}
