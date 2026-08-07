import { cartPageUrl } from '../../cart/api/cartApi';
import { useActiveCart } from '../../cart/hooks/useActiveCart';
import { CatalogHeader } from '../components/CatalogHeader';
import { CatalogSummary } from '../components/CatalogSummary';
import { ProductGrid } from '../components/ProductGrid';
import { useProducts } from '../hooks/useProducts';

export function CatalogPage() {
  const { filteredProducts, query, setQuery, isLoading, error } = useProducts();
  const { cart, itemCount, addingProductId, message, addToCart } = useActiveCart();

  return (
    <main className="app-shell">
      <CatalogHeader
        query={query}
        itemCount={itemCount}
        cartUrl={cartPageUrl(cart?.id)}
        onQueryChange={setQuery}
      />
      <CatalogSummary
        visibleProductCount={filteredProducts.length}
        cartItemCount={itemCount}
      />

      {isLoading && <p className="state-message">Loading catalog...</p>}
      {error && <p className="state-message error">{error}</p>}
      {message && (
        <p
          className={`state-message cart-message${message.error ? ' error' : ' success'}`}
          aria-live="polite"
        >
          {message.text}
        </p>
      )}

      {!isLoading && !error && (
        <ProductGrid
          products={filteredProducts}
          addingProductId={addingProductId}
          onAddToCart={addToCart}
        />
      )}
    </main>
  );
}
