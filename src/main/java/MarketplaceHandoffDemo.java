public final class MarketplaceHandoffDemo {
    public static void main(String[] args) throws Exception {
        MarketplaceServiceConfiguration configuration = MarketplaceServiceConfiguration.fromEnvironment();
        InfraiErrorsClient infrai = configuration.errorClient();
        MarketplaceAgentLoop loop = new MarketplaceAgentLoop(infrai);
        MarketplaceAgentLoop.SellerAsset asset = new MarketplaceAgentLoop.SellerAsset("asset-204", "seller-88", false);
        MarketplaceAgentLoop.BuyerUpdate update = new MarketplaceAgentLoop.BuyerUpdate("order-711", "buyer-42", true);
        try {
            loop.handOff(asset, update);
            throw new AssertionError("uncleared asset must reject the handoff");
        } catch (IllegalStateException expected) {
            System.out.println("handoff rejected and captured: " + expected.getMessage());
        }
    }
}
