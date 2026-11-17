export const ADMIN_PANEL_ROLES = [
  "ADMIN",
  "EMPLOYEE",
  "SALES",
  "IMPORT_STAFF",
  "INVENTORY_MANAGER",
  "ACCOUNTANT",
];

export function roleNameOf(role) {
  if (!role) return "";
  return typeof role === "string" ? role : role.name || "";
}

export function extractRoleNames(roles) {
  return (roles || []).map(roleNameOf).filter(Boolean);
}

export function hasAdminPanelAccess(roles) {
  return extractRoleNames(roles).some((name) => ADMIN_PANEL_ROLES.includes(name));
}
