package io.seeitlive;

/**
 * Labels a minted agent-view token so the dashboard's viewer list says where
 * the viewer came from (e.g. {@code "Odoo — crm.lead 42"}).
 *
 * <p>Leave {@code label} null and the API applies its own default, branched by
 * principal ("Integration viewer" for an API key/OAuth token).
 */
public record CreateAgentTokenParams(String label) {

    /** Convenience for the common case: let the API label the viewer. */
    public CreateAgentTokenParams() {
        this(null);
    }
}
