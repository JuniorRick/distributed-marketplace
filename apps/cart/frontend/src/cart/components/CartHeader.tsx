const catalogFrontendUrl = import.meta.env.VITE_CATALOG_FRONTEND_URL ?? 'http://localhost:5173';

export function CartHeader() {
  return (
    <section className="app-header">
      <div>
        <p className="eyebrow">Distributed Marketplace</p>
        <h1>Cart</h1>
      </div>
      <a className="catalog-link" href={catalogFrontendUrl}>Continue shopping</a>
    </section>
  );
}
