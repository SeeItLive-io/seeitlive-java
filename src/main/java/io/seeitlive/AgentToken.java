package io.seeitlive;

/**
 * A freshly minted agent-view grant, as returned by
 * {@code POST /sessions/:id/agent-token}. Tokens expire after 120 minutes;
 * mint another to re-open a viewer.
 */
public record AgentToken(String code, String token, String agentViewUrl) {
}
