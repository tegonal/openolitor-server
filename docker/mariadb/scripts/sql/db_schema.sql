CREATE TABLE IF NOT EXISTS persistence_journal (
  persistence_id VARCHAR(255) NOT NULL,
  sequence_nr BIGINT NOT NULL,
  marker VARCHAR(255) NOT NULL,
  message BLOB NOT NULL,
  created_at TIMESTAMP NOT NULL,
  PRIMARY KEY (persistence_id, sequence_nr)
);

CREATE TABLE IF NOT EXISTS persistence_snapshot (
  persistence_id VARCHAR(255) NOT NULL,
  sequence_nr BIGINT NOT NULL,
  created_at BIGINT NOT NULL,
  snapshot BLOB NOT NULL,
  PRIMARY KEY (persistence_id, sequence_nr)
);

CREATE TABLE IF NOT EXISTS DBSchema (
  id varchar(36) NOT NULL,
  revision BIGINT NOT NULL,
  status varchar(50) NOT NULL,
  execution_date TIMESTAMP NOT NULL,
  PRIMARY KEY (id)
);
