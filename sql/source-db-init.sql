-- Source Database (vendor_invoices) DDL
CREATE SCHEMA IF NOT EXISTS source_schema;

CREATE TABLE IF NOT EXISTS source_schema.vendor_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_name TEXT NOT NULL,
    amount NUMERIC(15, 2),
    billing_cycle DATE NOT NULL,
    line_item_id TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_vendor_invoices_vendor_name ON source_schema.vendor_invoices(vendor_name);
CREATE INDEX IF NOT EXISTS idx_vendor_invoices_billing_cycle ON source_schema.vendor_invoices(billing_cycle);
CREATE INDEX IF NOT EXISTS idx_vendor_invoices_line_item_id ON source_schema.vendor_invoices(line_item_id);

-- Sample data
INSERT INTO source_schema.vendor_invoices (vendor_name, amount, billing_cycle, line_item_id)
VALUES 
    ('Vendor A', 1500.00, '2026-04-01', 'LINE001'),
    ('Vendor B', 2500.50, '2026-04-01', 'LINE002'),
    ('Vendor C', NULL, '2026-04-01', 'LINE003'),
    ('Vendor A', 1200.00, '2026-03-01', 'LINE004')
ON CONFLICT DO NOTHING;
