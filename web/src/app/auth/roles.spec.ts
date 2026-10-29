import { rolesFromToken } from './roles';

function token(payload: object): string {
  const b64 = btoa(JSON.stringify(payload)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `eyJhbGciOiJSUzI1NiJ9.${b64}.signature`;
}

describe('rolesFromToken', () => {
  it('reads Keycloak realm roles', () => {
    expect(rolesFromToken(token({ realm_access: { roles: ['employee', 'manager'] } }))).toEqual(['employee', 'manager']);
  });

  it('ignores roles the app does not know about', () => {
    expect(rolesFromToken(token({ realm_access: { roles: ['offline_access', 'auditor', 'uma_authorization'] } }))).toEqual(['auditor']);
  });

  it('returns nothing for a missing or broken token', () => {
    expect(rolesFromToken(null)).toEqual([]);
    expect(rolesFromToken('not-a-jwt')).toEqual([]);
    expect(rolesFromToken('a.%%%.c')).toEqual([]);
    expect(rolesFromToken(token({ sub: 'alice' }))).toEqual([]);
  });
});
