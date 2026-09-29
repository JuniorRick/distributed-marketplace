import { currentUser, logout } from '@marketplace/auth';

const marketplaceUrl = import.meta.env.VITE_MARKETPLACE_URL ?? 'http://localhost:8080';
const catalogFrontendUrl = import.meta.env.VITE_CATALOG_FRONTEND_URL ?? `${marketplaceUrl}/catalog/`;

export function CartHeader() {
  return (
    <section className="app-header">
      <div>
        <p className="eyebrow">Distributed Marketplace</p>
        <h1>Cart</h1>
      </div>
      <div className="catalog-actions">
        <span>{currentUser()?.displayName}</span>
        <a className="catalog-link" href={catalogFrontendUrl}>Continue shopping</a>
        <button className="catalog-link" type="button" onClick={() => void logout()}>Sign out</button>
      </div>
    </section>
  );
}
