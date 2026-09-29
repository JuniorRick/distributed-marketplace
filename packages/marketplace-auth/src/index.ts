import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: import.meta.env.VITE_OIDC_URL ?? 'http://localhost:8090',
  realm: import.meta.env.VITE_OIDC_REALM ?? 'marketplace',
  clientId: import.meta.env.VITE_OIDC_CLIENT_ID ?? 'marketplace-spa',
});
const marketplaceUrl = import.meta.env.VITE_MARKETPLACE_URL ?? 'http://localhost:8080';

let initialized = false;

export type AuthenticatedUser = {
  subject: string;
  email: string;
  displayName: string;
};

export async function initializeAuth(loginRequired = false): Promise<void> {
  if (!initialized) {
    await keycloak.init({
      onLoad: loginRequired ? 'login-required' : 'check-sso',
      flow: 'standard',
      pkceMethod: 'S256',
      checkLoginIframe: true,
    });
    initialized = true;
  }
  if (keycloak.authenticated) {
    await provisionCustomer();
  }
}

export function currentUser(): AuthenticatedUser | null {
  if (!keycloak.authenticated || !keycloak.tokenParsed || !keycloak.subject) {
    return null;
  }
  const claims = keycloak.tokenParsed;
  const email = typeof claims.email === 'string' ? claims.email : '';
  const displayName = typeof claims.name === 'string' && claims.name ? claims.name : email;
  return { subject: keycloak.subject, email, displayName };
}

export async function login(): Promise<void> {
  await keycloak.login({ redirectUri: window.location.href });
}

export async function register(): Promise<void> {
  await keycloak.register({ redirectUri: window.location.href });
}

export async function logout(): Promise<void> {
  await keycloak.logout({ redirectUri: `${marketplaceUrl}/catalog/` });
}

export async function authenticatedFetch(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
  if (!keycloak.authenticated) {
    await login();
    throw new Error('Authentication is required');
  }
  await keycloak.updateToken(30);
  const headers = new Headers(init.headers);
  headers.set('Authorization', `Bearer ${keycloak.token}`);
  return fetch(input, { ...init, headers });
}

async function provisionCustomer(): Promise<void> {
  const response = await authenticatedFetch('/api/customers/me', { method: 'POST' });
  if (!response.ok) {
    throw new Error(`Customer profile provisioning failed with status ${response.status}`);
  }
}
