-- Records a safe pairing conflict without transferring device ownership.
ALTER TABLE desktop_pairing_requests
    ADD COLUMN conflict_code VARCHAR(64) NULL;
