const ROLE_LABELS: Record<string, string> = {
  DEV_ADMIN: "Administrador técnico",
  ADMIN_USER: "Administrador de usuários",
  ADMIN: "Administrador",
  PATIENT: "Paciente",
  DOCTOR: "Médico",
  PROFESSIONAL: "Profissional",
  RECEPTIONIST: "Recepcionista",
  COUNTER_ATTENDANT: "Atendente de guichê",
};

export function roleLabel(role: string): string {
  return ROLE_LABELS[role.replace("ROLE_", "")] ?? role;
}
