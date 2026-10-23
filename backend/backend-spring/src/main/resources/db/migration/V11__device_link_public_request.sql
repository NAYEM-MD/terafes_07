-- Opaque browser id for device link requests. Distinct from the desktop poll id.
ALTER TABLE desktop_pairing_requests
    ADD COLUMN public_request_id VARCHAR(36) NULL;

CREATE UNIQUE INDEX uq_desktop_pairing_public_request_id ON desktop_pairing_requests(public_request_id);
