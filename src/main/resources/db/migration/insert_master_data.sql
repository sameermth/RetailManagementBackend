-- =====================================================
-- INSERT INITIAL DATA
-- =====================================================

-- Insert default roles
INSERT INTO roles (name, description) VALUES
                                          ('ROLE_ADMIN', 'Administrator with full access'),
                                          ('ROLE_MANAGER', 'Manager with departmental access'),
                                          ('ROLE_CASHIER', 'Cashier for POS operations'),
                                          ('ROLE_INVENTORY_MANAGER', 'Manages inventory'),
                                          ('ROLE_PURCHASE_MANAGER', 'Manages purchases'),
                                          ('ROLE_SALES_MANAGER', 'Manages sales'),
                                          ('ROLE_ACCOUNTANT', 'Manages finances'),
                                          ('ROLE_EMPLOYEE', 'Regular employee');

-- Insert default permissions
INSERT INTO permissions (name, description) VALUES
                                                ('PRODUCT_READ', 'Can view products'),
                                                ('PRODUCT_WRITE', 'Can create/update products'),
                                                ('PRODUCT_DELETE', 'Can delete products'),
                                                ('INVENTORY_READ', 'Can view inventory'),
                                                ('INVENTORY_WRITE', 'Can update inventory'),
                                                ('INVENTORY_ADJUST', 'Can adjust inventory'),
                                                ('SALES_READ', 'Can view sales'),
                                                ('SALES_WRITE', 'Can create sales'),
                                                ('SALES_DELETE', 'Can delete sales'),
                                                ('SALES_REFUND', 'Can process refunds'),
                                                ('PURCHASE_READ', 'Can view purchases'),
                                                ('PURCHASE_WRITE', 'Can create purchases'),
                                                ('PURCHASE_APPROVE', 'Can approve purchases'),
                                                ('CUSTOMER_READ', 'Can view customers'),
                                                ('CUSTOMER_WRITE', 'Can create/update customers'),
                                                ('CUSTOMER_DUE', 'Can manage customer dues'),
                                                ('SUPPLIER_READ', 'Can view suppliers'),
                                                ('SUPPLIER_WRITE', 'Can create/update suppliers'),
                                                ('DISTRIBUTOR_READ', 'Can view distributors'),
                                                ('DISTRIBUTOR_WRITE', 'Can create/update distributors'),
                                                ('EXPENSE_READ', 'Can view expenses'),
                                                ('EXPENSE_WRITE', 'Can create expenses'),
                                                ('EXPENSE_APPROVE', 'Can approve expenses'),
                                                ('REPORT_READ', 'Can view reports'),
                                                ('REPORT_GENERATE', 'Can generate reports'),
                                                ('USER_READ', 'Can view users'),
                                                ('USER_WRITE', 'Can create/update users'),
                                                ('USER_DELETE', 'Can delete users');

-- Insert default admin user (password: admin123)
INSERT INTO users (username, email, password, first_name, last_name, active, created_by)
VALUES ('ROLE_ADMIN', 'admin@retail.com', '$2a$10$rTgqK5PLwGX7qX7qX7qX7uX7qX7qX7qX7qX7qX7qX7qX7qX7qX7q', 'ROLE_ADMIN', 'User', true, 'SYSTEM');

-- Assign admin role to admin user
INSERT INTO user_roles (user_id, role_id)
VALUES (1, 1);

-- Insert default expense categories
INSERT INTO expense_categories (category_code, name, type) VALUES
                                                               ('RENT', 'Rent', 'OPERATIONAL'),
                                                               ('SALARY', 'Salaries', 'OPERATIONAL'),
                                                               ('UTILITY', 'Utilities', 'OPERATIONAL'),
                                                               ('MARKETING', 'Marketing', 'MARKETING'),
                                                               ('MAINTENANCE', 'Maintenance', 'OPERATIONAL'),
                                                               ('TRAVEL', 'Travel', 'ADMINISTRATIVE');

-- Insert default warehouse
INSERT INTO warehouses (code, name, address, city, state, country, is_primary)
VALUES ('WH001', 'Main Warehouse', '123 Warehouse St', 'Mumbai', 'Maharashtra', 'India', true);
