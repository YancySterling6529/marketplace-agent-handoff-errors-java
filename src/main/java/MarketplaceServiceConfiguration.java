/** Configuration layer: process configuration stays outside the service and domain code. */
public final class MarketplaceServiceConfiguration {
    private final String infraiApiKey;

    private MarketplaceServiceConfiguration(String infraiApiKey) {
        this.infraiApiKey = infraiApiKey;
    }

    public static MarketplaceServiceConfiguration fromEnvironment() {
        return new MarketplaceServiceConfiguration(System.getenv("INFRAI_API_KEY"));
    }

    public InfraiErrorsClient errorClient() {
        return new InfraiErrorsClient(infraiApiKey);
    }
}
