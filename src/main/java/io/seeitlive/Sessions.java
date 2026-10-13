package io.seeitlive;

/** The {@code sessions} resource: create, read + status, as reachable by an API key or OAuth token. */
public final class Sessions {

    private final SeeItLiveHttpClient http;

    Sessions(SeeItLiveHttpClient http) {
        this.http = http;
    }

    /** {@code POST /sessions} — creates a new session invite. The body never includes a team id. */
    public Session create(CreateSessionParams params) {
        return http.post("/sessions", params, Session.class);
    }

    /**
     * {@code GET /sessions/:id} — reads a stored session, including how its
     * invitation was delivered (channel/recipient).
     */
    public SessionDetail get(String id) {
        return http.get("/sessions/" + SeeItLiveHttpClient.encodePathSegment(id), SessionDetail.class);
    }

    /** {@code GET /sessions/:id/status}. */
    public SessionStatus status(String id) {
        return http.get("/sessions/" + SeeItLiveHttpClient.encodePathSegment(id) + "/status", SessionStatus.class);
    }

    /**
     * {@code GET /sessions/:id/protection} — the secret to read to the customer:
     * the password, or the rotating code of the moment.
     */
    public ProtectionInfo getProtection(String id) {
        return http.get("/sessions/" + SeeItLiveHttpClient.encodePathSegment(id) + "/protection", ProtectionInfo.class);
    }

    /**
     * {@code PUT /sessions/:id/protection} — changes a session's guest challenge
     * after creation, including adding one to a session created without any,
     * which is otherwise impossible.
     */
    public ProtectionInfo setProtection(String id, SetProtectionParams params) {
        return http.put("/sessions/" + SeeItLiveHttpClient.encodePathSegment(id) + "/protection", params, ProtectionInfo.class);
    }

    /**
     * {@code POST /sessions/:id/protection/reset} — clears a guest lockout,
     * rotating the secret only when {@code rotate} is true.
     */
    public ProtectionInfo resetProtection(String id, ResetProtectionParams params) {
        return http.post("/sessions/" + SeeItLiveHttpClient.encodePathSegment(id) + "/protection/reset",
                params, ProtectionInfo.class);
    }

    /**
     * {@code POST /sessions/:id/agent-token} — mints a fresh agent-view token so
     * an embedded viewer can watch the session. Tokens expire after 120 minutes.
     */
    public AgentToken createAgentToken(String id, CreateAgentTokenParams params) {
        return http.post("/sessions/" + SeeItLiveHttpClient.encodePathSegment(id) + "/agent-token",
                params, AgentToken.class);
    }

    /**
     * {@code POST /sessions/:id/reshare} — revives an expired invitation.
     * {@link ReshareMode#REVIVE} (the default) extends the SAME link and code
     * in place, so a message the customer already has starts working again;
     * {@link ReshareMode#NEW} issues a fresh link and kills the old one.
     *
     * <p>A session created without a short link can be extended but not
     * re-sent — the plaintext token survives only in the short link's
     * encrypted fragment. {@code resend} on the result reports which happened.
     */
    public ReshareResult reshare(String id, ReshareParams params) {
        return http.post("/sessions/" + SeeItLiveHttpClient.encodePathSegment(id) + "/reshare",
                params, ReshareResult.class);
    }

    /** {@link #reshare(String, ReshareParams)} with every option left to reuse what was recorded. */
    public ReshareResult reshare(String id) {
        return reshare(id, new ReshareParams());
    }

}
