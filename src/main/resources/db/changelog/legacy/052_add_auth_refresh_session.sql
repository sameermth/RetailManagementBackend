CREATE TABLE auth_refresh_session (
  id                  BIGSERIAL PRIMARY KEY,
  account_id          BIGINT NOT NULL REFERENCES auth_account(id) ON DELETE CASCADE,
  organization_id     BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  user_id             BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  client_type         VARCHAR(20) NOT NULL,
  device_id           VARCHAR(255),
  device_name         VARCHAR(255),
  user_agent          TEXT,
  refresh_token_hash  VARCHAR(128) NOT NULL UNIQUE,
  issued_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_used_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at          TIMESTAMPTZ NOT NULL,
  revoked_at          TIMESTAMPTZ,
  revoke_reason       VARCHAR(255),
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT chk_auth_refresh_session_client_type CHECK (client_type IN ('WEB','MOBILE','TABLET'))
);

CREATE INDEX idx_auth_refresh_session_account ON auth_refresh_session(account_id);
CREATE INDEX idx_auth_refresh_session_org ON auth_refresh_session(organization_id);
CREATE INDEX idx_auth_refresh_session_user ON auth_refresh_session(user_id);
CREATE INDEX idx_auth_refresh_session_expires ON auth_refresh_session(expires_at);
