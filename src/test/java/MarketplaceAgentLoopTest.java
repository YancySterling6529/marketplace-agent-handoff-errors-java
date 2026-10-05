public final class MarketplaceAgentLoopTest {
    public static void main(String[] args) throws Exception {
        MarketplaceAgentLoop.SellerAsset asset = new MarketplaceAgentLoop.SellerAsset("asset-204", "seller-88", true);
        MarketplaceAgentLoop.BuyerUpdate update = new MarketplaceAgentLoop.BuyerUpdate("order-711", "buyer-42", true);
        MarketplaceAgentLoop.OrderHandoff handoff = new MarketplaceAgentLoop(null).handOff(asset, update);
        if (!"READY_FOR_HANDOFF".equals(handoff.state())) {
            throw new AssertionError("confirmed buyer and cleared asset must be ready for handoff");
        }
        System.out.println("business decision verified: READY_FOR_HANDOFF");
    }
}
