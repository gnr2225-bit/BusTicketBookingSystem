USE bus_ticket_system;

INSERT INTO operators (op_id, name, contact) VALUES
('OP001','TSRTC','9123456701'),
('OP002','APSRTC','9123456702'),
('OP003','KSRTC','9123456703'),
('OP004','TNSTC','9123456704'),
('OP005','KeralaRoad','9123456705'),
('OP006','Orange Travels','9123456706'),
('OP007','VRL Express','9123456707'),
('OP008','SRS Travels','9123456708'),
('OP009','Morning Star','9123456709'),
('OP010','SouthLink','9123456710')
ON DUPLICATE KEY UPDATE name=VALUES(name), contact=VALUES(contact);
