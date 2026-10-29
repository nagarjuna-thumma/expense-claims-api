export type Role = 'employee' | 'manager' | 'auditor';

/**
 * Reads Keycloak realm roles from an access token. This only decides what the UI shows.
 * The API checks the signature and the roles again on every call, so editing the token in the
 * browser gets you a 403, not someone else's data.
 */
export function rolesFromToken(accessToken: string | null): Role[] {
  if (!accessToken) return [];
  const payload = accessToken.split('.')[1];
  if (!payload) return [];
  try {
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    const roles: unknown = JSON.parse(json)?.realm_access?.roles;
    return Array.isArray(roles) ? roles.filter((r): r is Role => ['employee', 'manager', 'auditor'].includes(r)) : [];
  } catch {
    return [];
  }
}
