CREATE TABLE IF NOT EXISTS inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id VARCHAR(100) NOT NULL UNIQUE,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved INTEGER NOT NULL DEFAULT 0,
    warehouse_location VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    version BIGINT DEFAULT 0,
    active BOOLEAN DEFAULT TRUE
);

-- Seed data
INSERT INTO inventory (product_id, product_name, quantity, reserved, warehouse_location) VALUES
('PROD-001', 'Laptop Dell XPS 15', 50, 0, 'Warehouse-A'),
('PROD-002', 'iPhone 15 Pro', 100, 0, 'Warehouse-A'),
('PROD-003', 'Samsung Galaxy S24', 75, 0, 'Warehouse-B'),
('PROD-004', 'Sony WH-1000XM5', 200, 0, 'Warehouse-B'),
('PROD-005', 'iPad Air M2', 60, 0, 'Warehouse-A')
ON CONFLICT (product_id) DO NOTHING;
