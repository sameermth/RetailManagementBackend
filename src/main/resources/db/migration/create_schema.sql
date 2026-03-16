-- Create database (run this separately if not exists)
-- CREATE DATABASE retail_management_db;

-- Connect to the database
-- \c retail_management_db;

-- =====================================================
-- AUTH MODULE
-- =====================================================

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       first_name VARCHAR(50),
                       last_name VARCHAR(50),
                       phone VARCHAR(20),
                       active BOOLEAN DEFAULT true,
                       last_login_at TIMESTAMP,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       created_by VARCHAR(50),
                       updated_by VARCHAR(50)
);

CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(50) UNIQUE NOT NULL,
                       description VARCHAR(255)
);

CREATE TABLE permissions (
                             id BIGSERIAL PRIMARY KEY,
                             name VARCHAR(100) UNIQUE NOT NULL,
                             description VARCHAR(255)
);

CREATE TABLE user_roles (
                            user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
                            role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
                            PRIMARY KEY (user_id, role_id)
);

CREATE TABLE role_permissions (
                                  role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
                                  permission_id BIGINT REFERENCES permissions(id) ON DELETE CASCADE,
                                  PRIMARY KEY (role_id, permission_id)
);

-- =====================================================
-- PRODUCT MODULE
-- =====================================================

CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(100) UNIQUE NOT NULL,
                            description TEXT,
                            parent_category_id BIGINT REFERENCES categories(id),
                            image_url VARCHAR(255),
                            is_active BOOLEAN DEFAULT true,
                            display_order INTEGER
);

CREATE TABLE brands (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(100) UNIQUE NOT NULL,
                        description TEXT,
                        logo_url VARCHAR(255),
                        website VARCHAR(255),
                        is_active BOOLEAN DEFAULT true
);

CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          sku VARCHAR(50) UNIQUE NOT NULL,
                          name VARCHAR(200) NOT NULL,
                          description TEXT,
                          category_id BIGINT REFERENCES categories(id),
                          brand_id BIGINT REFERENCES brands(id),
                          unit_price DECIMAL(10,2) NOT NULL,
                          cost_price DECIMAL(10,2),
                          gst_rate DECIMAL(5,2),
                          hsn_code VARCHAR(20),
                          unit_of_measure VARCHAR(20),
                          reorder_level INTEGER,
                          reorder_quantity INTEGER,
                          specifications TEXT,
                          barcode VARCHAR(100) UNIQUE,
                          manufacturer VARCHAR(200),
                          country_of_origin VARCHAR(100),
                          is_active BOOLEAN DEFAULT true,
                          is_perishable BOOLEAN DEFAULT false,
                          shelf_life_days INTEGER,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          created_by VARCHAR(50),
                          updated_by VARCHAR(50)
);

CREATE TABLE product_images (
                                id BIGSERIAL PRIMARY KEY,
                                product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                image_url VARCHAR(255) NOT NULL,
                                is_primary BOOLEAN DEFAULT false,
                                display_order INTEGER,
                                alt_text VARCHAR(255)
);

CREATE TABLE product_variants (
                                  id BIGSERIAL PRIMARY KEY,
                                  product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                  sku VARCHAR(50) UNIQUE,
                                  size VARCHAR(50),
                                  color VARCHAR(50),
                                  style VARCHAR(50),
                                  additional_price DECIMAL(10,2) DEFAULT 0,
                                  stock_quantity INTEGER DEFAULT 0,
                                  is_active BOOLEAN DEFAULT true
);

-- =====================================================
-- INVENTORY MODULE
-- =====================================================

CREATE TABLE warehouses (
                            id BIGSERIAL PRIMARY KEY,
                            code VARCHAR(50) UNIQUE NOT NULL,
                            name VARCHAR(100) NOT NULL,
                            description TEXT,
                            address TEXT,
                            city VARCHAR(100),
                            state VARCHAR(100),
                            country VARCHAR(100),
                            pincode VARCHAR(20),
                            phone VARCHAR(20),
                            email VARCHAR(100),
                            manager VARCHAR(100),
                            latitude DECIMAL(10,6),
                            longitude DECIMAL(10,6),
                            is_active BOOLEAN DEFAULT true,
                            is_primary BOOLEAN DEFAULT false,
                            capacity INTEGER,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventory (
                           id BIGSERIAL PRIMARY KEY,
                           product_id BIGINT NOT NULL REFERENCES products(id),
                           warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
                           quantity INTEGER NOT NULL DEFAULT 0,
                           reserved_quantity INTEGER DEFAULT 0,
                           available_quantity INTEGER DEFAULT 0,
                           minimum_stock INTEGER,
                           maximum_stock INTEGER,
                           reorder_point INTEGER,
                           reorder_quantity INTEGER,
                           bin_location VARCHAR(50),
                           shelf_number VARCHAR(50),
                           average_cost DECIMAL(10,2),
                           last_purchase_price DECIMAL(10,2),
                           last_stock_take_date TIMESTAMP,
                           last_movement_date TIMESTAMP,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           UNIQUE(product_id, warehouse_id)
);

CREATE TABLE stock_movements (
                                 id BIGSERIAL PRIMARY KEY,
                                 reference_number VARCHAR(50) UNIQUE NOT NULL,
                                 product_id BIGINT NOT NULL REFERENCES products(id),
                                 from_warehouse_id BIGINT REFERENCES warehouses(id),
                                 to_warehouse_id BIGINT REFERENCES warehouses(id),
                                 movement_type VARCHAR(50) NOT NULL,
                                 quantity INTEGER NOT NULL,
                                 previous_stock INTEGER,
                                 new_stock INTEGER,
                                 unit_cost DECIMAL(10,2),
                                 total_cost DECIMAL(10,2),
                                 reference_type VARCHAR(50),
                                 reference_id BIGINT,
                                 reason TEXT,
                                 notes TEXT,
                                 performed_by VARCHAR(50),
                                 movement_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- CUSTOMER MODULE
-- =====================================================

CREATE TABLE customers (
                           id BIGSERIAL PRIMARY KEY,
                           customer_code VARCHAR(50) UNIQUE NOT NULL,
                           name VARCHAR(200) NOT NULL,
                           customer_type VARCHAR(50) DEFAULT 'INDIVIDUAL',
                           status VARCHAR(50) DEFAULT 'ACTIVE',
                           email VARCHAR(100),
                           phone VARCHAR(20),
                           alternate_phone VARCHAR(20),
                           address TEXT,
                           city VARCHAR(100),
                           state VARCHAR(100),
                           country VARCHAR(100),
                           pincode VARCHAR(20),
                           gst_number VARCHAR(50),
                           pan_number VARCHAR(50),
                           website VARCHAR(255),
                           business_name VARCHAR(200),
                           contact_person VARCHAR(100),
                           designation VARCHAR(100),
                           credit_limit DECIMAL(10,2),
                           total_due_amount DECIMAL(10,2) DEFAULT 0,
                           payment_terms INTEGER,
                           due_reminder_enabled BOOLEAN DEFAULT true,
                           reminder_frequency_days INTEGER DEFAULT 7,
                           last_reminder_sent TIMESTAMP,
                           last_due_date TIMESTAMP,
                           loyalty_points INTEGER DEFAULT 0,
                           loyalty_tier VARCHAR(50) DEFAULT 'BRONZE',
                           total_purchase_amount DECIMAL(10,2) DEFAULT 0,
                           last_purchase_date TIMESTAMP,
                           notes TEXT,
                           profile_image_url VARCHAR(255),
                           is_active BOOLEAN DEFAULT true,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           created_by VARCHAR(50),
                           updated_by VARCHAR(50)
);

CREATE TABLE customer_dues (
                               id BIGSERIAL PRIMARY KEY,
                               due_reference VARCHAR(50) UNIQUE NOT NULL,
                               customer_id BIGINT NOT NULL REFERENCES customers(id),
                               invoice_number VARCHAR(50),
                               sale_id BIGINT,
                               description TEXT,
                               due_date DATE NOT NULL,
                               original_amount DECIMAL(10,2) NOT NULL,
                               remaining_amount DECIMAL(10,2) NOT NULL,
                               paid_amount DECIMAL(10,2) DEFAULT 0,
                               status VARCHAR(50) DEFAULT 'PENDING',
                               reminder_count INTEGER DEFAULT 0,
                               last_reminder_sent TIMESTAMP,
                               last_payment_date TIMESTAMP,
                               notes TEXT,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE loyalty_transactions (
                                      id BIGSERIAL PRIMARY KEY,
                                      transaction_reference VARCHAR(50) UNIQUE NOT NULL,
                                      customer_id BIGINT NOT NULL REFERENCES customers(id),
                                      transaction_type VARCHAR(50) NOT NULL,
                                      points INTEGER NOT NULL,
                                      description TEXT,
                                      sale_id BIGINT,
                                      expiry_date TIMESTAMP,
                                      is_expired BOOLEAN DEFAULT false,
                                      redeemed_at TIMESTAMP,
                                      redeemed_for VARCHAR(255),
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- SALES MODULE
-- =====================================================

CREATE TABLE sales (
                       id BIGSERIAL PRIMARY KEY,
                       invoice_number VARCHAR(50) UNIQUE NOT NULL,
                       customer_id BIGINT REFERENCES customers(id),
                       user_id BIGINT NOT NULL REFERENCES users(id),
                       sale_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       subtotal DECIMAL(10,2) NOT NULL,
                       discount_amount DECIMAL(10,2) DEFAULT 0,
                       discount_percentage DECIMAL(5,2) DEFAULT 0,
                       tax_amount DECIMAL(10,2) DEFAULT 0,
                       shipping_amount DECIMAL(10,2) DEFAULT 0,
                       total_amount DECIMAL(10,2) NOT NULL,
                       paid_amount DECIMAL(10,2) DEFAULT 0,
                       pending_amount DECIMAL(10,2) DEFAULT 0,
                       status VARCHAR(50) DEFAULT 'COMPLETED',
                       payment_method VARCHAR(50),
                       payment_status VARCHAR(50) DEFAULT 'PENDING',
                       due_date TIMESTAMP,
                       notes TEXT,
                       billing_address TEXT,
                       shipping_address TEXT,
                       is_returned BOOLEAN DEFAULT false,
                       return_of_sale_id BIGINT,
                       return_reason TEXT,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sale_items (
                            id BIGSERIAL PRIMARY KEY,
                            sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
                            product_id BIGINT NOT NULL REFERENCES products(id),
                            quantity INTEGER NOT NULL,
                            unit_price DECIMAL(10,2) NOT NULL,
                            discount_amount DECIMAL(10,2) DEFAULT 0,
                            discount_percentage DECIMAL(5,2) DEFAULT 0,
                            tax_rate DECIMAL(5,2) DEFAULT 0,
                            tax_amount DECIMAL(10,2) DEFAULT 0,
                            total_price DECIMAL(10,2) NOT NULL,
                            notes TEXT
);

CREATE TABLE payments (
                          id BIGSERIAL PRIMARY KEY,
                          payment_reference VARCHAR(50) UNIQUE NOT NULL,
                          sale_id BIGINT NOT NULL REFERENCES sales(id),
                          payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          amount DECIMAL(10,2) NOT NULL,
                          payment_method VARCHAR(50) NOT NULL,
                          status VARCHAR(50) DEFAULT 'PAID',
                          transaction_id VARCHAR(100),
                          bank_name VARCHAR(100),
                          cheque_number VARCHAR(50),
                          cheque_date DATE,
                          card_last_four VARCHAR(4),
                          card_type VARCHAR(50),
                          upi_id VARCHAR(100),
                          notes TEXT,
                          received_by VARCHAR(50),
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE invoices (
                          id BIGSERIAL PRIMARY KEY,
                          invoice_number VARCHAR(50) UNIQUE NOT NULL,
                          sale_id BIGINT NOT NULL UNIQUE REFERENCES sales(id),
                          invoice_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          due_date TIMESTAMP,
                          customer_name VARCHAR(200),
                          customer_address TEXT,
                          customer_gst VARCHAR(50),
                          customer_phone VARCHAR(20),
                          customer_email VARCHAR(100),
                          subtotal DECIMAL(10,2) NOT NULL,
                          discount_amount DECIMAL(10,2) DEFAULT 0,
                          tax_amount DECIMAL(10,2) DEFAULT 0,
                          total_amount DECIMAL(10,2) NOT NULL,
                          paid_amount DECIMAL(10,2) DEFAULT 0,
                          balance_due DECIMAL(10,2) DEFAULT 0,
                          payment_terms TEXT,
                          notes TEXT,
                          terms_and_conditions TEXT,
                          bank_details TEXT,
                          signature TEXT,
                          is_printed BOOLEAN DEFAULT false,
                          is_emailed BOOLEAN DEFAULT false,
                          pdf_url VARCHAR(255),
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- SUPPLIER MODULE
-- =====================================================

CREATE TABLE suppliers (
                           id BIGSERIAL PRIMARY KEY,
                           supplier_code VARCHAR(50) UNIQUE NOT NULL,
                           name VARCHAR(200) NOT NULL,
                           status VARCHAR(50) DEFAULT 'ACTIVE',
                           email VARCHAR(100),
                           phone VARCHAR(20),
                           alternate_phone VARCHAR(20),
                           address TEXT,
                           city VARCHAR(100),
                           state VARCHAR(100),
                           country VARCHAR(100),
                           pincode VARCHAR(20),
                           gst_number VARCHAR(50),
                           pan_number VARCHAR(50),
                           website VARCHAR(255),
                           contact_person VARCHAR(100),
                           contact_person_phone VARCHAR(20),
                           contact_person_email VARCHAR(100),
                           credit_limit DECIMAL(10,2),
                           outstanding_amount DECIMAL(10,2) DEFAULT 0,
                           payment_terms INTEGER,
                           payment_method VARCHAR(50),
                           bank_name VARCHAR(100),
                           bank_account_number VARCHAR(50),
                           bank_ifsc_code VARCHAR(20),
                           bank_branch VARCHAR(100),
                           upi_id VARCHAR(100),
                           tax_type VARCHAR(50),
                           tax_registration_number VARCHAR(50),
                           business_type VARCHAR(100),
                           lead_time_days INTEGER,
                           minimum_order_value INTEGER,
                           maximum_order_value INTEGER,
                           notes TEXT,
                           is_active BOOLEAN DEFAULT true,
                           last_purchase_date TIMESTAMP,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           created_by VARCHAR(50),
                           updated_by VARCHAR(50)
);

CREATE TABLE supplier_contacts (
                                   id BIGSERIAL PRIMARY KEY,
                                   supplier_id BIGINT NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
                                   name VARCHAR(100) NOT NULL,
                                   designation VARCHAR(100),
                                   department VARCHAR(100),
                                   email VARCHAR(100),
                                   phone VARCHAR(20),
                                   mobile VARCHAR(20),
                                   is_primary BOOLEAN DEFAULT false,
                                   notes TEXT
);

CREATE TABLE supplier_ratings (
                                  id BIGSERIAL PRIMARY KEY,
                                  supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
                                  quality_rating INTEGER CHECK (quality_rating BETWEEN 1 AND 5),
                                  delivery_rating INTEGER CHECK (delivery_rating BETWEEN 1 AND 5),
                                  price_rating INTEGER CHECK (price_rating BETWEEN 1 AND 5),
                                  communication_rating INTEGER CHECK (communication_rating BETWEEN 1 AND 5),
                                  average_rating DECIMAL(3,2),
                                  comments TEXT,
                                  purchase_id BIGINT,
                                  rated_by VARCHAR(50),
                                  rated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- PURCHASE MODULE
-- =====================================================

CREATE TABLE purchases (
                           id BIGSERIAL PRIMARY KEY,
                           purchase_order_number VARCHAR(50) UNIQUE NOT NULL,
                           supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
                           user_id BIGINT NOT NULL REFERENCES users(id),
                           order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           expected_delivery_date TIMESTAMP,
                           received_date TIMESTAMP,
                           status VARCHAR(50) DEFAULT 'PENDING_APPROVAL',
                           subtotal DECIMAL(10,2) NOT NULL,
                           discount_amount DECIMAL(10,2) DEFAULT 0,
                           discount_percentage DECIMAL(5,2) DEFAULT 0,
                           tax_amount DECIMAL(10,2) DEFAULT 0,
                           shipping_amount DECIMAL(10,2) DEFAULT 0,
                           total_amount DECIMAL(10,2) NOT NULL,
                           paid_amount DECIMAL(10,2) DEFAULT 0,
                           pending_amount DECIMAL(10,2) DEFAULT 0,
                           payment_status VARCHAR(50) DEFAULT 'PENDING',
                           payment_terms VARCHAR(100),
                           shipping_method VARCHAR(100),
                           tracking_number VARCHAR(100),
                           invoice_number VARCHAR(100),
                           notes TEXT,
                           terms_and_conditions TEXT,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           created_by VARCHAR(50),
                           updated_by VARCHAR(50)
);

CREATE TABLE purchase_items (
                                id BIGSERIAL PRIMARY KEY,
                                purchase_id BIGINT NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
                                product_id BIGINT NOT NULL REFERENCES products(id),
                                quantity INTEGER NOT NULL,
                                received_quantity INTEGER DEFAULT 0,
                                unit_price DECIMAL(10,2) NOT NULL,
                                discount_amount DECIMAL(10,2) DEFAULT 0,
                                discount_percentage DECIMAL(5,2) DEFAULT 0,
                                tax_rate DECIMAL(5,2) DEFAULT 0,
                                tax_amount DECIMAL(10,2) DEFAULT 0,
                                total_price DECIMAL(10,2) NOT NULL,
                                notes TEXT
);

CREATE TABLE purchase_receipts (
                                   id BIGSERIAL PRIMARY KEY,
                                   receipt_number VARCHAR(50) UNIQUE NOT NULL,
                                   purchase_id BIGINT NOT NULL REFERENCES purchases(id),
                                   receipt_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   received_by VARCHAR(100),
                                   notes TEXT,
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE purchase_receipt_items (
                                        id BIGSERIAL PRIMARY KEY,
                                        receipt_id BIGINT NOT NULL REFERENCES purchase_receipts(id) ON DELETE CASCADE,
                                        purchase_item_id BIGINT NOT NULL REFERENCES purchase_items(id),
                                        product_id BIGINT NOT NULL REFERENCES products(id),
                                        quantity_received INTEGER NOT NULL,
                                        batch_number VARCHAR(100),
                                        expiry_date DATE,
                                        location VARCHAR(100)
);

CREATE TABLE supplier_payments (
                                   id BIGSERIAL PRIMARY KEY,
                                   payment_reference VARCHAR(50) UNIQUE NOT NULL,
                                   supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
                                   purchase_id BIGINT REFERENCES purchases(id),
                                   payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   amount DECIMAL(10,2) NOT NULL,
                                   payment_method VARCHAR(50) NOT NULL,
                                   status VARCHAR(50) DEFAULT 'PAID',
                                   transaction_id VARCHAR(100),
                                   bank_name VARCHAR(100),
                                   cheque_number VARCHAR(50),
                                   cheque_date DATE,
                                   card_last_four VARCHAR(4),
                                   card_type VARCHAR(50),
                                   upi_id VARCHAR(100),
                                   notes TEXT,
                                   received_by VARCHAR(50),
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- DISTRIBUTOR MODULE
-- =====================================================

CREATE TABLE distributors (
                              id BIGSERIAL PRIMARY KEY,
                              distributor_code VARCHAR(50) UNIQUE NOT NULL,
                              name VARCHAR(200) NOT NULL,
                              status VARCHAR(50) DEFAULT 'ACTIVE',
                              email VARCHAR(100),
                              phone VARCHAR(20),
                              alternate_phone VARCHAR(20),
                              address TEXT,
                              city VARCHAR(100),
                              state VARCHAR(100),
                              country VARCHAR(100),
                              pincode VARCHAR(20),
                              gst_number VARCHAR(50),
                              pan_number VARCHAR(50),
                              website VARCHAR(255),
                              contact_person VARCHAR(100),
                              contact_person_phone VARCHAR(20),
                              contact_person_email VARCHAR(100),
                              credit_limit DECIMAL(10,2),
                              outstanding_amount DECIMAL(10,2) DEFAULT 0,
                              payment_terms INTEGER,
                              payment_method VARCHAR(50),
                              bank_name VARCHAR(100),
                              bank_account_number VARCHAR(50),
                              bank_ifsc_code VARCHAR(20),
                              bank_branch VARCHAR(100),
                              upi_id VARCHAR(100),
                              region VARCHAR(100),
                              territory VARCHAR(100),
                              commission_rate DECIMAL(5,2),
                              delivery_time_days INTEGER,
                              minimum_order_value INTEGER,
                              notes TEXT,
                              is_active BOOLEAN DEFAULT true,
                              last_order_date TIMESTAMP,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              created_by VARCHAR(50),
                              updated_by VARCHAR(50)
);

CREATE TABLE distributor_orders (
                                    id BIGSERIAL PRIMARY KEY,
                                    order_number VARCHAR(50) UNIQUE NOT NULL,
                                    distributor_id BIGINT NOT NULL REFERENCES distributors(id),
                                    user_id BIGINT NOT NULL REFERENCES users(id),
                                    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    expected_delivery_date TIMESTAMP,
                                    delivered_date TIMESTAMP,
                                    status VARCHAR(50) DEFAULT 'PENDING_APPROVAL',
                                    subtotal DECIMAL(10,2) NOT NULL,
                                    discount_amount DECIMAL(10,2) DEFAULT 0,
                                    discount_percentage DECIMAL(5,2) DEFAULT 0,
                                    tax_amount DECIMAL(10,2) DEFAULT 0,
                                    shipping_amount DECIMAL(10,2) DEFAULT 0,
                                    total_amount DECIMAL(10,2) NOT NULL,
                                    paid_amount DECIMAL(10,2) DEFAULT 0,
                                    pending_amount DECIMAL(10,2) DEFAULT 0,
                                    payment_status VARCHAR(50) DEFAULT 'PENDING',
                                    shipping_method VARCHAR(100),
                                    tracking_number VARCHAR(100),
                                    notes TEXT,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    created_by VARCHAR(50),
                                    updated_by VARCHAR(50)
);

CREATE TABLE distributor_order_items (
                                         id BIGSERIAL PRIMARY KEY,
                                         order_id BIGINT NOT NULL REFERENCES distributor_orders(id) ON DELETE CASCADE,
                                         product_id BIGINT NOT NULL REFERENCES products(id),
                                         quantity INTEGER NOT NULL,
                                         shipped_quantity INTEGER DEFAULT 0,
                                         unit_price DECIMAL(10,2) NOT NULL,
                                         discount_amount DECIMAL(10,2) DEFAULT 0,
                                         discount_percentage DECIMAL(5,2) DEFAULT 0,
                                         tax_rate DECIMAL(5,2) DEFAULT 0,
                                         tax_amount DECIMAL(10,2) DEFAULT 0,
                                         total_price DECIMAL(10,2) NOT NULL,
                                         notes TEXT
);

CREATE TABLE distributor_payments (
                                      id BIGSERIAL PRIMARY KEY,
                                      payment_reference VARCHAR(50) UNIQUE NOT NULL,
                                      distributor_id BIGINT NOT NULL REFERENCES distributors(id),
                                      order_id BIGINT REFERENCES distributor_orders(id),
                                      payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      amount DECIMAL(10,2) NOT NULL,
                                      payment_method VARCHAR(50) NOT NULL,
                                      status VARCHAR(50) DEFAULT 'PAID',
                                      transaction_id VARCHAR(100),
                                      bank_name VARCHAR(100),
                                      cheque_number VARCHAR(50),
                                      cheque_date DATE,
                                      card_last_four VARCHAR(4),
                                      card_type VARCHAR(50),
                                      upi_id VARCHAR(100),
                                      notes TEXT,
                                      received_by VARCHAR(50),
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- EXPENSE MODULE
-- =====================================================

CREATE TABLE expense_categories (
                                    id BIGSERIAL PRIMARY KEY,
                                    category_code VARCHAR(50) UNIQUE NOT NULL,
                                    name VARCHAR(100) NOT NULL,
                                    description TEXT,
                                    type VARCHAR(50),
                                    parent_category_id BIGINT REFERENCES expense_categories(id),
                                    budget_amount DECIMAL(10,2),
                                    allocated_amount DECIMAL(10,2) DEFAULT 0,
                                    is_active BOOLEAN DEFAULT true,
                                    notes TEXT,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    created_by VARCHAR(50),
                                    updated_by VARCHAR(50)
);

CREATE TABLE expenses (
                          id BIGSERIAL PRIMARY KEY,
                          expense_number VARCHAR(50) UNIQUE NOT NULL,
                          category_id BIGINT NOT NULL REFERENCES expense_categories(id),
                          expense_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          description TEXT NOT NULL,
                          amount DECIMAL(10,2) NOT NULL,
                          payment_method VARCHAR(50),
                          status VARCHAR(50) DEFAULT 'PENDING_APPROVAL',
                          vendor VARCHAR(200),
                          vendor_invoice_number VARCHAR(100),
                          reference_number VARCHAR(100),
                          user_id BIGINT REFERENCES users(id),
                          paid_to VARCHAR(200),
                          receipt_url VARCHAR(255),
                          notes TEXT,
                          is_reimbursable BOOLEAN DEFAULT false,
                          is_billable BOOLEAN DEFAULT false,
                          customer_id BIGINT,
                          project_id BIGINT,
                          approved_at TIMESTAMP,
                          approved_by VARCHAR(50),
                          rejection_reason TEXT,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          created_by VARCHAR(50),
                          updated_by VARCHAR(50)
);

CREATE TABLE expense_attachments (
                                     id BIGSERIAL PRIMARY KEY,
                                     expense_id BIGINT NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
                                     file_name VARCHAR(255) NOT NULL,
                                     file_type VARCHAR(100),
                                     file_size BIGINT,
                                     file_url VARCHAR(500) NOT NULL,
                                     is_receipt BOOLEAN DEFAULT false,
                                     uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     uploaded_by VARCHAR(50)
);

CREATE TABLE recurring_expenses (
                                    id BIGSERIAL PRIMARY KEY,
                                    recurring_expense_number VARCHAR(50) UNIQUE NOT NULL,
                                    category_id BIGINT NOT NULL REFERENCES expense_categories(id),
                                    description TEXT NOT NULL,
                                    amount DECIMAL(10,2) NOT NULL,
                                    frequency VARCHAR(50) NOT NULL,
                                    start_date DATE NOT NULL,
                                    end_date DATE,
                                    occurrence_count INTEGER,
                                    occurrences_generated INTEGER DEFAULT 0,
                                    next_generation_date DATE,
                                    vendor VARCHAR(200),
                                    payment_method VARCHAR(50),
                                    status VARCHAR(50) DEFAULT 'APPROVED',
                                    notes TEXT,
                                    is_active BOOLEAN DEFAULT true,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    created_by VARCHAR(50),
                                    updated_by VARCHAR(50)
);

-- =====================================================
-- REPORT MODULE
-- =====================================================

CREATE TABLE reports (
                         id BIGSERIAL PRIMARY KEY,
                         report_id VARCHAR(50) UNIQUE NOT NULL,
                         report_name VARCHAR(200) NOT NULL,
                         report_type VARCHAR(50) NOT NULL,
                         format VARCHAR(20) NOT NULL,
                         generated_by BIGINT REFERENCES users(id),
                         generated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         start_date TIMESTAMP,
                         end_date TIMESTAMP,
                         description TEXT,
                         file_path VARCHAR(500),
                         file_url VARCHAR(500),
                         file_size BIGINT,
                         is_scheduled BOOLEAN DEFAULT false,
                         schedule_cron VARCHAR(100),
                         next_scheduled_date TIMESTAMP,
                         recipients TEXT,
                         download_count INTEGER DEFAULT 0,
                         status VARCHAR(50) DEFAULT 'COMPLETED',
                         error_message TEXT,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE report_parameters (
                                   report_id BIGINT NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
                                   param_key VARCHAR(100) NOT NULL,
                                   param_value TEXT,
                                   PRIMARY KEY (report_id, param_key)
);

CREATE TABLE report_schedules (
                                  id BIGSERIAL PRIMARY KEY,
                                  schedule_id VARCHAR(50) UNIQUE NOT NULL,
                                  schedule_name VARCHAR(200) NOT NULL,
                                  report_type VARCHAR(50) NOT NULL,
                                  format VARCHAR(20) NOT NULL,
                                  created_by BIGINT REFERENCES users(id),
                                  frequency VARCHAR(50) NOT NULL,
                                  cron_expression VARCHAR(100),
                                  start_date TIMESTAMP,
                                  end_date TIMESTAMP,
                                  last_run_date TIMESTAMP,
                                  next_run_date TIMESTAMP,
                                  recipients TEXT,
                                  is_active BOOLEAN DEFAULT true,
                                  success_count INTEGER DEFAULT 0,
                                  failure_count INTEGER DEFAULT 0,
                                  last_error TEXT,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE schedule_parameters (
                                     schedule_id BIGINT NOT NULL REFERENCES report_schedules(id) ON DELETE CASCADE,
                                     param_key VARCHAR(100) NOT NULL,
                                     param_value TEXT,
                                     PRIMARY KEY (schedule_id, param_key)
);

-- =====================================================
-- NOTIFICATION MODULE
-- =====================================================

CREATE TABLE notifications (
                               id BIGSERIAL PRIMARY KEY,
                               notification_id VARCHAR(50) UNIQUE NOT NULL,
                               user_id BIGINT REFERENCES users(id),
                               customer_id BIGINT REFERENCES customers(id),
                               supplier_id BIGINT REFERENCES suppliers(id),
                               distributor_id BIGINT REFERENCES distributors(id),
                               type VARCHAR(50) NOT NULL,
                               channel VARCHAR(50) NOT NULL,
                               status VARCHAR(50) DEFAULT 'PENDING',
                               priority VARCHAR(20) DEFAULT 'MEDIUM',
                               title VARCHAR(255) NOT NULL,
                               content TEXT NOT NULL,
                               recipient VARCHAR(255),
                               sender VARCHAR(100),
                               reference_type VARCHAR(50),
                               reference_id BIGINT,
                               sent_at TIMESTAMP,
                               read_at TIMESTAMP,
                               scheduled_for TIMESTAMP,
                               retry_count INTEGER DEFAULT 0,
                               error_message TEXT,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notification_templates (
                                        id BIGSERIAL PRIMARY KEY,
                                        template_code VARCHAR(50) UNIQUE NOT NULL,
                                        name VARCHAR(200) NOT NULL,
                                        description TEXT,
                                        type VARCHAR(50) NOT NULL,
                                        channel VARCHAR(50) NOT NULL,
                                        subject VARCHAR(255),
                                        content TEXT,
                                        content_html TEXT,
                                        sms_content TEXT,
                                        push_title VARCHAR(255),
                                        push_content TEXT,
                                        placeholders TEXT,
                                        is_active BOOLEAN DEFAULT true,
                                        created_by VARCHAR(50),
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);