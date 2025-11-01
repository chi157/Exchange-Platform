USE exchange_platform;
ALTER TABLE shipments DROP CHECK shipments_chk_1;
ALTER TABLE shipments ADD CONSTRAINT shipments_chk_1 CHECK (delivery_method IN ('shipnow', 'face_to_face'));
