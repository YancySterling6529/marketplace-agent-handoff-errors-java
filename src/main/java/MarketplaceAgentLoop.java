import java.util.List;
import java.util.Map;

/** Coordinates the asset, buyer update, and custody handoff in one agent turn. */
public final class MarketplaceAgentLoop {
    public record SellerAsset(String assetId, String sellerId, boolean clearedForSale) {}
    public record BuyerUpdate(String orderId, String buyerId, boolean paymentConfirmed) {}
    public record OrderHandoff(String orderId, String assetId, String buyerId, String state) {}

    private final InfraiErrorsClient infrai;

    public MarketplaceAgentLoop(InfraiErrorsClient infrai) {
        this.infrai = infrai;
    }

    public OrderHandoff handOff(SellerAsset asset, BuyerUpdate update) throws Exception {
        try {
            validate(asset, update);
            return new OrderHandoff(update.orderId(), asset.assetId(), update.buyerId(), "READY_FOR_HANDOFF");
        } catch (IllegalStateException exception) {
            infrai.errors.capture(Map.of(
                "message", "Marketplace order handoff requires review",
                "level", "error",
                "fingerprint", List.of("marketplace-agent", "order-handoff"),
                "exception", exception.toString(),
                "context", Map.of(
                    "orderId", update.orderId(),
                    "assetId", asset.assetId(),
                    "sellerId", asset.sellerId(),
                    "buyerId", update.buyerId())
            ));
            throw exception;
        }
    }

    private static void validate(SellerAsset asset, BuyerUpdate update) {
        if (!asset.clearedForSale()) {
            throw new IllegalStateException("seller asset is not cleared for sale");
        }
        if (!update.paymentConfirmed()) {
            throw new IllegalStateException("buyer payment is not confirmed");
        }
    }
}
