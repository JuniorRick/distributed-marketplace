import { ShoppingCart } from 'lucide-react';

type CatalogHeaderProps = {
  query: string;
  itemCount: number;
  cartUrl: string;
  onQueryChange: (query: string) => void;
  userName: string | null;
  onLogin: () => void;
  onRegister: () => void;
  onLogout: () => void;
};

export function CatalogHeader({
  query, itemCount, cartUrl, onQueryChange, userName, onLogin, onRegister, onLogout,
}: CatalogHeaderProps) {
  return (
    <section className="app-header">
      <div>
        <p className="eyebrow">Distributed Marketplace</p>
        <h1>Catalog</h1>
      </div>
      <div className="catalog-actions">
        {userName ? (
          <>
            <span>{userName}</span>
            <button className="auth-action" type="button" onClick={onLogout}>Sign out</button>
          </>
        ) : (
          <>
            <button className="auth-action" type="button" onClick={onLogin}>Sign in</button>
            <button className="auth-action primary" type="button" onClick={onRegister}>Create account</button>
          </>
        )}
        <label className="search-box">
          <span>Search</span>
          <input
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
            placeholder="Product, SKU, description"
          />
        </label>
        <a className="cart-link" href={cartUrl} aria-label={`Cart with ${itemCount} items`}>
          <ShoppingCart size={19} aria-hidden="true" />
          <span>Cart</span>
          <strong>{itemCount}</strong>
        </a>
      </div>
    </section>
  );
}
