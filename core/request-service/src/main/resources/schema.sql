CREATE TABLE IF NOT EXISTS request_statuses (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS participation_requests (
    id BIGSERIAL PRIMARY KEY,
    requester_id BIGINT,
    event_id BIGINT NOT NULL,
    status_id INT NOT NULL REFERENCES request_statuses(id),
    created TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT uq_request UNIQUE (requester_id, event_id)
);

CREATE INDEX IF NOT EXISTS idx_requests_requester_id ON participation_requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_requests_event_id ON participation_requests(event_id);
CREATE INDEX IF NOT EXISTS idx_requests_status_id ON participation_requests(status_id);
CREATE INDEX IF NOT EXISTS idx_requests_created ON participation_requests(created);

INSERT INTO request_statuses (name) VALUES
('PENDING'),
('CONFIRMED'),
('REJECTED'),
('CANCELED')ON CONFLICT (name) DO NOTHING;