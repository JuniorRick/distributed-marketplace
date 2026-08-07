const catalogFrontendUrl = import.meta.env.VITE_CATALOG_FRONTEND_URL ?? 'http://localhost:5173';

export function OrderHeader() {
  return (
    <section className="app-header">
      <div>
        <p className="eyebrow">Distributed Marketplace</p>
        <h1>Order</h1>
      </div>
      <a className="catalog-link" href={catalogFrontendUrl}>Continue shopping</a>
    </section>
  );
}
