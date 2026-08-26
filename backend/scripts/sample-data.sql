-- Dados fictícios para demonstração local do Catraca.
-- Execute depois das migrations, conectado ao banco definido em POSTGRES_DB.
-- As UUIDs com prefixo 9000 pertencem somente a este script.
-- Senha das contas locais: 123456 (consulte backend/TEST-USERS.md).

BEGIN;

INSERT INTO units (id, code, name, address) VALUES
('90000000-0000-0000-0000-000000000001', 'CENTRO', 'Unidade Centro', 'Av. Central, 1000'),
('90000000-0000-0000-0000-000000000002', 'NORTE', 'Unidade Zona Norte', 'Rua das Flores, 250')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, address = EXCLUDED.address, active = TRUE;

INSERT INTO departments (id, unit_id, code, name) VALUES
('90000000-0000-0000-0000-000000000010', '90000000-0000-0000-0000-000000000001', 'RECEP', 'Recepção'),
('90000000-0000-0000-0000-000000000011', '90000000-0000-0000-0000-000000000001', 'CLIN', 'Clínica geral'),
('90000000-0000-0000-0000-000000000012', '90000000-0000-0000-0000-000000000002', 'EXAMES', 'Coleta e exames')
ON CONFLICT (unit_id, code) DO UPDATE SET name = EXCLUDED.name, active = TRUE;

INSERT INTO rooms (id, unit_id, code, name, floor) VALUES
('90000000-0000-0000-0000-000000000020', '90000000-0000-0000-0000-000000000001', 'CONS-01', 'Consultório 1', '1º andar'),
('90000000-0000-0000-0000-000000000021', '90000000-0000-0000-0000-000000000001', 'TRIAGEM', 'Sala de triagem', 'Térreo'),
('90000000-0000-0000-0000-000000000022', '90000000-0000-0000-0000-000000000002', 'COLETA', 'Sala de coleta', 'Térreo')
ON CONFLICT (unit_id, code) DO UPDATE SET name = EXCLUDED.name, floor = EXCLUDED.floor, active = TRUE;

INSERT INTO counters (id, unit_id, code, name) VALUES
('90000000-0000-0000-0000-000000000030', '90000000-0000-0000-0000-000000000001', 'G01', 'Guichê 01'),
('90000000-0000-0000-0000-000000000031', '90000000-0000-0000-0000-000000000001', 'G02', 'Guichê prioritário'),
('90000000-0000-0000-0000-000000000032', '90000000-0000-0000-0000-000000000002', 'G01', 'Recepção Norte')
ON CONFLICT (unit_id, code) DO UPDATE SET name = EXCLUDED.name, active = TRUE;

INSERT INTO specialties (id, code, name, description) VALUES
('90000000-0000-0000-0000-000000000040', 'CLINICA', 'Clínica médica', 'Atendimento clínico geral'),
('90000000-0000-0000-0000-000000000041', 'CARDIO', 'Cardiologia', 'Avaliação cardiológica'),
('90000000-0000-0000-0000-000000000042', 'ENFERMAGEM', 'Enfermagem', 'Triagem e procedimentos')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, active = TRUE;

INSERT INTO professional_types (id, code, name) VALUES
('90000000-0000-0000-0000-000000000050', 'MEDICO', 'Médico'),
('90000000-0000-0000-0000-000000000051', 'ENFERMEIRO', 'Enfermeiro'),
('90000000-0000-0000-0000-000000000052', 'TECNICO', 'Técnico de atendimento')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, active = TRUE;

INSERT INTO services (id, code, name, description, default_duration_minutes, requires_professional, requires_counter) VALUES
('90000000-0000-0000-0000-000000000060', 'CONSULTA', 'Consulta clínica', 'Consulta agendada com profissional', 30, TRUE, FALSE),
('90000000-0000-0000-0000-000000000061', 'TRIAGEM', 'Triagem', 'Avaliação inicial de enfermagem', 15, TRUE, FALSE),
('90000000-0000-0000-0000-000000000062', 'DOCUMENTOS', 'Entrega de documentos', 'Atendimento administrativo no guichê', 10, FALSE, TRUE)
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description,
  default_duration_minutes = EXCLUDED.default_duration_minutes,
  requires_professional = EXCLUDED.requires_professional, requires_counter = EXCLUDED.requires_counter, active = TRUE;

INSERT INTO priorities (id, name, description, weight, display_order) VALUES
('90000000-0000-0000-0000-000000000070', 'Normal', 'Ordem comum de chegada', 0, 3),
('90000000-0000-0000-0000-000000000071', 'Preferencial', 'Atendimento preferencial previsto em lei', 50, 2),
('90000000-0000-0000-0000-000000000072', 'Urgente', 'Caso avaliado como urgente', 100, 1)
ON CONFLICT (name) DO UPDATE SET description = EXCLUDED.description, weight = EXCLUDED.weight,
  display_order = EXCLUDED.display_order, active = TRUE;

INSERT INTO user_profiles (user_id, phone) VALUES
('10000000-0000-0000-0000-000000000002', '(85) 99999-1002'),
('10000000-0000-0000-0000-000000000003', '(85) 99999-1003'),
('10000000-0000-0000-0000-000000000005', '(85) 99999-1005')
ON CONFLICT (user_id) DO UPDATE SET phone = EXCLUDED.phone, updated_at = now();

UPDATE patients SET phone = '(85) 99999-1002', birth_date = DATE '1990-05-20'
WHERE id = '20000000-0000-0000-0000-000000000001';

INSERT INTO employee_profiles (user_id, employee_number, badge_code, job_title, unit_id, hired_on, created_by) VALUES
('10000000-0000-0000-0000-000000000003', 'DEM-0001', 'CAT-DEMO-0001', 'Médico clínico', '90000000-0000-0000-0000-000000000001', DATE '2024-02-01', '10000000-0000-0000-0000-000000000001'),
('10000000-0000-0000-0000-000000000005', 'DEM-0002', 'CAT-DEMO-0002', 'Recepcionista', '90000000-0000-0000-0000-000000000001', DATE '2025-01-15', '10000000-0000-0000-0000-000000000001'),
('10000000-0000-0000-0000-000000000006', 'DEM-0003', 'CAT-DEMO-0003', 'Atendente de guichê', '90000000-0000-0000-0000-000000000001', DATE '2025-03-10', '10000000-0000-0000-0000-000000000001')
ON CONFLICT (user_id) DO UPDATE SET job_title = EXCLUDED.job_title, unit_id = EXCLUDED.unit_id, active = TRUE, updated_at = now();

INSERT INTO professionals (id, user_id, professional_type_id, registration_number, default_duration_minutes) VALUES
('90000000-0000-0000-0000-000000000080', '10000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000050', 'CRM-DEMO-1234', 30),
('90000000-0000-0000-0000-000000000081', '10000000-0000-0000-0000-000000000004', '90000000-0000-0000-0000-000000000051', 'COREN-DEMO-5678', 15)
ON CONFLICT (user_id) DO UPDATE SET professional_type_id = EXCLUDED.professional_type_id,
  registration_number = EXCLUDED.registration_number, default_duration_minutes = EXCLUDED.default_duration_minutes, active = TRUE;

INSERT INTO professional_specialties (professional_id, specialty_id) VALUES
('90000000-0000-0000-0000-000000000080', '90000000-0000-0000-0000-000000000040'),
('90000000-0000-0000-0000-000000000080', '90000000-0000-0000-0000-000000000041'),
('90000000-0000-0000-0000-000000000081', '90000000-0000-0000-0000-000000000042')
ON CONFLICT DO NOTHING;

INSERT INTO schedules (id, professional_id, unit_id, room_id, valid_from, valid_until) VALUES
('90000000-0000-0000-0000-000000000090', '90000000-0000-0000-0000-000000000080', '90000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000020', current_date, current_date + 90),
('90000000-0000-0000-0000-000000000091', '90000000-0000-0000-0000-000000000081', '90000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000021', current_date, current_date + 90)
ON CONFLICT (id) DO UPDATE SET valid_from = EXCLUDED.valid_from, valid_until = EXCLUDED.valid_until, active = TRUE;

INSERT INTO shifts (id, schedule_id, day_of_week, start_time, end_time, break_start, break_end, slot_duration_minutes, default_capacity) VALUES
('90000000-0000-0000-0000-000000000100', '90000000-0000-0000-0000-000000000090', 1, TIME '08:00', TIME '17:00', TIME '12:00', TIME '13:00', 30, 1),
('90000000-0000-0000-0000-000000000101', '90000000-0000-0000-0000-000000000091', 1, TIME '08:00', TIME '12:00', NULL, NULL, 15, 2)
ON CONFLICT (id) DO UPDATE SET start_time = EXCLUDED.start_time, end_time = EXCLUDED.end_time;

INSERT INTO time_slots (id, schedule_id, starts_at, ends_at, capacity, booked_count) VALUES
('90000000-0000-0000-0000-000000000110', '90000000-0000-0000-0000-000000000090', date_trunc('day', now()) + interval '1 day 9 hours', date_trunc('day', now()) + interval '1 day 9 hours 30 minutes', 1, 1),
('90000000-0000-0000-0000-000000000111', '90000000-0000-0000-0000-000000000090', date_trunc('day', now()) + interval '1 day 10 hours', date_trunc('day', now()) + interval '1 day 10 hours 30 minutes', 1, 0)
ON CONFLICT (id) DO UPDATE SET starts_at = EXCLUDED.starts_at, ends_at = EXCLUDED.ends_at, booked_count = EXCLUDED.booked_count;

INSERT INTO schedule_exceptions (id, schedule_id, starts_at, ends_at, reason, exception_type) VALUES
('90000000-0000-0000-0000-000000000112', '90000000-0000-0000-0000-000000000090', date_trunc('day', now()) + interval '7 days 12 hours', date_trunc('day', now()) + interval '7 days 17 hours', 'Treinamento da equipe', 'BLOCK')
ON CONFLICT (id) DO UPDATE SET reason = EXCLUDED.reason;

INSERT INTO appointments (id, patient_id, professional_id, specialty_id, service_id, unit_id, time_slot_id, status, created_by) VALUES
('90000000-0000-0000-0000-000000000120', '20000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000080', '90000000-0000-0000-0000-000000000040', '90000000-0000-0000-0000-000000000060', '90000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000110', 'CONFIRMED', '10000000-0000-0000-0000-000000000005')
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, updated_at = now();

INSERT INTO appointment_history (id, appointment_id, previous_status, new_status, changed_by, reason) VALUES
('90000000-0000-0000-0000-000000000121', '90000000-0000-0000-0000-000000000120', NULL, 'CONFIRMED', '10000000-0000-0000-0000-000000000005', 'Agendamento de demonstração')
ON CONFLICT (id) DO NOTHING;

INSERT INTO waiting_list (id, patient_id, service_id, professional_id, unit_id, desired_from, desired_until) VALUES
('90000000-0000-0000-0000-000000000122', '20000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000060', '90000000-0000-0000-0000-000000000080', '90000000-0000-0000-0000-000000000001', now() + interval '2 days', now() + interval '5 days')
ON CONFLICT (id) DO UPDATE SET status = 'WAITING';

INSERT INTO queues (id, unit_id, name, ticket_prefix, grace_period_minutes, no_show_after_minutes, automatic_reallocation_enabled) VALUES
('90000000-0000-0000-0000-000000000130', '90000000-0000-0000-0000-000000000001', 'Atendimento geral', 'A', 5, 15, TRUE),
('90000000-0000-0000-0000-000000000131', '90000000-0000-0000-0000-000000000001', 'Atendimento preferencial', 'P', 5, 10, TRUE)
ON CONFLICT (unit_id, name) DO UPDATE SET active = TRUE;

INSERT INTO ticket_sequences (queue_id, sequence_date, last_value) VALUES
('90000000-0000-0000-0000-000000000130', current_date, 2),
('90000000-0000-0000-0000-000000000131', current_date, 1)
ON CONFLICT (queue_id, sequence_date) DO UPDATE SET last_value = GREATEST(ticket_sequences.last_value, EXCLUDED.last_value);

INSERT INTO tickets (id, queue_id, sequence_date, sequence_number, display_code) VALUES
('90000000-0000-0000-0000-000000000140', '90000000-0000-0000-0000-000000000130', current_date, 1, 'A001'),
('90000000-0000-0000-0000-000000000141', '90000000-0000-0000-0000-000000000130', current_date, 2, 'A002'),
('90000000-0000-0000-0000-000000000142', '90000000-0000-0000-0000-000000000131', current_date, 1, 'P001')
ON CONFLICT (id) DO NOTHING;

INSERT INTO queue_entries (id, queue_id, ticket_id, appointment_id, patient_id, priority_id, entered_at, scheduled_at, status) VALUES
('90000000-0000-0000-0000-000000000150', '90000000-0000-0000-0000-000000000130', '90000000-0000-0000-0000-000000000140', '90000000-0000-0000-0000-000000000120', '20000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000070', now() - interval '20 minutes', now(), 'WAITING'),
('90000000-0000-0000-0000-000000000151', '90000000-0000-0000-0000-000000000131', '90000000-0000-0000-0000-000000000142', NULL, '20000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000071', now() - interval '10 minutes', NULL, 'WAITING')
ON CONFLICT (id) DO UPDATE SET status = 'WAITING', called_at = NULL, started_at = NULL, completed_at = NULL;

INSERT INTO attendances (id, ticket_id, patient_id, professional_id, attendant_user_id, room_id, started_at, completed_at, status) VALUES
('90000000-0000-0000-0000-000000000160', '90000000-0000-0000-0000-000000000141', '20000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000080', '10000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000020', now() - interval '1 hour', now() - interval '30 minutes', 'COMPLETED')
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status;

INSERT INTO display_panels (id, unit_id, code, name, floor, last_calls_limit) VALUES
('90000000-0000-0000-0000-000000000170', '90000000-0000-0000-0000-000000000001', 'TV-TERREO', 'Painel da recepção', 'Térreo', 5)
ON CONFLICT (unit_id, code) DO UPDATE SET name = EXCLUDED.name, active = TRUE;

INSERT INTO display_panel_queues (panel_id, queue_id) VALUES
('90000000-0000-0000-0000-000000000170', '90000000-0000-0000-0000-000000000130'),
('90000000-0000-0000-0000-000000000170', '90000000-0000-0000-0000-000000000131')
ON CONFLICT DO NOTHING;

INSERT INTO patient_notifications (id, recipient_user_id, appointment_id, notification_type, title, message, email_to) VALUES
('90000000-0000-0000-0000-000000000180', '10000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000120', 'APPOINTMENT_REMINDER', 'Lembrete de consulta', 'Sua consulta de demonstração está confirmada para amanhã.', 'patient.test@queueflow.local')
ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, message = EXCLUDED.message;

INSERT INTO audit_logs (id, actor_user_id, action, resource_type, resource_id, details, ip_address) VALUES
('90000000-0000-0000-0000-000000000190', '10000000-0000-0000-0000-000000000001', 'DEMO_DATA_LOADED', 'DATABASE', NULL, '{"source":"backend/scripts/sample-data.sql"}', '127.0.0.1')
ON CONFLICT (id) DO UPDATE SET occurred_at = now();

UPDATE organization_settings
SET establishment_name = 'Clínica Catraca — Demonstração',
    notification_email = 'notificacoes@exemplo.local',
    updated_by = '10000000-0000-0000-0000-000000000001', updated_at = now()
WHERE id = 1;

COMMIT;
