CREATE TABLE IF NOT EXISTS projects (
  id           UUID PRIMARY KEY,
  key          VARCHAR(32)  NOT NULL UNIQUE,
  name         VARCHAR(255) NOT NULL,
  status       VARCHAR(32)  NOT NULL DEFAULT 'active',
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS project_members (
  project_id   UUID         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  email        VARCHAR(255) NOT NULL,
  role         VARCHAR(32)  NOT NULL,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  PRIMARY KEY (project_id, email)
);

CREATE TABLE IF NOT EXISTS daily_backup_checks (
  id               UUID PRIMARY KEY,
  project_id       UUID         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  check_name       VARCHAR(255) NOT NULL,
  last_run_at      TIMESTAMPTZ,
  last_run_status  VARCHAR(32)  NOT NULL DEFAULT 'pending',
  payload          JSONB        NOT NULL DEFAULT '{}'::jsonb,
  created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO projects (id, key, name, status)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'QUESTIFY', 'Questify', 'active')
ON CONFLICT (id) DO NOTHING;

INSERT INTO project_members (project_id, email, role)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'admin@questify.local', 'owner'),
  ('11111111-1111-1111-1111-111111111111', 'ops@questify.local', 'operator')
ON CONFLICT DO NOTHING;

INSERT INTO daily_backup_checks (id, project_id, check_name, last_run_at, last_run_status, payload)
VALUES
  (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'postgres-daily-backup',
    now() - interval '1 day',
    'success',
    '{"retentionDays":14,"critical":true,"expectedFrequencyHours":24}'::jsonb
  )
ON CONFLICT (id) DO NOTHING;
