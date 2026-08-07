type CatalogSummaryProps = {
  visibleProductCount: number;
  cartItemCount: number;
};

export function CatalogSummary({ visibleProductCount, cartItemCount }: CatalogSummaryProps) {
  return (
    <section className="summary-strip" aria-label="Catalog summary">
      <div>
        <span className="summary-label">Visible products</span>
        <strong>{visibleProductCount}</strong>
      </div>
      <div>
        <span className="summary-label">Items in cart</span>
        <strong>{cartItemCount}</strong>
      </div>
      <div>
        <span className="summary-label">Source</span>
        <strong>Catalog API</strong>
      </div>
    </section>
  );
}
