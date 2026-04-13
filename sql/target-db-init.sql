-- Target Database (internal_ledger) DDL
CREATE SCHEMA IF NOT EXISTS target_schema;

CREATE TABLE IF NOT EXISTS target_schema.internal_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_name TEXT NOT NULL,
    amount NUMERIC(15, 2),
    billing_cycle DATE NOT NULL,
    line_item_id TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_internal_ledger_vendor_name ON target_schema.internal_ledger(vendor_name);
CREATE INDEX IF NOT EXISTS idx_internal_ledger_billing_cycle ON target_schema.internal_ledger(billing_cycle);
CREATE INDEX IF NOT EXISTS idx_internal_ledger_line_item_id ON target_schema.internal_ledger(line_item_id);

-- Sample data
INSERT INTO target_schema.internal_ledger (vendor_name, amount, billing_cycle, line_item_id)
VALUES 
    ('Vendor A', 1500.00, '2026-04-01', 'LINE001'),
    ('Vendor B', 2500.50, '2026-04-01', 'LINE002'),
    ('Vendor C', NULL, '2026-04-01', 'LINE003'),
    ('Vendor D', 3000.00, '2026-04-01', 'LINE005')
ON CONFLICT DO NOTHING;
